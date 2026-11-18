package com.managesoundmode.app.domain.engine

import com.managesoundmode.app.domain.model.ActiveCalendarEventWindow
import com.managesoundmode.app.domain.model.CalendarEventInstance
import com.managesoundmode.app.domain.model.CalendarRule
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SavedStateSnapshot
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.domain.model.Weekday
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class ActiveRuleCandidate(
  val ruleId: String,
  val ruleName: String,
  val subtitle: String,
  val source: TransitionTriggerSource,
  val profile: SoundProfile,
  val highPriority: Boolean,
  val sessionKey: String,
)

sealed interface RuleEvaluationDecision {
  data class TimerActive(
    val timer: TimedModeSession,
    val remainingMinutes: Int,
  ) : RuleEvaluationDecision

  data class ApplyRule(
    val winner: ActiveRuleCandidate,
    val allActiveCandidates: List<ActiveRuleCandidate>,
  ) : RuleEvaluationDecision

  data class RuleOverriddenByUser(
    val winner: ActiveRuleCandidate,
    val allActiveCandidates: List<ActiveRuleCandidate>,
  ) : RuleEvaluationDecision

  data class NoActiveRules(
    val restorePolicy: RestorePolicy,
    val fallbackProfile: SoundProfile,
    val savedSnapshot: SavedStateSnapshot?,
  ) : RuleEvaluationDecision
}

object RuleEvaluator {

  fun evaluate(
    nowDateTime: LocalDateTime,
    nowEpochMillis: Long,
    profiles: List<SoundProfile>,
    scheduleRules: List<ScheduleRule>,
    locationRules: List<LocationRule>,
    calendarRules: List<CalendarRule> = emptyList(),
    activeCalendarWindows: List<ActiveCalendarEventWindow> = emptyList(),
    activeTimer: TimedModeSession?,
    overriddenSessionKey: String?,
    restorePolicy: RestorePolicy,
    defaultDayProfileId: String = SoundProfile.DEFAULT_DAY_PROFILE_ID,
    savedSnapshot: SavedStateSnapshot? = null,
  ): RuleEvaluationDecision {
    // 1. Timer manual temporário sempre tem prioridade máxima
    if (activeTimer != null && nowEpochMillis < activeTimer.endsAtEpochMillis) {
      return RuleEvaluationDecision.TimerActive(
        timer = activeTimer,
        remainingMinutes = activeTimer.remainingMinutes(nowEpochMillis),
      )
    }

    val profilesById = profiles.associateBy { it.id }
    val fallbackProfile = profilesById[defaultDayProfileId]
      ?: profiles.firstOrNull()
      ?: SoundProfile.defaultPresets.first()

    // 2. Coleta regras de horário, local e agenda ativas neste instante
    val activeCandidates = mutableListOf<ActiveRuleCandidate>()

    for (schedule in scheduleRules) {
      if (isScheduleRuleActiveAt(schedule, nowDateTime)) {
        val profile = profilesById[schedule.targetProfileId] ?: fallbackProfile
        val sessionDateTag = computeScheduleSessionDateTag(schedule, nowDateTime)
        activeCandidates += ActiveRuleCandidate(
          ruleId = schedule.id,
          ruleName = schedule.name,
          subtitle = "Horário ${schedule.formattedWindow()} • Até ${schedule.formattedEnd()}",
          source = TransitionTriggerSource.SCHEDULE,
          profile = profile,
          highPriority = schedule.highPriority,
          sessionKey = "schedule:${schedule.id}:$sessionDateTag",
        )
      }
    }

    for (location in locationRules) {
      if (location.enabled && location.isCurrentlyInside) {
        val profile = profilesById[location.targetProfileId] ?: fallbackProfile
        val extraInfo = if (location.wifiSsid.isNotBlank()) {
          "Local (${location.radiusMeters}m • Wi-Fi ${location.wifiSsid})"
        } else {
          "Local (Raio de ${location.radiusMeters}m)"
        }
        activeCandidates += ActiveRuleCandidate(
          ruleId = location.id,
          ruleName = location.name,
          subtitle = extraInfo,
          source = TransitionTriggerSource.LOCATION,
          profile = profile,
          highPriority = location.highPriority,
          sessionKey = "location:${location.id}",
        )
      }
    }

    val calendarRulesById = calendarRules.associateBy { it.id }
    for (window in activeCalendarWindows) {
      val rule = calendarRulesById[window.ruleId]
      if (rule != null && rule.enabled) {
        val profile = profilesById[rule.targetProfileId] ?: fallbackProfile
        activeCandidates += ActiveRuleCandidate(
          ruleId = rule.id,
          ruleName = rule.name,
          subtitle = "Agenda • ${window.eventTitle}",
          source = TransitionTriggerSource.CALENDAR,
          profile = profile,
          highPriority = rule.highPriority,
          sessionKey = "calendar:${rule.id}:${window.eventId}:${window.beginMillis}",
        )
      }
    }

    // 3. Se houver regras ativas, desempata conforme a Matriz de Precedência (SPEC-004 e SPEC-008)
    if (activeCandidates.isNotEmpty()) {
      val sorted = activeCandidates.sortedWith(
        compareByDescending<ActiveRuleCandidate> { it.highPriority }
          .thenByDescending { it.profile.resolvedSoundState.restrictivenessRank }
          .thenBy { candidate ->
            when (candidate.source) {
              TransitionTriggerSource.CALENDAR -> 0
              TransitionTriggerSource.SCHEDULE -> 1
              TransitionTriggerSource.LOCATION -> 2
              else -> 3
            }
          }
          .thenBy { it.ruleId },
      )
      val winner = sorted.first()

      // 4. Verifica se o usuário fez override manual durante esta mesma sessão da regra
      return if (overriddenSessionKey != null && overriddenSessionKey == winner.sessionKey) {
        RuleEvaluationDecision.RuleOverriddenByUser(
          winner = winner,
          allActiveCandidates = sorted,
        )
      } else {
        RuleEvaluationDecision.ApplyRule(
          winner = winner,
          allActiveCandidates = sorted,
        )
      }
    }

    // 5. Nenhuma regra ativa -> Aplica política de restauração
    return RuleEvaluationDecision.NoActiveRules(
      restorePolicy = restorePolicy,
      fallbackProfile = fallbackProfile,
      savedSnapshot = savedSnapshot,
    )
  }

  /**
   * Avalia se uma [ScheduleRule] está ativa em [nowDateTime], suportando janelas no mesmo dia
   * (ex.: 08:30 às 18:00) e janelas que cruzam a meia-noite (ex.: 22:30 às 07:00).
   */
  fun isScheduleRuleActiveAt(rule: ScheduleRule, nowDateTime: LocalDateTime): Boolean {
    if (!rule.enabled || rule.daysOfWeek.isEmpty()) return false

    val nowMinutes = nowDateTime.hour * 60 + nowDateTime.minute
    val startMinutes = rule.startMinutesOfDay
    val endMinutes = rule.endMinutesOfDay
    val todayWeekday = Weekday.fromDayOfWeek(nowDateTime.dayOfWeek)

    return if (!rule.crossesMidnight) {
      if (startMinutes == endMinutes) {
        todayWeekday in rule.daysOfWeek
      } else {
        todayWeekday in rule.daysOfWeek && nowMinutes in startMinutes until endMinutes
      }
    } else {
      // Janela cruza a meia-noite (ex.: 22:30 -> 07:00)
      if (nowMinutes >= startMinutes) {
        // Parte noturna antes da meia-noite (22:30..23:59): verifica o dia de hoje
        todayWeekday in rule.daysOfWeek
      } else if (nowMinutes < endMinutes) {
        // Madrugada após a meia-noite (00:00..06:59): verifica se ONTEM estava habilitado
        val yesterdayWeekday = Weekday.fromDayOfWeek(nowDateTime.minusDays(1).dayOfWeek)
        yesterdayWeekday in rule.daysOfWeek
      } else {
        false
      }
    }
  }

  private fun computeScheduleSessionDateTag(rule: ScheduleRule, nowDateTime: LocalDateTime): String {
    val nowMinutes = nowDateTime.hour * 60 + nowDateTime.minute
    val sessionStartDate = if (rule.crossesMidnight && nowMinutes < rule.endMinutesOfDay) {
      nowDateTime.toLocalDate().minusDays(1)
    } else {
      nowDateTime.toLocalDate()
    }
    return "${sessionStartDate}:${rule.formattedStart()}"
  }

  /**
   * Calcula o próximo instante (epoch millis) em que qualquer [ScheduleRule] habilitada
   * iniciará ou terminará, para agendamento exato via `AlarmManager`.
   */
  fun computeNextScheduleTransitionMillis(
    scheduleRules: List<ScheduleRule>,
    now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault()),
  ): Long? {
    val enabledRules = scheduleRules.filter { it.enabled && it.daysOfWeek.isNotEmpty() }
    if (enabledRules.isEmpty()) return null

    val nowMillis = now.toInstant().toEpochMilli()
    var earliestCandidateMillis: Long? = null

    for (dayOffset in 0L..7L) {
      val date = now.toLocalDate().plusDays(dayOffset)
      val weekday = Weekday.fromDayOfWeek(date.dayOfWeek)

      for (rule in enabledRules) {
        if (weekday in rule.daysOfWeek) {
          val startZdt = ZonedDateTime.of(
            date,
            LocalTime.of(rule.startHour, rule.startMinute, 0),
            now.zone,
          )
          val startMillis = startZdt.toInstant().toEpochMilli()
          if (startMillis > nowMillis) {
            if (earliestCandidateMillis == null || startMillis < earliestCandidateMillis) {
              earliestCandidateMillis = startMillis
            }
          }

          val endDate = if (rule.crossesMidnight) date.plusDays(1) else date
          val endZdt = ZonedDateTime.of(
            endDate,
            LocalTime.of(rule.endHour, rule.endMinute, 0),
            now.zone,
          )
          val endMillis = endZdt.toInstant().toEpochMilli()
          if (endMillis > nowMillis) {
            if (earliestCandidateMillis == null || endMillis < earliestCandidateMillis) {
              earliestCandidateMillis = endMillis
            }
          }
        }
      }
    }

    return earliestCandidateMillis
  }

  /**
   * Avalia se uma [CalendarEventInstance] atende aos critérios e filtros de uma [CalendarRule]
   * no instante [nowEpochMillis].
   */
  fun matchesCalendarEvent(
    rule: CalendarRule,
    instance: CalendarEventInstance,
    nowEpochMillis: Long,
  ): Boolean {
    if (!rule.enabled || instance.isCancelledOrDeclined) return false
    if (instance.endMillis <= instance.beginMillis) return false
    if (nowEpochMillis !in instance.beginMillis until instance.endMillis) return false
    if (rule.calendarIds.isNotEmpty() && instance.calendarId !in rule.calendarIds) return false
    if (rule.ignoreAllDayEvents && instance.allDay) return false
    if (rule.busyOnly && instance.availability != CalendarEventInstance.AVAILABILITY_BUSY) return false
    if (rule.keywordFilter.isNotBlank()) {
      val tokens = rule.keywordFilter.split(",").map { it.trim() }.filter { it.isNotEmpty() }
      if (tokens.isNotEmpty()) {
        val matchesToken = tokens.any { token ->
          instance.title.contains(token, ignoreCase = true) ||
            instance.description.contains(token, ignoreCase = true)
        }
        if (!matchesToken) return false
      }
    }
    return true
  }

  /**
   * Resolve todas as janelas ativas de compromissos de calendário que atendem às regras cadastradas.
   */
  fun resolveCalendarWindows(
    rules: List<CalendarRule>,
    instances: List<CalendarEventInstance>,
    nowEpochMillis: Long,
  ): List<ActiveCalendarEventWindow> {
    val enabledRules = rules.filter { it.enabled }
    if (enabledRules.isEmpty() || instances.isEmpty()) return emptyList()

    val windows = mutableListOf<ActiveCalendarEventWindow>()
    for (rule in enabledRules) {
      for (instance in instances) {
        if (matchesCalendarEvent(rule, instance, nowEpochMillis)) {
          windows += ActiveCalendarEventWindow(
            ruleId = rule.id,
            eventId = instance.eventId,
            eventTitle = instance.title,
            beginMillis = instance.beginMillis,
            endMillis = instance.endMillis,
          )
        }
      }
    }
    return windows
  }

  /**
   * Calcula o próximo limite (beginMillis ou endMillis) de qualquer instância de evento de calendário
   * elegível que ocorrerá após [nowEpochMillis].
   */
  fun computeNextCalendarBoundaryMillis(
    calendarRules: List<CalendarRule>,
    instances: List<CalendarEventInstance>,
    nowEpochMillis: Long,
  ): Long? {
    val enabledRules = calendarRules.filter { it.enabled }
    if (enabledRules.isEmpty() || instances.isEmpty()) return null

    var earliestMillis: Long? = null
    for (rule in enabledRules) {
      for (instance in instances) {
        if (instance.isCancelledOrDeclined) continue
        if (instance.endMillis <= instance.beginMillis) continue
        if (rule.calendarIds.isNotEmpty() && instance.calendarId !in rule.calendarIds) continue
        if (rule.ignoreAllDayEvents && instance.allDay) continue
        if (rule.busyOnly && instance.availability != CalendarEventInstance.AVAILABILITY_BUSY) continue
        if (rule.keywordFilter.isNotBlank()) {
          val tokens = rule.keywordFilter.split(",").map { it.trim() }.filter { it.isNotEmpty() }
          if (tokens.isNotEmpty()) {
            val matchesToken = tokens.any { token ->
              instance.title.contains(token, ignoreCase = true) ||
                instance.description.contains(token, ignoreCase = true)
            }
            if (!matchesToken) continue
          }
        }
        if (instance.beginMillis > nowEpochMillis) {
          earliestMillis = minOf(earliestMillis ?: Long.MAX_VALUE, instance.beginMillis)
        }
        if (instance.endMillis > nowEpochMillis) {
          earliestMillis = minOf(earliestMillis ?: Long.MAX_VALUE, instance.endMillis)
        }
      }
    }
    return earliestMillis
  }

  /**
   * Calcula o próximo marco de transição unificado, considerando o menor timestamp futuro entre
   * as regras de horário fixo e os marcos de início/fim de eventos de calendário.
   */
  fun computeNextAutomationTransitionMillis(
    scheduleRules: List<ScheduleRule>,
    nextCalendarBoundaryMillis: Long? = null,
    now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault()),
  ): Long? {
    val nextScheduleMillis = computeNextScheduleTransitionMillis(scheduleRules, now)
    return when {
      nextScheduleMillis != null && nextCalendarBoundaryMillis != null ->
        minOf(nextScheduleMillis, nextCalendarBoundaryMillis)
      nextScheduleMillis != null -> nextScheduleMillis
      nextCalendarBoundaryMillis != null -> nextCalendarBoundaryMillis
      else -> null
    }
  }
}

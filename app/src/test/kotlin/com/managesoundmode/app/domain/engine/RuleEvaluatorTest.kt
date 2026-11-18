package com.managesoundmode.app.domain.engine

import com.managesoundmode.app.domain.model.AccessControlConfig
import com.managesoundmode.app.domain.model.ActiveCalendarEventWindow
import com.managesoundmode.app.domain.model.BaseAudioMode
import com.managesoundmode.app.domain.model.CalendarEventInstance
import com.managesoundmode.app.domain.model.CalendarRule
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SavedStateSnapshot
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.SoundState
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.TransitionLogEntry
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.domain.model.Weekday
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEvaluatorTest {

  private val profiles = SoundProfile.defaultPresets

  @Test
  fun isScheduleRuleActiveAt_handlesMidnightCrossing_eveningAndEarlyMorning() {
    // Regra Dormir: 22:30 às 07:00 (Dom a Qui)
    val sleepRule = ScheduleRule(
      id = "sleep",
      name = "Dormir",
      startHour = 22,
      startMinute = 30,
      endHour = 7,
      endMinute = 0,
      daysOfWeek = Weekday.sleepWeeknights, // SUN, MON, TUE, WED, THU
      targetProfileId = "preset_sleep",
    )

    // Domingo 2026-11-01 às 23:15 -> Ativo (iniciou no Domingo)
    val sundayNight = LocalDateTime.of(2026, 11, 1, 23, 15)
    assertTrue(RuleEvaluator.isScheduleRuleActiveAt(sleepRule, sundayNight))

    // Segunda-feira 2026-11-02 às 06:30 -> Ativo (madrugada da noite de Domingo)
    val mondayEarlyMorning = LocalDateTime.of(2026, 11, 2, 6, 30)
    assertTrue(RuleEvaluator.isScheduleRuleActiveAt(sleepRule, mondayEarlyMorning))

    // Segunda-feira 2026-11-02 às 07:00 -> Inativo (fim exato da janela)
    val mondaySevenAm = LocalDateTime.of(2026, 11, 2, 7, 0)
    assertFalse(RuleEvaluator.isScheduleRuleActiveAt(sleepRule, mondaySevenAm))

    // Sexta-feira 2026-11-06 às 23:15 -> Inativo (Sexta não está em sleepWeeknights)
    val fridayNight = LocalDateTime.of(2026, 11, 6, 23, 15)
    assertFalse(RuleEvaluator.isScheduleRuleActiveAt(sleepRule, fridayNight))

    // Sábado 2026-11-07 às 02:00 -> Inativo (noite anterior foi Sexta)
    val saturdayEarlyMorning = LocalDateTime.of(2026, 11, 7, 2, 0)
    assertFalse(RuleEvaluator.isScheduleRuleActiveAt(sleepRule, saturdayEarlyMorning))
  }

  @Test
  fun evaluate_conflictBetweenScheduleAndLocation_mostRestrictiveWinsByDefault() {
    // Segunda-feira 10:00: Horário Comercial (Normal/Alto) vs. Local Faculdade (Vibrar)
    val workSchedule = ScheduleRule(
      id = "work",
      name = "Trabalho Alto",
      startHour = 8,
      startMinute = 0,
      endHour = 18,
      endMinute = 0,
      daysOfWeek = Weekday.weekdays,
      targetProfileId = "preset_work_high", // NORMAL
      enabled = true,
      highPriority = false,
    )
    val silentLocation = LocationRule(
      id = "clinic",
      name = "Clínica Médica",
      latitude = 0.0,
      longitude = 0.0,
      targetProfileId = "preset_work", // VIBRATE
      enabled = true,
      isCurrentlyInside = true,
      highPriority = false,
    )

    val mondayTenAm = LocalDateTime.of(2026, 11, 2, 10, 0)
    val decision = RuleEvaluator.evaluate(
      nowDateTime = mondayTenAm,
      nowEpochMillis = 1_000_000L,
      profiles = profiles,
      scheduleRules = listOf(workSchedule),
      locationRules = listOf(silentLocation),
      activeTimer = null,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    )

    assertTrue(decision is RuleEvaluationDecision.ApplyRule)
    val apply = decision as RuleEvaluationDecision.ApplyRule
    assertEquals("clinic", apply.winner.ruleId)
    assertEquals(SoundMode.VIBRATE, apply.winner.profile.mode)
  }

  @Test
  fun evaluate_highPriorityRule_overridesMoreRestrictiveRule() {
    // Horário de Plantão (NORMAL com highPriority = true) vence Local Silencioso (VIBRATE)
    val onCallSchedule = ScheduleRule(
      id = "on_call",
      name = "Plantão Médico",
      startHour = 8,
      startMinute = 0,
      endHour = 18,
      endMinute = 0,
      daysOfWeek = Weekday.weekdays,
      targetProfileId = "preset_work_high", // NORMAL 100%
      enabled = true,
      highPriority = true,
    )
    val vibrateLocation = LocationRule(
      id = "cinema",
      name = "Shopping",
      latitude = 0.0,
      longitude = 0.0,
      targetProfileId = "preset_work", // VIBRATE
      enabled = true,
      isCurrentlyInside = true,
      highPriority = false,
    )

    val mondayTenAm = LocalDateTime.of(2026, 11, 2, 10, 0)
    val decision = RuleEvaluator.evaluate(
      nowDateTime = mondayTenAm,
      nowEpochMillis = 1_000_000L,
      profiles = profiles,
      scheduleRules = listOf(onCallSchedule),
      locationRules = listOf(vibrateLocation),
      activeTimer = null,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    )

    assertTrue(decision is RuleEvaluationDecision.ApplyRule)
    val apply = decision as RuleEvaluationDecision.ApplyRule
    assertEquals("on_call", apply.winner.ruleId)
    assertEquals(SoundMode.NORMAL, apply.winner.profile.mode)
  }

  @Test
  fun evaluate_whenTimerIsActive_timerBeatsAllRules() {
    val sleepRule = ScheduleRule.defaultSchedules.first()
    val timer = TimedModeSession(
      targetMode = SoundMode.VIBRATE,
      previousMode = SoundMode.NORMAL,
      previousStreams = VolumeStreams(),
      durationMinutes = 30,
      startedAtEpochMillis = 1_000L,
      endsAtEpochMillis = 1_801_000L,
    )

    val sundayNight = LocalDateTime.of(2026, 11, 1, 23, 0)
    val decision = RuleEvaluator.evaluate(
      nowDateTime = sundayNight,
      nowEpochMillis = 600_000L,
      profiles = profiles,
      scheduleRules = listOf(sleepRule),
      locationRules = emptyList(),
      activeTimer = timer,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    )

    assertTrue(decision is RuleEvaluationDecision.TimerActive)
  }

  @Test
  fun evaluate_whenSessionKeyMatchesManualOverride_returnsRuleOverriddenByUser() {
    val workSchedule = ScheduleRule(
      id = "sched_work",
      name = "Trabalho",
      startHour = 8,
      startMinute = 30,
      endHour = 18,
      endMinute = 0,
      daysOfWeek = Weekday.weekdays,
      targetProfileId = "preset_work_high",
      enabled = true,
    )
    val mondayTenAm = LocalDateTime.of(2026, 11, 2, 10, 0)

    val firstEval = RuleEvaluator.evaluate(
      nowDateTime = mondayTenAm,
      nowEpochMillis = 1_000L,
      profiles = profiles,
      scheduleRules = listOf(workSchedule),
      locationRules = emptyList(),
      activeTimer = null,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    ) as RuleEvaluationDecision.ApplyRule

    val overriddenEval = RuleEvaluator.evaluate(
      nowDateTime = mondayTenAm,
      nowEpochMillis = 2_000L,
      profiles = profiles,
      scheduleRules = listOf(workSchedule),
      locationRules = emptyList(),
      activeTimer = null,
      overriddenSessionKey = firstEval.winner.sessionKey,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    )

    assertTrue(overriddenEval is RuleEvaluationDecision.RuleOverriddenByUser)
  }

  @Test
  fun computeNextScheduleTransitionMillis_returnsUpcomingBoundary() {
    val now = ZonedDateTime.of(2026, 11, 2, 8, 0, 0, 0, ZoneId.of("America/Sao_Paulo"))
    val nextMillis = RuleEvaluator.computeNextScheduleTransitionMillis(
      scheduleRules = ScheduleRule.defaultSchedules,
      now = now,
    )
    assertNotNull(nextMillis)
    val expectedEightThirty = ZonedDateTime.of(2026, 11, 2, 8, 30, 0, 0, ZoneId.of("America/Sao_Paulo"))
      .toInstant()
      .toEpochMilli()
    assertEquals(expectedEightThirty, nextMillis)
  }

  @Test
  fun accessControlConfig_verifyPin_supportsSha256AndConstantTimeComparison() {
    val hashedPin = AccessControlConfig.hashPin("7391")
    assertTrue(hashedPin.startsWith("sha256:"))
    val configHashed = AccessControlConfig(pinCode = hashedPin)
    assertTrue(configHashed.verifyPin("7391"))
    assertTrue(!configHashed.verifyPin("0000"))

    val configPlain = AccessControlConfig(pinCode = "4210")
    assertTrue(configPlain.verifyPin("4210"))
    assertTrue(!configPlain.verifyPin("9999"))
  }

  @Test
  fun soundState_restrictivenessRankAndLegacyDeserialization_coverAllFour2x2Cells() {
    val soundVib = SoundState(audioMode = BaseAudioMode.SOUND, vibrateEnabled = true)
    val soundNoVib = SoundState(audioMode = BaseAudioMode.SOUND, vibrateEnabled = false)
    val silentVib = SoundState(audioMode = BaseAudioMode.SILENT, vibrateEnabled = true)
    val silentNoVib = SoundState(audioMode = BaseAudioMode.SILENT, vibrateEnabled = false)

    assertEquals(1, soundVib.restrictivenessRank)
    assertEquals(2, soundNoVib.restrictivenessRank)
    assertEquals(3, silentVib.restrictivenessRank)
    assertEquals(4, silentNoVib.restrictivenessRank)

    assertEquals(soundVib, SoundState.fromLegacyMode(SoundMode.NORMAL))
    assertEquals(silentVib, SoundState.fromLegacyMode(SoundMode.VIBRATE))
    assertEquals(silentNoVib, SoundState.fromLegacyMode(SoundMode.SILENT))

    val json = Json { ignoreUnknownKeys = true }
    val legacyProfileJson = """
      {"id":"v1_vib","name":"V1 Vibrar","description":"Legado","mode":"VIBRATE","streams":{"ringPercent":0,"notificationPercent":0,"mediaPercent":0,"alarmPercent":80},"isPreset":false}
    """.trimIndent()
    val decodedProfile = json.decodeFromString<SoundProfile>(legacyProfileJson)
    assertEquals(silentVib, decodedProfile.resolvedSoundState)
    assertEquals(BaseAudioMode.SILENT, decodedProfile.normalized().audioMode)
    assertEquals(true, decodedProfile.normalized().vibrateEnabled)

    val legacySnapshotJson = """
      {"mode":"SILENT","streams":{"ringPercent":0,"notificationPercent":0,"mediaPercent":0,"alarmPercent":90},"capturedAtEpochMillis":12345}
    """.trimIndent()
    val decodedSnapshot = json.decodeFromString<SavedStateSnapshot>(legacySnapshotJson)
    assertEquals(silentNoVib, decodedSnapshot.resolvedSoundState)

    val legacyTimerJson = """
      {"targetMode":"VIBRATE","previousMode":"NORMAL","previousStreams":{"ringPercent":80,"notificationPercent":75,"mediaPercent":50,"alarmPercent":90},"durationMinutes":30,"startedAtEpochMillis":1000,"endsAtEpochMillis":61000}
    """.trimIndent()
    val decodedTimer = json.decodeFromString<TimedModeSession>(legacyTimerJson)
    assertEquals(silentVib, decodedTimer.resolvedSoundState)
    assertEquals(soundVib, decodedTimer.resolvedPreviousSoundState)

    val legacyLogJson = """
      {"id":"log1","timestampEpochMillis":1000,"title":"Teste","detail":"Detalhe","source":"MANUAL","appliedMode":"NORMAL"}
    """.trimIndent()
    val decodedLog = json.decodeFromString<TransitionLogEntry>(legacyLogJson)
    assertEquals(soundVib, decodedLog.resolvedSoundState)
  }

  @Test
  fun evaluate_conflictAcrossAllFourMatrixStates_enforcesStrict2x2Precedence() {
    val pSoundVib = SoundProfile(
      id = "p_sound_vib",
      name = "Som + Vibrar",
      description = "",
      audioMode = BaseAudioMode.SOUND,
      vibrateEnabled = true,
      streams = VolumeStreams(),
      isPreset = false,
    )
    val pSoundNoVib = SoundProfile(
      id = "p_sound_novib",
      name = "Som Sem Vibrar",
      description = "",
      audioMode = BaseAudioMode.SOUND,
      vibrateEnabled = false,
      streams = VolumeStreams(),
      isPreset = false,
    )
    val pSilentVib = SoundProfile(
      id = "p_silent_vib",
      name = "Silêncio + Vibrar",
      description = "",
      audioMode = BaseAudioMode.SILENT,
      vibrateEnabled = true,
      streams = VolumeStreams(),
      isPreset = false,
    )
    val pSilentNoVib = SoundProfile(
      id = "p_silent_novib",
      name = "Silêncio Total",
      description = "",
      audioMode = BaseAudioMode.SILENT,
      vibrateEnabled = false,
      streams = VolumeStreams(),
      isPreset = false,
    )
    val matrixProfiles = listOf(pSoundVib, pSoundNoVib, pSilentVib, pSilentNoVib)
    val mondayTenAm = LocalDateTime.of(2026, 11, 2, 10, 0)

    // 1. SOUND+Vib (rank 1) vs SOUND+NoVib (rank 2) -> SOUND+NoVib vence
    val rSoundVib = ScheduleRule(
      id = "r1",
      name = "R1",
      startHour = 8,
      startMinute = 0,
      endHour = 18,
      endMinute = 0,
      daysOfWeek = Weekday.weekdays,
      targetProfileId = pSoundVib.id,
    )
    val rSoundNoVib = ScheduleRule(
      id = "r2",
      name = "R2",
      startHour = 8,
      startMinute = 0,
      endHour = 18,
      endMinute = 0,
      daysOfWeek = Weekday.weekdays,
      targetProfileId = pSoundNoVib.id,
    )
    val d12 = RuleEvaluator.evaluate(
      nowDateTime = mondayTenAm,
      nowEpochMillis = 10_000L,
      profiles = matrixProfiles,
      scheduleRules = listOf(rSoundVib, rSoundNoVib),
      locationRules = emptyList(),
      activeTimer = null,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    ) as RuleEvaluationDecision.ApplyRule
    assertEquals("r2", d12.winner.ruleId)
    assertEquals(SoundState(BaseAudioMode.SOUND, false), d12.winner.profile.resolvedSoundState)

    // 2. SOUND+NoVib (rank 2) vs SILENT+Vib (rank 3) -> SILENT+Vib vence
    val locSilentVib = LocationRule(
      id = "l3",
      name = "L3",
      latitude = 0.0,
      longitude = 0.0,
      targetProfileId = pSilentVib.id,
      enabled = true,
      isCurrentlyInside = true,
    )
    val d23 = RuleEvaluator.evaluate(
      nowDateTime = mondayTenAm,
      nowEpochMillis = 10_000L,
      profiles = matrixProfiles,
      scheduleRules = listOf(rSoundNoVib),
      locationRules = listOf(locSilentVib),
      activeTimer = null,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    ) as RuleEvaluationDecision.ApplyRule
    assertEquals("l3", d23.winner.ruleId)

    // 3. SILENT+Vib (rank 3) vs SILENT+NoVib (rank 4) -> SILENT+NoVib vence
    val locSilentNoVib = LocationRule(
      id = "l4",
      name = "L4",
      latitude = 0.0,
      longitude = 0.0,
      targetProfileId = pSilentNoVib.id,
      enabled = true,
      isCurrentlyInside = true,
    )
    val d34 = RuleEvaluator.evaluate(
      nowDateTime = mondayTenAm,
      nowEpochMillis = 10_000L,
      profiles = matrixProfiles,
      scheduleRules = listOf(rSoundVib, rSoundNoVib),
      locationRules = listOf(locSilentVib, locSilentNoVib),
      activeTimer = null,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    ) as RuleEvaluationDecision.ApplyRule
    assertEquals("l4", d34.winner.ruleId)
    assertEquals(SoundState(BaseAudioMode.SILENT, false), d34.winner.profile.resolvedSoundState)
  }

  @Test
  fun matchesCalendarEvent_verifiesBusyOnly_keywords_calendarIds_and_allDay() {
    val rule = CalendarRule(
      id = "cal_rule_1",
      name = "Trabalho",
      calendarIds = setOf(1L, 2L),
      busyOnly = true,
      ignoreAllDayEvents = true,
      keywordFilter = "Reunião, Daily, Sprint",
      targetProfileId = "preset_silent",
    )

    val now = 100_000L
    // Evento perfeitamente coincidente (ocupado, calendário 1, palavra 'Daily', não all-day, dentro do horário)
    val validInstance = CalendarEventInstance(
      eventId = 10L,
      calendarId = 1L,
      title = "Daily de Engenharia",
      beginMillis = 90_000L,
      endMillis = 110_000L,
      allDay = false,
      availability = CalendarEventInstance.AVAILABILITY_BUSY,
    )
    assertTrue(RuleEvaluator.matchesCalendarEvent(rule, validInstance, now))

    // 1. Falha por não ser "Ocupado" (AVAILABILITY_FREE)
    val freeInstance = validInstance.copy(availability = CalendarEventInstance.AVAILABILITY_FREE)
    assertFalse(RuleEvaluator.matchesCalendarEvent(rule, freeInstance, now))

    // 2. Falha por calendário diferente (id 3L)
    val otherCalInstance = validInstance.copy(calendarId = 3L)
    assertFalse(RuleEvaluator.matchesCalendarEvent(rule, otherCalInstance, now))

    // 3. Falha por palavra-chave não correspondente
    val differentTitleInstance = validInstance.copy(title = "Almoço com amigos")
    assertFalse(RuleEvaluator.matchesCalendarEvent(rule, differentTitleInstance, now))

    // 4. Falha por ser evento de dia inteiro (quando ignoreAllDayEvents = true)
    val allDayInstance = validInstance.copy(allDay = true)
    assertFalse(RuleEvaluator.matchesCalendarEvent(rule, allDayInstance, now))

    // 5. Se ignoreAllDayEvents = false, evento all-day deve passar
    val ruleAllowsAllDay = rule.copy(ignoreAllDayEvents = false)
    assertTrue(RuleEvaluator.matchesCalendarEvent(ruleAllowsAllDay, allDayInstance, now))

    // 6. Fora da janela de tempo (evento já finalizou)
    val pastInstance = validInstance.copy(beginMillis = 10_000L, endMillis = 50_000L)
    assertFalse(RuleEvaluator.matchesCalendarEvent(rule, pastInstance, now))

    // 7. Fora da janela de tempo (evento futuro)
    val futureInstance = validInstance.copy(beginMillis = 150_000L, endMillis = 200_000L)
    assertFalse(RuleEvaluator.matchesCalendarEvent(rule, futureInstance, now))
  }

  @Test
  fun resolveCalendarWindows_extractsActiveWindowsForMatchingEvents() {
    val rule = CalendarRule(
      id = "cal_1",
      name = "Daily",
      busyOnly = true,
      targetProfileId = "preset_vibrate",
    )
    val instance = CalendarEventInstance(
      eventId = 42L,
      calendarId = 1L,
      title = "Daily Standup",
      beginMillis = 1_000L,
      endMillis = 5_000L,
      allDay = false,
      availability = CalendarEventInstance.AVAILABILITY_BUSY,
    )

    val windows = RuleEvaluator.resolveCalendarWindows(
      rules = listOf(rule),
      instances = listOf(instance),
      nowEpochMillis = 3_000L,
    )
    assertEquals(1, windows.size)
    assertEquals("cal_1", windows[0].ruleId)
    assertEquals(42L, windows[0].eventId)
    assertEquals(1_000L, windows[0].beginMillis)
    assertEquals(5_000L, windows[0].endMillis)
  }

  @Test
  fun evaluate_tieBreaker_calendarBeatsScheduleAndLocationWithSameRank() {
    // Ambos com perfil Silencioso sem vibração (rank 4)
    val silentProfile = SoundProfile(
      id = "p_silent_novib",
      name = "Silencioso",
      description = "Mudo absoluto",
      audioMode = BaseAudioMode.SILENT,
      vibrateEnabled = false,
      streams = VolumeStreams(),
      isPreset = false,
    )
    val allProfiles = listOf(silentProfile)

    val scheduleRule = ScheduleRule(
      id = "sched_1",
      name = "Horário",
      startHour = 9,
      startMinute = 0,
      endHour = 17,
      endMinute = 0,
      daysOfWeek = Weekday.weekdays,
      targetProfileId = silentProfile.id,
    )
    val locationRule = LocationRule(
      id = "loc_1",
      name = "Local",
      latitude = 0.0,
      longitude = 0.0,
      targetProfileId = silentProfile.id,
      isCurrentlyInside = true,
    )
    val calendarRule = CalendarRule(
      id = "cal_1",
      name = "Reunião",
      targetProfileId = silentProfile.id,
    )
    val calendarWindow = ActiveCalendarEventWindow(
      ruleId = calendarRule.id,
      eventId = 88L,
      eventTitle = "Reunião Diretoria",
      beginMillis = 10_000L,
      endMillis = 20_000L,
    )

    val nowDateTime = LocalDateTime.of(2026, 11, 2, 10, 0)
    val decision = RuleEvaluator.evaluate(
      nowDateTime = nowDateTime,
      nowEpochMillis = 15_000L,
      profiles = allProfiles,
      scheduleRules = listOf(scheduleRule),
      locationRules = listOf(locationRule),
      calendarRules = listOf(calendarRule),
      activeCalendarWindows = listOf(calendarWindow),
      activeTimer = null,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    ) as RuleEvaluationDecision.ApplyRule

    // Desempate: CALENDAR (0) > SCHEDULE (1) > LOCATION (2)
    assertEquals("cal_1", decision.winner.ruleId)
    assertEquals(TransitionTriggerSource.CALENDAR, decision.winner.source)
    assertEquals("calendar:cal_1:88:10000", decision.winner.sessionKey)
  }

  @Test
  fun evaluate_calendarHighPriority_beatsOtherRules() {
    val normalProfile = SoundProfile(
      id = "p_normal",
      name = "Normal",
      description = "Som",
      audioMode = BaseAudioMode.SOUND,
      vibrateEnabled = true,
      streams = VolumeStreams(),
      isPreset = false,
    )
    val silentProfile = SoundProfile(
      id = "p_silent",
      name = "Mudo",
      description = "Mudo",
      audioMode = BaseAudioMode.SILENT,
      vibrateEnabled = false,
      streams = VolumeStreams(),
      isPreset = false,
    )

    val calendarRule = CalendarRule(
      id = "cal_urgent",
      name = "Palestra Alta Prioridade",
      targetProfileId = silentProfile.id,
      highPriority = true,
    )
    val calendarWindow = ActiveCalendarEventWindow(
      ruleId = calendarRule.id,
      eventId = 99L,
      eventTitle = "Palestra",
      beginMillis = 1_000L,
      endMillis = 5_000L,
    )

    val scheduleRule = ScheduleRule(
      id = "sched_normal",
      name = "Trabalho Normal",
      startHour = 9,
      startMinute = 0,
      endHour = 18,
      endMinute = 0,
      daysOfWeek = Weekday.weekdays,
      targetProfileId = normalProfile.id,
      highPriority = false,
    )

    val decision = RuleEvaluator.evaluate(
      nowDateTime = LocalDateTime.of(2026, 11, 2, 10, 0),
      nowEpochMillis = 2_000L,
      profiles = listOf(normalProfile, silentProfile),
      scheduleRules = listOf(scheduleRule),
      locationRules = emptyList(),
      calendarRules = listOf(calendarRule),
      activeCalendarWindows = listOf(calendarWindow),
      activeTimer = null,
      overriddenSessionKey = null,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    ) as RuleEvaluationDecision.ApplyRule

    assertEquals("cal_urgent", decision.winner.ruleId)
    assertEquals(TransitionTriggerSource.CALENDAR, decision.winner.source)
  }

  @Test
  fun evaluate_calendarEventManualOverride_suppressesSpecificWindow() {
    val silentProfile = SoundProfile(
      id = "p_silent",
      name = "Mudo",
      description = "Mudo",
      audioMode = BaseAudioMode.SILENT,
      vibrateEnabled = false,
      streams = VolumeStreams(),
      isPreset = false,
    )
    val defaultProfile = SoundProfile(
      id = "p_default",
      name = "Padrão",
      description = "Padrão",
      audioMode = BaseAudioMode.SOUND,
      vibrateEnabled = true,
      streams = VolumeStreams(),
      isPreset = false,
    )

    val calendarRule = CalendarRule(
      id = "cal_1",
      name = "Reunião",
      targetProfileId = silentProfile.id,
    )
    val calendarWindow = ActiveCalendarEventWindow(
      ruleId = calendarRule.id,
      eventId = 77L,
      eventTitle = "Reunião",
      beginMillis = 10_000L,
      endMillis = 20_000L,
    )

    // Override manual do usuário exatamente nesta sessão do evento
    val sessionKey = "calendar:cal_1:77:10000"

    val decision = RuleEvaluator.evaluate(
      nowDateTime = LocalDateTime.of(2026, 11, 2, 10, 0),
      nowEpochMillis = 15_000L,
      profiles = listOf(silentProfile, defaultProfile),
      scheduleRules = emptyList(),
      locationRules = emptyList(),
      calendarRules = listOf(calendarRule),
      activeCalendarWindows = listOf(calendarWindow),
      activeTimer = null,
      overriddenSessionKey = sessionKey,
      defaultDayProfileId = defaultProfile.id,
      restorePolicy = RestorePolicy.DEFAULT_PROFILE,
    )

    // Candidato foi suprimido pelo override manual do evento -> cai no RuleOverriddenByUser
    assertTrue(decision is RuleEvaluationDecision.RuleOverriddenByUser)
  }

  @Test
  fun computeNextCalendarBoundaryMillis_and_automationTransition() {
    val rule = CalendarRule(
      id = "cal_1",
      name = "Reunião",
      targetProfileId = "preset_silent",
    )
    val instance1 = CalendarEventInstance(
      eventId = 1L,
      calendarId = 1L,
      title = "Em andamento",
      beginMillis = 1_000L,
      endMillis = 5_000L,
      allDay = false,
      availability = CalendarEventInstance.AVAILABILITY_BUSY,
    )
    val instance2 = CalendarEventInstance(
      eventId = 2L,
      calendarId = 1L,
      title = "Futuro",
      beginMillis = 8_000L,
      endMillis = 12_000L,
      allDay = false,
      availability = CalendarEventInstance.AVAILABILITY_BUSY,
    )

    val now = 3_000L
    // Próximo boundary de calendário: ou o término do evento 1 (5_000L) ou início do evento 2 (8_000L) -> 5_000L
    val nextBoundary = RuleEvaluator.computeNextCalendarBoundaryMillis(
      calendarRules = listOf(rule),
      instances = listOf(instance1, instance2),
      nowEpochMillis = now,
    )
    assertEquals(5_000L, nextBoundary)

    // Comparando com schedule: se schedule rules for vazio e calendar for 5_000L, resultado é 5_000L
    val nextAutomation = RuleEvaluator.computeNextAutomationTransitionMillis(
      scheduleRules = emptyList(),
      nextCalendarBoundaryMillis = nextBoundary,
    )
    assertEquals(5_000L, nextAutomation)
  }
}

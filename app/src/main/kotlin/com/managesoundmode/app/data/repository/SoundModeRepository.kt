package com.managesoundmode.app.data.repository

import com.managesoundmode.app.data.local.PersistedSoundPrefs
import com.managesoundmode.app.data.local.SoundPreferencesDataSource
import com.managesoundmode.app.data.system.SoundSystemController
import com.managesoundmode.app.domain.engine.RuleEvaluationDecision
import com.managesoundmode.app.domain.engine.RuleEvaluator
import com.managesoundmode.app.domain.model.AccessControlConfig
import com.managesoundmode.app.domain.model.BaseAudioMode
import com.managesoundmode.app.domain.model.DeviceSoundSnapshot
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SavedStateSnapshot
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.SoundState
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.TransitionLogEntry
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.system.BackgroundGeofenceManager
import com.managesoundmode.app.system.GeofencePresenceDecision
import com.managesoundmode.app.system.LocationProximityHelper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

interface SoundModeRepository {
  val snapshotStream: Flow<DeviceSoundSnapshot>
  val profilesStream: Flow<List<SoundProfile>>
  val preferencesStream: Flow<PersistedSoundPrefs>

  fun refreshSystemState()
  suspend fun setSoundMode(
    mode: SoundMode,
    source: TransitionTriggerSource = TransitionTriggerSource.MANUAL,
  ): Result<Unit>
  suspend fun setSoundState(
    state: SoundState,
    source: TransitionTriggerSource = TransitionTriggerSource.MANUAL,
  ): Result<SoundState> = setSoundMode(state.toLegacyApproximation(), source).map { state }
  suspend fun setBaseAudioMode(
    audioMode: BaseAudioMode,
    source: TransitionTriggerSource = TransitionTriggerSource.MANUAL,
  ): Result<SoundState> = setSoundState(SoundState(audioMode = audioMode, vibrateEnabled = true), source)
  suspend fun setVibrateEnabled(
    enabled: Boolean,
    source: TransitionTriggerSource = TransitionTriggerSource.MANUAL,
  ): Result<SoundState> = setSoundState(SoundState(audioMode = BaseAudioMode.SOUND, vibrateEnabled = enabled), source)
  suspend fun toggleVibrate(
    source: TransitionTriggerSource = TransitionTriggerSource.MANUAL,
  ): Result<SoundState> = setVibrateEnabled(enabled = true, source = source)
  suspend fun setStreamVolume(type: VolumeStreamType, percent: Int): Result<Unit>
  suspend fun activateProfile(
    profile: SoundProfile,
    source: TransitionTriggerSource = TransitionTriggerSource.MANUAL,
  ): Result<Unit>
  suspend fun startTimedSession(
    targetMode: SoundMode,
    durationMinutes: Int,
    nowEpochMillis: Long = System.currentTimeMillis(),
  ): Result<TimedModeSession>
  suspend fun startTimedStateSession(
    targetState: SoundState,
    durationMinutes: Int,
    nowEpochMillis: Long = System.currentTimeMillis(),
  ): Result<TimedModeSession> = startTimedSession(targetState.toLegacyApproximation(), durationMinutes, nowEpochMillis)
  suspend fun extendTimedSession(
    additionalMinutes: Int,
    nowEpochMillis: Long = System.currentTimeMillis(),
  ): Result<TimedModeSession>
  suspend fun cancelOrRestoreTimedSession(): Result<Unit>
  suspend fun saveCurrentAsProfile(name: String, description: String): Result<SoundProfile>
  suspend fun upsertProfile(profile: SoundProfile, applyImmediately: Boolean = false): Result<SoundProfile>
  suspend fun deleteCustomProfile(profileId: String): Result<Unit>

  suspend fun upsertScheduleRule(rule: ScheduleRule): Result<Unit>
  suspend fun deleteScheduleRule(ruleId: String): Result<Unit>
  suspend fun upsertLocationRule(rule: LocationRule): Result<Unit>
  suspend fun deleteLocationRule(ruleId: String): Result<Unit>
  suspend fun toggleLocationPresence(ruleId: String, inside: Boolean): Result<Unit>
  suspend fun syncLocationsWithDeviceCoordinates(latitude: Double, longitude: Double): Result<Int>

  suspend fun setRestorePolicy(policy: RestorePolicy): Result<Unit>
  suspend fun setDefaultDayProfileId(profileId: String): Result<Unit>
  suspend fun setUseDynamicColor(enabled: Boolean): Result<Unit>
  suspend fun setThemeMode(mode: ThemeMode): Result<Unit>
  suspend fun setLocationDisclosureAccepted(accepted: Boolean): Result<Unit>
  suspend fun setMuteMediaOnSilent(enabled: Boolean): Result<Unit>
  suspend fun setPreserveMediaOnBluetooth(enabled: Boolean): Result<Unit>
  suspend fun setSmartVolumeTriggerEnabled(enabled: Boolean): Result<Unit>
  suspend fun setPersistentQuickBarEnabled(enabled: Boolean): Result<Unit>
  suspend fun setQuickTileSlotProfileId(slotIndex: Int, profileId: String): Result<Unit>
  suspend fun toggleQuickMediaMute(): Result<Int>
  suspend fun clearTransitionLogs(): Result<Unit>
  suspend fun setAppLanguageCode(languageCode: String): Result<Unit>
  suspend fun setAutomationsPaused(paused: Boolean): Result<Unit>
  suspend fun setHapticFeedbackEnabled(enabled: Boolean): Result<Unit>
  suspend fun setNotifyOnAutoTransition(enabled: Boolean): Result<Unit>
  suspend fun setDefaultTimerMinutes(minutes: Int): Result<Unit>
  suspend fun setOnboardingCompleted(completed: Boolean): Result<Unit>
  suspend fun setAccessControlConfig(config: AccessControlConfig): Result<Unit> = Result.success(Unit)
  suspend fun setSystemZenRulesEnabled(enabled: Boolean): Result<Unit> = Result.success(Unit)
  suspend fun syncSystemZenRulesNow(): Result<Int> = Result.success(0)
  suspend fun syncBackgroundGeofencesNow(): Result<Int> = Result.success(0)
  suspend fun processBackgroundGeofenceSignal(
    latitude: Double?,
    longitude: Double?,
    connectedWifiSsid: String?,
    nowEpochMillis: Long = System.currentTimeMillis(),
    dwellDurationMillis: Long = BackgroundGeofenceManager.DEFAULT_DWELL_DURATION_MILLIS,
  ): Result<Int> = Result.success(0)
  suspend fun resetFactoryDefaults(): Result<Unit>
  suspend fun exportBackupJson(): Result<String>
  suspend fun importBackupJson(rawJson: String): Result<Unit>

  suspend fun reapplyActiveRule(): Result<Unit>
  suspend fun evaluateAndApplyRules(
    source: TransitionTriggerSource = TransitionTriggerSource.SCHEDULE,
    nowEpochMillis: Long = System.currentTimeMillis(),
  ): Result<RuleEvaluationDecision>
  suspend fun cycleSoundModeFromQuickTile(): Result<SoundMode>
  suspend fun handleExternalAudioChange(): Result<Unit>
}

class DefaultSoundModeRepository(
  private val systemController: SoundSystemController,
  private val preferencesDataSource: SoundPreferencesDataSource,
  private val onScheduleAutoRestore: (durationMinutes: Int) -> Unit = {},
  private val onCancelAutoRestore: () -> Unit = {},
  private val onRescheduleRuleAlarms: (scheduleRules: List<ScheduleRule>) -> Unit = {},
  private val onSmartRestorePrompt: (mode: SoundMode, mediaAutoMuted: Boolean) -> Unit = { _, _ -> },
  private val onDismissSmartPrompt: () -> Unit = {},
  private val onSyncActiveTimerNotification: (session: TimedModeSession?) -> Unit = {},
  private val onRuleOverrideAlert: (ruleName: String) -> Unit = {},
  private val onRefreshExternalSurfaces: suspend () -> Unit = {},
  private val onSyncSystemZenRules: (
    profiles: List<SoundProfile>,
    activeProfileId: String?,
    enabled: Boolean,
  ) -> Int = { profiles, _, enabled -> if (enabled) profiles.size else 0 },
  private val onSyncGeofenceRegistrations: (
    locationRules: List<LocationRule>,
    automationsPaused: Boolean,
  ) -> Int = { rules, paused -> if (paused) 0 else rules.count { it.enabled && it.hasCoordinates } },
  private val onScheduleGeofenceDwellCheck: (delayMillis: Long) -> Unit = {},
  private val clockNowMillis: () -> Long = { System.currentTimeMillis() },
) : SoundModeRepository {

  private val manualRefreshTrigger = MutableStateFlow(0L)
  private val lastObservedState = AtomicReference(systemController.readSoundState())
  private val syncedInitialFallback = AtomicReference(false)
  private val lastCanWriteSettings = AtomicReference(false)

  override val preferencesStream: Flow<PersistedSoundPrefs> = preferencesDataSource.prefsFlow

  override val profilesStream: Flow<List<SoundProfile>> =
    preferencesDataSource.prefsFlow.combine(manualRefreshTrigger) { prefs, _ ->
      mergeProfiles(prefs.customProfiles, prefs.deletedProfileIds)
    }

  override val snapshotStream: Flow<DeviceSoundSnapshot> = combine(
    systemController.systemChanges,
    manualRefreshTrigger,
    preferencesDataSource.prefsFlow,
  ) { _, _, prefs ->
    val canWriteNow = systemController.canWriteSystemSettings()
    val wasCanWrite = lastCanWriteSettings.getAndSet(canWriteNow)
    if (!syncedInitialFallback.getAndSet(true) || (!wasCanWrite && canWriteNow)) {
      systemController.syncFallbackSoundVibrate(prefs.soundVibrateEnabledFallback)
    }
    val nowMillis = clockNowMillis()
    val nowDateTime = LocalDateTime.ofInstant(
      Instant.ofEpochMilli(nowMillis),
      ZoneId.systemDefault(),
    )
    val allProfiles = mergeProfiles(prefs.customProfiles, prefs.deletedProfileIds)

    val decision = RuleEvaluator.evaluate(
      nowDateTime = nowDateTime,
      nowEpochMillis = nowMillis,
      profiles = allProfiles,
      scheduleRules = if (prefs.automationsPaused) emptyList() else prefs.scheduleRules,
      locationRules = if (prefs.automationsPaused) emptyList() else prefs.locationRules,
      activeTimer = prefs.activeTimer,
      overriddenSessionKey = prefs.overriddenSessionKey,
      restorePolicy = prefs.restorePolicy,
      defaultDayProfileId = prefs.defaultDayProfileId,
      savedSnapshot = prefs.savedSnapshot,
    )

    val (ruleTitle, ruleSubtitle, ruleKey, overridden) = when (decision) {
      is RuleEvaluationDecision.TimerActive -> Quadruple(
        "Modo Temporizado (${decision.timer.resolvedSoundState.title})",
        "Auto-Restore ativo • Restam ${decision.remainingMinutes} min",
        null,
        false,
      )
      is RuleEvaluationDecision.ApplyRule -> Quadruple(
        "Rotina '${decision.winner.ruleName}' ativa",
        "${decision.winner.subtitle} • Perfil ${decision.winner.profile.name}",
        decision.winner.sessionKey,
        false,
      )
      is RuleEvaluationDecision.RuleOverriddenByUser -> Quadruple(
        "Rotina '${decision.winner.ruleName}' pausada manualmente",
        "Alterado pelos botões/painel • Retoma no próximo período",
        decision.winner.sessionKey,
        true,
      )
      is RuleEvaluationDecision.NoActiveRules -> Quadruple(
        if (prefs.automationsPaused) "Automações Pausadas (Modo Férias)" else null,
        if (prefs.automationsPaused) {
          "Rotinas de horário e localização temporariamente suspensas"
        } else {
          "Padrão do Sistema • Nenhuma automação restritiva no momento"
        },
        null,
        false,
      )
    }

    val currentSoundState = systemController.readSoundState()
    DeviceSoundSnapshot(
      currentMode = currentSoundState.toLegacyApproximation(),
      currentSoundState = currentSoundState,
      streams = systemController.readVolumeStreams(),
      hasDndPermission = systemController.hasDndPermission(),
      canWriteSystemSettings = systemController.canWriteSystemSettings(),
      canScheduleExactAlarms = systemController.canScheduleExactAlarms(),
      hasLocationPermission = systemController.hasLocationPermission(),
      hasBackgroundLocationPermission = systemController.hasBackgroundLocationPermission(),
      hasNotificationPermission = systemController.hasNotificationPermission(),
      isMusicActive = systemController.isMusicActive(),
      isBluetoothHeadsetConnected = systemController.isBluetoothHeadsetConnected(),
      activeProfileId = prefs.activeProfileId,
      activeTimer = prefs.activeTimer,
      activeRuleTitle = ruleTitle,
      activeRuleSubtitle = ruleSubtitle,
      activeRuleSessionKey = ruleKey,
      isManuallyOverridden = overridden,
    )
  }

  override fun refreshSystemState() {
    manualRefreshTrigger.value += 1L
  }

  override suspend fun setSoundMode(
    mode: SoundMode,
    source: TransitionTriggerSource,
  ): Result<Unit> = setSoundState(SoundState.fromLegacyMode(mode), source).map { Unit }

  override suspend fun setSoundState(
    state: SoundState,
    source: TransitionTriggerSource,
  ): Result<SoundState> {
    val previousState = systemController.readSoundState()
    val prefs = preferencesDataSource.prefsFlow.first()
    val currentMedia = systemController.readVolumeStreams().mediaPercent

    return systemController.applySoundState(state).map { appliedState ->
      lastObservedState.set(appliedState)
      if (appliedState.audioMode == BaseAudioMode.SOUND) {
        preferencesDataSource.setSoundVibrateEnabledFallback(appliedState.vibrateEnabled)
      }

      if (previousState.audioMode == BaseAudioMode.SOUND &&
        appliedState.audioMode == BaseAudioMode.SILENT
      ) {
        val skipBt = prefs.preserveMediaOnBluetooth && systemController.isBluetoothHeadsetConnected()
        if (prefs.muteMediaOnSilent && !skipBt && currentMedia > 0) {
          preferencesDataSource.setSavedMediaBeforeMute(currentMedia)
          systemController.applyStreamVolume(VolumeStreamType.MEDIA, 0)
        }
      } else if (previousState.audioMode == BaseAudioMode.SILENT &&
        appliedState.audioMode == BaseAudioMode.SOUND
      ) {
        onDismissSmartPrompt()
        val savedMedia = prefs.savedMediaBeforeMute
        if (savedMedia != null && systemController.readVolumeStreams().mediaPercent == 0) {
          systemController.applyStreamVolume(VolumeStreamType.MEDIA, savedMedia)
          preferencesDataSource.setSavedMediaBeforeMute(null)
        }
      }

      markManualOverrideIfRuleActive()
      preferencesDataSource.setActiveProfileId(null)
      logTransition(
        title = "Modo alterado para ${appliedState.title}",
        detail = "Alternância direta de modo de toque e vibração",
        source = source,
        state = appliedState,
      )
      onRefreshExternalSurfaces()
      refreshSystemState()
      appliedState
    }
  }

  override suspend fun setBaseAudioMode(
    audioMode: BaseAudioMode,
    source: TransitionTriggerSource,
  ): Result<SoundState> {
    val current = systemController.readSoundState()
    return setSoundState(
      state = SoundState(audioMode = audioMode, vibrateEnabled = current.vibrateEnabled),
      source = source,
    )
  }

  override suspend fun setVibrateEnabled(
    enabled: Boolean,
    source: TransitionTriggerSource,
  ): Result<SoundState> {
    val current = systemController.readSoundState()
    return setSoundState(
      state = SoundState(audioMode = current.audioMode, vibrateEnabled = enabled),
      source = source,
    )
  }

  override suspend fun toggleVibrate(
    source: TransitionTriggerSource,
  ): Result<SoundState> {
    val current = systemController.readSoundState()
    return setVibrateEnabled(enabled = !current.vibrateEnabled, source = source)
  }

  override suspend fun setStreamVolume(type: VolumeStreamType, percent: Int): Result<Unit> {
    val stateBefore = systemController.readSoundState()
    if ((type == VolumeStreamType.RING || type == VolumeStreamType.NOTIFICATION) &&
      percent > 0 &&
      stateBefore.audioMode == BaseAudioMode.SILENT &&
      (stateBefore.vibrateEnabled || systemController.hasDndPermission())
    ) {
      val soundTarget = SoundState(
        audioMode = BaseAudioMode.SOUND,
        vibrateEnabled = stateBefore.vibrateEnabled,
      )
      systemController.applySoundState(soundTarget)
      lastObservedState.set(soundTarget)
    }
    return systemController.applyStreamVolume(type, percent).map {
      lastObservedState.set(systemController.readSoundState())
      if (type == VolumeStreamType.MEDIA) {
        preferencesDataSource.setSavedMediaBeforeMute(null)
      }
      markManualOverrideIfRuleActive()
      preferencesDataSource.setActiveProfileId(null)
      onRefreshExternalSurfaces()
      refreshSystemState()
    }
  }

  override suspend fun activateProfile(
    profile: SoundProfile,
    source: TransitionTriggerSource,
  ): Result<Unit> {
    val targetState = profile.resolvedSoundState
    return applyProfileToSystem(profile).map {
      lastObservedState.set(targetState)
      if (targetState.audioMode == BaseAudioMode.SOUND) {
        preferencesDataSource.setSoundVibrateEnabledFallback(targetState.vibrateEnabled)
      }
      if (source == TransitionTriggerSource.MANUAL) {
        markManualOverrideIfRuleActive()
      }
      preferencesDataSource.setActiveProfileId(profile.id)
      logTransition(
        title = "Perfil '${profile.name}' ativado",
        detail = "${targetState.title} • Toque ${profile.streams.ringPercent}%, Mídia ${profile.streams.mediaPercent}%",
        source = source,
        state = targetState,
      )
      onRefreshExternalSurfaces()
      refreshSystemState()
    }
  }

  override suspend fun startTimedSession(
    targetMode: SoundMode,
    durationMinutes: Int,
    nowEpochMillis: Long,
  ): Result<TimedModeSession> = startTimedStateSession(
    targetState = SoundState.fromLegacyMode(targetMode),
    durationMinutes = durationMinutes,
    nowEpochMillis = nowEpochMillis,
  )

  override suspend fun startTimedStateSession(
    targetState: SoundState,
    durationMinutes: Int,
    nowEpochMillis: Long,
  ): Result<TimedModeSession> {
    val previousState = systemController.readSoundState()
    val previousStreams = systemController.readVolumeStreams()
    val prefs = preferencesDataSource.prefsFlow.first()

    return systemController.applySoundState(targetState).map { appliedState ->
      lastObservedState.set(appliedState)
      if (appliedState.audioMode == BaseAudioMode.SILENT && prefs.muteMediaOnSilent) {
        val skipBt = prefs.preserveMediaOnBluetooth && systemController.isBluetoothHeadsetConnected()
        if (!skipBt && previousStreams.mediaPercent > 0) {
          systemController.applyStreamVolume(VolumeStreamType.MEDIA, 0)
        }
      }
      val session = TimedModeSession(
        targetMode = appliedState.toLegacyApproximation(),
        previousMode = previousState.toLegacyApproximation(),
        previousStreams = previousStreams,
        durationMinutes = durationMinutes,
        startedAtEpochMillis = nowEpochMillis,
        endsAtEpochMillis = nowEpochMillis + durationMinutes * 60_000L,
        audioMode = appliedState.audioMode,
        vibrateEnabled = appliedState.vibrateEnabled,
        previousAudioMode = previousState.audioMode,
        previousVibrateEnabled = previousState.vibrateEnabled,
      )
      preferencesDataSource.setActiveTimer(session)
      onScheduleAutoRestore(durationMinutes)
      onSyncActiveTimerNotification(session)
      logTransition(
        title = "Timer iniciado: ${appliedState.title} por ${durationMinutes}m",
        detail = "Restaurará automaticamente para ${previousState.title}",
        source = TransitionTriggerSource.TIMER,
        state = appliedState,
      )
      onRefreshExternalSurfaces()
      refreshSystemState()
      session
    }
  }

  override suspend fun extendTimedSession(
    additionalMinutes: Int,
    nowEpochMillis: Long,
  ): Result<TimedModeSession> = runCatching {
    val prefs = preferencesDataSource.prefsFlow.first()
    val current = prefs.activeTimer
      ?: return startTimedStateSession(
        targetState = systemController.readSoundState().let { state ->
          if (state.audioMode == BaseAudioMode.SOUND) {
            SoundState(audioMode = BaseAudioMode.SILENT, vibrateEnabled = true)
          } else {
            state
          }
        },
        durationMinutes = additionalMinutes,
        nowEpochMillis = nowEpochMillis,
      )

    val baseEnd = maxOf(current.endsAtEpochMillis, nowEpochMillis)
    val newEnd = baseEnd + additionalMinutes * 60_000L
    val totalRemainingMinutes = (((newEnd - nowEpochMillis) + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
    val updated = current.copy(
      durationMinutes = current.durationMinutes + additionalMinutes,
      endsAtEpochMillis = newEnd,
    )
    preferencesDataSource.setActiveTimer(updated)
    onScheduleAutoRestore(totalRemainingMinutes)
    onSyncActiveTimerNotification(updated)
    logTransition(
      title = "Timer estendido (+${additionalMinutes}m)",
      detail = "Restam ${totalRemainingMinutes} min em ${updated.resolvedSoundState.title}",
      source = TransitionTriggerSource.NOTIFICATION,
      state = updated.resolvedSoundState,
    )
    onRefreshExternalSurfaces()
    refreshSystemState()
    updated
  }

  override suspend fun cancelOrRestoreTimedSession(): Result<Unit> = runCatching {
    onCancelAutoRestore()
    onSyncActiveTimerNotification(null)
    val prefs = preferencesDataSource.prefsFlow.first()
    val savedTimer = prefs.activeTimer
    preferencesDataSource.setActiveTimer(null)

    val restoreState = savedTimer?.resolvedPreviousSoundState ?: SoundState.DEFAULT
    systemController.applySoundState(restoreState).getOrThrow()
    lastObservedState.set(restoreState)
    if (restoreState.audioMode == BaseAudioMode.SOUND) {
      preferencesDataSource.setSoundVibrateEnabledFallback(restoreState.vibrateEnabled)
      if (savedTimer != null) {
        systemController.applyAllStreams(savedTimer.previousStreams)
      }
    }
    logTransition(
      title = "Timer finalizado • Modo restaurado",
      detail = "Retornou para ${restoreState.title}",
      source = TransitionTriggerSource.RESTORE,
      state = restoreState,
    )
    // Reavalia se alguma regra de horário/local está ativa agora
    evaluateAndApplyRules(source = TransitionTriggerSource.RESTORE)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun saveCurrentAsProfile(name: String, description: String): Result<SoundProfile> = runCatching {
    val trimmedName = name.trim()
    require(trimmedName.isNotEmpty()) { "O nome do perfil não pode ser vazio" }
    val currentState = systemController.readSoundState()
    val profile = SoundProfile(
      id = "custom_${clockNowMillis()}",
      name = trimmedName,
      description = description.trim().ifEmpty { "Perfil customizado pelo usuário" },
      mode = currentState.toLegacyApproximation(),
      audioMode = currentState.audioMode,
      vibrateEnabled = currentState.vibrateEnabled,
      streams = systemController.readVolumeStreams(),
      isPreset = false,
    )
    preferencesDataSource.upsertCustomProfile(profile)
    preferencesDataSource.setActiveProfileId(profile.id)
    refreshSystemState()
    profile
  }

  override suspend fun upsertProfile(
    profile: SoundProfile,
    applyImmediately: Boolean,
  ): Result<SoundProfile> = runCatching {
    val trimmedName = profile.name.trim()
    require(trimmedName.isNotEmpty()) { "O nome do perfil não pode ser vazio" }
    val normalized = profile.normalized().copy(
      name = trimmedName,
      description = profile.description.trim().ifEmpty { profile.summaryLabel() },
    )
    val prefsBefore = preferencesDataSource.prefsFlow.first()
    preferencesDataSource.upsertCustomProfile(normalized)
    preferencesDataSource.setOverriddenSessionKey(null)

    if (applyImmediately || prefsBefore.activeProfileId == normalized.id) {
      activateProfile(normalized, source = TransitionTriggerSource.MANUAL).getOrThrow()
    } else {
      preferencesDataSource.setLastAppliedRuleSessionKey(null)
      evaluateAndApplyRules(source = TransitionTriggerSource.MANUAL)
      onRefreshExternalSurfaces()
      refreshSystemState()
    }
    normalized
  }

  override suspend fun deleteCustomProfile(profileId: String): Result<Unit> = runCatching {
    preferencesDataSource.deleteCustomProfile(profileId)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun upsertScheduleRule(rule: ScheduleRule): Result<Unit> = runCatching {
    val prevPrefs = preferencesDataSource.prefsFlow.first()
    val wasPreviouslyActive = prevPrefs.lastAppliedRuleSessionKey?.contains(rule.id) == true
    preferencesDataSource.upsertScheduleRule(rule)
    preferencesDataSource.setOverriddenSessionKey(null)
    if (!rule.enabled && wasPreviouslyActive) {
      // Marca para garantir restauração se nenhuma outra regra estiver ativa
      preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_${rule.id}")
    } else if (rule.enabled) {
      preferencesDataSource.setLastAppliedRuleSessionKey(null)
    }
    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    onRescheduleRuleAlarms(updatedPrefs.scheduleRules)
    evaluateAndApplyRules(source = TransitionTriggerSource.SCHEDULE)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun deleteScheduleRule(ruleId: String): Result<Unit> = runCatching {
    val prevPrefs = preferencesDataSource.prefsFlow.first()
    val wasPreviouslyActive = prevPrefs.lastAppliedRuleSessionKey?.contains(ruleId) == true
    preferencesDataSource.deleteScheduleRule(ruleId)
    preferencesDataSource.setOverriddenSessionKey(null)
    if (wasPreviouslyActive) {
      preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_$ruleId")
    }
    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    onRescheduleRuleAlarms(updatedPrefs.scheduleRules)
    evaluateAndApplyRules(source = TransitionTriggerSource.SCHEDULE)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun upsertLocationRule(rule: LocationRule): Result<Unit> = runCatching {
    val prevPrefs = preferencesDataSource.prefsFlow.first()
    val wasPreviouslyActive = prevPrefs.lastAppliedRuleSessionKey?.contains(rule.id) == true
    preferencesDataSource.upsertLocationRule(rule)
    preferencesDataSource.setOverriddenSessionKey(null)
    if ((!rule.enabled || !rule.isCurrentlyInside) && wasPreviouslyActive) {
      preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_${rule.id}")
    } else if (rule.enabled && rule.isCurrentlyInside) {
      preferencesDataSource.setLastAppliedRuleSessionKey(null)
    }
    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    onSyncGeofenceRegistrations(updatedPrefs.locationRules, updatedPrefs.automationsPaused)
    evaluateAndApplyRules(source = TransitionTriggerSource.LOCATION)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun deleteLocationRule(ruleId: String): Result<Unit> = runCatching {
    val prevPrefs = preferencesDataSource.prefsFlow.first()
    val wasPreviouslyActive = prevPrefs.lastAppliedRuleSessionKey?.contains(ruleId) == true
    preferencesDataSource.deleteLocationRule(ruleId)
    preferencesDataSource.setOverriddenSessionKey(null)
    if (wasPreviouslyActive) {
      preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_$ruleId")
    }
    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    onSyncGeofenceRegistrations(updatedPrefs.locationRules, updatedPrefs.automationsPaused)
    evaluateAndApplyRules(source = TransitionTriggerSource.LOCATION)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun toggleLocationPresence(ruleId: String, inside: Boolean): Result<Unit> = runCatching {
    val prefs = preferencesDataSource.prefsFlow.first()
    val target = prefs.locationRules.firstOrNull { it.id == ruleId } ?: return@runCatching
    val updated = target.copy(isCurrentlyInside = inside)
    preferencesDataSource.upsertLocationRule(updated)
    preferencesDataSource.setOverriddenSessionKey(null)
    if (!inside && prefs.lastAppliedRuleSessionKey?.contains(ruleId) == true) {
      preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_$ruleId")
    } else if (inside) {
      preferencesDataSource.setLastAppliedRuleSessionKey(null)
    }
    evaluateAndApplyRules(source = TransitionTriggerSource.LOCATION)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun syncLocationsWithDeviceCoordinates(
    latitude: Double,
    longitude: Double,
  ): Result<Int> = runCatching {
    val prefs = preferencesDataSource.prefsFlow.first()
    var matchedCount = 0
    var stateChanged = false
    var exitedRuleId: String? = null
    for (rule in prefs.locationRules) {
      val dist = LocationProximityHelper.distanceMeters(
        lat1 = latitude,
        lon1 = longitude,
        lat2 = rule.latitude,
        lon2 = rule.longitude,
      )
      val insideNow = dist <= rule.radiusMeters
      if (insideNow && rule.enabled) {
        matchedCount++
      }
      if (rule.isCurrentlyInside != insideNow) {
        stateChanged = true
        if (!insideNow) {
          exitedRuleId = rule.id
        }
        preferencesDataSource.upsertLocationRule(rule.copy(isCurrentlyInside = insideNow))
      }
    }
    if (stateChanged) {
      preferencesDataSource.setOverriddenSessionKey(null)
      if (matchedCount == 0 && exitedRuleId != null && prefs.lastAppliedRuleSessionKey?.contains(exitedRuleId) == true) {
        preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_$exitedRuleId")
      } else if (matchedCount > 0) {
        preferencesDataSource.setLastAppliedRuleSessionKey(null)
      }
      evaluateAndApplyRules(source = TransitionTriggerSource.LOCATION)
      onRefreshExternalSurfaces()
      refreshSystemState()
    }
    matchedCount
  }

  override suspend fun syncBackgroundGeofencesNow(): Result<Int> = runCatching {
    val prefs = preferencesDataSource.prefsFlow.first()
    onSyncGeofenceRegistrations(prefs.locationRules, prefs.automationsPaused)
  }

  override suspend fun processBackgroundGeofenceSignal(
    latitude: Double?,
    longitude: Double?,
    connectedWifiSsid: String?,
    nowEpochMillis: Long,
    dwellDurationMillis: Long,
  ): Result<Int> = runCatching {
    val prefs = preferencesDataSource.prefsFlow.first()
    if (prefs.automationsPaused) return@runCatching 0

    var insideConfirmedCount = 0
    var stateChanged = false
    var exitedRuleId: String? = null
    var minPendingDwellMillis: Long? = null

    for (rule in prefs.locationRules) {
      val decision = BackgroundGeofenceManager.evaluatePresenceWithDwell(
        rule = rule,
        latitude = latitude,
        longitude = longitude,
        connectedWifiSsid = connectedWifiSsid,
        nowEpochMillis = nowEpochMillis,
        dwellDurationMillis = dwellDurationMillis,
      )
      when (decision) {
        is GeofencePresenceDecision.ConfirmedInside -> {
          if (rule.enabled) {
            insideConfirmedCount++
          }
          if (!rule.isCurrentlyInside) {
            stateChanged = true
            preferencesDataSource.upsertLocationRule(rule.copy(isCurrentlyInside = true))
          }
        }
        is GeofencePresenceDecision.PendingDwell -> {
          val currentMin = minPendingDwellMillis
          minPendingDwellMillis = if (currentMin == null) {
            decision.remainingMillis
          } else {
            minOf(currentMin, decision.remainingMillis)
          }
        }
        GeofencePresenceDecision.ConfirmedOutside -> {
          if (rule.isCurrentlyInside) {
            stateChanged = true
            exitedRuleId = rule.id
            preferencesDataSource.upsertLocationRule(rule.copy(isCurrentlyInside = false))
          }
        }
      }
    }

    if (minPendingDwellMillis != null) {
      onScheduleGeofenceDwellCheck(minPendingDwellMillis)
    }

    if (stateChanged) {
      preferencesDataSource.setOverriddenSessionKey(null)
      if (insideConfirmedCount == 0 && exitedRuleId != null && prefs.lastAppliedRuleSessionKey?.contains(exitedRuleId) == true) {
        preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_$exitedRuleId")
      } else if (insideConfirmedCount > 0) {
        preferencesDataSource.setLastAppliedRuleSessionKey(null)
      }
      evaluateAndApplyRules(source = TransitionTriggerSource.LOCATION, nowEpochMillis = nowEpochMillis)
      onRefreshExternalSurfaces()
      refreshSystemState()
    }
    insideConfirmedCount
  }

  override suspend fun setRestorePolicy(policy: RestorePolicy): Result<Unit> = runCatching {
    preferencesDataSource.setRestorePolicy(policy)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun setDefaultDayProfileId(profileId: String): Result<Unit> = runCatching {
    preferencesDataSource.setDefaultDayProfileId(profileId)
    val prefs = preferencesDataSource.prefsFlow.first()
    if (prefs.activeTimer == null && prefs.lastAppliedRuleSessionKey == null) {
      val allProfiles = mergeProfiles(prefs.customProfiles, prefs.deletedProfileIds)
      val selectedDefault = allProfiles.firstOrNull { it.id == profileId }
      if (selectedDefault != null) {
        applyProfileToSystem(selectedDefault)
        preferencesDataSource.setActiveProfileId(selectedDefault.id)
      }
    }
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun setUseDynamicColor(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setUseDynamicColor(enabled)
    refreshSystemState()
  }

  override suspend fun setThemeMode(mode: ThemeMode): Result<Unit> = runCatching {
    preferencesDataSource.setThemeMode(mode)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun setLocationDisclosureAccepted(accepted: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setLocationDisclosureAccepted(accepted)
    refreshSystemState()
  }

  override suspend fun setMuteMediaOnSilent(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setMuteMediaOnSilent(enabled)
    syncMediaProtectionNow()
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun setPreserveMediaOnBluetooth(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setPreserveMediaOnBluetooth(enabled)
    syncMediaProtectionNow()
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun setSmartVolumeTriggerEnabled(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setSmartVolumeTriggerEnabled(enabled)
    if (!enabled) {
      onDismissSmartPrompt()
    }
    refreshSystemState()
  }

  override suspend fun setPersistentQuickBarEnabled(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setPersistentQuickBarEnabled(enabled)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun setQuickTileSlotProfileId(slotIndex: Int, profileId: String): Result<Unit> = runCatching {
    if (slotIndex == 1) {
      preferencesDataSource.setQuickTileSlot1ProfileId(profileId)
    } else {
      preferencesDataSource.setQuickTileSlot2ProfileId(profileId)
    }
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun toggleQuickMediaMute(): Result<Int> = runCatching {
    val currentStreams = systemController.readVolumeStreams()
    val prefs = preferencesDataSource.prefsFlow.first()
    val targetPercent = if (currentStreams.mediaPercent > 0) {
      preferencesDataSource.setSavedMediaBeforeMute(currentStreams.mediaPercent)
      0
    } else {
      val restored = (prefs.savedMediaBeforeMute ?: 50).coerceIn(20, 100)
      preferencesDataSource.setSavedMediaBeforeMute(null)
      restored
    }
    systemController.applyStreamVolume(VolumeStreamType.MEDIA, targetPercent).getOrThrow()
    logTransition(
      title = if (targetPercent == 0) "Mídia zerada (0%)" else "Mídia restaurada ($targetPercent%)",
      detail = "Acionado pelo bloco Zero Mídia no Header",
      source = TransitionTriggerSource.QUICK_TILE,
      state = systemController.readSoundState(),
    )
    onRefreshExternalSurfaces()
    refreshSystemState()
    targetPercent
  }

  override suspend fun clearTransitionLogs(): Result<Unit> = runCatching {
    preferencesDataSource.clearTransitionLogs()
    refreshSystemState()
  }

  override suspend fun setAppLanguageCode(languageCode: String): Result<Unit> = runCatching {
    preferencesDataSource.setAppLanguageCode(languageCode)
    refreshSystemState()
  }

  override suspend fun setAutomationsPaused(paused: Boolean): Result<Unit> = runCatching {
    val prevPrefs = preferencesDataSource.prefsFlow.first()
    val wasRuleActive = prevPrefs.lastAppliedRuleSessionKey != null
    preferencesDataSource.setAutomationsPaused(paused)
    preferencesDataSource.setOverriddenSessionKey(null)
    if (paused && wasRuleActive) {
      preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_vacation_mode")
    } else if (!paused) {
      preferencesDataSource.setLastAppliedRuleSessionKey(null)
    }
    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    onSyncGeofenceRegistrations(updatedPrefs.locationRules, updatedPrefs.automationsPaused)
    evaluateAndApplyRules(source = TransitionTriggerSource.MANUAL)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun setHapticFeedbackEnabled(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setHapticFeedbackEnabled(enabled)
    refreshSystemState()
  }

  override suspend fun setNotifyOnAutoTransition(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setNotifyOnAutoTransition(enabled)
    refreshSystemState()
  }

  override suspend fun setDefaultTimerMinutes(minutes: Int): Result<Unit> = runCatching {
    preferencesDataSource.setDefaultTimerMinutes(minutes)
    refreshSystemState()
  }

  override suspend fun setOnboardingCompleted(completed: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setOnboardingCompleted(completed)
    refreshSystemState()
  }

  override suspend fun setAccessControlConfig(config: AccessControlConfig): Result<Unit> = runCatching {
    preferencesDataSource.setAccessControlConfig(config)
    refreshSystemState()
  }

  override suspend fun setSystemZenRulesEnabled(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setSystemZenRulesEnabled(enabled)
    val prefs = preferencesDataSource.prefsFlow.first()
    val allProfiles = mergeProfiles(prefs.customProfiles, prefs.deletedProfileIds)
    onSyncSystemZenRules(allProfiles, prefs.activeProfileId, enabled)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun syncSystemZenRulesNow(): Result<Int> = runCatching {
    val prefs = preferencesDataSource.prefsFlow.first()
    val allProfiles = mergeProfiles(prefs.customProfiles, prefs.deletedProfileIds)
    onSyncSystemZenRules(allProfiles, prefs.activeProfileId, prefs.systemZenRulesEnabled)
  }

  override suspend fun resetFactoryDefaults(): Result<Unit> = runCatching {
    preferencesDataSource.resetFactoryDefaults()
    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    val allProfiles = mergeProfiles(updatedPrefs.customProfiles, updatedPrefs.deletedProfileIds)
    val defaultProfile = allProfiles.firstOrNull { it.id == updatedPrefs.defaultDayProfileId }
      ?: allProfiles.first()
    applyProfileToSystem(defaultProfile)
    preferencesDataSource.setActiveProfileId(defaultProfile.id)
    onRescheduleRuleAlarms(updatedPrefs.scheduleRules)
    onSyncGeofenceRegistrations(updatedPrefs.locationRules, updatedPrefs.automationsPaused)
    onSyncSystemZenRules(allProfiles, defaultProfile.id, updatedPrefs.systemZenRulesEnabled)
    logTransition(
      title = "Padrões de fábrica restaurados",
      detail = "Perfis, horários e locais padrão redefinidos",
      source = TransitionTriggerSource.MANUAL,
      state = defaultProfile.resolvedSoundState,
    )
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun exportBackupJson(): Result<String> = runCatching {
    preferencesDataSource.exportBackupJson()
  }

  override suspend fun importBackupJson(rawJson: String): Result<Unit> = runCatching {
    preferencesDataSource.importBackupJson(rawJson)
    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    val allProfiles = mergeProfiles(updatedPrefs.customProfiles, updatedPrefs.deletedProfileIds)
    onRescheduleRuleAlarms(updatedPrefs.scheduleRules)
    onSyncGeofenceRegistrations(updatedPrefs.locationRules, updatedPrefs.automationsPaused)
    onSyncSystemZenRules(allProfiles, updatedPrefs.activeProfileId, updatedPrefs.systemZenRulesEnabled)
    evaluateAndApplyRules(source = TransitionTriggerSource.MANUAL)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun reapplyActiveRule(): Result<Unit> = runCatching {
    preferencesDataSource.setOverriddenSessionKey(null)
    preferencesDataSource.setLastAppliedRuleSessionKey(null)
    systemController.clearExternalBroadcastFlag()
    evaluateAndApplyRules(source = TransitionTriggerSource.MANUAL).getOrThrow()
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun evaluateAndApplyRules(
    source: TransitionTriggerSource,
    nowEpochMillis: Long,
  ): Result<RuleEvaluationDecision> = runCatching {
    val prefs = preferencesDataSource.prefsFlow.first()
    val allProfiles = mergeProfiles(prefs.customProfiles, prefs.deletedProfileIds)
    val nowDateTime = LocalDateTime.ofInstant(
      Instant.ofEpochMilli(nowEpochMillis),
      ZoneId.systemDefault(),
    )

    // Se o sistema detectou que o usuário apertou o botão físico de volume durante uma regra ativa
    if (systemController.lastBroadcastWasExternal) {
      markManualOverrideIfRuleActive()
      systemController.clearExternalBroadcastFlag()
    }

    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    val decision = RuleEvaluator.evaluate(
      nowDateTime = nowDateTime,
      nowEpochMillis = nowEpochMillis,
      profiles = allProfiles,
      scheduleRules = if (updatedPrefs.automationsPaused) emptyList() else updatedPrefs.scheduleRules,
      locationRules = if (updatedPrefs.automationsPaused) emptyList() else updatedPrefs.locationRules,
      activeTimer = updatedPrefs.activeTimer,
      overriddenSessionKey = updatedPrefs.overriddenSessionKey,
      restorePolicy = updatedPrefs.restorePolicy,
      defaultDayProfileId = updatedPrefs.defaultDayProfileId,
      savedSnapshot = updatedPrefs.savedSnapshot,
    )

    when (decision) {
      is RuleEvaluationDecision.TimerActive -> {
        // Mantém o timer atual sem sobrescrever
      }

      is RuleEvaluationDecision.RuleOverriddenByUser -> {
        // Respeita o override manual do usuário até a próxima mudança de sessão
      }

      is RuleEvaluationDecision.ApplyRule -> {
        val winner = decision.winner
        // Se estamos entrando em uma sessão de regra a partir do estado livre, salva snapshot de entrada
        if (updatedPrefs.lastAppliedRuleSessionKey == null) {
          val currentState = systemController.readSoundState()
          preferencesDataSource.setSavedSnapshot(
            SavedStateSnapshot(
              mode = currentState.toLegacyApproximation(),
              streams = systemController.readVolumeStreams(),
              capturedAtEpochMillis = nowEpochMillis,
              audioMode = currentState.audioMode,
              vibrateEnabled = currentState.vibrateEnabled,
            ),
          )
        }
        // Se a sessão mudou, limpa qualquer override antigo
        if (updatedPrefs.overriddenSessionKey != null &&
          updatedPrefs.overriddenSessionKey != winner.sessionKey
        ) {
          preferencesDataSource.setOverriddenSessionKey(null)
        }

        // Aplica o perfil APENAS quando entrar na sessão pela primeira vez (jamais sobrescreve volumes no meio da sessão)
        if (updatedPrefs.lastAppliedRuleSessionKey != winner.sessionKey) {
          val winnerState = winner.profile.resolvedSoundState
          applyProfileToSystem(winner.profile).getOrThrow()
          if (winnerState.audioMode == BaseAudioMode.SOUND) {
            preferencesDataSource.setSoundVibrateEnabledFallback(winnerState.vibrateEnabled)
          }
          preferencesDataSource.setActiveProfileId(winner.profile.id)
          preferencesDataSource.setLastAppliedRuleSessionKey(winner.sessionKey)
          logTransition(
            title = "Regra '${winner.ruleName}' ativada",
            detail = "${winner.subtitle} -> ${winner.profile.name} (${winnerState.title})",
            source = winner.source,
            state = winnerState,
          )
        }
      }

      is RuleEvaluationDecision.NoActiveRules -> {
        // Se havia uma regra ativa que acabou de se encerrar, executa a política de restauração
        if (updatedPrefs.lastAppliedRuleSessionKey != null) {
          preferencesDataSource.setLastAppliedRuleSessionKey(null)
          preferencesDataSource.setOverriddenSessionKey(null)

          if (decision.restorePolicy == RestorePolicy.SNAPSHOT && decision.savedSnapshot != null) {
            val snap = decision.savedSnapshot
            val snapState = snap.resolvedSoundState
            systemController.applySoundState(snapState).getOrThrow()
            lastObservedState.set(snapState)
            if (snapState.audioMode == BaseAudioMode.SOUND) {
              preferencesDataSource.setSoundVibrateEnabledFallback(snapState.vibrateEnabled)
              systemController.applyAllStreams(snap.streams).getOrThrow()
            }
            preferencesDataSource.setActiveProfileId(null)
            preferencesDataSource.setSavedSnapshot(null)
            logTransition(
              title = "Período encerrado • Snapshot restaurado",
              detail = "Retornou para o estado anterior (${snapState.title})",
              source = TransitionTriggerSource.RESTORE,
              state = snapState,
            )
          } else {
            val fallback = decision.fallbackProfile
            val fallbackState = fallback.resolvedSoundState
            applyProfileToSystem(fallback).getOrThrow()
            if (fallbackState.audioMode == BaseAudioMode.SOUND) {
              preferencesDataSource.setSoundVibrateEnabledFallback(fallbackState.vibrateEnabled)
            }
            preferencesDataSource.setActiveProfileId(fallback.id)
            preferencesDataSource.setSavedSnapshot(null)
            logTransition(
              title = "Período encerrado • ${fallback.name}",
              detail = "Restaurado para o perfil padrão do dia (${fallbackState.title})",
              source = TransitionTriggerSource.RESTORE,
              state = fallbackState,
            )
          }
        }
      }
    }

    onRescheduleRuleAlarms(updatedPrefs.scheduleRules)
    onSyncActiveTimerNotification(updatedPrefs.activeTimer)
    onRefreshExternalSurfaces()
    refreshSystemState()
    decision
  }

  override suspend fun cycleSoundModeFromQuickTile(): Result<SoundMode> = runCatching {
    val current = systemController.readSoundState()
    val nextState = when {
      current.audioMode == BaseAudioMode.SOUND ->
        SoundState(audioMode = BaseAudioMode.SILENT, vibrateEnabled = true)
      current.vibrateEnabled ->
        if (systemController.hasDndPermission()) {
          SoundState(audioMode = BaseAudioMode.SILENT, vibrateEnabled = false)
        } else {
          SoundState(audioMode = BaseAudioMode.SOUND, vibrateEnabled = true)
        }
      else ->
        SoundState(audioMode = BaseAudioMode.SOUND, vibrateEnabled = true)
    }
    setSoundState(nextState, source = TransitionTriggerSource.QUICK_TILE).getOrThrow()
    nextState.toLegacyApproximation()
  }

  override suspend fun handleExternalAudioChange(): Result<Unit> = runCatching {
    if (!systemController.lastBroadcastWasExternal) return@runCatching
    systemController.clearExternalBroadcastFlag()

    val prefs = preferencesDataSource.prefsFlow.first()
    val currentState = systemController.readSoundState()
    val previousState = lastObservedState.getAndSet(currentState)
    val streams = systemController.readVolumeStreams()

    // 1. Se havia qualquer regra de horário/local ativa, marca como override manual para não sobrescrever o volume do usuário
    val overriddenRuleName = markManualOverrideIfRuleActive()
    if (overriddenRuleName != null && prefs.smartVolumeTriggerEnabled) {
      onRuleOverrideAlert(overriddenRuleName)
    }

    val modeTransitionedToSilent =
      previousState.audioMode == BaseAudioMode.SOUND && currentState.audioMode == BaseAudioMode.SILENT
    val modeTransitionedToNormal =
      previousState.audioMode == BaseAudioMode.SILENT && currentState.audioMode == BaseAudioMode.SOUND

    // 2. Sincronização automática de Mídia + Notificação Inteligente APENAS quando o aparelho ACABAR DE ENTRAR em Silêncio
    if (modeTransitionedToSilent) {
      var mediaAutoMuted = false
      val skipBt = prefs.preserveMediaOnBluetooth && systemController.isBluetoothHeadsetConnected()
      if (prefs.muteMediaOnSilent && !skipBt && streams.mediaPercent > 0) {
        preferencesDataSource.setSavedMediaBeforeMute(streams.mediaPercent)
        systemController.applyStreamVolume(VolumeStreamType.MEDIA, 0)
        mediaAutoMuted = true
      }

      if (prefs.smartVolumeTriggerEnabled && prefs.activeTimer == null) {
        onSmartRestorePrompt(currentState.toLegacyApproximation(), mediaAutoMuted)
      }

      logTransition(
        title = "Botão de volume → ${currentState.title}",
        detail = if (mediaAutoMuted) "Mídia silenciada automaticamente (0%)" else "Alterado pelos botões físicos",
        source = TransitionTriggerSource.HARDWARE_BUTTON,
        state = currentState,
      )
    } else if (currentState.audioMode == BaseAudioMode.SILENT) {
      // O aparelho JÁ estava em Silêncio e o usuário aumentou/alterou o volume manualmente -> respeitar 100%!
      if (streams.mediaPercent > 0) {
        preferencesDataSource.setSavedMediaBeforeMute(null)
      }
    } else if (modeTransitionedToNormal) {
      // O aparelho acabou de voltar de Silêncio para Som
      onDismissSmartPrompt()
      val savedMedia = prefs.savedMediaBeforeMute
      if (savedMedia != null && streams.mediaPercent == 0) {
        systemController.applyStreamVolume(VolumeStreamType.MEDIA, savedMedia)
        preferencesDataSource.setSavedMediaBeforeMute(null)
      }
    } else {
      // O aparelho JÁ estava em Som e o usuário ajustou algum volume -> respeitar 100%!
      if (streams.mediaPercent > 0 && prefs.savedMediaBeforeMute != null) {
        preferencesDataSource.setSavedMediaBeforeMute(null)
      }
    }

    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  private suspend fun syncMediaProtectionNow() {
    val currentState = systemController.readSoundState()
    if (currentState.audioMode == BaseAudioMode.SOUND) return
    val prefs = preferencesDataSource.prefsFlow.first()
    val streams = systemController.readVolumeStreams()
    val skipBt = prefs.preserveMediaOnBluetooth && systemController.isBluetoothHeadsetConnected()
    val shouldMute = prefs.muteMediaOnSilent && !skipBt

    if (shouldMute && streams.mediaPercent > 0) {
      preferencesDataSource.setSavedMediaBeforeMute(streams.mediaPercent)
      systemController.applyStreamVolume(VolumeStreamType.MEDIA, 0)
    } else if (!shouldMute && streams.mediaPercent == 0 && prefs.savedMediaBeforeMute != null) {
      systemController.applyStreamVolume(VolumeStreamType.MEDIA, prefs.savedMediaBeforeMute)
      preferencesDataSource.setSavedMediaBeforeMute(null)
    }
  }

  private fun applyProfileToSystem(profile: SoundProfile): Result<Unit> = runCatching {
    val targetState = profile.resolvedSoundState
    systemController.applySoundState(targetState).getOrThrow()
    lastObservedState.set(targetState)

    val skipMediaBecausePlaying = profile.preserveMediaIfPlaying && systemController.isMusicActive()
    val skipMediaBecauseBt = profile.skipMediaOnBluetooth && systemController.isBluetoothHeadsetConnected()
    val shouldChangeMedia = !skipMediaBecausePlaying && !skipMediaBecauseBt

    if (targetState.audioMode == BaseAudioMode.SOUND) {
      systemController.applyStreamVolume(VolumeStreamType.RING, profile.streams.ringPercent).getOrThrow()
      systemController.applyStreamVolume(VolumeStreamType.NOTIFICATION, profile.streams.notificationPercent).getOrThrow()
    }
    if (shouldChangeMedia) {
      systemController.applyStreamVolume(VolumeStreamType.MEDIA, profile.streams.mediaPercent).getOrThrow()
    }
    systemController.applyStreamVolume(VolumeStreamType.ALARM, profile.streams.alarmPercent).getOrThrow()
  }

  private suspend fun markManualOverrideIfRuleActive(): String? {
    val prefs = preferencesDataSource.prefsFlow.first()
    val nowMillis = clockNowMillis()
    val nowDateTime = LocalDateTime.ofInstant(
      Instant.ofEpochMilli(nowMillis),
      ZoneId.systemDefault(),
    )
    val allProfiles = mergeProfiles(prefs.customProfiles, prefs.deletedProfileIds)
    val decision = RuleEvaluator.evaluate(
      nowDateTime = nowDateTime,
      nowEpochMillis = nowMillis,
      profiles = allProfiles,
      scheduleRules = if (prefs.automationsPaused) emptyList() else prefs.scheduleRules,
      locationRules = if (prefs.automationsPaused) emptyList() else prefs.locationRules,
      activeTimer = prefs.activeTimer,
      overriddenSessionKey = null,
      restorePolicy = prefs.restorePolicy,
      defaultDayProfileId = prefs.defaultDayProfileId,
      savedSnapshot = prefs.savedSnapshot,
    )
    if (decision is RuleEvaluationDecision.ApplyRule) {
      val wasAlreadyOverridden = prefs.overriddenSessionKey == decision.winner.sessionKey
      preferencesDataSource.setLastAppliedRuleSessionKey(decision.winner.sessionKey)
      preferencesDataSource.setOverriddenSessionKey(decision.winner.sessionKey)
      if (!wasAlreadyOverridden) {
        return decision.winner.ruleName
      }
    }
    return null
  }

  private suspend fun logTransition(
    title: String,
    detail: String,
    source: TransitionTriggerSource,
    state: SoundState,
  ) {
    val now = clockNowMillis()
    preferencesDataSource.appendTransitionLog(
      TransitionLogEntry(
        id = "log_${now}_${title.hashCode()}",
        timestampEpochMillis = now,
        title = title,
        detail = detail,
        source = source,
        appliedMode = state.toLegacyApproximation(),
        audioMode = state.audioMode,
        vibrateEnabled = state.vibrateEnabled,
      ),
    )
  }

  private fun mergeProfiles(
    savedProfiles: List<SoundProfile>,
    deletedIds: Set<String> = emptySet(),
  ): List<SoundProfile> {
    val savedById = savedProfiles.map { it.normalized() }.associateBy { it.id }
    val presetIds = SoundProfile.defaultPresets.map { it.id }.toSet()
    val resolvedPresets = SoundProfile.defaultPresets.map { preset ->
      savedById[preset.id]?.copy(isPreset = true) ?: preset
    }
    val extraCustom = savedProfiles.map { it.normalized() }.filterNot { it.id in presetIds }
    val merged = (resolvedPresets + extraCustom).filterNot { it.id in deletedIds }
    return merged.ifEmpty { listOf(SoundProfile.defaultPresets.first()) }
  }

  private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
  )
}

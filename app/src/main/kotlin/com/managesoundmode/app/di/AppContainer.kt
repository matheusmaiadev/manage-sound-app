package com.managesoundmode.app.di

import android.content.Context
import com.managesoundmode.app.data.local.DataStoreSoundPreferencesDataSource
import com.managesoundmode.app.data.local.SoundPreferencesDataSource
import com.managesoundmode.app.data.repository.DefaultSoundModeRepository
import com.managesoundmode.app.data.repository.SoundModeRepository
import com.managesoundmode.app.data.system.AndroidSoundSystemController
import com.managesoundmode.app.data.system.SoundSystemController
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.system.AppShortcutsManager
import com.managesoundmode.app.system.BackgroundGeofenceManager
import com.managesoundmode.app.system.ExactRuleAlarmScheduler
import com.managesoundmode.app.system.SmartNotificationManager
import com.managesoundmode.app.system.SystemZenRuleManager
import com.managesoundmode.app.tile.SoundModeTileService
import com.managesoundmode.app.widget.SoundModeGlanceWidget
import com.managesoundmode.app.work.RestoreSoundModeWorker
import kotlinx.coroutines.flow.first

interface AppContainer {
  val soundSystemController: SoundSystemController
  val preferencesDataSource: SoundPreferencesDataSource
  val soundModeRepository: SoundModeRepository
}

class DefaultAppContainer(
  private val appContext: Context,
) : AppContainer {

  override val soundSystemController: SoundSystemController by lazy {
    AndroidSoundSystemController(appContext)
  }

  override val preferencesDataSource: SoundPreferencesDataSource by lazy {
    DataStoreSoundPreferencesDataSource(appContext)
  }

  override val soundModeRepository: SoundModeRepository by lazy {
    DefaultSoundModeRepository(
      systemController = soundSystemController,
      preferencesDataSource = preferencesDataSource,
      onScheduleAutoRestore = { minutes -> RestoreSoundModeWorker.schedule(appContext, minutes) },
      onCancelAutoRestore = { RestoreSoundModeWorker.cancel(appContext) },
      onScheduleExactTimerAlarm = { endsAtMillis ->
        ExactRuleAlarmScheduler.scheduleTimerExpiration(appContext, endsAtMillis)
      },
      onCancelExactTimerAlarm = {
        ExactRuleAlarmScheduler.cancelTimerExpiration(appContext)
      },
      onRescheduleRuleAlarms = { rules ->
        ExactRuleAlarmScheduler.scheduleNextTransition(appContext, rules)
      },
      onSmartRestorePrompt = { mode, mediaAutoMuted ->
        SmartNotificationManager.showSmartRestorePrompt(
          context = appContext,
          mode = mode,
          mediaAutoMuted = mediaAutoMuted,
        )
      },
      onDismissSmartPrompt = {
        SmartNotificationManager.dismissSmartPrompt(appContext)
      },
      onSyncActiveTimerNotification = { session ->
        SmartNotificationManager.syncActiveTimerNotification(appContext, session)
      },
      onRuleOverrideAlert = { ruleName ->
        SmartNotificationManager.showRuleOverrideAlert(appContext, ruleName)
      },
      onAutoRoutineTransitionAlert = { title, subtitle, state ->
        SmartNotificationManager.showAutoRoutineTransition(
          context = appContext,
          title = title,
          subtitle = subtitle,
          state = state,
        )
      },
      onLanguageChanged = {
        SmartNotificationManager.ensureChannels(appContext)
        AppShortcutsManager.publishDynamicShortcuts(appContext)
      },
      onRefreshExternalSurfaces = {
        SoundModeGlanceWidget.refreshAllWidgets(appContext)
        runCatching {
          val prefs = preferencesDataSource.prefsFlow.first()
          val allProfiles = SoundProfile.mergeProfiles(
            savedProfiles = prefs.customProfiles,
            deletedIds = prefs.deletedProfileIds,
          )
          val activeProfileName = prefs.activeProfileId?.let { activeId ->
            allProfiles.firstOrNull { it.id == activeId }?.name
          }
          val currentSoundState = soundSystemController.readSoundState()
          SmartNotificationManager.syncPersistentQuickBarNotification(
            context = appContext,
            enabled = prefs.persistentQuickBarEnabled,
            currentMode = currentSoundState.toLegacyApproximation(),
            currentSoundState = currentSoundState,
            streams = soundSystemController.readVolumeStreams(),
            activeProfileName = activeProfileName,
            activeTimer = prefs.activeTimer,
            hasDndPermission = soundSystemController.hasDndPermission(),
          )
          SystemZenRuleManager.syncProfilesToSystemZenRules(
            context = appContext,
            profiles = allProfiles,
            activeProfileId = prefs.activeProfileId,
            enabled = prefs.systemZenRulesEnabled,
          )
        }
        runCatching {
          SoundModeTileService.refreshAllTiles(appContext)
        }
      },
      onSyncSystemZenRules = { profiles, activeProfileId, enabled ->
        SystemZenRuleManager.syncProfilesToSystemZenRules(
          context = appContext,
          profiles = profiles,
          activeProfileId = activeProfileId,
          enabled = enabled,
        )
      },
      onSyncGeofenceRegistrations = { locationRules, automationsPaused ->
        BackgroundGeofenceManager.syncGeofenceRegistrations(
          context = appContext,
          locationRules = locationRules,
          automationsPaused = automationsPaused,
        )
      },
      onScheduleGeofenceDwellCheck = { delayMillis ->
        BackgroundGeofenceManager.scheduleDwellVerificationAlarm(
          context = appContext,
          delayMillis = delayMillis,
        )
      },
    )
  }
}

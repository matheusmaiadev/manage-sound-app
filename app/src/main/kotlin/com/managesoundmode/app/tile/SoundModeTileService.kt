package com.managesoundmode.app.tile

import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.R
import com.managesoundmode.app.domain.model.AppPermission
import com.managesoundmode.app.domain.model.BaseAudioMode
import com.managesoundmode.app.domain.model.SoundState
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.ui.common.AppLabels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class QuickTileKind(
  val serviceClass: Class<out TileService>,
  @param:DrawableRes val iconRes: Int,
) {
  CYCLE(
    serviceClass = SoundModeTileService::class.java,
    iconRes = R.drawable.ic_qs_sound_mode,
  ),
  MODE_NORMAL(
    serviceClass = NormalModeTileService::class.java,
    iconRes = R.drawable.ic_widget_volume_up,
  ),
  MODE_VIBRATE(
    serviceClass = VibrateModeTileService::class.java,
    iconRes = R.drawable.ic_widget_vibration,
  ),
  MODE_SILENT(
    serviceClass = SilentModeTileService::class.java,
    iconRes = R.drawable.ic_widget_silent,
  ),
  PROFILE_SLOT_1(
    serviceClass = ProfileSlot1TileService::class.java,
    iconRes = R.drawable.ic_widget_style,
  ),
  PROFILE_SLOT_2(
    serviceClass = ProfileSlot2TileService::class.java,
    iconRes = R.drawable.ic_widget_style,
  ),
  PROFILE_SLOT_3(
    serviceClass = ProfileSlot3TileService::class.java,
    iconRes = R.drawable.ic_widget_style,
  ),
  QUICK_TIMER(
    serviceClass = QuickTimerTileService::class.java,
    iconRes = R.drawable.ic_qs_timer,
  ),
  MEDIA_MUTE(
    serviceClass = MediaMuteTileService::class.java,
    iconRes = R.drawable.ic_qs_media_mute,
  ),
  ;

  val title: String
    get() = when (this) {
      CYCLE -> AppLabels.Tiles.cycleTitle
      MODE_NORMAL -> AppLabels.Tiles.normalTitle
      MODE_VIBRATE -> AppLabels.Tiles.vibrateTitle
      MODE_SILENT -> AppLabels.Tiles.silentTitle
      PROFILE_SLOT_1 -> AppLabels.Tiles.slot1Title
      PROFILE_SLOT_2 -> AppLabels.Tiles.slot2Title
      PROFILE_SLOT_3 -> AppLabels.Tiles.slot3Title
      QUICK_TIMER -> AppLabels.Tiles.quickTimerTitle
      MEDIA_MUTE -> AppLabels.Tiles.mediaMuteTitle
    }

  val subtitle: String
    get() = when (this) {
      CYCLE -> AppLabels.Tiles.cycleSubtitle
      MODE_NORMAL -> AppLabels.Tiles.normalDesc
      MODE_VIBRATE -> AppLabels.Tiles.vibrateDesc
      MODE_SILENT -> AppLabels.Tiles.silentDesc
      PROFILE_SLOT_1 -> AppLabels.Tiles.slot1Desc
      PROFILE_SLOT_2 -> AppLabels.Tiles.slot2Desc
      PROFILE_SLOT_3 -> AppLabels.Tiles.slot3Desc
      QUICK_TIMER -> AppLabels.Tiles.quickTimerDesc
      MEDIA_MUTE -> AppLabels.Tiles.mediaMuteDesc
    }
}

abstract class BaseSoundTileService : TileService() {

  protected var serviceScope: CoroutineScope? = null

  override fun onStartListening() {
    super.onStartListening()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    serviceScope = scope
    scope.launch {
      refreshTileState()
    }
  }

  override fun onStopListening() {
    serviceScope?.cancel()
    serviceScope = null
    super.onStopListening()
  }

  override fun onClick() {
    super.onClick()
    val app = applicationContext as? ManageSoundApplication ?: return
    val scope = serviceScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    scope.launch {
      val prefs = app.container.preferencesDataSource.prefsFlow.first()
      if (!prefs.accessControlConfig.hasPermission(AppPermission.CHANGE_SOUND_AND_TIMER)) {
        refreshTileState()
        return@launch
      }
      handleTileClick(app)
      refreshTileState()
      SoundModeTileService.refreshAllTiles(applicationContext)
    }
  }

  @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated")
  protected fun openDndPermissionSettings() {
    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startSettingsIntentSafely(intent, requestCode = 910)
  }

  @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated")
  protected fun openWriteSettingsPermissionSettings() {
    val intent = Intent(
      Settings.ACTION_MANAGE_WRITE_SETTINGS,
      Uri.parse("package:$packageName"),
    ).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startSettingsIntentSafely(intent, requestCode = 911)
  }

  @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated")
  private fun startSettingsIntentSafely(intent: Intent, requestCode: Int) {
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val pendingIntent = PendingIntent.getActivity(
          this,
          requestCode,
          intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        startActivityAndCollapse(pendingIntent)
      } else {
        @Suppress("DEPRECATION")
        startActivityAndCollapse(intent)
      }
    }.onFailure { error ->
      Log.w(TAG, "Falha ao abrir configurações do sistema pelo Quick Tile", error)
    }
  }

  protected abstract suspend fun handleTileClick(app: ManageSoundApplication)
  protected abstract suspend fun refreshTileState()

  private companion object {
    const val TAG = "BaseSoundTileService"
  }
}

/**
 * 0. Bloco Alternador Único (Ciclo: Som -> Silêncio + Vibrar -> Silêncio Total -> Som)
 */
class SoundModeTileService : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    app.container.soundModeRepository.cycleSoundModeFromQuickTile()
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val state = app.container.soundSystemController.readSoundState()
    val streams = app.container.soundSystemController.readVolumeStreams()
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val profiles = app.container.soundModeRepository.profilesStream.first()
    val activeProfile = profiles.firstOrNull { it.id == prefs.activeProfileId }
    val remainingMin = prefs.activeTimer?.remainingMinutes(System.currentTimeMillis())

    val iconRes = when (state.audioMode) {
      BaseAudioMode.SOUND -> R.drawable.ic_widget_volume_up
      BaseAudioMode.SILENT ->
        if (state.vibrateEnabled) R.drawable.ic_widget_vibration else R.drawable.ic_widget_silent
    }

    tile.state = Tile.STATE_ACTIVE
    tile.icon = Icon.createWithResource(this, iconRes)
    tile.label = activeProfile?.name ?: AppLabels.Tiles.cycleLabel(state.title)
    tile.subtitle = when {
      remainingMin != null && remainingMin > 0 -> "${state.title} • ${remainingMin}m"
      state.audioMode == BaseAudioMode.SOUND ->
        AppLabels.Tiles.soundActiveSubtitle(streams.ringPercent, state.vibrateEnabled)
      else -> "${state.title} • ${streams.mediaPercent}%"
    }
    tile.updateTile()
  }

  companion object {
    private const val TAG = "SoundModeTileService"

    fun refreshAllTiles(context: Context) {
      QuickTileKind.entries.forEach { kind ->
        runCatching {
          requestListeningState(context, ComponentName(context, kind.serviceClass))
        }.onFailure { error ->
          Log.d(TAG, "Falha ao atualizar estado do tile ${kind.name}", error)
        }
      }
    }

    fun requestAddTile(
      context: Context,
      kind: QuickTileKind = QuickTileKind.CYCLE,
      customLabel: String? = null,
      onResultMessage: (String) -> Unit,
    ) {
      val displayTitle = customLabel ?: kind.title
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
        if (statusBarManager == null) {
          onResultMessage(AppLabels.Tiles.tileManualEditHint(displayTitle))
          return
        }
        val componentName = ComponentName(context, kind.serviceClass)
        val icon = Icon.createWithResource(context, kind.iconRes)
        statusBarManager.requestAddTileService(
          componentName,
          displayTitle,
          icon,
          ContextCompat.getMainExecutor(context),
        ) { resultCode ->
          val msg = when (resultCode) {
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
              AppLabels.Tiles.tileAddedMessage(displayTitle)
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
              AppLabels.Tiles.tileAlreadyAddedMessage(displayTitle)
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
              AppLabels.Tiles.tileCancelledMessage(displayTitle)
            else ->
              AppLabels.Tiles.tileManualEditHint(displayTitle)
          }
          onResultMessage(msg)
        }
      } else {
        onResultMessage(AppLabels.Tiles.tileManualEditHint(displayTitle))
      }
    }
  }
}

/**
 * Opção A.1 — Bloco Direto: Eixo de Áudio `Som` (`BaseAudioMode.SOUND`), preservando vibração.
 */
class NormalModeTileService : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    val controller = app.container.soundSystemController
    val currentState = controller.readSoundState()
    if (currentState.audioMode == BaseAudioMode.SILENT &&
      !currentState.vibrateEnabled &&
      !controller.hasDndPermission()
    ) {
      openDndPermissionSettings()
      return
    }
    app.container.soundModeRepository.setBaseAudioMode(
      audioMode = BaseAudioMode.SOUND,
      source = TransitionTriggerSource.QUICK_TILE,
    )
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val state = app.container.soundSystemController.readSoundState()
    val streams = app.container.soundSystemController.readVolumeStreams()
    val isActive = state.audioMode == BaseAudioMode.SOUND

    tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.icon = Icon.createWithResource(this, R.drawable.ic_widget_volume_up)
    tile.label = AppLabels.Tiles.soundLabel
    tile.subtitle = if (isActive) {
      AppLabels.Tiles.soundActiveSubtitle(streams.ringPercent, state.vibrateEnabled)
    } else {
      AppLabels.Tiles.tapToActivateSubtitle
    }
    tile.updateTile()
  }
}

/**
 * Opção A.2 — Toggle Ortogonal Não-Excludente: `Vibrar` (`vibrateEnabled`).
 * Permanece `STATE_ACTIVE` sempre que `vibrateEnabled == true` (tanto em Som quanto em Silêncio).
 */
class VibrateModeTileService : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    val controller = app.container.soundSystemController
    val currentState = controller.readSoundState()
    if (currentState.audioMode == BaseAudioMode.SILENT &&
      currentState.vibrateEnabled &&
      !controller.hasDndPermission()
    ) {
      openDndPermissionSettings()
      return
    }
    app.container.soundModeRepository.toggleVibrate(
      source = TransitionTriggerSource.QUICK_TILE,
    )
    if (currentState.audioMode == BaseAudioMode.SOUND && !controller.canWriteSystemSettings()) {
      openWriteSettingsPermissionSettings()
    }
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val controller = app.container.soundSystemController
    val state = controller.readSoundState()
    val isActive = state.vibrateEnabled

    tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.icon = Icon.createWithResource(this, R.drawable.ic_widget_vibration)
    tile.label = AppLabels.Tiles.vibrateLabel
    tile.subtitle = if (state.audioMode == BaseAudioMode.SOUND && !controller.canWriteSystemSettings()) {
      AppLabels.Tiles.authorizeWriteSettingsSubtitle
    } else {
      AppLabels.Tiles.vibrateTileSubtitle(state.vibrateEnabled, state.audioMode)
    }
    tile.updateTile()
  }
}

/**
 * Opção A.3 — Bloco Direto: Eixo de Áudio `Silêncio` (`BaseAudioMode.SILENT`), preservando vibração.
 */
class SilentModeTileService : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    val controller = app.container.soundSystemController
    val currentState = controller.readSoundState()
    val targetAudioMode = if (currentState.audioMode == BaseAudioMode.SILENT) {
      BaseAudioMode.SOUND
    } else {
      BaseAudioMode.SILENT
    }
    if (!currentState.vibrateEnabled && !controller.hasDndPermission()) {
      openDndPermissionSettings()
      return
    }
    app.container.soundModeRepository.setBaseAudioMode(
      audioMode = targetAudioMode,
      source = TransitionTriggerSource.QUICK_TILE,
    )
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val controller = app.container.soundSystemController
    val hasDnd = controller.hasDndPermission()
    val state = controller.readSoundState()
    val streams = controller.readVolumeStreams()
    val isActive = state.audioMode == BaseAudioMode.SILENT

    tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.icon = Icon.createWithResource(this, R.drawable.ic_widget_silent)
    tile.label = AppLabels.Tiles.silentLabel
    tile.subtitle = when {
      !state.vibrateEnabled && !hasDnd -> AppLabels.Tiles.authorizeDndSubtitle
      isActive -> AppLabels.Tiles.silentActiveSubtitle(streams.alarmPercent, state.vibrateEnabled)
      else -> AppLabels.Tiles.tapToSilenceSubtitle
    }
    tile.updateTile()
  }
}

/**
 * Opção B — Slots Dinâmicos de Perfis Favoritos (Slot 1 e Slot 2)
 */
abstract class BaseProfileSlotTileService(
  private val slotIndex: Int,
) : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val profiles = app.container.soundModeRepository.profilesStream.first()
    val targetId = when (slotIndex) {
      1 -> prefs.quickTileSlot1ProfileId
      2 -> prefs.quickTileSlot2ProfileId
      3 -> prefs.quickTileSlot3ProfileId
      else -> prefs.quickTileSlot1ProfileId
    }
    val targetProfile = profiles.firstOrNull { it.id == targetId } ?: profiles.firstOrNull() ?: return
    val targetState = targetProfile.resolvedSoundState

    if (targetState.audioMode == BaseAudioMode.SILENT &&
      !targetState.vibrateEnabled &&
      !app.container.soundSystemController.hasDndPermission()
    ) {
      openDndPermissionSettings()
      return
    }

    if (prefs.activeProfileId == targetProfile.id) {
      val fallback = profiles.firstOrNull { it.id == prefs.defaultDayProfileId }
      if (fallback != null && fallback.id != targetProfile.id) {
        app.container.soundModeRepository.activateProfile(
          profile = fallback,
          source = TransitionTriggerSource.QUICK_TILE,
        )
      } else {
        app.container.soundModeRepository.setBaseAudioMode(
          audioMode = BaseAudioMode.SOUND,
          source = TransitionTriggerSource.QUICK_TILE,
        )
      }
    } else {
      app.container.soundModeRepository.activateProfile(
        profile = targetProfile,
        source = TransitionTriggerSource.QUICK_TILE,
      )
    }
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val profiles = app.container.soundModeRepository.profilesStream.first()
    val targetId = when (slotIndex) {
      1 -> prefs.quickTileSlot1ProfileId
      2 -> prefs.quickTileSlot2ProfileId
      3 -> prefs.quickTileSlot3ProfileId
      else -> prefs.quickTileSlot1ProfileId
    }
    val targetProfile = profiles.firstOrNull { it.id == targetId } ?: profiles.firstOrNull()

    if (targetProfile == null) {
      tile.state = Tile.STATE_INACTIVE
      tile.label = AppLabels.Tiles.profileSlotFallbackLabel(slotIndex)
      tile.subtitle = AppLabels.Tiles.configureInAppSubtitle
      tile.updateTile()
      return
    }

    val isActive = prefs.activeProfileId == targetProfile.id
    val targetState = targetProfile.resolvedSoundState
    val iconRes = when (targetState.audioMode) {
      BaseAudioMode.SOUND -> R.drawable.ic_widget_volume_up
      BaseAudioMode.SILENT ->
        if (targetState.vibrateEnabled) R.drawable.ic_widget_vibration else R.drawable.ic_widget_silent
    }

    tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.icon = Icon.createWithResource(this, iconRes)
    tile.label = targetProfile.name
    tile.subtitle = if (isActive) {
      AppLabels.Tiles.profileActiveSubtitle(targetState.title)
    } else {
      targetProfile.summaryLabel()
    }
    tile.updateTile()
  }
}

class ProfileSlot1TileService : BaseProfileSlotTileService(slotIndex = 1)
class ProfileSlot2TileService : BaseProfileSlotTileService(slotIndex = 2)
class ProfileSlot3TileService : BaseProfileSlotTileService(slotIndex = 3)

/**
 * Opção C — Bloco de Timer Rápido (30m -> 1h -> 2h -> Cancelar)
 */
class QuickTimerTileService : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    val now = System.currentTimeMillis()
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val activeTimer = prefs.activeTimer

    if (activeTimer == null) {
      val currentState = app.container.soundSystemController.readSoundState()
      val targetState = if (currentState.audioMode == BaseAudioMode.SOUND) {
        SoundState(audioMode = BaseAudioMode.SILENT, vibrateEnabled = true)
      } else {
        currentState
      }
      lastTapTimestampMillis = now
      app.container.soundModeRepository.startTimedStateSession(
        targetState = targetState,
        durationMinutes = 30,
        nowEpochMillis = now,
      )
    } else if (now - lastTapTimestampMillis <= 8_000L && activeTimer.durationMinutes < 120) {
      lastTapTimestampMillis = now
      val addMinutes = if (activeTimer.durationMinutes < 60) 30 else 60
      app.container.soundModeRepository.extendTimedSession(
        additionalMinutes = addMinutes,
        nowEpochMillis = now,
      )
    } else {
      lastTapTimestampMillis = 0L
      app.container.soundModeRepository.cancelOrRestoreTimedSession()
    }
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val activeTimer = prefs.activeTimer
    val remainingMin = activeTimer?.remainingMinutes(System.currentTimeMillis())

    tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_timer)
    if (activeTimer != null && remainingMin != null && remainingMin > 0) {
      tile.state = Tile.STATE_ACTIVE
      tile.label = AppLabels.Tiles.timerActiveLabel(remainingMin)
      tile.subtitle = AppLabels.Tiles.timerActiveSubtitle(activeTimer.resolvedSoundState.title)
    } else {
      tile.state = Tile.STATE_INACTIVE
      tile.label = AppLabels.Tiles.quickTimerInactiveLabel
      tile.subtitle = AppLabels.Tiles.quickTimerInactiveSubtitle
    }
    tile.updateTile()
  }

  private companion object {
    @Volatile
    var lastTapTimestampMillis: Long = 0L
  }
}

/**
 * Opção D — Bloco Zero Mídia (Mudo Instantâneo de Mídia)
 */
class MediaMuteTileService : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    app.container.soundModeRepository.toggleQuickMediaMute()
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val streams = app.container.soundSystemController.readVolumeStreams()
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val isMediaMuted = streams.mediaPercent == 0

    tile.state = if (isMediaMuted) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_media_mute)
    if (isMediaMuted) {
      val restoreTarget = prefs.savedMediaBeforeMute ?: 50
      tile.label = AppLabels.Tiles.mediaMutedLabel
      tile.subtitle = AppLabels.Tiles.mediaMutedSubtitle(restoreTarget)
    } else {
      tile.label = AppLabels.Tiles.zeroMediaLabel
      tile.subtitle = AppLabels.Tiles.mediaActiveSubtitle(streams.mediaPercent)
    }
    tile.updateTile()
  }
}

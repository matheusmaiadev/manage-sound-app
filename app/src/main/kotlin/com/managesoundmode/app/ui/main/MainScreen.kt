package com.managesoundmode.app.ui.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.managesoundmode.app.domain.model.AppAccessRole
import com.managesoundmode.app.domain.model.AppPermission
import com.managesoundmode.app.domain.model.BaseAudioMode
import com.managesoundmode.app.domain.model.CalendarRule
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.SoundState
import com.managesoundmode.app.domain.model.StreamSortMode
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.system.SmartNotificationManager
import com.managesoundmode.app.system.VolumePreviewScope
import com.managesoundmode.app.tile.QuickTileKind
import com.managesoundmode.app.tile.SoundModeTileService
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppLanguage
import com.managesoundmode.app.ui.common.AppTestTags
import com.managesoundmode.app.ui.components.FirstRunOnboardingDialog
import com.managesoundmode.app.ui.components.PillVolumeSliderRow
import com.managesoundmode.app.ui.components.ProfileEditorBottomSheet
import com.managesoundmode.app.ui.components.ProminentLocationDisclosureDialog
import com.managesoundmode.app.ui.components.SilentPreviewConfirmDialog
import com.managesoundmode.app.ui.components.ZenithSplitModeAndVibrateRow
import com.managesoundmode.app.ui.components.audioModeIcon
import com.managesoundmode.app.ui.components.profileIconForKey
import com.managesoundmode.app.ui.components.streamIcon
import com.managesoundmode.app.ui.theme.LocalZenithIsDark
import com.managesoundmode.app.ui.theme.ManageSoundModeTheme
import com.managesoundmode.app.ui.theme.onActivePillColor
import com.managesoundmode.app.ui.theme.onTonalContainerColor
import com.managesoundmode.app.ui.theme.orbGlowColor
import com.managesoundmode.app.ui.theme.tonalAccentColor
import com.managesoundmode.app.ui.theme.tonalContainerColor
import com.managesoundmode.app.ui.theme.zenithOnActivePillColor
import com.managesoundmode.app.ui.theme.zenithVibrateAccentColor
import com.managesoundmode.app.ui.theme.zenithVibrateContainerColor
import com.managesoundmode.app.widget.SoundModeGlanceWidget

enum class MainAppTab(
  val icon: ImageVector,
  val tag: String,
) {
  NOW(
    icon = Icons.AutoMirrored.Filled.VolumeUp,
    tag = AppTestTags.Tabs.NOW,
  ),
  PROFILES(
    icon = Icons.Default.Style,
    tag = AppTestTags.Tabs.PROFILES,
  ),
  SCHEDULES(
    icon = Icons.Default.Schedule,
    tag = AppTestTags.Tabs.SCHEDULES,
  ),
  LOCATIONS(
    icon = Icons.Default.LocationOn,
    tag = AppTestTags.Tabs.LOCATIONS,
  ),
  ;

  val label: String
    get() = when (this) {
      NOW -> AppLabels.Tabs.nowLabel
      PROFILES -> AppLabels.Tabs.profilesLabel
      SCHEDULES -> AppLabels.Tabs.schedulesLabel
      LOCATIONS -> AppLabels.Tabs.locationsLabel
    }

  val topBarTitle: String
    get() = when (this) {
      NOW -> AppLabels.Tabs.nowTopBarTitle
      PROFILES -> AppLabels.Tabs.profilesTopBarTitle
      SCHEDULES -> AppLabels.Tabs.schedulesTopBarTitle
      LOCATIONS -> AppLabels.Tabs.locationsTopBarTitle
    }
}

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel(factory = MainScreenViewModel.Factory),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  val openAppNotificationSettings: () -> Unit = {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
      putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
      .onFailure {
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = Uri.fromParts("package", context.packageName, null)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(fallback) }
          .onFailure { err -> Log.w("MainScreen", "Falha ao abrir configurações de notificações", err) }
      }
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { isGranted ->
    viewModel.onNotificationPermissionResult(isGranted)
    if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val activity = context.findActivity()
      val shouldShowRationaleAfter = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(
          it,
          Manifest.permission.POST_NOTIFICATIONS,
        )
      } ?: false
      if (!shouldShowRationaleAfter) {
        openAppNotificationSettings()
      }
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { _ ->
    viewModel.onRefreshSystemState()
  }

  val calendarPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { _ ->
    viewModel.loadVisibleCalendars()
    viewModel.onRefreshSystemState()
  }

  LifecycleResumeEffect(Unit) {
    viewModel.onRefreshSystemState()
    onPauseOrDispose {
      viewModel.onStopVolumePreview()
    }
  }

  when (val state = uiState) {
    MainScreenUiState.Loading -> {
      Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
    }

    is MainScreenUiState.Error -> {
      Box(
        modifier = modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = state.message,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.error,
        )
      }
    }

    is MainScreenUiState.Ready -> {
      ManageSoundModeTheme(
        themeMode = state.themeMode,
        dynamicColor = state.useDynamicColor,
      ) {
        MainDashboardContent(
          state = state,
          onSelectMode = viewModel::onSelectMode,
          onSelectAudioMode = viewModel::onSelectAudioMode,
          onToggleVibrate = viewModel::onToggleVibrate,
          onStreamVolumeChange = viewModel::onStreamVolumeChange,
          onActivateProfile = viewModel::onActivateProfile,
          onStartTimer = viewModel::onStartTimer,
          onStartStateTimer = viewModel::onStartStateTimer,
          onExtendTimer = viewModel::onExtendTimer,
          onCancelTimer = viewModel::onCancelTimer,
          onSaveProfile = viewModel::onSaveCurrentAsProfile,
          onUpsertProfile = { profile, applyNow ->
            viewModel.onUpsertProfile(profile, applyNow, showFeedback = true)
          },
          onAutoSaveProfile = { profile ->
            viewModel.onUpsertProfile(profile, applyImmediately = false, showFeedback = false)
          },
          onDismissMessage = viewModel::onDismissMessage,
          onRequestDndPermission = {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
              .onFailure { Log.w("MainScreen", "Falha ao abrir configurações de DND", it) }
          },
          onRequestWriteSettingsPermission = {
            val intent = Intent(
              Settings.ACTION_MANAGE_WRITE_SETTINGS,
              Uri.parse("package:${context.packageName}"),
            ).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
              .onFailure { Log.w("MainScreen", "Falha ao abrir configurações WRITE_SETTINGS", it) }
          },
          onDismissWriteSettingsPrompt = viewModel::onDismissWriteSettingsPrompt,
          onRequestExactAlarmPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              runCatching { context.startActivity(intent) }
            }
          },
          onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.hasNotificationPermission) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              openAppNotificationSettings()
            }
          },
          onReapplyActiveRule = viewModel::onReapplyActiveRule,
          onUpsertScheduleRule = { rule ->
            viewModel.onUpsertScheduleRule(rule, showFeedback = true)
          },
          onAutoSaveScheduleRule = { rule ->
            viewModel.onUpsertScheduleRule(rule, showFeedback = false)
          },
          onToggleScheduleRule = viewModel::onToggleScheduleRule,
          onDeleteScheduleRule = viewModel::onDeleteScheduleRule,
          onUpsertCalendarRule = { rule ->
            viewModel.onUpsertCalendarRule(rule, showFeedback = true)
          },
          onAutoSaveCalendarRule = { rule ->
            viewModel.onUpsertCalendarRule(rule, showFeedback = false)
          },
          onToggleCalendarRule = viewModel::onToggleCalendarRule,
          onDeleteCalendarRule = viewModel::onDeleteCalendarRule,
          onDuplicateCalendarRule = viewModel::onDuplicateCalendarRule,
          onRequestCalendarPermission = {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
          },
          onUpsertLocationRule = { rule ->
            viewModel.onUpsertLocationRule(rule, showFeedback = true)
          },
          onAutoSaveLocationRule = { rule ->
            viewModel.onUpsertLocationRule(rule, showFeedback = false)
          },
          onToggleLocationRule = viewModel::onToggleLocationRule,
          onToggleLocationPresence = viewModel::onToggleLocationPresence,
          onDeleteLocationRule = viewModel::onDeleteLocationRule,
          onSyncLocationsWithGps = viewModel::onSyncLocationsWithGps,
          onShowStatusMessage = viewModel::onShowStatusMessage,
          onAcceptLocationDisclosure = viewModel::onAcceptLocationDisclosure,
          onDeleteCustomProfile = viewModel::onDeleteCustomProfile,
          onSetRestorePolicy = viewModel::onSetRestorePolicy,
          onSetDefaultDayProfileId = viewModel::onSetDefaultDayProfileId,
          onSetThemeMode = viewModel::onSetThemeMode,
          onSetUseDynamicColor = viewModel::onSetUseDynamicColor,
          onSetMuteMediaOnSilent = viewModel::onSetMuteMediaOnSilent,
          onSetPreserveMediaOnBluetooth = viewModel::onSetPreserveMediaOnBluetooth,
          onSetSmartVolumeTriggerEnabled = viewModel::onSetSmartVolumeTriggerEnabled,
          onSetPersistentQuickBarEnabled = { enabled ->
            viewModel.onSetPersistentQuickBarEnabled(enabled)
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.hasNotificationPermission) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          },
          onRequestAddQuickTile = { kind, customLabel ->
            SoundModeTileService.requestAddTile(
              context = context,
              kind = kind,
              customLabel = customLabel,
            ) { message ->
              viewModel.onShowStatusMessage(message)
            }
          },
          onSetQuickTileSlotProfileId = viewModel::onSetQuickTileSlotProfileId,
          onTestSmartNotification = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.hasNotificationPermission) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              viewModel.onShowStatusMessage(AppLabels.Settings.notificationPermissionRequiredMessage)
            } else {
              val previewState = if (state.currentSoundState.audioMode == BaseAudioMode.SOUND) {
                SoundState(audioMode = BaseAudioMode.SILENT, vibrateEnabled = true)
              } else {
                state.currentSoundState
              }
              SmartNotificationManager.showSmartRestorePrompt(
                context = context,
                mode = previewState.toLegacyApproximation(),
                mediaAutoMuted = state.muteMediaOnSilent,
                state = previewState,
              )
              viewModel.onShowStatusMessage(AppLabels.Settings.testSmartNotificationSentMessage)
            }
          },
          onPinHomeWidget = {
            val pinned = SoundModeGlanceWidget.requestPinWidget(context)
            if (!pinned) {
              val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              runCatching { context.startActivity(intent) }
            }
          },
          onClearLogs = viewModel::onClearTransitionLogs,
          onSetAppLanguage = viewModel::onSetAppLanguage,
          onSetAutomationsPaused = viewModel::onSetAutomationsPaused,
          onSetHapticFeedbackEnabled = viewModel::onSetHapticFeedbackEnabled,
          onSetNotifyOnAutoTransition = viewModel::onSetNotifyOnAutoTransition,
          onSetDefaultTimerMinutes = viewModel::onSetDefaultTimerMinutes,
          onResetFactoryDefaults = viewModel::onResetFactoryDefaults,
          onDuplicateProfile = viewModel::onDuplicateProfile,
          onDuplicateScheduleRule = viewModel::onDuplicateScheduleRule,
          onExportBackupJson = viewModel::onExportBackupJson,
          onImportBackupJson = viewModel::onImportBackupJson,
          onRequestLocationPermission = {
            if (!state.hasLocationPermission) {
              locationPermissionLauncher.launch(
                arrayOf(
                  Manifest.permission.ACCESS_FINE_LOCATION,
                  Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
              )
            } else {
              val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              runCatching { context.startActivity(intent) }
            }
          },
          onCompleteOnboarding = viewModel::onCompleteOnboarding,
          onReopenOnboarding = viewModel::onReopenOnboarding,
          onSetAccessRole = viewModel::onSetAccessRole,
          onToggleAccessPermission = viewModel::onToggleAccessPermission,
          onSetAccessPin = viewModel::onSetAccessPin,
          onUnlockAdminWithPin = viewModel::onUnlockAdminWithPin,
          onSetSystemZenRulesEnabled = viewModel::onSetSystemZenRulesEnabled,
          onSyncSystemZenRulesNow = viewModel::onSyncSystemZenRulesNow,
          onSyncBackgroundGeofencesNow = viewModel::onSyncBackgroundGeofencesNow,
          onToggleRingNotificationLink = viewModel::onToggleRingNotificationLink,
          onSetRingNotificationLinked = viewModel::onSetRingNotificationLinked,
          onSetVisibleFixedStreamsCount = viewModel::onSetVisibleFixedStreamsCount,
          onSetStreamSortMode = viewModel::onSetStreamSortMode,
          onMoveStreamInManualOrder = viewModel::onMoveStreamInManualOrder,
          onSetSecondaryDrawerExpanded = viewModel::onSetSecondaryDrawerExpanded,
          onLatchStreamOrder = viewModel::onLatchStreamOrder,
          onToggleVolumePreviewQuick = viewModel::onToggleVolumePreviewQuick,
          onSetVolumePreviewEnabled = viewModel::onSetVolumePreviewEnabled,
          onSetUseSystemSoundsForPreview = viewModel::onSetUseSystemSoundsForPreview,
          onSetSilentPreAdjustEnabled = viewModel::onSetSilentPreAdjustEnabled,
          onToggleStreamPreview = viewModel::onToggleStreamPreview,
          onStartStreamLoopPreview = viewModel::onStartStreamLoopPreview,
          onUpdateStreamLoopPercent = viewModel::onUpdateStreamLoopPercent,
          onStopVolumePreview = viewModel::onStopVolumePreview,
          modifier = modifier,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainDashboardContent(
  state: MainScreenUiState.Ready,
  onSelectMode: (SoundMode) -> Unit,
  onStreamVolumeChange: (VolumeStreamType, Int) -> Unit,
  onActivateProfile: (SoundProfile) -> Unit,
  onStartTimer: (SoundMode, Int) -> Unit,
  onCancelTimer: () -> Unit,
  onSaveProfile: (String, String) -> Unit,
  onDismissMessage: () -> Unit,
  onRequestDndPermission: () -> Unit,
  onSelectAudioMode: (BaseAudioMode) -> Unit = { targetAudio ->
    val nextState = SoundState(audioMode = targetAudio, vibrateEnabled = state.vibrateEnabled)
    onSelectMode(nextState.toLegacyApproximation())
  },
  onToggleVibrate: () -> Unit = {
    val nextState = SoundState(audioMode = state.audioMode, vibrateEnabled = !state.vibrateEnabled)
    onSelectMode(nextState.toLegacyApproximation())
  },
  onStartStateTimer: (SoundState, Int) -> Unit = { targetState, mins ->
    onStartTimer(targetState.toLegacyApproximation(), mins)
  },
  onRequestWriteSettingsPermission: () -> Unit = {},
  onDismissWriteSettingsPrompt: () -> Unit = {},
  onExtendTimer: (Int) -> Unit = {},
  onUpsertProfile: (SoundProfile, Boolean) -> Unit = { profile, _ ->
    onSaveProfile(profile.name, profile.description)
  },
  onAutoSaveProfile: (SoundProfile) -> Unit = { profile ->
    onUpsertProfile(profile, false)
  },
  onRequestExactAlarmPermission: () -> Unit = {},
  onRequestNotificationPermission: () -> Unit = {},
  onReapplyActiveRule: () -> Unit = {},
  onUpsertScheduleRule: (ScheduleRule) -> Unit = {},
  onAutoSaveScheduleRule: (ScheduleRule) -> Unit = { rule ->
    onUpsertScheduleRule(rule)
  },
  onToggleScheduleRule: (ScheduleRule, Boolean) -> Unit = { _, _ -> },
  onDeleteScheduleRule: (String) -> Unit = {},
  onUpsertCalendarRule: (CalendarRule) -> Unit = {},
  onAutoSaveCalendarRule: (CalendarRule) -> Unit = { rule ->
    onUpsertCalendarRule(rule)
  },
  onToggleCalendarRule: (CalendarRule, Boolean) -> Unit = { _, _ -> },
  onDeleteCalendarRule: (String) -> Unit = {},
  onDuplicateCalendarRule: (CalendarRule) -> Unit = {},
  onRequestCalendarPermission: () -> Unit = {},
  onUpsertLocationRule: (LocationRule) -> Unit = {},
  onAutoSaveLocationRule: (LocationRule) -> Unit = { rule ->
    onUpsertLocationRule(rule)
  },
  onToggleLocationRule: (LocationRule, Boolean) -> Unit = { _, _ -> },
  onToggleLocationPresence: (String, Boolean) -> Unit = { _, _ -> },
  onDeleteLocationRule: (String) -> Unit = {},
  onSyncLocationsWithGps: (Double, Double) -> Unit = { _, _ -> },
  onShowStatusMessage: (String) -> Unit = {},
  onAcceptLocationDisclosure: () -> Unit = {},
  onDeleteCustomProfile: (String) -> Unit = {},
  onSetRestorePolicy: (RestorePolicy) -> Unit = {},
  onSetDefaultDayProfileId: (String) -> Unit = {},
  onSetThemeMode: (ThemeMode) -> Unit = {},
  onSetUseDynamicColor: (Boolean) -> Unit = {},
  onSetMuteMediaOnSilent: (Boolean) -> Unit = {},
  onSetPreserveMediaOnBluetooth: (Boolean) -> Unit = {},
  onSetSmartVolumeTriggerEnabled: (Boolean) -> Unit = {},
  onSetPersistentQuickBarEnabled: (Boolean) -> Unit = {},
  onRequestAddQuickTile: (QuickTileKind, String?) -> Unit = { _, _ -> },
  onSetQuickTileSlotProfileId: (Int, String) -> Unit = { _, _ -> },
  onTestSmartNotification: () -> Unit = {},
  onPinHomeWidget: () -> Unit = {},
  onClearLogs: () -> Unit = {},
  onSetAppLanguage: (AppLanguage) -> Unit = { AppLabels.currentLanguage = it },
  onSetAutomationsPaused: (Boolean) -> Unit = {},
  onSetHapticFeedbackEnabled: (Boolean) -> Unit = {},
  onSetNotifyOnAutoTransition: (Boolean) -> Unit = {},
  onSetDefaultTimerMinutes: (Int) -> Unit = {},
  onResetFactoryDefaults: () -> Unit = {},
  onDuplicateProfile: (SoundProfile) -> Unit = {},
  onDuplicateScheduleRule: (ScheduleRule) -> Unit = {},
  onExportBackupJson: ((String) -> Unit) -> Unit = {},
  onImportBackupJson: (String) -> Unit = {},
  onRequestLocationPermission: () -> Unit = {},
  onCompleteOnboarding: () -> Unit = {},
  onReopenOnboarding: () -> Unit = {},
  onSetAccessRole: (AppAccessRole) -> Unit = {},
  onToggleAccessPermission: (AppPermission, Boolean) -> Unit = { _, _ -> },
  onSetAccessPin: (String?) -> Unit = {},
  onUnlockAdminWithPin: (String, () -> Unit) -> Unit = { _, onSuccess -> onSuccess() },
  onSetSystemZenRulesEnabled: (Boolean) -> Unit = {},
  onSyncSystemZenRulesNow: () -> Unit = {},
  onSyncBackgroundGeofencesNow: () -> Unit = {},
  onToggleRingNotificationLink: () -> Unit = {},
  onSetRingNotificationLinked: (Boolean) -> Unit = {},
  onSetVisibleFixedStreamsCount: (Int) -> Unit = {},
  onSetStreamSortMode: (StreamSortMode) -> Unit = {},
  onMoveStreamInManualOrder: (VolumeStreamType, Boolean) -> Unit = { _, _ -> },
  onSetSecondaryDrawerExpanded: (Boolean) -> Unit = {},
  onLatchStreamOrder: () -> Unit = {},
  onToggleVolumePreviewQuick: () -> Unit = {},
  onSetVolumePreviewEnabled: (Boolean) -> Unit = {},
  onSetUseSystemSoundsForPreview: (Boolean) -> Unit = {},
  onSetSilentPreAdjustEnabled: (Boolean) -> Unit = {},
  onToggleStreamPreview: (VolumeStreamType, Int, VolumePreviewScope, Boolean) -> Unit = { _, _, _, _ -> },
  onStartStreamLoopPreview: (VolumeStreamType, Int, VolumePreviewScope, Boolean) -> Unit = { _, _, _, _ -> },
  onUpdateStreamLoopPercent: (VolumeStreamType, Int, VolumePreviewScope) -> Unit = { _, _, _ -> },
  onStopVolumePreview: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  var selectedTab by rememberSaveable { mutableStateOf(MainAppTab.NOW) }
  var showGlobalOverflowMenu by remember { mutableStateOf(false) }
  var showSettingsFullScreen by rememberSaveable { mutableStateOf(false) }
  var globalSettingsDialogCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
  var showGlobalResetConfirmDialog by rememberSaveable { mutableStateOf(false) }
  var showGlobalPinChallengeDialog by rememberSaveable { mutableStateOf(false) }
  var pendingLocationPermissionAfterDisclosure by rememberSaveable { mutableStateOf(false) }

  val isDark = LocalZenithIsDark.current
  val modeAccent = state.audioMode.tonalAccentColor()

  val openAccessControlOrChallenge: () -> Unit = {
    if (state.accessControlConfig.isPinRequired && state.accessControlConfig.activeRole != AppAccessRole.ADMIN) {
      showGlobalPinChallengeDialog = true
    } else {
      globalSettingsDialogCategory = SettingsCategory.ACCESS_CONTROL
    }
  }

  var editingProfile by remember { mutableStateOf<SoundProfile?>(null) }
  var showProfileBottomSheet by rememberSaveable { mutableStateOf(false) }

  var editingScheduleRule by remember { mutableStateOf<ScheduleRule?>(null) }
  var showScheduleBottomSheet by rememberSaveable { mutableStateOf(false) }

  var schedulesSubTab by rememberSaveable { mutableIntStateOf(0) }
  var editingCalendarRule by remember { mutableStateOf<CalendarRule?>(null) }
  var showCalendarRuleEditor by rememberSaveable { mutableStateOf(false) }
  var showCalendarDisclosureDialog by rememberSaveable { mutableStateOf(false) }

  var editingLocationRule by remember { mutableStateOf<LocationRule?>(null) }
  var showLocationDisclosureDialog by rememberSaveable { mutableStateOf(false) }
  var showLocationBottomSheet by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(state.statusMessage, showSettingsFullScreen, globalSettingsDialogCategory) {
    if (showSettingsFullScreen || globalSettingsDialogCategory != null) return@LaunchedEffect
    val msg = state.statusMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    onDismissMessage()
  }

  if (showProfileBottomSheet) {
    ProfileEditorBottomSheet(
      initialProfile = editingProfile,
      defaultMode = state.currentMode,
      defaultSoundState = state.currentSoundState,
      defaultStreams = state.streams,
      streamMinPercents = state.streamMinPercents,
      ringNotificationLinkedDefault = state.ringNotificationLinked,
      activeVolumePreview = state.activeVolumePreview,
      onToggleEditorStreamPreview = { type, percent ->
        onToggleStreamPreview(type, percent, VolumePreviewScope.PROFILE_EDITOR, true)
      },
      onStartEditorStreamLoop = { type, percent ->
        onStartStreamLoopPreview(type, percent, VolumePreviewScope.PROFILE_EDITOR, true)
      },
      onUpdateEditorStreamLoopPercent = { type, percent ->
        onUpdateStreamLoopPercent(type, percent, VolumePreviewScope.PROFILE_EDITOR)
      },
      onStopEditorStreamPreview = onStopVolumePreview,
      onSaveProfile = { profile, applyNow ->
        showProfileBottomSheet = false
        editingProfile = null
        onUpsertProfile(profile, applyNow)
        if (applyNow) {
          onLatchStreamOrder()
          selectedTab = MainAppTab.NOW
        }
      },
      onAutoSaveProfile = onAutoSaveProfile,
      onDeleteProfile = if (state.profiles.size > 1) {
        { profileId ->
          showProfileBottomSheet = false
          editingProfile = null
          onDeleteCustomProfile(profileId)
        }
      } else {
        null
      },
      onDismiss = {
        showProfileBottomSheet = false
        editingProfile = null
      },
    )
  }

  if (showScheduleBottomSheet) {
    ScheduleEditorBottomSheet(
      initialRule = editingScheduleRule,
      profiles = state.profiles,
      onConfirm = { rule ->
        showScheduleBottomSheet = false
        editingScheduleRule = null
        onUpsertScheduleRule(rule)
      },
      onAutoSave = onAutoSaveScheduleRule,
      onDelete = { ruleId ->
        showScheduleBottomSheet = false
        editingScheduleRule = null
        onDeleteScheduleRule(ruleId)
      },
      onDismiss = {
        showScheduleBottomSheet = false
        editingScheduleRule = null
      },
    )
  }

  if (showCalendarDisclosureDialog) {
    CalendarDisclosureDialog(
      onAccept = {
        showCalendarDisclosureDialog = false
        onRequestCalendarPermission()
      },
      onDismiss = {
        showCalendarDisclosureDialog = false
      },
    )
  }

  if (showCalendarRuleEditor) {
    CalendarRuleEditorDialog(
      initialRule = editingCalendarRule,
      profiles = state.profiles,
      availableCalendars = state.availableCalendars,
      onConfirm = { rule ->
        showCalendarRuleEditor = false
        editingCalendarRule = null
        onUpsertCalendarRule(rule)
      },
      onAutoSave = onAutoSaveCalendarRule,
      onDelete = { ruleId ->
        showCalendarRuleEditor = false
        editingCalendarRule = null
        onDeleteCalendarRule(ruleId)
      },
      onDismiss = {
        showCalendarRuleEditor = false
        editingCalendarRule = null
      },
    )
  }

  val handleLocationPermissionRequest: () -> Unit = {
    if (!state.hasAcceptedLocationDisclosure && !state.hasLocationPermission) {
      pendingLocationPermissionAfterDisclosure = true
      showLocationDisclosureDialog = true
    } else {
      onRequestLocationPermission()
    }
  }

  if (!state.hasCompletedOnboarding) {
    FirstRunOnboardingDialog(
      state = state,
      onCompleteOnboarding = onCompleteOnboarding,
      onSetAppLanguage = onSetAppLanguage,
      onSetThemeMode = onSetThemeMode,
      onRequestDndPermission = onRequestDndPermission,
      onRequestNotificationPermission = onRequestNotificationPermission,
      onRequestExactAlarmPermission = onRequestExactAlarmPermission,
      onRequestLocationPermission = handleLocationPermissionRequest,
      onSetDefaultDayProfileId = onSetDefaultDayProfileId,
      onSetMuteMediaOnSilent = onSetMuteMediaOnSilent,
      onSetSmartVolumeTriggerEnabled = onSetSmartVolumeTriggerEnabled,
      onSetPersistentQuickBarEnabled = onSetPersistentQuickBarEnabled,
    )
  }

  if (showLocationDisclosureDialog) {
    ProminentLocationDisclosureDialog(
      onAcceptAndContinue = {
        showLocationDisclosureDialog = false
        onAcceptLocationDisclosure()
        if (pendingLocationPermissionAfterDisclosure) {
          pendingLocationPermissionAfterDisclosure = false
          onRequestLocationPermission()
        } else {
          editingLocationRule = null
          showLocationBottomSheet = true
        }
      },
      onDismiss = {
        showLocationDisclosureDialog = false
        pendingLocationPermissionAfterDisclosure = false
      },
    )
  }

  if (showLocationBottomSheet) {
    LocationEditorBottomSheet(
      initialRule = editingLocationRule,
      profiles = state.profiles,
      knownWifiSsids = state.locationRules
        .mapNotNull { it.wifiSsid.trim().takeIf(String::isNotEmpty) }
        .distinct(),
      onConfirm = { rule ->
        showLocationBottomSheet = false
        editingLocationRule = null
        onUpsertLocationRule(rule)
      },
      onAutoSave = onAutoSaveLocationRule,
      onTogglePresence = onToggleLocationPresence,
      onDelete = { ruleId ->
        showLocationBottomSheet = false
        editingLocationRule = null
        onDeleteLocationRule(ruleId)
      },
      onDismiss = {
        showLocationBottomSheet = false
        editingLocationRule = null
      },
    )
  }

  if (showGlobalPinChallengeDialog) {
    AccessPinChallengeDialog(
      config = state.accessControlConfig,
      onDismiss = { showGlobalPinChallengeDialog = false },
      onVerifyAndOpenSubScreen = {
        showGlobalPinChallengeDialog = false
        globalSettingsDialogCategory = SettingsCategory.ACCESS_CONTROL
      },
      onUnlockAdminWithPin = { pin ->
        onUnlockAdminWithPin(pin) {
          showGlobalPinChallengeDialog = false
        }
      },
      onShowStatusMessage = onShowStatusMessage,
    )
  }

  if (showGlobalResetConfirmDialog) {
    FactoryResetConfirmDialog(
      onConfirm = {
        showGlobalResetConfirmDialog = false
        globalSettingsDialogCategory = null
        onResetFactoryDefaults()
      },
      onDismiss = { showGlobalResetConfirmDialog = false },
    )
  }

  if (showSettingsFullScreen) {
    Dialog(
      onDismissRequest = {
        showSettingsFullScreen = false
        onLatchStreamOrder()
      },
      properties = DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnClickOutside = false,
        dismissOnBackPress = true,
      ),
    ) {
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
          TopAppBar(
            navigationIcon = {
              IconButton(
                onClick = {
                  showSettingsFullScreen = false
                  onLatchStreamOrder()
                },
                modifier = Modifier.testTag(AppTestTags.Settings.CLOSE_SETTINGS_BUTTON),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = AppLabels.Common.back,
                )
              }
            },
            title = {
              Text(
                text = AppLabels.Tabs.settingsTopBarTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
              )
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
              titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
          )
        },
      ) { settingsPadding ->
        SettingsAndProfilesTabContent(
          state = state,
          onSetRestorePolicy = onSetRestorePolicy,
          onSetDefaultDayProfileId = onSetDefaultDayProfileId,
          onSetThemeMode = onSetThemeMode,
          onSetUseDynamicColor = onSetUseDynamicColor,
          onSetAppLanguage = onSetAppLanguage,
          onSetAutomationsPaused = onSetAutomationsPaused,
          onSetHapticFeedbackEnabled = onSetHapticFeedbackEnabled,
          onSetNotifyOnAutoTransition = onSetNotifyOnAutoTransition,
          onSetDefaultTimerMinutes = onSetDefaultTimerMinutes,
          onSetMuteMediaOnSilent = onSetMuteMediaOnSilent,
          onSetPreserveMediaOnBluetooth = onSetPreserveMediaOnBluetooth,
          onSetSmartVolumeTriggerEnabled = onSetSmartVolumeTriggerEnabled,
          onSetPersistentQuickBarEnabled = onSetPersistentQuickBarEnabled,
          onRequestAddQuickTile = onRequestAddQuickTile,
          onSetQuickTileSlotProfileId = onSetQuickTileSlotProfileId,
          onTestSmartNotification = onTestSmartNotification,
          onPinHomeWidget = onPinHomeWidget,
          onRequestDndPermission = onRequestDndPermission,
          onRequestWriteSettingsPermission = onRequestWriteSettingsPermission,
          onRequestExactAlarmPermission = onRequestExactAlarmPermission,
          onRequestNotificationPermission = onRequestNotificationPermission,
          onClearLogs = onClearLogs,
          onDismissMessage = onDismissMessage,
          onResetFactoryDefaults = onResetFactoryDefaults,
          onExportBackupJson = onExportBackupJson,
          onImportBackupJson = onImportBackupJson,
          onShowStatusMessage = onShowStatusMessage,
          onRequestLocationPermission = handleLocationPermissionRequest,
          onReopenOnboarding = {
            showSettingsFullScreen = false
            onReopenOnboarding()
          },
          onSetAccessRole = onSetAccessRole,
          onToggleAccessPermission = onToggleAccessPermission,
          onSetAccessPin = onSetAccessPin,
          onUnlockAdminWithPin = onUnlockAdminWithPin,
          onSetSystemZenRulesEnabled = onSetSystemZenRulesEnabled,
          onSyncSystemZenRulesNow = onSyncSystemZenRulesNow,
          onSetRingNotificationLinked = onSetRingNotificationLinked,
          onSetVisibleFixedStreamsCount = onSetVisibleFixedStreamsCount,
          onSetStreamSortMode = onSetStreamSortMode,
          onMoveStreamInManualOrder = onMoveStreamInManualOrder,
          onSetVolumePreviewEnabled = onSetVolumePreviewEnabled,
          onSetUseSystemSoundsForPreview = onSetUseSystemSoundsForPreview,
          onSetSilentPreAdjustEnabled = onSetSilentPreAdjustEnabled,
          onRequestCalendarPermission = onRequestCalendarPermission,
          modifier = Modifier.padding(settingsPadding),
        )
      }
    }
  }

  if (globalSettingsDialogCategory != null) {
    SettingsCategoryFullScreenDialog(
      category = globalSettingsDialogCategory!!,
      state = state,
      onDismiss = {
        globalSettingsDialogCategory = null
        onLatchStreamOrder()
      },
      onSetRestorePolicy = onSetRestorePolicy,
      onSetDefaultDayProfileId = onSetDefaultDayProfileId,
      onSetThemeMode = onSetThemeMode,
      onSetUseDynamicColor = onSetUseDynamicColor,
      onSetAppLanguage = onSetAppLanguage,
      onSetAutomationsPaused = onSetAutomationsPaused,
      onSetHapticFeedbackEnabled = onSetHapticFeedbackEnabled,
      onSetNotifyOnAutoTransition = onSetNotifyOnAutoTransition,
      onSetDefaultTimerMinutes = onSetDefaultTimerMinutes,
      onSetMuteMediaOnSilent = onSetMuteMediaOnSilent,
      onSetPreserveMediaOnBluetooth = onSetPreserveMediaOnBluetooth,
      onSetSmartVolumeTriggerEnabled = onSetSmartVolumeTriggerEnabled,
      onTestSmartNotification = onTestSmartNotification,
      onSetPersistentQuickBarEnabled = onSetPersistentQuickBarEnabled,
      onRequestAddQuickTile = onRequestAddQuickTile,
      onSetQuickTileSlotProfileId = onSetQuickTileSlotProfileId,
      onPinHomeWidget = onPinHomeWidget,
      onRequestDndPermission = onRequestDndPermission,
      onRequestWriteSettingsPermission = onRequestWriteSettingsPermission,
      onRequestExactAlarmPermission = onRequestExactAlarmPermission,
      onRequestNotificationPermission = onRequestNotificationPermission,
      onClearLogs = onClearLogs,
      onDismissMessage = onDismissMessage,
      onRequestFactoryResetConfirm = { showGlobalResetConfirmDialog = true },
      onExportBackupJson = onExportBackupJson,
      onImportBackupJson = onImportBackupJson,
      onShowStatusMessage = onShowStatusMessage,
      onRequestLocationPermission = handleLocationPermissionRequest,
      onReopenOnboarding = {
        globalSettingsDialogCategory = null
        onReopenOnboarding()
      },
      onSetAccessRole = onSetAccessRole,
      onToggleAccessPermission = onToggleAccessPermission,
      onSetAccessPin = onSetAccessPin,
      onSetSystemZenRulesEnabled = onSetSystemZenRulesEnabled,
      onSyncSystemZenRulesNow = onSyncSystemZenRulesNow,
      onSetRingNotificationLinked = onSetRingNotificationLinked,
      onSetVisibleFixedStreamsCount = onSetVisibleFixedStreamsCount,
      onSetStreamSortMode = onSetStreamSortMode,
      onMoveStreamInManualOrder = onMoveStreamInManualOrder,
      onSetVolumePreviewEnabled = onSetVolumePreviewEnabled,
      onSetUseSystemSoundsForPreview = onSetUseSystemSoundsForPreview,
      onSetSilentPreAdjustEnabled = onSetSilentPreAdjustEnabled,
      onRequestCalendarPermission = onRequestCalendarPermission,
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(modeAccent),
            )
            Text(
              text = selectedTab.topBarTitle,
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        actions = {
          if (state.accessControlConfig.isRestricted || state.accessControlConfig.isPinRequired) {
            Surface(
              shape = RoundedCornerShape(50),
              color = if (state.accessControlConfig.isRestricted) {
                MaterialTheme.colorScheme.tertiaryContainer
              } else {
                MaterialTheme.colorScheme.secondaryContainer
              },
              border = BorderStroke(
                width = 1.dp,
                color = if (state.accessControlConfig.isRestricted) {
                  MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                } else {
                  MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                },
              ),
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = openAccessControlOrChallenge)
                .testTag(AppTestTags.TopBar.ROLE_BADGE_CHIP),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = state.accessControlConfig.activeRole.shortBadge,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }

          // Alternador rápido Dark (Noite Boreal) / Light (Alvorada Nórdica)
          IconButton(
            onClick = {
              val nextTheme = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
              onSetThemeMode(nextTheme)
            },
            modifier = Modifier.testTag(AppTestTags.TopBar.QUICK_THEME_TOGGLE),
          ) {
            Icon(
              imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = if (isDark) {
                AppLabels.TopBar.enableLightMode
              } else {
                AppLabels.TopBar.enableDarkMode
              },
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Box {
            IconButton(
              onClick = { showGlobalOverflowMenu = true },
              modifier = Modifier.testTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON),
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = AppLabels.TopBar.overflowMenuDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            DropdownMenu(
              expanded = showGlobalOverflowMenu,
              onDismissRequest = { showGlobalOverflowMenu = false },
            ) {
              DropdownMenuItem(
                text = {
                  Text(
                    text = AppLabels.TopBar.openSettingsMenu,
                    fontWeight = FontWeight.SemiBold,
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  showSettingsFullScreen = true
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_SETTINGS),
              )

              HorizontalDivider()

              DropdownMenuItem(
                text = {
                  Text(
                    if (state.automationsPaused) {
                      AppLabels.TopBar.resumeAutomationsMenu
                    } else {
                      AppLabels.TopBar.pauseAutomationsMenu
                    },
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = if (state.automationsPaused) {
                      Icons.Default.PlayCircleFilled
                    } else {
                      Icons.Default.PauseCircleFilled
                    },
                    contentDescription = null,
                    tint = if (state.automationsPaused) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    },
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  onSetAutomationsPaused(!state.automationsPaused)
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_PAUSE_AUTOMATIONS),
              )

              DropdownMenuItem(
                text = { Text(AppLabels.TopBar.transitionHistoryMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  globalSettingsDialogCategory = SettingsCategory.HISTORY
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_HISTORY),
              )

              DropdownMenuItem(
                text = { Text(AppLabels.TopBar.permissionsDiagnosticMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  globalSettingsDialogCategory = SettingsCategory.PERMISSIONS
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_PERMISSIONS),
              )

              DropdownMenuItem(
                text = { Text(AppLabels.TopBar.accessControlMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  openAccessControlOrChallenge()
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_ACCESS_CONTROL),
              )

              DropdownMenuItem(
                text = { Text(AppLabels.TopBar.setupWizardMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  onReopenOnboarding()
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_SETUP_WIZARD),
              )

              HorizontalDivider()

              DropdownMenuItem(
                text = {
                  Text(
                    text = AppLabels.TopBar.restoreFactoryDefaultsMenu,
                    color = MaterialTheme.colorScheme.error,
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  if (!state.accessControlConfig.hasPermission(AppPermission.BACKUP_AND_FACTORY_RESET)) {
                    onShowStatusMessage(
                      AppLabels.AccessControl.permissionDeniedMessage(AppPermission.BACKUP_AND_FACTORY_RESET),
                    )
                  } else {
                    showGlobalResetConfirmDialog = true
                  }
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_RESTORE_DEFAULTS),
              )

              DropdownMenuItem(
                text = { Text(AppLabels.TopBar.aboutAndPrivacyMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  globalSettingsDialogCategory = SettingsCategory.ABOUT
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_ABOUT),
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
      )
    },
    floatingActionButton = {
      when (selectedTab) {
        MainAppTab.PROFILES -> {
          ExtendedFloatingActionButton(
            onClick = {
              if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_PROFILES)) {
                onShowStatusMessage(
                  AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_PROFILES),
                )
              } else {
                editingProfile = null
                showProfileBottomSheet = true
              }
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = modeAccent,
            contentColor = state.currentMode.onActivePillColor(),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(AppLabels.Fab.newProfile, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag(AppTestTags.Fab.SAVE_PROFILE_BUTTON),
          )
        }
        MainAppTab.SCHEDULES -> {
          if (schedulesSubTab == 0) {
            ExtendedFloatingActionButton(
              onClick = {
                if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_SCHEDULES)) {
                  onShowStatusMessage(
                    AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_SCHEDULES),
                  )
                } else {
                  editingScheduleRule = null
                  showScheduleBottomSheet = true
                }
              },
              shape = RoundedCornerShape(22.dp),
              containerColor = modeAccent,
              contentColor = state.currentMode.onActivePillColor(),
              icon = { Icon(Icons.Default.Add, contentDescription = null) },
              text = { Text(AppLabels.Fab.newSchedule, fontWeight = FontWeight.Bold) },
              modifier = Modifier.testTag(AppTestTags.Fab.ADD_SCHEDULE_FAB),
            )
          } else {
            ExtendedFloatingActionButton(
              onClick = {
                if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_SCHEDULES)) {
                  onShowStatusMessage(
                    AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_SCHEDULES),
                  )
                } else if (!state.hasReadCalendarPermission) {
                  showCalendarDisclosureDialog = true
                } else {
                  editingCalendarRule = null
                  showCalendarRuleEditor = true
                }
              },
              shape = RoundedCornerShape(22.dp),
              containerColor = modeAccent,
              contentColor = state.currentMode.onActivePillColor(),
              icon = { Icon(Icons.Default.Add, contentDescription = null) },
              text = { Text(AppLabels.Fab.newCalendarRule, fontWeight = FontWeight.Bold) },
              modifier = Modifier.testTag(AppTestTags.Fab.ADD_CALENDAR_RULE_FAB),
            )
          }
        }
        MainAppTab.LOCATIONS -> {
          ExtendedFloatingActionButton(
            onClick = {
              if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_LOCATIONS)) {
                onShowStatusMessage(
                  AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_LOCATIONS),
                )
              } else if (!state.hasAcceptedLocationDisclosure) {
                showLocationDisclosureDialog = true
              } else {
                editingLocationRule = null
                showLocationBottomSheet = true
              }
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = modeAccent,
            contentColor = state.currentMode.onActivePillColor(),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(AppLabels.Fab.newLocation, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag(AppTestTags.Fab.ADD_LOCATION_FAB),
          )
        }
        else -> {}
      }
    },
    bottomBar = {
      ZenithFloatingPillNavigationBar(
        selectedTab = selectedTab,
        activeAccent = modeAccent,
        onSelectTab = { nextTab ->
          if (selectedTab != nextTab) {
            onStopVolumePreview()
          }
          if (nextTab == MainAppTab.NOW && selectedTab != MainAppTab.NOW) {
            onLatchStreamOrder()
          }
          selectedTab = nextTab
        },
      )
    },
  ) { innerPadding ->
    when (selectedTab) {
      MainAppTab.NOW -> {
        NowTabContent(
          state = state,
          onSelectAudioMode = onSelectAudioMode,
          onToggleVibrate = onToggleVibrate,
          onStreamVolumeChange = onStreamVolumeChange,
          onToggleRingNotificationLink = onToggleRingNotificationLink,
          onSetSecondaryDrawerExpanded = onSetSecondaryDrawerExpanded,
          onToggleVolumePreviewQuick = onToggleVolumePreviewQuick,
          onToggleStreamPreview = onToggleStreamPreview,
          onStartStreamLoopPreview = onStartStreamLoopPreview,
          onUpdateStreamLoopPercent = onUpdateStreamLoopPercent,
          onStopVolumePreview = onStopVolumePreview,
          onOpenProfilesTab = {
            onStopVolumePreview()
            selectedTab = MainAppTab.PROFILES
          },
          onStartStateTimer = onStartStateTimer,
          onExtendTimer = onExtendTimer,
          onCancelTimer = onCancelTimer,
          onRequestDndPermission = onRequestDndPermission,
          onRequestWriteSettingsPermission = onRequestWriteSettingsPermission,
          onDismissWriteSettingsPrompt = onDismissWriteSettingsPrompt,
          onReapplyActiveRule = onReapplyActiveRule,
          modifier = Modifier.padding(innerPadding),
        )
      }

      MainAppTab.PROFILES -> {
        ProfilesTabContent(
          state = state,
          onActivateProfile = { profile ->
            onActivateProfile(profile)
            if (state.accessControlConfig.hasPermission(AppPermission.CHANGE_SOUND_AND_TIMER)) {
              onLatchStreamOrder()
              selectedTab = MainAppTab.NOW
            }
          },
          onEditProfile = { profile ->
            if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_PROFILES)) {
              onShowStatusMessage(
                AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_PROFILES),
              )
            } else {
              editingProfile = profile
              showProfileBottomSheet = true
            }
          },
          onDuplicateProfile = onDuplicateProfile,
          onDeleteProfile = onDeleteCustomProfile,
          onSetDefaultDayProfileId = onSetDefaultDayProfileId,
          modifier = Modifier.padding(innerPadding),
        )
      }

      MainAppTab.SCHEDULES -> {
        SchedulesTabContent(
          scheduleRules = state.scheduleRules,
          calendarRules = state.calendarRules,
          profiles = state.profiles,
          canScheduleExactAlarms = state.canScheduleExactAlarms,
          hasReadCalendarPermission = state.hasReadCalendarPermission,
          availableCalendars = state.availableCalendars,
          selectedSubTab = schedulesSubTab,
          onSelectSubTab = { schedulesSubTab = it },
          onToggleRule = onToggleScheduleRule,
          onEditRule = { rule ->
            if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_SCHEDULES)) {
              onShowStatusMessage(
                AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_SCHEDULES),
              )
            } else {
              editingScheduleRule = rule
              showScheduleBottomSheet = true
            }
          },
          onDuplicateRule = onDuplicateScheduleRule,
          onDeleteRule = onDeleteScheduleRule,
          onToggleCalendarRule = onToggleCalendarRule,
          onEditCalendarRule = { rule ->
            if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_SCHEDULES)) {
              onShowStatusMessage(
                AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_SCHEDULES),
              )
            } else {
              editingCalendarRule = rule
              showCalendarRuleEditor = true
            }
          },
          onDeleteCalendarRule = onDeleteCalendarRule,
          onDuplicateCalendarRule = onDuplicateCalendarRule,
          onRequestExactAlarmPermission = onRequestExactAlarmPermission,
          onRequestCalendarPermission = {
            if (!state.hasReadCalendarPermission) {
              showCalendarDisclosureDialog = true
            } else {
              onRequestCalendarPermission()
            }
          },
          modifier = Modifier.padding(innerPadding),
        )
      }

      MainAppTab.LOCATIONS -> {
        LocationsTabContent(
          locationRules = state.locationRules,
          profiles = state.profiles,
          onToggleRule = onToggleLocationRule,
          onTogglePresence = onToggleLocationPresence,
          onEditRule = { rule ->
            if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_LOCATIONS)) {
              onShowStatusMessage(
                AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_LOCATIONS),
              )
            } else {
              editingLocationRule = rule
              showLocationBottomSheet = true
            }
          },
          onDeleteRule = onDeleteLocationRule,
          onSyncGpsCoordinates = onSyncLocationsWithGps,
          onShowStatusMessage = onShowStatusMessage,
          hasAcceptedLocationDisclosure = state.hasAcceptedLocationDisclosure,
          onAcceptLocationDisclosure = onAcceptLocationDisclosure,
          onSyncBackgroundGeofencesNow = onSyncBackgroundGeofencesNow,
          modifier = Modifier.padding(innerPadding),
        )
      }
    }
  }
}

/**
 * Barra de Navegação Inferior em Pílula Flutuante ZENITH (`Floating Glass Pill Dock`).
 */
@Composable
private fun ZenithFloatingPillNavigationBar(
  selectedTab: MainAppTab,
  activeAccent: Color,
  onSelectTab: (MainAppTab) -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(30.dp),
      color = MaterialTheme.colorScheme.surfaceContainer,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        MainAppTab.entries.forEach { tab ->
          val selected = selectedTab == tab
          val pillBg by animateColorAsState(
            targetValue = if (selected) {
              activeAccent.copy(alpha = 0.16f)
            } else {
              Color.Transparent
            },
            animationSpec = tween(180),
            label = "zenithNavTabBg_${tab.name}",
          )
          val itemColor by animateColorAsState(
            targetValue = if (selected) {
              activeAccent
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
            animationSpec = tween(180),
            label = "zenithNavTabContent_${tab.name}",
          )

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(24.dp))
              .background(pillBg)
              .clickable { onSelectTab(tab) }
              .padding(vertical = 8.dp, horizontal = 4.dp)
              .testTag(tab.tag),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = itemColor,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = itemColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NowTabContent(
  state: MainScreenUiState.Ready,
  onSelectAudioMode: (BaseAudioMode) -> Unit,
  onToggleVibrate: () -> Unit,
  onStreamVolumeChange: (VolumeStreamType, Int) -> Unit,
  onToggleRingNotificationLink: () -> Unit,
  onSetSecondaryDrawerExpanded: (Boolean) -> Unit,
  onToggleVolumePreviewQuick: () -> Unit,
  onToggleStreamPreview: (VolumeStreamType, Int, VolumePreviewScope, Boolean) -> Unit,
  onStartStreamLoopPreview: (VolumeStreamType, Int, VolumePreviewScope, Boolean) -> Unit,
  onUpdateStreamLoopPercent: (VolumeStreamType, Int, VolumePreviewScope) -> Unit,
  onStopVolumePreview: () -> Unit,
  onOpenProfilesTab: () -> Unit,
  onStartStateTimer: (SoundState, Int) -> Unit,
  onExtendTimer: (Int) -> Unit,
  onCancelTimer: () -> Unit,
  onRequestDndPermission: () -> Unit,
  onRequestWriteSettingsPermission: () -> Unit,
  onDismissWriteSettingsPrompt: () -> Unit,
  onReapplyActiveRule: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val activeProfile = state.profiles.firstOrNull { it.id == state.activeProfileId }

  DisposableEffect(Unit) {
    onDispose {
      onStopVolumePreview()
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag(AppTestTags.Now.LIST),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    if (!state.hasDndPermission) {
      item {
        DndPermissionBanner(onRequestDndPermission = onRequestDndPermission)
      }
    }

    if (state.showWriteSettingsPrompt && !state.canWriteSystemSettings) {
      item {
        WriteSettingsPermissionBanner(
          onRequestWriteSettingsPermission = onRequestWriteSettingsPermission,
          onDismiss = onDismissWriteSettingsPrompt,
        )
      }
    }

    if (state.isManuallyOverridden) {
      item {
        ManualOverrideBanner(onReapplyActiveRule = onReapplyActiveRule)
      }
    }

    // Bloco 1: Orbe Acústico ZENITH + Split Capsule 2+1 ([Som | Silêncio] + [Vibrar]) + Timer Contextual
    item {
      UnifiedHeroAndModeCard(
        soundState = state.currentSoundState,
        mediaPercent = state.streams.mediaPercent,
        activeProfile = activeProfile,
        activeTimer = state.activeTimer,
        activeRuleTitle = state.activeRuleTitle,
        activeRuleSubtitle = state.activeRuleSubtitle,
        onSelectAudioMode = { audioMode ->
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onSelectAudioMode(audioMode)
        },
        onToggleVibrate = {
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onToggleVibrate()
        },
        onOpenProfilesTab = onOpenProfilesTab,
        onStartTimer = { minutes ->
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          val targetState = if (state.audioMode == BaseAudioMode.SOUND) {
            SoundState(audioMode = BaseAudioMode.SILENT, vibrateEnabled = state.vibrateEnabled)
          } else {
            state.currentSoundState
          }
          onStartStateTimer(targetState, minutes)
        },
        onExtendTimer = { addMinutes ->
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onExtendTimer(addMinutes)
        },
        onCancelTimer = {
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onCancelTimer()
        },
      )
    }

    // Bloco 2: 7 Canais de Volume ZENITH (Barras Fixas 1..7 + Gaveta Expansível Inteligente)
    item {
      VolumeStreamsCard(
        state = state,
        onStreamVolumeChange = onStreamVolumeChange,
        onToggleRingNotificationLink = onToggleRingNotificationLink,
        onToggleSecondaryDrawer = onSetSecondaryDrawerExpanded,
        onToggleVolumePreviewQuick = onToggleVolumePreviewQuick,
        onToggleStreamPreview = onToggleStreamPreview,
        onStartStreamLoopPreview = onStartStreamLoopPreview,
        onUpdateStreamLoopPercent = onUpdateStreamLoopPercent,
        onStopVolumePreview = onStopVolumePreview,
      )
    }
  }
}

/**
 * Orbe Acústico Respirante ZENITH (`ZenithOrbHeroCard`) + Split Capsule `2 + 1` + Gaveta de Timer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnifiedHeroAndModeCard(
  soundState: SoundState,
  mediaPercent: Int,
  activeProfile: SoundProfile?,
  activeTimer: TimedModeSession?,
  activeRuleTitle: String?,
  activeRuleSubtitle: String?,
  onSelectAudioMode: (BaseAudioMode) -> Unit,
  onToggleVibrate: () -> Unit,
  onOpenProfilesTab: () -> Unit,
  onStartTimer: (Int) -> Unit,
  onExtendTimer: (Int) -> Unit,
  onCancelTimer: () -> Unit,
) {
  val animatedAccent by animateColorAsState(
    targetValue = soundState.audioMode.tonalAccentColor(),
    animationSpec = tween(durationMillis = 280),
    label = "zenithHeroAccent",
  )
  val animatedContainer by animateColorAsState(
    targetValue = soundState.audioMode.tonalContainerColor(),
    animationSpec = tween(durationMillis = 280),
    label = "zenithHeroContainer",
  )
  val animatedOnContainer by animateColorAsState(
    targetValue = soundState.audioMode.onTonalContainerColor(),
    animationSpec = tween(durationMillis = 280),
    label = "zenithHeroOnContainer",
  )
  val animatedGlow by animateColorAsState(
    targetValue = soundState.audioMode.orbGlowColor(),
    animationSpec = tween(durationMillis = 280),
    label = "zenithHeroGlow",
  )
  val vibrateAccent = zenithVibrateAccentColor()
  val vibrateContainer = zenithVibrateContainerColor()

  val showContextualTimerRow = soundState.audioMode == BaseAudioMode.SILENT || activeTimer != null

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(AppTestTags.Now.HERO_MODE_CARD),
    shape = RoundedCornerShape(32.dp),
    border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.28f)),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Linha Superior: Badge de Status Atmosférico + Chip de Cena/Perfil Ativo
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = animatedContainer,
          border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.30f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(animatedAccent),
            )
            Text(
              text = activeRuleTitle ?: "ZENITH ACOUSTIC",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = animatedOnContainer,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Chip Compacto do Perfil Ativo (Tocar abre a aba Perfis)
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onOpenProfilesTab)
            .testTag(AppTestTags.Now.ACTIVE_PROFILE_CHIP),
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = activeProfile?.let { profileIconForKey(it.iconKey) } ?: Icons.Default.Style,
              contentDescription = null,
              tint = animatedAccent,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = activeProfile?.name ?: AppLabels.Tabs.profilesLabel,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(15.dp),
            )
          }
        }
      }

      // Centro Gravitacional: Orbe Acústico ZENITH + Anel Tracejado Âmbar & Badge Satélite de Vibração
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(148.dp),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.size(148.dp)) {
          val center = Offset(size.width / 2f, size.height / 2f)
          val maxR = size.minDimension / 2f

          // Halo radial atmosférico
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                animatedGlow.copy(alpha = 0.34f),
                animatedGlow.copy(alpha = 0.10f),
                Color.Transparent,
              ),
              center = center,
              radius = maxR,
            ),
            radius = maxR,
            center = center,
          )

          // Anel acústico externo tracejado (Solar Amber quando vibrateEnabled == true)
          val outerRingColor = if (soundState.vibrateEnabled) {
            vibrateAccent.copy(alpha = 0.55f)
          } else {
            animatedAccent.copy(alpha = 0.22f)
          }
          drawCircle(
            color = outerRingColor,
            radius = maxR * 0.90f,
            center = center,
            style = Stroke(
              width = if (soundState.vibrateEnabled) 1.6.dp.toPx() else 1.2.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            ),
          )

          // Anel acústico intermediário
          drawCircle(
            color = animatedAccent.copy(alpha = 0.36f),
            radius = maxR * 0.72f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
          )

          // Núcleo esférico do Orbe Zenith
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                animatedAccent.copy(alpha = 0.42f),
                animatedContainer,
              ),
              center = Offset(center.x - maxR * 0.14f, center.y - maxR * 0.14f),
              radius = maxR * 0.56f,
            ),
            radius = maxR * 0.54f,
            center = center,
          )

          drawCircle(
            color = animatedAccent.copy(alpha = 0.75f),
            radius = maxR * 0.54f,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
          )
        }

        // Ícone central único do eixo de áudio + mini-badge satélite vetorial quando vibrateEnabled == true
        Box(
          modifier = Modifier.size(84.dp),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier
              .size(78.dp)
              .clip(CircleShape)
              .clickable {
                val nextAudio = if (soundState.audioMode == BaseAudioMode.SOUND) {
                  BaseAudioMode.SILENT
                } else {
                  BaseAudioMode.SOUND
                }
                onSelectAudioMode(nextAudio)
              },
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = soundState.audioMode.audioModeIcon(),
              contentDescription = null,
              tint = animatedOnContainer,
              modifier = Modifier.size(34.dp),
            )
          }

          if (soundState.vibrateEnabled) {
            Surface(
              shape = CircleShape,
              color = vibrateContainer,
              border = BorderStroke(1.dp, vibrateAccent.copy(alpha = 0.70f)),
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .testTag(AppTestTags.Now.ORB_VIBRATE_BADGE),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Vibration,
                  contentDescription = null,
                  tint = vibrateAccent,
                  modifier = Modifier.size(13.dp),
                )
              }
            }
          }
        }
      }

      // Tipografia Central do Estado Acústico (4 estados combinados)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = soundState.title,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val cleanSubtitle = when {
          soundState.audioMode == BaseAudioMode.SILENT ->
            AppLabels.Now.modeWithMediaSubtitle(soundState.subtitle, mediaPercent)
          activeRuleSubtitle != null -> activeRuleSubtitle
          else -> soundState.subtitle
        }
        Text(
          text = cleanSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Split Capsule `2 + 1`: [Som | Silêncio] + [Vibrar] independente
      ZenithSplitModeAndVibrateRow(
        audioMode = soundState.audioMode,
        vibrateEnabled = soundState.vibrateEnabled,
        onSelectAudioMode = onSelectAudioMode,
        onToggleVibrate = onToggleVibrate,
        soundButtonTag = AppTestTags.Now.AUDIO_MODE_SOUND_BUTTON,
        silentButtonTag = AppTestTags.Now.AUDIO_MODE_SILENT_BUTTON,
        vibrateButtonTag = AppTestTags.Now.VIBRATE_TOGGLE_BUTTON,
      )

      // Gaveta Contextual de Timer: SÓ aparece em Vibrar ou Silencioso (ou quando há timer ativo)
      AnimatedVisibility(
        visible = showContextualTimerRow,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
          )
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.padding(top = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = animatedAccent,
                modifier = Modifier.size(16.dp),
              )
              val remainingMinutes = activeTimer?.remainingMinutes(System.currentTimeMillis())
              Text(
                text = if (activeTimer != null && remainingMinutes != null && remainingMinutes > 0) {
                  AppLabels.Now.remainingTimerLabel(remainingMinutes)
                } else {
                  AppLabels.Now.keepForLabel
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
              )
            }

            if (activeTimer != null) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onExtendTimer(30) }
                    .testTag(AppTestTags.Now.EXTEND_TIMER_BUTTON),
                  shape = RoundedCornerShape(50),
                  color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.35f)),
                ) {
                  Text(
                    text = AppLabels.Now.extend30MinChip,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = animatedAccent,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                  )
                }
                FilledTonalButton(
                  onClick = onCancelTimer,
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag(AppTestTags.Now.CANCEL_TIMER_BUTTON),
                ) {
                  Text(
                    text = AppLabels.Common.restore,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            } else {
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h").forEach { (minutes, label) ->
                  Surface(
                    modifier = Modifier
                      .clip(RoundedCornerShape(50))
                      .clickable { onStartTimer(minutes) }
                      .testTag(AppTestTags.Now.timerChip(minutes)),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  ) {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      softWrap = false,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ManualOverrideBanner(
  onReapplyActiveRule: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(AppTestTags.Now.MANUAL_OVERRIDE_BANNER),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.40f)),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.tertiaryContainer,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = AppLabels.Now.manualOverrideTitle,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
          text = AppLabels.Now.manualOverrideSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
        )
      }
      FilledTonalButton(
        onClick = onReapplyActiveRule,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag(AppTestTags.Now.REAPPLY_RULE_BUTTON),
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(AppLabels.Now.reapplyButton, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun DndPermissionBanner(
  onRequestDndPermission: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(AppTestTags.Now.DND_PERMISSION_BANNER),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Default.WarningAmber,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Column {
          Text(
            text = AppLabels.Now.dndBannerTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
          Text(
            text = AppLabels.Now.dndBannerSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
          )
        }
      }
      Button(
        onClick = onRequestDndPermission,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError,
        ),
        modifier = Modifier.testTag(AppTestTags.Now.DND_ENABLE_BUTTON),
      ) {
        Text(AppLabels.Common.enable, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun WriteSettingsPermissionBanner(
  onRequestWriteSettingsPermission: () -> Unit,
  onDismiss: () -> Unit,
) {
  val vibrateAccent = zenithVibrateAccentColor()
  val vibrateContainer = zenithVibrateContainerColor()

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(AppTestTags.Now.WRITE_SETTINGS_BANNER),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, vibrateAccent.copy(alpha = 0.45f)),
    colors = CardDefaults.cardColors(
      containerColor = vibrateContainer,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Vibration,
          contentDescription = null,
          tint = vibrateAccent,
        )
        Column {
          Text(
            text = AppLabels.Now.writeSettingsBannerTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = AppLabels.Now.writeSettingsBannerSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      Button(
        onClick = onRequestWriteSettingsPermission,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = vibrateAccent,
          contentColor = zenithOnActivePillColor(),
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.testTag(AppTestTags.Now.WRITE_SETTINGS_ENABLE_BUTTON),
      ) {
        Text(AppLabels.Common.authorize, fontWeight = FontWeight.Bold)
      }
      IconButton(
        onClick = onDismiss,
        modifier = Modifier
          .size(32.dp)
          .testTag(AppTestTags.Now.WRITE_SETTINGS_DISMISS_BUTTON),
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = AppLabels.Common.cancel,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun VolumeStreamsCard(
  state: MainScreenUiState.Ready,
  onStreamVolumeChange: (VolumeStreamType, Int) -> Unit,
  onToggleRingNotificationLink: () -> Unit,
  onToggleSecondaryDrawer: (Boolean) -> Unit,
  onToggleVolumePreviewQuick: () -> Unit,
  onToggleStreamPreview: (VolumeStreamType, Int, VolumePreviewScope, Boolean) -> Unit,
  onStartStreamLoopPreview: (VolumeStreamType, Int, VolumePreviewScope, Boolean) -> Unit,
  onUpdateStreamLoopPercent: (VolumeStreamType, Int, VolumePreviewScope) -> Unit,
  onStopVolumePreview: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  val audioMode = state.audioMode
  val activeAccent = audioMode.tonalAccentColor()
  val ringNotificationLinked = state.ringNotificationLinked
  val secondaryDrawerExpanded = state.secondaryDrawerExpanded
  val normalizedOrder = VolumeStreamType.normalizeManualStreamOrder(state.orderedStreams)
  val clampedFixedCount = state.visibleFixedStreamsCount.coerceIn(1, normalizedOrder.size)
  val fixedStreams = normalizedOrder.take(clampedFixedCount)
  val drawerStreams = normalizedOrder.drop(clampedFixedCount)

  var linkedDragPreviewPercent by remember { mutableStateOf<Int?>(null) }
  var pendingSilentPreviewRequest by remember { mutableStateOf<Pair<VolumeStreamType, Int>?>(null) }

  LaunchedEffect(ringNotificationLinked) {
    if (!ringNotificationLinked) {
      linkedDragPreviewPercent = null
    }
  }

  if (pendingSilentPreviewRequest != null) {
    SilentPreviewConfirmDialog(
      onKeepMuted = { pendingSilentPreviewRequest = null },
      onListenNow = {
        val request = pendingSilentPreviewRequest
        pendingSilentPreviewRequest = null
        if (request != null) {
          val resolvedPercent = if (request.first.isMutedBySilentRinger && request.second <= 0) {
            65
          } else {
            request.second
          }
          onToggleStreamPreview(request.first, resolvedPercent, VolumePreviewScope.NOW_TAB, true)
        }
      },
    )
  }

  fun requestNowTapPreview(streamType: VolumeStreamType, currentPercent: Int) {
    val isCurrentlyPlayingThis = state.activeVolumePreview?.scope == VolumePreviewScope.NOW_TAB &&
      state.activeVolumePreview.streamType == streamType
    if (isCurrentlyPlayingThis) {
      onStopVolumePreview()
    } else if (audioMode == BaseAudioMode.SILENT) {
      pendingSilentPreviewRequest = streamType to currentPercent
    } else if (state.volumePreviewEnabled) {
      onToggleStreamPreview(streamType, currentPercent, VolumePreviewScope.NOW_TAB, false)
    }
  }

  fun requestNowHoldPreview(streamType: VolumeStreamType, currentPercent: Int) {
    if (audioMode == BaseAudioMode.SILENT) {
      pendingSilentPreviewRequest = streamType to currentPercent
    } else if (state.volumePreviewEnabled) {
      onStartStreamLoopPreview(streamType, currentPercent, VolumePreviewScope.NOW_TAB, false)
    }
  }

  @Composable
  fun RenderStreamCapsule(streamType: VolumeStreamType) {
    val isRingOrNotif =
      streamType == VolumeStreamType.RING || streamType == VolumeStreamType.NOTIFICATION
    val activePreviewKind = state.activeVolumePreview
      ?.takeIf { it.scope == VolumePreviewScope.NOW_TAB && it.streamType == streamType }
      ?.kind

    PillVolumeSliderRow(
      type = streamType,
      icon = streamType.streamIcon(),
      percent = state.displayedStreamPercent(streamType),
      minPercent = state.minPercentFor(streamType),
      accentColor = activeAccent,
      isLinked = if (isRingOrNotif) ringNotificationLinked else null,
      onToggleLink = if (isRingOrNotif) {
        {
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          linkedDragPreviewPercent = null
          onToggleRingNotificationLink()
        }
      } else {
        null
      },
      isPendingForSound = state.isStreamPendingForSound(streamType),
      activePreviewKind = activePreviewKind,
      onSingleTapPreview = { requestNowTapPreview(streamType, it) },
      onDoubleTapPreview = { requestNowTapPreview(streamType, it) },
      onStartHoldPreview = { requestNowHoldPreview(streamType, it) },
      onHoldLivePercentChange = {
        if (audioMode == BaseAudioMode.SOUND && state.volumePreviewEnabled) {
          onUpdateStreamLoopPercent(streamType, it, VolumePreviewScope.NOW_TAB)
        }
      },
      onStopHoldPreview = { _, _ ->
        if (state.activeVolumePreview?.scope == VolumePreviewScope.NOW_TAB) {
          onStopVolumePreview()
        }
      },
      externalPreviewPercent = if (isRingOrNotif && ringNotificationLinked) {
        linkedDragPreviewPercent
      } else {
        null
      },
      onLivePreviewChange = if (isRingOrNotif) {
        { preview ->
          linkedDragPreviewPercent = if (ringNotificationLinked) preview else null
        }
      } else {
        null
      },
      onValueChangeFinished = { nextPercent ->
        onStreamVolumeChange(streamType, nextPercent)
      },
    )
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(AppTestTags.Now.VOLUME_STREAMS_CARD),
    shape = RoundedCornerShape(28.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f).padding(end = 8.dp),
        ) {
          Text(
            text = AppLabels.Now.volumesTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            Text(
              text = AppLabels.Now.channelsBadge(
                fixedCount = clampedFixedCount,
                totalCount = normalizedOrder.size,
              ),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }

        val chipIsSilentProtected = audioMode == BaseAudioMode.SILENT
        val chipIcon = when {
          chipIsSilentProtected -> Icons.Default.Shield
          state.volumePreviewEnabled -> Icons.AutoMirrored.Filled.VolumeUp
          else -> Icons.AutoMirrored.Filled.VolumeOff
        }
        val chipLabel = when {
          chipIsSilentProtected -> AppLabels.Now.previewChipSilentProtectedLabel
          state.volumePreviewEnabled -> AppLabels.Now.previewChipActiveLabel
          else -> AppLabels.Now.previewChipMutedLabel
        }
        val chipContainer = when {
          chipIsSilentProtected -> BaseAudioMode.SILENT.tonalContainerColor()
          state.volumePreviewEnabled -> audioMode.tonalContainerColor()
          else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val chipContent = when {
          chipIsSilentProtected -> BaseAudioMode.SILENT.onTonalContainerColor()
          state.volumePreviewEnabled -> audioMode.onTonalContainerColor()
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(
          shape = RoundedCornerShape(50),
          color = chipContainer,
          border = BorderStroke(
            width = 1.dp,
            color = if (state.volumePreviewEnabled || chipIsSilentProtected) {
              activeAccent.copy(alpha = 0.35f)
            } else {
              MaterialTheme.colorScheme.outlineVariant
            },
          ),
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(
              if (!chipIsSilentProtected) {
                Modifier.clickable {
                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                  onToggleVolumePreviewQuick()
                }
              } else {
                Modifier
              },
            )
            .testTag(AppTestTags.Now.VOLUME_PREVIEW_CHIP),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = chipIcon,
              contentDescription = null,
              tint = chipContent,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = chipLabel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = chipContent,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }

      fixedStreams.forEach { streamType ->
        key(streamType) {
          RenderStreamCapsule(streamType)
        }
      }

      if (drawerStreams.isNotEmpty()) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surfaceContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable {
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              onToggleSecondaryDrawer(!secondaryDrawerExpanded)
            }
            .testTag(AppTestTags.Now.SECONDARY_DRAWER_TOGGLE),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (secondaryDrawerExpanded) {
                AppLabels.Now.secondaryDrawerExpandedLabel
              } else {
                AppLabels.Now.secondaryDrawerCollapsedLabel(drawerStreams)
              },
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            )
            Icon(
              imageVector = if (secondaryDrawerExpanded) {
                Icons.Default.ExpandLess
              } else {
                Icons.Default.ExpandMore
              },
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
        }

        AnimatedVisibility(
          visible = secondaryDrawerExpanded,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut(),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            drawerStreams.forEach { streamType ->
              key(streamType) {
                RenderStreamCapsule(streamType)
              }
            }
          }
        }
      }
    }
  }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun MainDashboardPreview() {
  ManageSoundModeTheme(
    themeMode = ThemeMode.DARK,
    dynamicColor = false,
  ) {
    MainDashboardContent(
      state = MainScreenUiState.Ready(
        currentMode = SoundMode.NORMAL,
        streams = VolumeStreams(
          ringPercent = 85,
          notificationPercent = 75,
          mediaPercent = 50,
          alarmPercent = 90,
        ),
        hasDndPermission = true,
        profiles = SoundProfile.defaultPresets,
        activeProfileId = "preset_work_high",
        activeRuleTitle = "Rotina 'Trabalho' ativa",
        activeRuleSubtitle = "Horário 08:30 – 18:00 • Perfil Trabalho",
        scheduleRules = ScheduleRule.defaultSchedules,
        locationRules = LocationRule.defaultLocations,
      ),
      onSelectMode = {},
      onStreamVolumeChange = { _, _ -> },
      onActivateProfile = {},
      onStartTimer = { _, _ -> },
      onCancelTimer = {},
      onSaveProfile = { _, _ -> },
      onDismissMessage = {},
      onRequestDndPermission = {},
    )
  }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

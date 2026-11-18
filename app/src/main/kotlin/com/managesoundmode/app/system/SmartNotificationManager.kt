package com.managesoundmode.app.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.managesoundmode.app.MainActivity
import com.managesoundmode.app.R
import com.managesoundmode.app.domain.model.BaseAudioMode
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundState
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.receiver.SystemAutomationReceiver
import com.managesoundmode.app.ui.common.AppLabels

object SmartNotificationManager {

  private const val TAG = "SmartNotificationMgr"

  internal const val CHANNEL_SMART_PROMPT = "smart_volume_prompt"
  internal const val CHANNEL_ACTIVE_TIMER = "active_timer_status"
  internal const val CHANNEL_RULE_OVERRIDE = "rule_override_alert"
  internal const val CHANNEL_PERSISTENT_QUICK_BAR = "persistent_quick_bar"
  internal const val CHANNEL_AUTO_TRANSITION = "auto_routine_transition"

  internal const val NOTIFICATION_ID_SMART_PROMPT = 3101
  internal const val NOTIFICATION_ID_ACTIVE_TIMER = 3102
  internal const val NOTIFICATION_ID_RULE_OVERRIDE = 3103
  internal const val NOTIFICATION_ID_PERSISTENT_BAR = 3104
  internal const val NOTIFICATION_ID_AUTO_TRANSITION = 3105
  internal const val NOTIFICATION_ID_DND_GUIDANCE = 3106

  internal const val GROUP_SMART_PROMPT = "msm_group_smart_prompt"
  internal const val GROUP_ACTIVE_TIMER = "msm_group_active_timer"
  internal const val GROUP_RULE_OVERRIDE = "msm_group_rule_override"
  internal const val GROUP_QUICK_BAR = "msm_group_quick_bar"
  internal const val GROUP_AUTO_TRANSITION = "msm_group_auto_transition"
  internal const val GROUP_DND_GUIDANCE = "msm_group_dnd_guidance"

  fun ensureChannels(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return

    val promptChannel = NotificationChannel(
      CHANNEL_SMART_PROMPT,
      AppLabels.Notifications.promptChannelName,
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = AppLabels.Notifications.promptChannelDescription
      setSound(null, null)
      enableVibration(false)
    }

    val timerChannel = NotificationChannel(
      CHANNEL_ACTIVE_TIMER,
      AppLabels.Notifications.timerChannelName,
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = AppLabels.Notifications.timerChannelDescription
      setSound(null, null)
      enableVibration(false)
    }

    val overrideChannel = NotificationChannel(
      CHANNEL_RULE_OVERRIDE,
      AppLabels.Notifications.overrideChannelName,
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = AppLabels.Notifications.overrideChannelDescription
      setSound(null, null)
      enableVibration(false)
    }

    val quickBarChannel = NotificationChannel(
      CHANNEL_PERSISTENT_QUICK_BAR,
      AppLabels.Notifications.quickBarChannelName,
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = AppLabels.Notifications.quickBarChannelDescription
      setSound(null, null)
      enableVibration(false)
      setShowBadge(false)
    }

    val autoTransitionChannel = NotificationChannel(
      CHANNEL_AUTO_TRANSITION,
      AppLabels.Notifications.autoTransitionChannelName,
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = AppLabels.Notifications.autoTransitionChannelDescription
      setSound(null, null)
      enableVibration(false)
      setShowBadge(false)
    }

    manager.createNotificationChannels(
      listOf(
        promptChannel,
        timerChannel,
        overrideChannel,
        quickBarChannel,
        autoTransitionChannel,
      ),
    )
  }

  @Suppress("UNUSED_PARAMETER")
  fun syncPersistentQuickBarNotification(
    context: Context,
    enabled: Boolean,
    currentMode: SoundMode,
    streams: VolumeStreams,
    activeProfileName: String?,
    activeTimer: TimedModeSession? = null,
    currentSoundState: SoundState = SoundState.fromLegacyMode(currentMode),
    hasDndPermission: Boolean = hasDndAccess(context),
  ) {
    val manager = NotificationManagerCompat.from(context)
    if (!enabled || !canPostNotifications(context)) {
      manager.cancel(NOTIFICATION_ID_PERSISTENT_BAR)
      return
    }

    ensureChannels(context)

    val compactTitle = AppLabels.Notifications.quickBarCompactTitle(
      state = currentSoundState,
      activeProfileName = activeProfileName,
    )
    val compactSubtitle = AppLabels.Notifications.quickBarCompactSubtitle(
      state = currentSoundState,
      streams = streams,
    )
    val expandedTitle = AppLabels.Notifications.quickBarTitle(
      state = currentSoundState,
      activeProfileName = activeProfileName,
    )
    val expandedSubtitle = AppLabels.Notifications.quickBarSubtitle(
      state = currentSoundState,
      streams = streams,
    )

    val compactViews = buildQuickBarRemoteViews(
      context = context,
      layoutResId = R.layout.notification_persistent_quick_bar,
      title = compactTitle,
      subtitle = compactSubtitle,
      state = currentSoundState,
      hasDndPermission = hasDndPermission,
      includeVibrateLabel = false,
    )
    val expandedViews = buildQuickBarRemoteViews(
      context = context,
      layoutResId = R.layout.notification_persistent_quick_bar_expanded,
      title = expandedTitle,
      subtitle = expandedSubtitle,
      state = currentSoundState,
      hasDndPermission = hasDndPermission,
      includeVibrateLabel = true,
    )

    val smallIconRes = smallIconForState(currentSoundState)

    val notification = NotificationCompat.Builder(context, CHANNEL_PERSISTENT_QUICK_BAR)
      .setSmallIcon(smallIconRes)
      .setContentTitle(compactTitle)
      .setContentText(compactSubtitle)
      .setStyle(NotificationCompat.DecoratedCustomViewStyle())
      .setCustomContentView(compactViews)
      .setCustomBigContentView(expandedViews)
      .setContentIntent(openAppPendingIntent(context))
      .setDeleteIntent(restoreQuickBarPendingIntent(context))
      .setGroup(GROUP_QUICK_BAR)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setShowWhen(false)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .build()

    postNotificationSafely(context, NOTIFICATION_ID_PERSISTENT_BAR, notification)
  }

  private fun buildQuickBarRemoteViews(
    context: Context,
    layoutResId: Int,
    title: String,
    subtitle: String,
    state: SoundState,
    hasDndPermission: Boolean,
    includeVibrateLabel: Boolean,
  ): RemoteViews {
    val isSoundActive = state.audioMode == BaseAudioMode.SOUND
    val isSilentActive = state.audioMode == BaseAudioMode.SILENT
    val isVibrateEnabled = state.vibrateEnabled

    val soundNeedsDnd = !hasDndPermission && isSilentActive && !isVibrateEnabled
    val silentNeedsDnd = !hasDndPermission && (!isVibrateEnabled || isSilentActive)
    val vibrateNeedsDnd = !hasDndPermission && isSilentActive && isVibrateEnabled

    val views = RemoteViews(context.packageName, layoutResId)
    views.setTextViewText(R.id.quick_bar_title, title)
    views.setTextViewText(R.id.quick_bar_subtitle, subtitle)
    views.setTextViewText(R.id.quick_bar_label_sound, AppLabels.Notifications.quickBarSoundPillLabel)
    views.setTextViewText(R.id.quick_bar_label_silent, AppLabels.Notifications.quickBarSilentPillLabel)
    if (includeVibrateLabel) {
      views.setTextViewText(
        R.id.quick_bar_label_vibrate,
        AppLabels.Notifications.quickBarVibratePillLabel(isVibrateEnabled),
      )
    }

    // [Som] pill visual state & click
    views.setInt(
      R.id.quick_bar_btn_sound,
      "setBackgroundResource",
      if (isSoundActive) R.drawable.bg_notif_pill_sound_active else android.R.color.transparent,
    )
    val soundFgRes = if (isSoundActive) R.color.notif_sound_active_fg else R.color.notif_pill_inactive_fg
    applyRemoteTextColor(context, views, R.id.quick_bar_label_sound, soundFgRes)
    applyRemoteIconTint(context, views, R.id.quick_bar_icon_sound, soundFgRes)
    views.setContentDescription(
      R.id.quick_bar_btn_sound,
      AppLabels.Notifications.quickBarSoundContentDesc(isSoundActive),
    )
    views.setOnClickPendingIntent(
      R.id.quick_bar_btn_sound,
      if (soundNeedsDnd) {
        openDndSettingsPendingIntent(context, requestCode = 430)
      } else {
        setAudioModePendingIntent(context, requestCode = 420, audioMode = BaseAudioMode.SOUND)
      },
    )

    // [Silêncio] pill visual state & click (with DND fallback)
    views.setInt(
      R.id.quick_bar_btn_silent,
      "setBackgroundResource",
      if (isSilentActive) R.drawable.bg_notif_pill_silent_active else android.R.color.transparent,
    )
    val silentFgRes = if (isSilentActive) R.color.notif_silent_active_fg else R.color.notif_pill_inactive_fg
    applyRemoteTextColor(context, views, R.id.quick_bar_label_silent, silentFgRes)
    applyRemoteIconTint(context, views, R.id.quick_bar_icon_silent, silentFgRes)
    views.setContentDescription(
      R.id.quick_bar_btn_silent,
      AppLabels.Notifications.quickBarSilentContentDesc(isSilentActive, silentNeedsDnd),
    )
    views.setOnClickPendingIntent(
      R.id.quick_bar_btn_silent,
      if (silentNeedsDnd) {
        openDndSettingsPendingIntent(context, requestCode = 431)
      } else {
        setAudioModePendingIntent(context, requestCode = 421, audioMode = BaseAudioMode.SILENT)
      },
    )

    // [Vibrar] orthogonal toggle visual state & click
    views.setInt(
      R.id.quick_bar_btn_vibrate,
      "setBackgroundResource",
      if (isVibrateEnabled) R.drawable.bg_notif_pill_vibrate_active else R.drawable.bg_notif_pill_inactive,
    )
    val vibrateFgRes = if (isVibrateEnabled) R.color.notif_vibrate_active_fg else R.color.notif_pill_inactive_fg
    if (includeVibrateLabel) {
      applyRemoteTextColor(context, views, R.id.quick_bar_label_vibrate, vibrateFgRes)
    }
    applyRemoteIconTint(context, views, R.id.quick_bar_icon_vibrate, vibrateFgRes)
    views.setContentDescription(
      R.id.quick_bar_btn_vibrate,
      AppLabels.Notifications.quickBarVibrateContentDesc(isVibrateEnabled, vibrateNeedsDnd),
    )
    views.setOnClickPendingIntent(
      R.id.quick_bar_btn_vibrate,
      if (vibrateNeedsDnd) {
        openDndSettingsPendingIntent(context, requestCode = 432)
      } else {
        toggleVibratePendingIntent(context, requestCode = 422)
      },
    )

    return views
  }

  private fun applyRemoteTextColor(
    context: Context,
    views: RemoteViews,
    viewId: Int,
    @ColorRes colorRes: Int,
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      views.setColorStateList(viewId, "setTextColor", colorRes)
    } else {
      views.setTextColor(viewId, ContextCompat.getColor(context, colorRes))
    }
  }

  private fun applyRemoteIconTint(
    context: Context,
    views: RemoteViews,
    viewId: Int,
    @ColorRes colorRes: Int,
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      views.setColorStateList(viewId, "setImageTintList", colorRes)
    } else {
      views.setInt(viewId, "setColorFilter", ContextCompat.getColor(context, colorRes))
    }
  }

  fun showSmartRestorePrompt(
    context: Context,
    mode: SoundMode,
    mediaAutoMuted: Boolean,
    state: SoundState = SoundState.fromLegacyMode(mode),
  ) {
    if (!canPostNotifications(context)) return
    ensureChannels(context)

    // IMPORTANCE_HIGH channel with sound=null and vibration=false WITHOUT .setSilent(true)
    // enables a 100% silent Heads-Up pop-up on Android 10–16.
    val notification = NotificationCompat.Builder(context, CHANNEL_SMART_PROMPT)
      .setSmallIcon(smallIconForState(state))
      .setContentTitle(AppLabels.Notifications.smartPromptTitle(state.title))
      .setContentText(AppLabels.Notifications.smartPromptSubtitle(state.title, mediaAutoMuted))
      .setContentIntent(openAppPendingIntent(context))
      .setGroup(GROUP_SMART_PROMPT)
      .setAutoCancel(true)
      .setTimeoutAfter(120_000L)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .addAction(
        R.drawable.ic_qs_timer,
        AppLabels.Notifications.action30Min,
        timerActionPendingIntent(context, requestCode = 401, durationMinutes = 30),
      )
      .addAction(
        R.drawable.ic_qs_timer,
        AppLabels.Notifications.action1Hour,
        timerActionPendingIntent(context, requestCode = 402, durationMinutes = 60),
      )
      .addAction(
        R.drawable.ic_qs_timer,
        AppLabels.Notifications.action2Hours,
        timerActionPendingIntent(context, requestCode = 403, durationMinutes = 120),
      )
      .build()

    postNotificationSafely(context, NOTIFICATION_ID_SMART_PROMPT, notification)
  }

  fun syncActiveTimerNotification(
    context: Context,
    session: TimedModeSession?,
  ) {
    val manager = NotificationManagerCompat.from(context)
    val now = System.currentTimeMillis()
    if (session == null || session.endsAtEpochMillis <= now) {
      manager.cancel(NOTIFICATION_ID_ACTIVE_TIMER)
      return
    }

    manager.cancel(NOTIFICATION_ID_SMART_PROMPT)

    if (!canPostNotifications(context)) return
    ensureChannels(context)

    val remainingMillis = (session.endsAtEpochMillis - now).coerceAtLeast(1L)

    val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVE_TIMER)
      .setSmallIcon(R.drawable.ic_qs_timer)
      .setContentTitle(AppLabels.Notifications.activeTimerTitle(session.resolvedSoundState.title))
      .setContentText(AppLabels.Notifications.activeTimerSubtitle(session.resolvedPreviousSoundState.title))
      .setContentIntent(openAppPendingIntent(context))
      .setGroup(GROUP_ACTIVE_TIMER)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setUsesChronometer(true)
      .setChronometerCountDown(true)
      .setWhen(session.endsAtEpochMillis)
      .setTimeoutAfter(remainingMillis)
      .setShowWhen(true)
      .setCategory(NotificationCompat.CATEGORY_PROGRESS)
      .addAction(
        R.drawable.ic_qs_timer,
        AppLabels.Notifications.actionExtend30Min,
        extendTimerPendingIntent(context, addMinutes = 30),
      )
      .addAction(
        R.drawable.ic_widget_volume_up,
        AppLabels.Notifications.actionRestoreNow,
        restoreNowPendingIntent(context),
      )
      .build()

    postNotificationSafely(context, NOTIFICATION_ID_ACTIVE_TIMER, notification)
  }

  fun showRuleOverrideAlert(
    context: Context,
    ruleName: String,
  ) {
    if (!canPostNotifications(context)) return
    ensureChannels(context)

    val notification = NotificationCompat.Builder(context, CHANNEL_RULE_OVERRIDE)
      .setSmallIcon(R.drawable.ic_qs_sound_mode)
      .setContentTitle(AppLabels.Notifications.ruleOverrideTitle(ruleName))
      .setContentText(AppLabels.Notifications.ruleOverrideSubtitle)
      .setContentIntent(openAppPendingIntent(context))
      .setGroup(GROUP_RULE_OVERRIDE)
      .setAutoCancel(true)
      .setSilent(true)
      .setTimeoutAfter(90_000L)
      .addAction(
        R.drawable.ic_qs_sound_mode,
        AppLabels.Notifications.actionReapplyRule,
        reapplyRulePendingIntent(context),
      )
      .build()

    postNotificationSafely(context, NOTIFICATION_ID_RULE_OVERRIDE, notification)
  }

  fun showAutoRoutineTransition(
    context: Context,
    title: String,
    subtitle: String,
    state: SoundState,
  ) {
    if (!canPostNotifications(context)) return
    ensureChannels(context)

    val notification = NotificationCompat.Builder(context, CHANNEL_AUTO_TRANSITION)
      .setSmallIcon(smallIconForState(state))
      .setContentTitle(title)
      .setContentText(subtitle)
      .setContentIntent(openAppPendingIntent(context))
      .setGroup(GROUP_AUTO_TRANSITION)
      .setAutoCancel(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setTimeoutAfter(180_000L)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .build()

    postNotificationSafely(context, NOTIFICATION_ID_AUTO_TRANSITION, notification)
  }

  fun showDndPermissionAlert(context: Context) {
    if (!canPostNotifications(context)) return
    ensureChannels(context)

    val dndIntent = openDndSettingsPendingIntent(context, requestCode = 433)
    val notification = NotificationCompat.Builder(context, CHANNEL_SMART_PROMPT)
      .setSmallIcon(R.drawable.ic_widget_silent)
      .setContentTitle(AppLabels.Notifications.dndRequiredAlertTitle)
      .setContentText(AppLabels.Notifications.dndRequiredAlertSubtitle)
      .setContentIntent(dndIntent)
      .setGroup(GROUP_DND_GUIDANCE)
      .setAutoCancel(true)
      .setTimeoutAfter(60_000L)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .addAction(
        R.drawable.ic_widget_silent,
        AppLabels.Notifications.actionOpenDndSettings,
        dndIntent,
      )
      .build()

    postNotificationSafely(context, NOTIFICATION_ID_DND_GUIDANCE, notification)
  }

  private fun smallIconForState(state: SoundState): Int = when (state.audioMode) {
    BaseAudioMode.SOUND -> R.drawable.ic_widget_volume_up
    BaseAudioMode.SILENT ->
      if (state.vibrateEnabled) {
        R.drawable.ic_widget_vibration
      } else {
        R.drawable.ic_widget_silent
      }
  }

  @android.annotation.SuppressLint("MissingPermission")
  private fun postNotificationSafely(
    context: Context,
    notificationId: Int,
    notification: android.app.Notification,
  ) {
    if (!canPostNotifications(context)) return
    runCatching {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    }.onFailure { error ->
      Log.w(TAG, "Falha ao publicar notificação ($notificationId)", error)
    }
  }

  fun dismissSmartPrompt(context: Context) {
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SMART_PROMPT)
  }

  fun dismissRuleOverrideAlert(context: Context) {
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_RULE_OVERRIDE)
  }

  private fun hasDndAccess(context: Context): Boolean {
    return context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted ?: false
  }

  internal fun canPostNotifications(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  private fun openAppPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    InternalIntentSecurity.signIntent(context, intent)
    return PendingIntent.getActivity(
      context,
      400,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun openDndSettingsPendingIntent(
    context: Context,
    requestCode: Int,
  ): PendingIntent {
    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return PendingIntent.getActivity(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun restoreQuickBarPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_RESTORE_PERSISTENT_QUICK_BAR
    }
    InternalIntentSecurity.signIntent(context, intent)
    return PendingIntent.getBroadcast(
      context,
      425,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun setAudioModePendingIntent(
    context: Context,
    requestCode: Int,
    audioMode: BaseAudioMode,
  ): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_WIDGET_SET_AUDIO_MODE
      putExtra(SystemAutomationReceiver.EXTRA_BASE_AUDIO_MODE, audioMode.name)
    }
    InternalIntentSecurity.signIntent(context, intent)
    return PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun toggleVibratePendingIntent(
    context: Context,
    requestCode: Int,
  ): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_WIDGET_TOGGLE_VIBRATE
    }
    InternalIntentSecurity.signIntent(context, intent)
    return PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun timerActionPendingIntent(
    context: Context,
    requestCode: Int,
    durationMinutes: Int,
  ): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_NOTIFICATION_START_TIMER
      putExtra(SystemAutomationReceiver.EXTRA_DURATION_MINUTES, durationMinutes)
    }
    InternalIntentSecurity.signIntent(context, intent)
    return PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun extendTimerPendingIntent(
    context: Context,
    addMinutes: Int,
  ): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_NOTIFICATION_EXTEND_TIMER
      putExtra(SystemAutomationReceiver.EXTRA_DURATION_MINUTES, addMinutes)
    }
    InternalIntentSecurity.signIntent(context, intent)
    return PendingIntent.getBroadcast(
      context,
      410,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun restoreNowPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_NOTIFICATION_RESTORE_NOW
    }
    InternalIntentSecurity.signIntent(context, intent)
    return PendingIntent.getBroadcast(
      context,
      411,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun reapplyRulePendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_NOTIFICATION_REAPPLY_RULE
    }
    InternalIntentSecurity.signIntent(context, intent)
    return PendingIntent.getBroadcast(
      context,
      412,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}

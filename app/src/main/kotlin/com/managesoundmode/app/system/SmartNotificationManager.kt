package com.managesoundmode.app.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
  private const val CHANNEL_SMART_PROMPT = "smart_volume_prompt"
  private const val CHANNEL_ACTIVE_TIMER = "active_timer_status"
  private const val CHANNEL_RULE_OVERRIDE = "rule_override_alert"
  private const val CHANNEL_PERSISTENT_QUICK_BAR = "persistent_quick_bar"

  private const val NOTIFICATION_ID_SMART_PROMPT = 3101
  private const val NOTIFICATION_ID_ACTIVE_TIMER = 3102
  private const val NOTIFICATION_ID_RULE_OVERRIDE = 3103
  private const val NOTIFICATION_ID_PERSISTENT_BAR = 3104

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

    manager.createNotificationChannels(
      listOf(promptChannel, timerChannel, overrideChannel, quickBarChannel),
    )
  }

  fun syncPersistentQuickBarNotification(
    context: Context,
    enabled: Boolean,
    currentMode: SoundMode,
    streams: VolumeStreams,
    activeProfileName: String?,
    activeTimer: TimedModeSession?,
    currentSoundState: SoundState = SoundState.fromLegacyMode(currentMode),
  ) {
    val manager = NotificationManagerCompat.from(context)
    if (!enabled || !canPostNotifications(context)) {
      manager.cancel(NOTIFICATION_ID_PERSISTENT_BAR)
      return
    }

    ensureChannels(context)
    val now = System.currentTimeMillis()
    val remainingMin = activeTimer?.remainingMinutes(now)?.takeIf { it > 0 }

    val title = AppLabels.Notifications.quickBarTitle(
      state = currentSoundState,
      remainingMin = remainingMin,
      activeProfileName = activeProfileName,
    )
    val subtitle = AppLabels.Notifications.quickBarSubtitle(
      state = currentSoundState,
      streams = streams,
    )

    val smallIconRes = when (currentSoundState.audioMode) {
      BaseAudioMode.SOUND -> R.drawable.ic_widget_volume_up
      BaseAudioMode.SILENT ->
        if (currentSoundState.vibrateEnabled) {
          R.drawable.ic_widget_vibration
        } else {
          R.drawable.ic_widget_silent
        }
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_PERSISTENT_QUICK_BAR)
      .setSmallIcon(smallIconRes)
      .setContentTitle(title)
      .setContentText(subtitle)
      .setContentIntent(openAppPendingIntent(context))
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setShowWhen(false)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .addAction(
        R.drawable.ic_widget_volume_up,
        AppLabels.Notifications.quickBarSoundAction(currentSoundState.audioMode == BaseAudioMode.SOUND),
        setAudioModePendingIntent(context, requestCode = 420, audioMode = BaseAudioMode.SOUND),
      )
      .addAction(
        R.drawable.ic_widget_silent,
        AppLabels.Notifications.quickBarSilentAction(currentSoundState.audioMode == BaseAudioMode.SILENT),
        setAudioModePendingIntent(context, requestCode = 421, audioMode = BaseAudioMode.SILENT),
      )
      .addAction(
        R.drawable.ic_widget_vibration,
        AppLabels.Notifications.quickBarVibrateAction(currentSoundState.vibrateEnabled),
        toggleVibratePendingIntent(context, requestCode = 422),
      )
      .build()

    postNotificationSafely(context, NOTIFICATION_ID_PERSISTENT_BAR, notification)
  }

  fun showSmartRestorePrompt(
    context: Context,
    mode: SoundMode,
    mediaAutoMuted: Boolean,
  ) {
    if (!canPostNotifications(context)) return
    ensureChannels(context)
    val state = SoundState.fromLegacyMode(mode)

    val notification = NotificationCompat.Builder(context, CHANNEL_SMART_PROMPT)
      .setSmallIcon(R.drawable.ic_qs_sound_mode)
      .setContentTitle(AppLabels.Notifications.smartPromptTitle(state.title))
      .setContentText(AppLabels.Notifications.smartPromptSubtitle(state.title, mediaAutoMuted))
      .setContentIntent(openAppPendingIntent(context))
      .setAutoCancel(true)
      .setSilent(true)
      .setTimeoutAfter(120_000L)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .addAction(
        0,
        AppLabels.Notifications.action30Min,
        timerActionPendingIntent(context, requestCode = 401, durationMinutes = 30),
      )
      .addAction(
        0,
        AppLabels.Notifications.action1Hour,
        timerActionPendingIntent(context, requestCode = 402, durationMinutes = 60),
      )
      .addAction(
        0,
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

    val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVE_TIMER)
      .setSmallIcon(R.drawable.ic_qs_sound_mode)
      .setContentTitle(AppLabels.Notifications.activeTimerTitle(session.resolvedSoundState.title))
      .setContentText(AppLabels.Notifications.activeTimerSubtitle(session.resolvedPreviousSoundState.title))
      .setContentIntent(openAppPendingIntent(context))
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setUsesChronometer(true)
      .setChronometerCountDown(true)
      .setWhen(session.endsAtEpochMillis)
      .setShowWhen(true)
      .setCategory(NotificationCompat.CATEGORY_PROGRESS)
      .addAction(
        0,
        AppLabels.Notifications.actionExtend30Min,
        extendTimerPendingIntent(context, addMinutes = 30),
      )
      .addAction(
        0,
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
      .setAutoCancel(true)
      .setSilent(true)
      .setTimeoutAfter(90_000L)
      .addAction(
        0,
        AppLabels.Notifications.actionReapplyRule,
        reapplyRulePendingIntent(context),
      )
      .build()

    postNotificationSafely(context, NOTIFICATION_ID_RULE_OVERRIDE, notification)
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

  private fun canPostNotifications(context: Context): Boolean {
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

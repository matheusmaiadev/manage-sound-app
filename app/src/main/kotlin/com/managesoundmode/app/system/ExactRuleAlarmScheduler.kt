package com.managesoundmode.app.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.managesoundmode.app.domain.engine.RuleEvaluator
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.receiver.SystemAutomationReceiver

object ExactRuleAlarmScheduler {

  private const val TAG = "ExactRuleAlarmScheduler"
  private const val ALARM_REQUEST_CODE = 4201
  private const val TIMER_ALARM_REQUEST_CODE = 4202

  fun scheduleNextTransition(
    context: Context,
    scheduleRules: List<ScheduleRule>,
    nextCalendarBoundaryMillis: Long? = null,
  ) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val pendingIntent = createAlarmPendingIntent(context)

    val nextTriggerMillis = RuleEvaluator.computeNextAutomationTransitionMillis(
      scheduleRules = scheduleRules,
      nextCalendarBoundaryMillis = nextCalendarBoundaryMillis,
    )
    if (nextTriggerMillis == null) {
      alarmManager.cancel(pendingIntent)
      return
    }

    scheduleWakeupAlarmSafely(
      alarmManager = alarmManager,
      triggerAtMillis = nextTriggerMillis,
      pendingIntent = pendingIntent,
      errorMessage = "Falha ao agendar próximo alarme de regra de som",
    )
  }

  fun scheduleTimerExpiration(
    context: Context,
    endsAtEpochMillis: Long,
  ) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val pendingIntent = createTimerAlarmPendingIntent(context)
    scheduleWakeupAlarmSafely(
      alarmManager = alarmManager,
      triggerAtMillis = endsAtEpochMillis,
      pendingIntent = pendingIntent,
      errorMessage = "Falha ao agendar alarme exato de expiração do timer",
    )
  }

  fun cancelTimerExpiration(context: Context) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    runCatching {
      alarmManager.cancel(createTimerAlarmPendingIntent(context))
    }.onFailure { error ->
      Log.w(TAG, "Falha ao cancelar alarme de expiração do timer", error)
    }
  }

  private fun scheduleWakeupAlarmSafely(
    alarmManager: AlarmManager,
    triggerAtMillis: Long,
    pendingIntent: PendingIntent,
    errorMessage: String,
  ) {
    runCatching {
      val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager.canScheduleExactAlarms()
      } else {
        true
      }

      if (canExact) {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent,
        )
      } else {
        alarmManager.setAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent,
        )
      }
    }.onFailure { error ->
      Log.w(TAG, errorMessage, error)
    }
  }

  private fun createAlarmPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_EVALUATE_RULES_ALARM
    }
    InternalIntentSecurity.signIntent(context, intent)
    return PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun createTimerAlarmPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_TIMER_EXPIRED_ALARM
    }
    InternalIntentSecurity.signIntent(context, intent)
    return PendingIntent.getBroadcast(
      context,
      TIMER_ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}

package com.managesoundmode.app.system

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.managesoundmode.app.MainActivity
import com.managesoundmode.app.R
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.ui.common.AppLabels

object AppShortcutsManager {

  private const val TAG = "AppShortcutsManager"
  const val EXTRA_SHORTCUT_ACTION = "shortcut_action"
  const val ACTION_SILENCE_1H = "shortcut_silence_1h"
  const val ACTION_PROFILE_WORK = "shortcut_profile_work"
  const val ACTION_PROFILE_SLEEP = "shortcut_profile_sleep"
  const val ACTION_RESTORE_NORMAL = "shortcut_restore_normal"

  fun publishDynamicShortcuts(context: Context) {
    runCatching {
      val vibrateIcon = IconCompat.createWithResource(context, R.drawable.ic_widget_vibration)
      val soundIcon = IconCompat.createWithResource(context, R.drawable.ic_widget_volume_up)
      val silentIcon = IconCompat.createWithResource(context, R.drawable.ic_widget_silent)
      val styleIcon = IconCompat.createWithResource(context, R.drawable.ic_qs_sound_mode)

      val shortcuts = listOf(
        ShortcutInfoCompat.Builder(context, ACTION_SILENCE_1H)
          .setShortLabel(AppLabels.Shortcuts.silence1hShort)
          .setLongLabel(AppLabels.Shortcuts.silence1hLong)
          .setIcon(vibrateIcon)
          .setIntent(shortcutIntent(context, ACTION_SILENCE_1H))
          .build(),
        ShortcutInfoCompat.Builder(context, ACTION_PROFILE_WORK)
          .setShortLabel(AppLabels.Shortcuts.workShort)
          .setLongLabel(AppLabels.Shortcuts.workLong)
          .setIcon(styleIcon)
          .setIntent(shortcutIntent(context, ACTION_PROFILE_WORK))
          .build(),
        ShortcutInfoCompat.Builder(context, ACTION_PROFILE_SLEEP)
          .setShortLabel(AppLabels.Shortcuts.sleepShort)
          .setLongLabel(AppLabels.Shortcuts.sleepLong)
          .setIcon(silentIcon)
          .setIntent(shortcutIntent(context, ACTION_PROFILE_SLEEP))
          .build(),
        ShortcutInfoCompat.Builder(context, ACTION_RESTORE_NORMAL)
          .setShortLabel(AppLabels.Shortcuts.normalShort)
          .setLongLabel(AppLabels.Shortcuts.normalLong)
          .setIcon(soundIcon)
          .setIntent(shortcutIntent(context, ACTION_RESTORE_NORMAL))
          .build(),
      )

      ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }.onFailure { error ->
      Log.w(TAG, "Falha ao publicar atalhos dinâmicos do app", error)
    }
  }

  private fun shortcutIntent(context: Context, actionKey: String): Intent =
    Intent(context, MainActivity::class.java).apply {
      action = Intent.ACTION_VIEW
      putExtra(EXTRA_SHORTCUT_ACTION, actionKey)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }.also { InternalIntentSecurity.signIntent(context, it) }

  fun resolveProfileIdForShortcut(actionKey: String): String? = when (actionKey) {
    ACTION_PROFILE_WORK -> "preset_work_high"
    ACTION_PROFILE_SLEEP -> "preset_sleep"
    ACTION_RESTORE_NORMAL -> SoundProfile.DEFAULT_DAY_PROFILE_ID
    else -> null
  }
}

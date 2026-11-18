package com.managesoundmode.app

import android.app.Application
import com.managesoundmode.app.di.AppContainer
import com.managesoundmode.app.di.DefaultAppContainer
import com.managesoundmode.app.system.AppShortcutsManager
import com.managesoundmode.app.system.SmartNotificationManager
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ManageSoundApplication : Application() {
  lateinit var container: AppContainer
    private set

  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun onCreate() {
    super.onCreate()
    container = DefaultAppContainer(applicationContext)
    SmartNotificationManager.ensureChannels(applicationContext)
    AppShortcutsManager.publishDynamicShortcuts(applicationContext)

    applicationScope.launch {
      runCatching {
        val prefs = container.preferencesDataSource.prefsFlow.first()
        val resolvedLang = runCatching { AppLanguage.valueOf(prefs.appLanguageCode) }
          .getOrDefault(AppLanguage.PT_BR)
        if (AppLabels.currentLanguage != resolvedLang) {
          AppLabels.currentLanguage = resolvedLang
          SmartNotificationManager.ensureChannels(applicationContext)
          AppShortcutsManager.publishDynamicShortcuts(applicationContext)
        }
        container.soundModeRepository.syncExternalSurfacesNow()
      }
    }

    applicationScope.launch {
      container.soundSystemController.systemChanges.collectLatest {
        if (container.soundSystemController.lastBroadcastWasExternal) {
          container.soundModeRepository.handleExternalAudioChange()
        }
      }
    }
  }
}

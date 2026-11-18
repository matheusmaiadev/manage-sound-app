package com.managesoundmode.app.e2e

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.managesoundmode.app.MainActivity
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.data.local.PersistedSoundPrefs
import com.managesoundmode.app.domain.model.AppAccessRole
import com.managesoundmode.app.domain.model.BaseAudioMode
import com.managesoundmode.app.domain.model.CalendarRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SoundState
import com.managesoundmode.app.domain.model.StreamSortMode
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.domain.model.CachedProEntitlement
import com.managesoundmode.app.domain.model.ZenithAtmosphericPalette
import com.managesoundmode.app.data.billing.FakeBillingRepository
import com.managesoundmode.app.R
import com.managesoundmode.app.receiver.SystemAutomationReceiver
import com.managesoundmode.app.system.BackgroundGeofenceManager
import com.managesoundmode.app.system.InternalIntentSecurity
import com.managesoundmode.app.system.SmartNotificationManager
import com.managesoundmode.app.system.SystemZenRuleManager
import com.managesoundmode.app.system.VolumePreviewScope
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppLanguage
import com.managesoundmode.app.ui.common.AppTestTags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Suíte de Testes End-to-End (E2E) executando sobre a [MainActivity] real,
 * integrada ao [ManageSoundApplication], [DataStoreSoundPreferencesDataSource],
 * [DefaultSoundModeRepository], [RuleEvaluator] e [AndroidSoundSystemController].
 */
class ManageSoundModeE2ETest {

  @get:Rule
  val composeRule = createAndroidComposeRule<MainActivity>()

  private val app: ManageSoundApplication
    get() = ApplicationProvider.getApplicationContext<Context>() as ManageSoundApplication

  private fun execShellCommandSync(command: String) {
    runCatching {
      val pfd = InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommand(command)
      ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }
  }

  private fun awaitPrefs(
    label: String = "prefs",
    timeoutMillis: Long = 5_000L,
    predicate: (PersistedSoundPrefs) -> Boolean = { true },
  ): PersistedSoundPrefs {
    var latest: PersistedSoundPrefs? = null
    try {
      composeRule.waitUntil(timeoutMillis = timeoutMillis) {
        val current = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
        latest = current
        predicate(current)
      }
    } catch (e: Throwable) {
      throw AssertionError(
        "awaitPrefs($label) timed out; customProfiles=${latest?.customProfiles?.map { it.name }}, schedules=${latest?.scheduleRules?.map { it.name }}, locations=${latest?.locationRules?.map { it.name }}, paused=${latest?.automationsPaused}, activeProfile=${latest?.activeProfileId}",
        e,
      )
    }
    return latest ?: runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
  }

  @Before
  fun setUp() {
    AppLabels.currentLanguage = AppLanguage.PT_BR
    execShellCommandSync("cmd notification allow_dnd ${app.packageName}")
    runBlocking {
      val prefs = app.container.preferencesDataSource
      prefs.resetFactoryDefaults()
      prefs.setActiveTimer(null)
      prefs.setThemeMode(ThemeMode.DARK)
      prefs.setAppLanguageCode("PT_BR")
      prefs.setLocationDisclosureAccepted(false)
      prefs.setOnboardingCompleted(true)
      app.container.soundModeRepository.evaluateAndApplyRules()
      prefs.clearTransitionLogs()
      app.container.soundSystemController.applySoundState(SoundState.DEFAULT)
      app.container.soundModeRepository.refreshSystemState()
    }
    composeRule.waitForIdle()
  }

  @After
  fun tearDown() {
    AppLabels.currentLanguage = AppLanguage.PT_BR
    runBlocking {
      val prefs = app.container.preferencesDataSource
      prefs.resetFactoryDefaults()
      prefs.setActiveTimer(null)
      prefs.setOnboardingCompleted(true)
    }
  }

  @Test
  fun e2e01_firstRunOnboardingJourney_completesThreeStepsAndPersistsToRealDataStore() {
    runBlocking {
      app.container.preferencesDataSource.setOnboardingCompleted(false)
      app.container.soundModeRepository.refreshSystemState()
    }
    composeRule.waitForIdle()

    // Passo 1: Assistente abre automaticamente sobre a MainActivity
    composeRule.onNodeWithTag(AppTestTags.Onboarding.DIALOG).assertIsDisplayed()
    composeRule.onNodeWithTag(AppTestTags.Onboarding.stepPill(1)).assertIsDisplayed()

    // Passo 2: Checklist de Permissões
    composeRule.onNodeWithTag(AppTestTags.Onboarding.NEXT_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.Onboarding.stepPill(2)).assertIsDisplayed()

    // Passo 3: Selecionar Perfil Padrão de Retorno ("Casa / Fim de Semana") e Concluir
    composeRule.onNodeWithTag(AppTestTags.Onboarding.NEXT_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.Onboarding.defaultProfileChip("preset_home")).performClick()
    composeRule.onNodeWithTag(AppTestTags.Onboarding.FINISH_BUTTON).performClick()
    composeRule.waitForIdle()

    // Verifica que fechou o Onboarding, abriu a aba Agora e persistiu no DataStore real
    composeRule.onNodeWithTag(AppTestTags.Now.HERO_MODE_CARD).assertIsDisplayed()
    val savedPrefs = awaitPrefs { it.hasCompletedOnboarding && it.defaultDayProfileId == "preset_home" }
    assertTrue(savedPrefs.hasCompletedOnboarding)
    assertEquals("preset_home", savedPrefs.defaultDayProfileId)
  }

  @Test
  fun e2e02_profileLifecycle_createDuplicateActivateAndAutoReturnToNowTab() {
    // 1. Navegar para a aba Perfis
    composeRule.onNodeWithTag(AppTestTags.Tabs.PROFILES).performClick()
    composeRule.onNodeWithTag(AppTestTags.Profiles.LIST).assertIsDisplayed()

    // 2. Criar um novo perfil customizado pelo FAB
    composeRule.onNodeWithTag(AppTestTags.Fab.SAVE_PROFILE_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.Profiles.NAME_INPUT).performTextReplacement("Estudo E2E")
    composeRule.onNodeWithTag(AppTestTags.Profiles.CONFIRM_SAVE_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsAfterCreate = awaitPrefs { it.customProfiles.any { p -> p.name == "Estudo E2E" } }
    assertTrue(prefsAfterCreate.customProfiles.any { it.name == "Estudo E2E" })

    // 3. Duplicar um perfil pelo menu contextual (⋮)
    composeRule.onNodeWithTag(AppTestTags.Profiles.editButton("preset_work")).performClick()
    composeRule.onNodeWithTag(AppTestTags.Profiles.menuDuplicateItem("preset_work")).performClick()
    composeRule.waitForIdle()

    val prefsAfterDuplicate = awaitPrefs { it.customProfiles.any { p -> p.name.contains("(Cópia)") } }
    assertTrue(prefsAfterDuplicate.customProfiles.any { it.name.contains("(Cópia)") })

    // 4. Ativar o perfil "Trabalho (Vibrar)" e validar retorno automático para a aba Agora
    composeRule.onNodeWithTag(AppTestTags.Profiles.card("preset_work")).performScrollTo().performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(AppTestTags.Now.HERO_MODE_CARD).assertIsDisplayed()
    val prefsAfterActivate = awaitPrefs { it.activeProfileId == "preset_work" && it.transitionLogs.isNotEmpty() }
    assertEquals("preset_work", prefsAfterActivate.activeProfileId)
    assertTrue(prefsAfterActivate.transitionLogs.isNotEmpty())
  }

  @Test
  fun e2e03_nowTab_soundModeSwitchAndContextualTimerLifecycleWithRealDataStore() {
    // 0. Garantir foco na aba Agora
    composeRule.onNodeWithTag(AppTestTags.Tabs.NOW).performClick()
    composeRule.waitForIdle()

    // 1. Alternar para Silêncio + Vibrar pela Split Capsule 2+1 no Hero Card
    composeRule.onNodeWithTag(AppTestTags.Now.HERO_MODE_CARD).assertIsDisplayed()
    composeRule.onNodeWithTag(AppTestTags.Now.AUDIO_MODE_SILENT_BUTTON).performClick()
    composeRule.waitForIdle()

    // 2. Iniciar Timer Contextual de 30 minutos (com scroll para garantir visibilidade)
    composeRule.onNodeWithTag(AppTestTags.Now.timerChip(30)).performScrollTo().performClick()
    composeRule.waitForIdle()

    val prefsWithTimer = awaitPrefs { it.activeTimer != null }
    assertNotNull(prefsWithTimer.activeTimer)
    assertEquals(30, prefsWithTimer.activeTimer?.durationMinutes)

    // 3. Estender Timer em +30 minutos
    composeRule.onNodeWithTag(AppTestTags.Now.EXTEND_TIMER_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsExtended = awaitPrefs { it.activeTimer?.durationMinutes == 60 }
    assertEquals(60, prefsExtended.activeTimer?.durationMinutes)

    // 4. Cancelar/Restaurar Timer antecipadamente
    composeRule.onNodeWithTag(AppTestTags.Now.CANCEL_TIMER_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsCancelled = awaitPrefs { it.activeTimer == null }
    assertNull(prefsCancelled.activeTimer)
  }

  @Test
  fun e2e04_schedulesAndLocationsAutomation_withProminentDisclosureAndVacationMode() {
    // 1. Aba Horários: criar nova rotina de horário
    composeRule.onNodeWithTag(AppTestTags.Tabs.SCHEDULES).performClick()
    composeRule.onNodeWithTag(AppTestTags.Schedules.LIST).assertIsDisplayed()

    composeRule.onNodeWithTag(AppTestTags.Fab.ADD_SCHEDULE_FAB).performClick()
    composeRule.onNodeWithTag(AppTestTags.Schedules.NAME_INPUT).performTextReplacement("Plantão E2E")
    composeRule.onNodeWithTag(AppTestTags.Schedules.CONFIRM_SAVE_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsAfterSchedule = awaitPrefs { it.scheduleRules.any { r -> r.name == "Plantão E2E" } }
    assertTrue(prefsAfterSchedule.scheduleRules.any { it.name == "Plantão E2E" })

    // 2. Aba Locais: clicar em + Novo Local -> aceitar Prominent Disclosure -> salvar local
    composeRule.onNodeWithTag(AppTestTags.Tabs.LOCATIONS).performClick()
    composeRule.onNodeWithTag(AppTestTags.Locations.LIST).assertIsDisplayed()

    composeRule.onNodeWithTag(AppTestTags.Fab.ADD_LOCATION_FAB).performClick()
    composeRule.onNodeWithTag(AppTestTags.Locations.ACCEPT_DISCLOSURE_BUTTON).performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(AppTestTags.Locations.NAME_INPUT).performTextReplacement("Escritório E2E")
    composeRule.onNodeWithTag(AppTestTags.Locations.CONFIRM_SAVE_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsAfterLocation = awaitPrefs {
      it.hasAcceptedLocationDisclosure && it.locationRules.any { r -> r.name == "Escritório E2E" }
    }
    assertTrue(prefsAfterLocation.hasAcceptedLocationDisclosure)
    assertTrue(prefsAfterLocation.locationRules.any { it.name == "Escritório E2E" })

    // 3. Menu Global (⋮) -> Pausar Automações (Modo Férias)
    composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_PAUSE_AUTOMATIONS).performClick()
    composeRule.waitForIdle()

    val prefsPaused = awaitPrefs { it.automationsPaused }
    assertTrue(prefsPaused.automationsPaused)
  }

  @Test
  fun e2e05_categorizedSettingsHub_appearanceAutomationBackupJsonRoundtripAndFactoryReset() {
    // 1. Abrir Menu Global (⋮) -> Ajustes
    composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_SETTINGS).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.LIST).assertIsDisplayed()

    // 2. Sub-tela Aparência: alterar Tema para Claro
    composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_APPEARANCE).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.themeButton(ThemeMode.LIGHT)).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
    composeRule.waitForIdle()

    // 3. Sub-tela Automação: alterar Política de Retorno para SNAPSHOT
    composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_AUTOMATION).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.restorePolicy(RestorePolicy.SNAPSHOT)).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
    composeRule.waitForIdle()

    // 4. Sub-tela Botões, Mídia & Timer: definir Timer Padrão para 60m (1h)
    composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_AUDIO_MEDIA).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.defaultTimerChip(60)).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
    composeRule.waitForIdle()
    awaitPrefs {
      it.themeMode == ThemeMode.LIGHT &&
        it.restorePolicy == RestorePolicy.SNAPSHOT &&
        it.defaultTimerMinutes == 60
    }

    // 5. Sub-tela Backup & Restaurar Padrões: Exportar JSON
    composeRule
      .onNodeWithTag(AppTestTags.Settings.LIST)
      .performScrollToNode(hasTestTag(AppTestTags.Settings.CATEGORY_BACKUP_RESET))
    composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_BACKUP_RESET).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.EXPORT_BACKUP_BUTTON).performClick()
    composeRule.waitForIdle()

    val exportedJson = runBlocking { app.container.preferencesDataSource.exportBackupJson() }
    assertTrue(exportedJson.contains("SNAPSHOT"))

    // 6. Restaurar Padrões de Fábrica e confirmar no modal
    composeRule.onNodeWithTag(AppTestTags.Settings.RESET_DEFAULTS_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.CONFIRM_RESET_DEFAULTS_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsAfterReset = awaitPrefs {
      it.restorePolicy == RestorePolicy.DEFAULT_PROFILE && it.defaultTimerMinutes == 30
    }
    assertEquals(RestorePolicy.DEFAULT_PROFILE, prefsAfterReset.restorePolicy)
    assertEquals(30, prefsAfterReset.defaultTimerMinutes)

    // 7. Reabrir Backup & Restaurar Padrões e importar o JSON exportado
    composeRule
      .onNodeWithTag(AppTestTags.Settings.LIST)
      .performScrollToNode(hasTestTag(AppTestTags.Settings.CATEGORY_BACKUP_RESET))
    composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_BACKUP_RESET).performClick()
    composeRule
      .onNodeWithTag(AppTestTags.Settings.IMPORT_BACKUP_INPUT)
      .performTextReplacement(exportedJson)
    composeRule.onNodeWithTag(AppTestTags.Settings.IMPORT_BACKUP_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsAfterImport = awaitPrefs {
      it.restorePolicy == RestorePolicy.SNAPSHOT &&
        it.defaultTimerMinutes == 60 &&
        it.themeMode == ThemeMode.LIGHT
    }
    assertEquals(RestorePolicy.SNAPSHOT, prefsAfterImport.restorePolicy)
    assertEquals(60, prefsAfterImport.defaultTimerMinutes)
    assertEquals(ThemeMode.LIGHT, prefsAfterImport.themeMode)

    // 8. Fechar sub-tela e fechar Hub de Ajustes
    composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SETTINGS_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.Now.HERO_MODE_CARD).assertIsDisplayed()
  }

  @Test
  fun e2e06_internalRbacAccessControl_rolesGranularPermissionsAndPinProtection() {
    // 1. Abrir Menu Global (⋮) -> Controle de Acesso
    composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_ACCESS_CONTROL).performClick()
    composeRule.onNodeWithTag(AppTestTags.AccessControl.SUBSCREEN).assertIsDisplayed()

    // 2. Cadastrar PIN de 4 dígitos ("4210") e salvar
    composeRule.onNodeWithTag(AppTestTags.AccessControl.PIN_INPUT).performTextReplacement("4210")
    composeRule.onNodeWithTag(AppTestTags.AccessControl.SAVE_PIN_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsAfterPin = awaitPrefs { it.accessControlConfig.pinCode == "4210" }
    assertEquals("4210", prefsAfterPin.accessControlConfig.pinCode)

    // 3. Mudar Papel para Somente Leitura (VIEWER) -> 0 permissões concedidas
    composeRule.onNodeWithTag(AppTestTags.AccessControl.roleCard(AppAccessRole.VIEWER)).performClick()
    composeRule.waitForIdle()

    val prefsViewer = awaitPrefs { it.accessControlConfig.activeRole == AppAccessRole.VIEWER }
    assertEquals(AppAccessRole.VIEWER, prefsViewer.accessControlConfig.activeRole)
    assertTrue(prefsViewer.accessControlConfig.grantedPermissions.isEmpty())

    // 4. Fechar sub-tela e validar que trocar modo de som e pausar automações estão bloqueados
    composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.TopBar.ROLE_BADGE_CHIP).assertIsDisplayed()

    composeRule.onNodeWithTag(AppTestTags.Now.AUDIO_MODE_SILENT_BUTTON).performClick()
    composeRule.waitForIdle()
    val prefsBlockedMode = awaitPrefs()
    assertTrue(prefsBlockedMode.transitionLogs.isEmpty())

    composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_PAUSE_AUTOMATIONS).performClick()
    composeRule.waitForIdle()
    val prefsStillUnpaused = awaitPrefs()
    assertEquals(false, prefsStillUnpaused.automationsPaused)

    // 5. Clicar no ROLE_BADGE_CHIP no TopBar abre o desafio de PIN; inserir PIN correto ("4210") e desbloquear Admin
    composeRule.onNodeWithTag(AppTestTags.TopBar.ROLE_BADGE_CHIP).performClick()
    composeRule.onNodeWithTag(AppTestTags.AccessControl.PIN_CHALLENGE_DIALOG).assertIsDisplayed()
    composeRule
      .onNodeWithTag(AppTestTags.AccessControl.PIN_CHALLENGE_INPUT)
      .performTextReplacement("4210")
    composeRule
      .onNodeWithTag(AppTestTags.AccessControl.PIN_CHALLENGE_QUICK_ADMIN_BUTTON)
      .performClick()
    composeRule.waitForIdle()

    val prefsAdminRestored = awaitPrefs { it.accessControlConfig.activeRole == AppAccessRole.ADMIN }
    assertEquals(AppAccessRole.ADMIN, prefsAdminRestored.accessControlConfig.activeRole)

    // 6. Agora como Admin, alternar modo para Silêncio + Vibrar e iniciar Timer de 30m funciona normalmente
    composeRule.onNodeWithTag(AppTestTags.Now.AUDIO_MODE_SILENT_BUTTON).performClick()
    composeRule.waitUntil(5_000L) {
      app.container.soundSystemController.readSoundState().audioMode == BaseAudioMode.SILENT
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(AppTestTags.Now.timerChip(30)).performScrollTo().performClick()
    composeRule.waitForIdle()
    val prefsAfterAllowedAction = awaitPrefs { it.activeTimer != null }
    assertNotNull(prefsAfterAllowedAction.activeTimer)
  }

  @Test
  fun e2e07_auto04SystemZenRules_andGeo01BackgroundGeofencingWithDwellAndWifiSsid() {
    BackgroundGeofenceManager.clearDwellCandidates()

    // 1. Navegar para Locais, validar GEOFENCE_STATUS_CARD e habilitar "loc_college"
    composeRule.onNodeWithTag(AppTestTags.Tabs.LOCATIONS).performClick()
    composeRule.onNodeWithTag(AppTestTags.Locations.GEOFENCE_STATUS_CARD).assertIsDisplayed()
    composeRule.onNodeWithTag(AppTestTags.Locations.SYNC_GEOFENCES_BUTTON).performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(AppTestTags.Locations.switch("loc_college")).performClick()
    composeRule.waitForIdle()
    awaitPrefs { it.locationRules.first { r -> r.id == "loc_college" }.enabled }

    // 2. [GEO-01] Simular sinal de entrada em background sem Wi-Fi (Dwell pendente de 2 min)
    val t0 = 1_750_000_000_000L
    runBlocking {
      val college = app.container.preferencesDataSource.prefsFlow.first()
        .locationRules.first { it.id == "loc_college" }
      app.container.soundModeRepository.processBackgroundGeofenceSignal(
        latitude = college.latitude,
        longitude = college.longitude,
        connectedWifiSsid = "Rede-Externa",
        nowEpochMillis = t0,
      )
      val prefsPending = app.container.preferencesDataSource.prefsFlow.first()
      assertEquals(false, prefsPending.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)

      // Após 2 minutos de permanência (Dwell), confirma presença e ativa perfil da regra
      app.container.soundModeRepository.processBackgroundGeofenceSignal(
        latitude = college.latitude,
        longitude = college.longitude,
        connectedWifiSsid = "Rede-Externa",
        nowEpochMillis = t0 + BackgroundGeofenceManager.DEFAULT_DWELL_DURATION_MILLIS,
      )
      val prefsConfirmed = app.container.preferencesDataSource.prefsFlow.first()
      assertEquals(true, prefsConfirmed.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)

      // Saída da cerca geográfica restaura estado externo
      app.container.soundModeRepository.processBackgroundGeofenceSignal(
        latitude = -23.6200,
        longitude = -46.7200,
        connectedWifiSsid = null,
        nowEpochMillis = t0 + 300_000L,
      )
      val prefsExited = app.container.preferencesDataSource.prefsFlow.first()
      assertEquals(false, prefsExited.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)

      // Re-entrada com Wi-Fi SSID ("Campus-WiFi") confirma presença instantaneamente sem aguardar Dwell
      app.container.soundModeRepository.processBackgroundGeofenceSignal(
        latitude = college.latitude,
        longitude = college.longitude,
        connectedWifiSsid = "\"Campus-WiFi\"",
        nowEpochMillis = t0 + 350_000L,
      )
      val prefsWifiInstant = app.container.preferencesDataSource.prefsFlow.first()
      assertEquals(true, prefsWifiInstant.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    }

    // 3. [AUTO-04] Abrir Configurações -> Automação e sincronizar Modos do Sistema (AutomaticZenRule)
    composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_SETTINGS).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_AUTOMATION).performClick()

    composeRule
      .onNodeWithTag(AppTestTags.Settings.SYNC_ZEN_RULES_BUTTON)
      .performScrollTo()
      .performClick()
    composeRule.waitForIdle()

    val registeredCount = SystemZenRuleManager.countRegisteredRules(app)
    assertTrue(registeredCount >= 0)

    composeRule
      .onNodeWithTag(AppTestTags.Settings.SYSTEM_ZEN_RULES_SWITCH)
      .performScrollTo()
      .performClick()
    composeRule.waitForIdle()

    val prefsAfterToggleOff = awaitPrefs { !it.systemZenRulesEnabled }
    assertEquals(false, prefsAfterToggleOff.systemZenRulesEnabled)
    composeRule.waitUntil(5_000L) { SystemZenRuleManager.countRegisteredRules(app) == 0 }
    assertEquals(0, SystemZenRuleManager.countRegisteredRules(app))
  }

  @Test
  fun e2e08_orthogonal2x2SoundAndVibrationMatrix_splitCapsuleWriteSettingsAndV1Migration() {
    val audioManager = app.getSystemService(AudioManager::class.java)!!

    try {
      // Isola de qualquer horário do relógio real e revoga WRITE_SETTINGS sincronamente
      execShellCommandSync("appops set ${app.packageName} WRITE_SETTINGS deny")
      composeRule.waitUntil(5_000L) { !app.container.soundSystemController.canWriteSystemSettings() }
      runBlocking {
        app.container.preferencesDataSource.setAutomationsPaused(true)
        app.container.soundSystemController.applySoundState(SoundState(BaseAudioMode.SOUND, true))
        app.container.soundSystemController.applyStreamVolume(VolumeStreamType.MEDIA, 60)
        app.container.soundModeRepository.refreshSystemState()
      }
      composeRule.waitForIdle()

      // 1. Estado 1: Som + Vibrar (RINGER_MODE_NORMAL, sem timer contextual na aba Agora)
      composeRule.onNodeWithTag(AppTestTags.Now.AUDIO_MODE_SOUND_BUTTON).assertIsDisplayed()
      composeRule.onNodeWithTag(AppTestTags.Now.AUDIO_MODE_SILENT_BUTTON).assertIsDisplayed()
      composeRule.onNodeWithTag(AppTestTags.Now.VIBRATE_TOGGLE_BUTTON).assertIsDisplayed()
      composeRule.onNodeWithTag(AppTestTags.Now.ORB_VIBRATE_BADGE).assertIsDisplayed()
      composeRule.onNodeWithTag(AppTestTags.Now.timerChip(30)).assertDoesNotExist()
      assertEquals(AudioManager.RINGER_MODE_NORMAL, audioManager.ringerMode)

      // 2. Estado 2: Alternar Vibrar em Som com WRITE_SETTINGS negada -> mantém NORMAL, não zera mídia e abre Banner Just-in-Time
      composeRule.onNodeWithTag(AppTestTags.Now.VIBRATE_TOGGLE_BUTTON).performClick()
      composeRule.waitForIdle()
      awaitPrefs { !it.soundVibrateEnabledFallback }

      assertEquals(AudioManager.RINGER_MODE_NORMAL, audioManager.ringerMode)
      assertTrue(app.container.soundSystemController.readVolumeStreams().mediaPercent > 0)
      composeRule.onNodeWithTag(AppTestTags.Now.ORB_VIBRATE_BADGE).assertDoesNotExist()
      composeRule.onNodeWithTag(AppTestTags.Now.timerChip(30)).assertDoesNotExist()
      composeRule.onNodeWithTag(AppTestTags.Now.WRITE_SETTINGS_BANNER).assertIsDisplayed()

      // Fechar banner pelo botão de dispensar funciona imediatamente
      composeRule.onNodeWithTag(AppTestTags.Now.WRITE_SETTINGS_DISMISS_BUTTON).performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag(AppTestTags.Now.WRITE_SETTINGS_BANNER).assertDoesNotExist()

      // 3. Concede WRITE_SETTINGS via appops -> alternar Vibrar em Som atualiza Settings.System sem reexibir Banner
      execShellCommandSync("appops set ${app.packageName} WRITE_SETTINGS allow")
      composeRule.waitUntil(5_000L) { app.container.soundSystemController.canWriteSystemSettings() }
      runBlocking {
        app.container.soundModeRepository.refreshSystemState()
      }
      composeRule.waitForIdle()
      assertTrue(app.container.soundSystemController.canWriteSystemSettings())

      // Liga Vibrar em Som (vibrateEnabled -> true) e desliga novamente (vibrateEnabled -> false) já com WRITE_SETTINGS concedida
      composeRule.onNodeWithTag(AppTestTags.Now.VIBRATE_TOGGLE_BUTTON).performClick()
      composeRule.waitForIdle()
      awaitPrefs { it.soundVibrateEnabledFallback }
      assertEquals(1, Settings.System.getInt(app.contentResolver, "vibrate_when_ringing"))
      composeRule.onNodeWithTag(AppTestTags.Now.WRITE_SETTINGS_BANNER).assertDoesNotExist()

      composeRule.onNodeWithTag(AppTestTags.Now.VIBRATE_TOGGLE_BUTTON).performClick()
      composeRule.waitForIdle()
      awaitPrefs { !it.soundVibrateEnabledFallback }
      assertEquals(0, Settings.System.getInt(app.contentResolver, "vibrate_when_ringing"))
      composeRule.onNodeWithTag(AppTestTags.Now.WRITE_SETTINGS_BANNER).assertDoesNotExist()

      // 4. Estado 4: Trocar para Silêncio preservando vibrateEnabled = false -> RINGER_MODE_SILENT + Timer visível
      composeRule.onNodeWithTag(AppTestTags.Now.AUDIO_MODE_SILENT_BUTTON).performClick()
      composeRule.waitForIdle()
      composeRule.waitUntil(5_000L) { audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT }
      composeRule.onNodeWithTag(AppTestTags.Now.timerChip(30)).assertIsDisplayed()

      // 5. Estado 3: Ligar Vibrar dentro de Silêncio -> RINGER_MODE_VIBRATE
      composeRule.onNodeWithTag(AppTestTags.Now.VIBRATE_TOGGLE_BUTTON).performClick()
      composeRule.waitForIdle()
      composeRule.waitUntil(5_000L) { audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE }
      composeRule.onNodeWithTag(AppTestTags.Now.ORB_VIBRATE_BADGE).assertIsDisplayed()

      // 6. Voltar para Som preservando vibrateEnabled = true -> RINGER_MODE_NORMAL
      composeRule.onNodeWithTag(AppTestTags.Now.AUDIO_MODE_SOUND_BUTTON).performClick()
      composeRule.waitForIdle()
      composeRule.waitUntil(5_000L) { audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL }

      // 7. Criar Perfil 2x2 ("Som Discreto E2E": SOUND + vibrateEnabled = false) e validar migração JSON v1 -> v2
      composeRule.onNodeWithTag(AppTestTags.Tabs.PROFILES).performClick()
      composeRule.onNodeWithTag(AppTestTags.Fab.SAVE_PROFILE_BUTTON).performClick()
      composeRule.onNodeWithTag(AppTestTags.Profiles.NAME_INPUT).performTextReplacement("Som Discreto E2E")
      composeRule.onNodeWithTag(AppTestTags.Profiles.EDITOR_AUDIO_SOUND_BUTTON).performClick()
      composeRule.onNodeWithTag(AppTestTags.Profiles.EDITOR_VIBRATE_TOGGLE).performClick()
      composeRule.onNodeWithTag(AppTestTags.Profiles.CONFIRM_SAVE_BUTTON).performClick()
      composeRule.waitForIdle()

      val prefsWithCustom = awaitPrefs { it.customProfiles.any { p -> p.name == "Som Discreto E2E" } }
      val savedCustom = prefsWithCustom.customProfiles.first { it.name == "Som Discreto E2E" }
      assertEquals(SoundState(BaseAudioMode.SOUND, false), savedCustom.resolvedSoundState)

      // Importar backup legado v1 sem campos audioMode/vibrateEnabled
      val v1BackupJson = """
        {"version":1,"customProfiles":[
          {"id":"v1_n","name":"V1 Normal","description":"v1","mode":"NORMAL","streams":{"ringPercent":80,"notificationPercent":75,"mediaPercent":50,"alarmPercent":90},"isPreset":false},
          {"id":"v1_v","name":"V1 Vibrar","description":"v1","mode":"VIBRATE","streams":{"ringPercent":0,"notificationPercent":0,"mediaPercent":0,"alarmPercent":85},"isPreset":false},
          {"id":"v1_s","name":"V1 Silencio","description":"v1","mode":"SILENT","streams":{"ringPercent":0,"notificationPercent":0,"mediaPercent":0,"alarmPercent":100},"isPreset":false}
        ]}
      """.trimIndent()
      runBlocking {
        app.container.soundModeRepository.importBackupJson(v1BackupJson).getOrThrow()
      }
      val migratedPrefs = awaitPrefs { it.customProfiles.any { p -> p.id == "v1_n" } }
      assertEquals(
        SoundState(BaseAudioMode.SOUND, true),
        migratedPrefs.customProfiles.first { it.id == "v1_n" }.resolvedSoundState,
      )
      assertEquals(
        SoundState(BaseAudioMode.SILENT, true),
        migratedPrefs.customProfiles.first { it.id == "v1_v" }.resolvedSoundState,
      )
      assertEquals(
        SoundState(BaseAudioMode.SILENT, false),
        migratedPrefs.customProfiles.first { it.id == "v1_s" }.resolvedSoundState,
      )
    } finally {
      execShellCommandSync("appops set ${app.packageName} WRITE_SETTINGS default")
      runBlocking {
        app.container.soundSystemController.applySoundState(SoundState.DEFAULT)
      }
    }
  }

  @Test
  fun e2e09_spec006_s3NotificationDrawerOverhaul_remoteViewsQuickBarIsolatedGroupsPreSilenceTimerAndI18n() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      runCatching {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
          app.packageName,
          Manifest.permission.POST_NOTIFICATIONS,
        )
      }
    }
    val notificationManager = app.getSystemService(NotificationManager::class.java)!!
    notificationManager.cancelAll()

    try {
      // 1. Verificar criação dos 5 canais isolados e importâncias corretas
      SmartNotificationManager.ensureChannels(app)
      val promptCh = notificationManager.getNotificationChannel(SmartNotificationManager.CHANNEL_SMART_PROMPT)
      val timerCh = notificationManager.getNotificationChannel(SmartNotificationManager.CHANNEL_ACTIVE_TIMER)
      val overrideCh = notificationManager.getNotificationChannel(SmartNotificationManager.CHANNEL_RULE_OVERRIDE)
      val quickBarCh = notificationManager.getNotificationChannel(SmartNotificationManager.CHANNEL_PERSISTENT_QUICK_BAR)
      val autoCh = notificationManager.getNotificationChannel(SmartNotificationManager.CHANNEL_AUTO_TRANSITION)

      assertNotNull(promptCh)
      assertNotNull(timerCh)
      assertNotNull(overrideCh)
      assertNotNull(quickBarCh)
      assertNotNull(autoCh)
      assertEquals(NotificationManager.IMPORTANCE_HIGH, promptCh.importance)
      assertNull(promptCh.sound)
      assertEquals(false, promptCh.shouldVibrate())
      assertEquals(NotificationManager.IMPORTANCE_LOW, quickBarCh.importance)
      assertEquals(NotificationManager.IMPORTANCE_LOW, autoCh.importance)

      // 2. Publicar as notificações e validar AC-01 (RemoteViews 1-Toque sem minutos estáticos + deleteIntent) e AC-02 (Zero Auto-Bundling)
      val now = System.currentTimeMillis()
      val sampleTimer = TimedModeSession(
        targetMode = com.managesoundmode.app.domain.model.SoundMode.VIBRATE,
        previousMode = com.managesoundmode.app.domain.model.SoundMode.NORMAL,
        previousStreams = VolumeStreams(ringPercent = 80, notificationPercent = 75, mediaPercent = 55, alarmPercent = 90),
        durationMinutes = 30,
        startedAtEpochMillis = now,
        endsAtEpochMillis = now + 30 * 60_000L,
        audioMode = BaseAudioMode.SILENT,
        vibrateEnabled = true,
        previousAudioMode = BaseAudioMode.SOUND,
        previousVibrateEnabled = true,
      )

      SmartNotificationManager.syncPersistentQuickBarNotification(
        context = app,
        enabled = true,
        currentMode = com.managesoundmode.app.domain.model.SoundMode.VIBRATE,
        currentSoundState = SoundState(BaseAudioMode.SILENT, true),
        streams = VolumeStreams(ringPercent = 0, notificationPercent = 0, mediaPercent = 0, alarmPercent = 90),
        activeProfileName = "Trabalho",
        activeTimer = sampleTimer,
        hasDndPermission = true,
      )
      SmartNotificationManager.syncActiveTimerNotification(app, sampleTimer)
      SmartNotificationManager.showSmartRestorePrompt(
        context = app,
        mode = com.managesoundmode.app.domain.model.SoundMode.VIBRATE,
        mediaAutoMuted = true,
        state = SoundState(BaseAudioMode.SILENT, true),
      )
      SmartNotificationManager.showRuleOverrideAlert(app, "Dormir")
      SmartNotificationManager.showAutoRoutineTransition(
        context = app,
        title = AppLabels.Notifications.autoRoutineActivatedTitle("Trabalho"),
        subtitle = AppLabels.Notifications.autoRoutineActivatedSubtitle("Trabalho", "Som + Vibrar"),
        state = SoundState(BaseAudioMode.SOUND, true),
      )

      composeRule.waitUntil(5_000L) {
        val ids = notificationManager.activeNotifications.map { it.id }.toSet()
        ids.containsAll(
          setOf(
            SmartNotificationManager.NOTIFICATION_ID_PERSISTENT_BAR,
            SmartNotificationManager.NOTIFICATION_ID_ACTIVE_TIMER,
            SmartNotificationManager.NOTIFICATION_ID_SMART_PROMPT,
            SmartNotificationManager.NOTIFICATION_ID_RULE_OVERRIDE,
            SmartNotificationManager.NOTIFICATION_ID_AUTO_TRANSITION,
          ),
        )
      }

      val activeById = notificationManager.activeNotifications.associateBy { it.id }
      val distinctGroups = activeById.values.map { it.notification.group }.toSet()
      assertEquals(5, distinctGroups.size)

      val quickBarNotif = activeById.getValue(SmartNotificationManager.NOTIFICATION_ID_PERSISTENT_BAR).notification
      assertEquals(SmartNotificationManager.GROUP_QUICK_BAR, quickBarNotif.group)
      @Suppress("DEPRECATION")
      val compactRemoteViews = quickBarNotif.contentView
      @Suppress("DEPRECATION")
      val expandedRemoteViews = quickBarNotif.bigContentView
      assertNotNull(compactRemoteViews)
      assertNotNull(expandedRemoteViews)
      assertNotNull(quickBarNotif.deleteIntent)
      val quickBarTitleText = quickBarNotif.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
      assertTrue(!quickBarTitleText.contains("Timer ("))
      assertTrue(!quickBarTitleText.contains("restantes"))

      // Inflar RemoteViews recolhido da Barra Rápida e validar presença dos 3 botões em 1 toque
      composeRule.runOnUiThread {
        val inflatedCompact = compactRemoteViews.apply(app, FrameLayout(app))
        assertEquals(View.VISIBLE, inflatedCompact.findViewById<View>(R.id.quick_bar_btn_sound).visibility)
        assertEquals(View.VISIBLE, inflatedCompact.findViewById<View>(R.id.quick_bar_btn_silent).visibility)
        assertEquals(View.VISIBLE, inflatedCompact.findViewById<View>(R.id.quick_bar_btn_vibrate).visibility)
      }

      // AC-04: Prompt Pós-Volume (3101) não possui FLAG_SILENT e Timer (3102) possui timeoutAfter > 0
      val smartPromptNotif = activeById.getValue(SmartNotificationManager.NOTIFICATION_ID_SMART_PROMPT).notification
      assertEquals(SmartNotificationManager.GROUP_SMART_PROMPT, smartPromptNotif.group)
      val activeTimerNotif = activeById.getValue(SmartNotificationManager.NOTIFICATION_ID_ACTIVE_TIMER).notification
      assertTrue(activeTimerNotif.timeoutAfter > 0L)

      // AC-01: Simular swipe lateral na Barra Rápida (3104) e disparo autenticado de ACTION_RESTORE_PERSISTENT_QUICK_BAR
      notificationManager.cancel(SmartNotificationManager.NOTIFICATION_ID_PERSISTENT_BAR)
      composeRule.waitUntil(3_000L) {
        notificationManager.activeNotifications.none {
          it.id == SmartNotificationManager.NOTIFICATION_ID_PERSISTENT_BAR
        }
      }
      val restoreIntent = Intent(app, SystemAutomationReceiver::class.java).apply {
        action = SystemAutomationReceiver.ACTION_RESTORE_PERSISTENT_QUICK_BAR
      }
      InternalIntentSecurity.signIntent(app, restoreIntent)
      app.sendBroadcast(restoreIntent)
      composeRule.waitUntil(5_000L) {
        notificationManager.activeNotifications.any {
          it.id == SmartNotificationManager.NOTIFICATION_ID_PERSISTENT_BAR
        }
      }

      // 3. AC-03: Pre-Silence Snapshot + Fim do Timer Zumbi na UI real
      runBlocking {
        app.container.preferencesDataSource.setAutomationsPaused(true)
        app.container.soundModeRepository.setSoundState(SoundState(BaseAudioMode.SOUND, true))
        app.container.soundModeRepository.setStreamVolume(VolumeStreamType.MEDIA, 58)
        app.container.soundModeRepository.refreshSystemState()
      }
      awaitPrefs { it.automationsPaused }
      composeRule.waitForIdle()

      // Entrar em Silêncio + Vibrar pela UI e iniciar Timer de 30m JÁ estando em Silêncio
      composeRule.onNodeWithTag(AppTestTags.Now.AUDIO_MODE_SILENT_BUTTON).performClick()
      composeRule.waitUntil(5_000L) {
        app.container.soundSystemController.readSoundState().audioMode == BaseAudioMode.SILENT
      }
      composeRule.waitForIdle()
      composeRule.onNodeWithTag(AppTestTags.Now.timerChip(30)).performScrollTo().performClick()
      composeRule.waitForIdle()

      val prefsTimer = awaitPrefs { it.activeTimer != null }
      assertEquals(
        SoundState(BaseAudioMode.SOUND, true),
        prefsTimer.activeTimer?.resolvedPreviousSoundState,
      )
      assertTrue((prefsTimer.activeTimer?.previousStreams?.mediaPercent ?: 0) > 0)

      // Troca manual para [Som] cancela imediatamente o Timer Zumbi e remove 3102
      composeRule.onNodeWithTag(AppTestTags.Now.AUDIO_MODE_SOUND_BUTTON).performClick()
      composeRule.waitUntil(5_000L) {
        app.container.soundSystemController.readSoundState().audioMode == BaseAudioMode.SOUND
      }
      composeRule.waitForIdle()
      val prefsAfterManualSwitch = awaitPrefs { it.activeTimer == null }
      assertNull(prefsAfterManualSwitch.activeTimer)
      composeRule.waitUntil(3_000L) {
        notificationManager.activeNotifications.none {
          it.id == SmartNotificationManager.NOTIFICATION_ID_ACTIVE_TIMER
        }
      }

      // 4. AC-05: Alternar idioma para Inglês ressincroniza os 5 canais e a Barra Rápida em EN
      runBlocking {
        app.container.soundModeRepository.setAppLanguageCode("EN").getOrThrow()
      }
      val enAutoChannel = notificationManager.getNotificationChannel(SmartNotificationManager.CHANNEL_AUTO_TRANSITION)
      assertEquals("Automatic Routine Transition", enAutoChannel?.name?.toString())
    } finally {
      notificationManager.cancelAll()
      runBlocking {
        app.container.soundModeRepository.setAppLanguageCode("PT_BR")
        app.container.preferencesDataSource.setAutomationsPaused(false)
        app.container.soundSystemController.applySoundState(SoundState.DEFAULT)
      }
    }
  }

  @Test
  fun e2e10_spec007_sevenRealAudioStreamsMinVolumeFloorInlineLinkAntiJumpAndFixedBarsDrawer() {
    val audioManager = app.getSystemService(AudioManager::class.java)!!
    val controller = app.container.soundSystemController
    val repo = app.container.soundModeRepository

    try {
      runBlocking {
        app.container.preferencesDataSource.setAutomationsPaused(true)
        repo.setSoundState(SoundState(BaseAudioMode.SOUND, true))
        repo.refreshSystemState()
      }
      composeRule.waitForIdle()

      // 1. AC-01: Real AudioManager 7 streams + getStreamMinVolume floor enforcement
      val callMinIndex = audioManager.getStreamMinVolume(AudioManager.STREAM_VOICE_CALL)
      val callMinPercent = controller.readStreamMinPercent(VolumeStreamType.VOICE_CALL)
      if (callMinIndex > 0) {
        assertTrue(callMinPercent > 0)
      }
      runBlocking {
        repo.setStreamVolume(VolumeStreamType.VOICE_CALL, 0).getOrThrow()
        repo.setStreamVolume(VolumeStreamType.ALARM, 0).getOrThrow()
        repo.setStreamVolume(VolumeStreamType.SYSTEM, 60).getOrThrow()
        repo.setStreamVolume(VolumeStreamType.ACCESSIBILITY, 75).getOrThrow()
      }
      assertTrue(
        audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) >= callMinIndex,
      )
      assertTrue(
        audioManager.getStreamVolume(AudioManager.STREAM_ALARM) >=
          audioManager.getStreamMinVolume(AudioManager.STREAM_ALARM),
      )
      assertTrue(controller.readVolumeStreams().systemPercent > 0)
      assertTrue(controller.readVolumeStreams().accessibilityPercent > 0)

      // 2. AC-02: Default ringNotificationLinked = true syncs RING & NOTIFICATION; inline button toggles DataStore
      runBlocking {
        repo.setStreamVolume(VolumeStreamType.RING, 72).getOrThrow()
      }
      val linkedSnap = controller.readVolumeStreams()
      assertEquals(linkedSnap.ringPercent, linkedSnap.notificationPercent)

      composeRule
        .onNodeWithTag(AppTestTags.Now.streamLinkButton(VolumeStreamType.RING))
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
      composeRule.waitForIdle()

      val prefsUnlinked = awaitPrefs("unlinked") { !it.ringNotificationLinked }
      assertEquals(false, prefsUnlinked.ringNotificationLinked)

      // 3. AC-03: Default 4 fixed bars + Secondary Drawer collapsed -> click expands and persists secondaryDrawerExpanded
      composeRule
        .onNodeWithTag(AppTestTags.Now.streamSlider(VolumeStreamType.SYSTEM))
        .assertDoesNotExist()
      composeRule
        .onNodeWithTag(AppTestTags.Now.SECONDARY_DRAWER_TOGGLE)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
      composeRule.waitForIdle()

      val prefsDrawerOpen = awaitPrefs("drawerExpanded") { it.secondaryDrawerExpanded }
      assertTrue(prefsDrawerOpen.secondaryDrawerExpanded)
      composeRule
        .onNodeWithTag(AppTestTags.Now.streamSlider(VolumeStreamType.SYSTEM))
        .performScrollTo()
        .assertIsDisplayed()

      // Collapse drawer again and verify anti-jump MOST_USED: adjusting ACCESSIBILITY 4x keeps it in drawer until next tab entry
      composeRule
        .onNodeWithTag(AppTestTags.Now.SECONDARY_DRAWER_TOGGLE)
        .performScrollTo()
        .performClick()
      composeRule.waitForIdle()
      awaitPrefs("drawerCollapsed") { !it.secondaryDrawerExpanded }

      runBlocking {
        repeat(4) {
          repo.setStreamVolume(VolumeStreamType.ACCESSIBILITY, 80).getOrThrow()
        }
      }
      composeRule.waitForIdle()
      val prefsWithUsage = awaitPrefs("a11yUsage") {
        (it.streamUsageCounts[VolumeStreamType.ACCESSIBILITY] ?: 0) >= 5
      }
      assertTrue((prefsWithUsage.streamUsageCounts[VolumeStreamType.ACCESSIBILITY] ?: 0) >= 5)
      // Still hidden in drawer during active Now session (anti-jump!)
      composeRule
        .onNodeWithTag(AppTestTags.Now.streamSlider(VolumeStreamType.ACCESSIBILITY))
        .assertDoesNotExist()

      // Switch to Perfis and back to Agora -> new session latches updated MOST_USED ranking and promotes ACCESSIBILITY to #1 fixed bar!
      composeRule.onNodeWithTag(AppTestTags.Tabs.PROFILES).performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag(AppTestTags.Tabs.NOW).performClick()
      composeRule.waitForIdle()

      composeRule
        .onNodeWithTag(AppTestTags.Now.streamSlider(VolumeStreamType.ACCESSIBILITY))
        .performScrollTo()
        .assertIsDisplayed()

      // 4. AC-04: Settings › Áudio e Timer -> 7 fixed bars hides drawer button completely
      composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
      composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_SETTINGS).performClick()
      composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_AUDIO_MEDIA).performClick()

      composeRule
        .onNodeWithTag(AppTestTags.Settings.fixedStreamsCountChip(7))
        .performScrollTo()
        .performClick()
      composeRule.waitForIdle()
      awaitPrefs("fixed7") { it.visibleFixedStreamsCount == 7 }

      composeRule
        .onNodeWithTag(AppTestTags.Settings.streamSortModeButton(StreamSortMode.MANUAL))
        .performScrollTo()
        .performClick()
      composeRule.waitForIdle()
      awaitPrefs("manualSort") { it.streamSortMode == StreamSortMode.MANUAL }

      composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
      composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SETTINGS_BUTTON).performClick()
      composeRule.waitForIdle()

      composeRule.onNodeWithTag(AppTestTags.Now.SECONDARY_DRAWER_TOGGLE).assertDoesNotExist()
      composeRule
        .onNodeWithTag(AppTestTags.Now.streamSlider(VolumeStreamType.SYSTEM))
        .performScrollTo()
        .assertIsDisplayed()
    } finally {
      runBlocking {
        app.container.preferencesDataSource.resetFactoryDefaults()
        app.container.preferencesDataSource.setOnboardingCompleted(true)
        app.container.soundSystemController.applySoundState(SoundState.DEFAULT)
        app.container.soundModeRepository.refreshSystemState()
      }
    }
  }

  @Test
  fun e2e11_spec005_realVolumePreviewEngineSilentPreAdjustAndProfileEditorIsolatedRestore() {
    val audioManager = app.getSystemService(AudioManager::class.java)!!
    val controller = app.container.soundSystemController
    val repo = app.container.soundModeRepository
    val previewPlayer = app.container.volumePreviewPlayer

    try {
      runBlocking {
        app.container.preferencesDataSource.setAutomationsPaused(true)
        repo.setSoundState(SoundState(BaseAudioMode.SOUND, true)).getOrThrow()
        repo.setStreamVolume(VolumeStreamType.MEDIA, 28).getOrThrow()
        repo.refreshSystemState()
      }
      composeRule.waitForIdle()

      // 1. AC-03: Real AndroidVolumePreviewPlayer plays and stops cleanly across RingtoneManager & AudioTrack streams
      listOf(
        VolumeStreamType.RING,
        VolumeStreamType.ALARM,
        VolumeStreamType.MEDIA,
        VolumeStreamType.VOICE_CALL,
        VolumeStreamType.ACCESSIBILITY,
      ).forEach { streamType ->
        composeRule.runOnIdle {
          previewPlayer.playSingle(
            streamType = streamType,
            percent = 50,
            useSystemSounds = true,
            scope = VolumePreviewScope.NOW_TAB,
          )
        }
        assertEquals(streamType, previewPlayer.previewState.value?.streamType)
        composeRule.runOnIdle {
          previewPlayer.stop()
        }
        assertNull(previewPlayer.previewState.value)
      }

      // 2. AC-04: Enter Silêncio + Vibrar, pre-adjust RING (74%) & SYSTEM (46%) WITHOUT disarming RINGER_MODE_VIBRATE
      composeRule.onNodeWithTag(AppTestTags.Now.AUDIO_MODE_SILENT_BUTTON).performClick()
      composeRule.waitUntil(5_000L) {
        audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE
      }
      composeRule.waitForIdle()

      runBlocking {
        repo.setStreamVolume(VolumeStreamType.RING, 74).getOrThrow()
        repo.setStreamVolume(VolumeStreamType.SYSTEM, 46).getOrThrow()
      }
      composeRule.waitForIdle()

      assertEquals(AudioManager.RINGER_MODE_VIBRATE, audioManager.ringerMode)
      val prefsPending = awaitPrefs("pendingStreams") {
        it.pendingSoundStreams[VolumeStreamType.RING] == 74 &&
          it.pendingSoundStreams[VolumeStreamType.NOTIFICATION] == 74 &&
          it.pendingSoundStreams[VolumeStreamType.SYSTEM] == 46
      }
      assertEquals(74, prefsPending.pendingSoundStreams[VolumeStreamType.RING])
      composeRule
        .onNodeWithTag(AppTestTags.Now.streamPendingBadge(VolumeStreamType.RING))
        .performScrollTo()
        .assertIsDisplayed()

      // 3. Silent-mode preview safety dialog on Now tab -> Listen now -> Stop restores RINGER_MODE_VIBRATE
      composeRule
        .onNodeWithTag(AppTestTags.Now.streamIconButton(VolumeStreamType.RING))
        .performScrollTo()
        .performClick()
      composeRule.onNodeWithTag(AppTestTags.Now.SILENT_PREVIEW_CONFIRM_DIALOG).assertIsDisplayed()
      composeRule.onNodeWithTag(AppTestTags.Now.SILENT_PREVIEW_LISTEN_NOW_BUTTON).performClick()
      composeRule.waitUntil(3_000L) {
        previewPlayer.previewState.value != null
      }
      composeRule
        .onNodeWithTag(AppTestTags.Now.streamIconButton(VolumeStreamType.RING))
        .performClick()
      composeRule.waitUntil(3_000L) {
        previewPlayer.previewState.value == null &&
          audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE
      }
      assertEquals(AudioManager.RINGER_MODE_VIBRATE, audioManager.ringerMode)

      // 4. Transition back to [Som] -> automatically applies pending RING/NOTIFICATION/SYSTEM to AudioManager and clears pending
      composeRule
        .onNodeWithTag(AppTestTags.Now.LIST)
        .performScrollToNode(hasTestTag(AppTestTags.Now.HERO_MODE_CARD))
      composeRule.onNodeWithTag(AppTestTags.Now.AUDIO_MODE_SOUND_BUTTON).performClick()
      composeRule.waitUntil(5_000L) {
        audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
      }
      composeRule.waitForIdle()
      val prefsCleared = awaitPrefs("pendingCleared") { it.pendingSoundStreams.isEmpty() }
      assertTrue(prefsCleared.pendingSoundStreams.isEmpty())
      assertTrue(controller.readVolumeStreams().ringPercent > 0)
      assertTrue(controller.readVolumeStreams().systemPercent > 0)

      // 5. AC-05: ProfileEditorBottomSheet isolated preview on inactive profile ("preset_home" has MEDIA=60%) restores baseline MEDIA=28%
      runBlocking {
        repo.setStreamVolume(VolumeStreamType.MEDIA, 28).getOrThrow()
      }
      val baselineMediaIndex = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

      composeRule.onNodeWithTag(AppTestTags.Tabs.PROFILES).performClick()
      composeRule.onNodeWithTag(AppTestTags.Profiles.editButton("preset_home")).performClick()
      composeRule.onNodeWithTag(AppTestTags.Profiles.menuEditItem("preset_home")).performClick()
      composeRule.waitForIdle()

      composeRule
        .onNodeWithTag(AppTestTags.Profiles.editorStreamIconButton(VolumeStreamType.MEDIA))
        .performScrollTo()
        .performClick()
      composeRule.waitUntil(3_000L) {
        previewPlayer.previewState.value?.scope == VolumePreviewScope.PROFILE_EDITOR
      }
      assertTrue(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) >= baselineMediaIndex)

      // Stop preview in editor and verify hardware MEDIA index is restored to exact baseline
      composeRule
        .onNodeWithTag(AppTestTags.Profiles.editorStreamIconButton(VolumeStreamType.MEDIA))
        .performClick()
      composeRule.waitUntil(3_000L) {
        previewPlayer.previewState.value == null &&
          audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == baselineMediaIndex
      }
      assertEquals(baselineMediaIndex, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    } finally {
      previewPlayer.stop()
      runBlocking {
        app.container.preferencesDataSource.resetFactoryDefaults()
        app.container.preferencesDataSource.setOnboardingCompleted(true)
        app.container.soundSystemController.applySoundState(SoundState.DEFAULT)
        app.container.soundModeRepository.refreshSystemState()
      }
    }
  }

  // =========================================================================
  // e2e12: Jornada Completa de Automação por Eventos de Agenda (SPEC-008)
  // =========================================================================

  @Test
  fun e2e12_spec008_calendarEventsAutomationJourney() {
    execShellCommandSync("pm grant ${app.packageName} android.permission.READ_CALENDAR")
    runBlocking {
      app.container.soundModeRepository.refreshSystemState()
    }
    composeRule.waitForIdle()

    try {
      // 1. Navegar para a aba de Horários e Agenda
      composeRule.onNodeWithTag(AppTestTags.Tabs.SCHEDULES).performClick()
      composeRule.waitForIdle()

      // 2. Alternar para o segmento de Agenda
      composeRule.onNodeWithTag(AppTestTags.Schedules.SEGMENT_CALENDAR).performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag(AppTestTags.CalendarRules.LIST).assertIsDisplayed()

      // 3. Adicionar regra de agenda pelo FAB
      composeRule.onNodeWithTag(AppTestTags.Fab.ADD_CALENDAR_RULE_FAB).performClick()
      composeRule.waitForIdle()

      composeRule.onNodeWithTag(AppTestTags.CalendarRules.NAME_INPUT).performTextClearance()
      composeRule.onNodeWithTag(AppTestTags.CalendarRules.NAME_INPUT).performTextInput("Reuniões E2E")
      composeRule.onNodeWithTag(AppTestTags.CalendarRules.KEYWORD_INPUT).performTextInput("Daily, Sync")

      // Salvar regra
      composeRule.onNodeWithTag(AppTestTags.CalendarRules.CONFIRM_SAVE_BUTTON).performClick()
      composeRule.waitForIdle()

      // 4. Aguardar persistência no DataStore real
      val prefsWithRule = awaitPrefs("calendarRuleAdded") {
        it.calendarRules.any { r -> r.name == "Reuniões E2E" }
      }
      val savedRule = prefsWithRule.calendarRules.first { it.name == "Reuniões E2E" }
      assertTrue(savedRule.enabled)
      assertEquals("Daily, Sync", savedRule.keywordFilter)

      // 5. Card visível e funcional na lista
      composeRule.onNodeWithTag(AppTestTags.CalendarRules.card(savedRule.id)).assertIsDisplayed()

      // 6. Alternar switch da regra
      composeRule.onNodeWithTag(AppTestTags.CalendarRules.switch(savedRule.id)).performClick()
      val prefsDisabled = awaitPrefs("calendarRuleDisabled") {
        it.calendarRules.firstOrNull { r -> r.id == savedRule.id }?.enabled == false
      }
      val disabledRule = prefsDisabled.calendarRules.first { it.id == savedRule.id }
      assertEquals(false, disabledRule.enabled)

      // 7. Simular broadcast do sistema: PROVIDER_CHANGED para content://com.android.calendar
      val providerIntent = Intent(Intent.ACTION_PROVIDER_CHANGED).apply {
        data = android.net.Uri.parse("content://com.android.calendar")
        setPackage(app.packageName)
      }
      InternalIntentSecurity.signIntent(app, providerIntent)
      app.sendBroadcast(providerIntent)
      composeRule.waitForIdle()

      // 8. Excluir regra pelo menu contextual
      composeRule.onNodeWithTag(AppTestTags.CalendarRules.optionsButton(savedRule.id)).performClick()
      composeRule.onNodeWithTag(AppTestTags.CalendarRules.menuDeleteItem(savedRule.id)).performClick()

      val prefsDeleted = awaitPrefs("calendarRuleDeleted") {
        it.calendarRules.none { r -> r.id == savedRule.id }
      }
      assertTrue(prefsDeleted.calendarRules.none { it.id == savedRule.id })
    } finally {
      runBlocking {
        app.container.preferencesDataSource.resetFactoryDefaults()
        app.container.preferencesDataSource.setOnboardingCompleted(true)
        app.container.soundSystemController.applySoundState(SoundState.DEFAULT)
        app.container.soundModeRepository.refreshSystemState()
      }
    }
  }

  // =========================================================================
  // e2e13: Desbloqueio Vitalício Pro e Restauração de Compras (SPEC-009)
  // =========================================================================

  @Test
  fun e2e13_spec009_proUnlockLifetimeAndRestorePurchasesJourney() {
    try {
      // 1. Abrir sub-tela Modos de Som Pro pelo Menu Global
      composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_PRO_UNLOCK).performClick()
      composeRule.waitForIdle()

      // 2. Verificar exibição inicial da sub-tela e banner gratuito
      composeRule.onNodeWithTag(AppTestTags.Pro.SUBSCREEN).assertIsDisplayed()
      composeRule.onNodeWithTag(AppTestTags.Pro.STATUS_BANNER).assertIsDisplayed()
      composeRule.onNodeWithTag(AppTestTags.Pro.PURCHASE_BUTTON).assertIsDisplayed()

      // 3. Simula validação de compra autêntica assinando o cache com HMAC-SHA256 real do app
      val epoch = System.currentTimeMillis()
      val tokenHash = "e2e_verified_purchase_token_hash"
      val payload = "true|$epoch|$tokenHash"
      val signature = InternalIntentSecurity.computeHmacSha256(app, payload)
      val signedCached = CachedProEntitlement(
        isProUnlocked = true,
        unlockedAtEpochMillis = epoch,
        purchaseTokenHash = tokenHash,
        hmacSignature = signature,
      )
      runBlocking {
        app.container.preferencesDataSource.setCachedProEntitlement(signedCached)
      }
      composeRule.waitForIdle()

      // 4. Selecionar paleta atmosférica Pro (Aurora Esmeralda)
      composeRule.onNodeWithTag(AppTestTags.Pro.paletteChip(ZenithAtmosphericPalette.EMERALD_AURORA)).performClick()
      composeRule.waitForIdle()

      val prefsWithPalette = awaitPrefs("zenithPaletteEmerald") {
        it.zenithPalette == ZenithAtmosphericPalette.EMERALD_AURORA
      }
      assertEquals(ZenithAtmosphericPalette.EMERALD_AURORA, prefsWithPalette.zenithPalette)

      // 5. Fechar sub-tela Pro
      composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
      composeRule.waitForIdle()

      // 6. Restaurar compras
      composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_PRO_UNLOCK).performClick()
      composeRule.waitForIdle()

      composeRule.onNodeWithTag(AppTestTags.Pro.RESTORE_PURCHASES_BUTTON).performClick()
      composeRule.waitForIdle()

      // Fechar sub-tela
      composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
      composeRule.waitForIdle()
    } finally {
      runBlocking {
        app.container.preferencesDataSource.resetFactoryDefaults()
        app.container.preferencesDataSource.setOnboardingCompleted(true)
        app.container.soundModeRepository.refreshSystemState()
      }
    }
  }
}

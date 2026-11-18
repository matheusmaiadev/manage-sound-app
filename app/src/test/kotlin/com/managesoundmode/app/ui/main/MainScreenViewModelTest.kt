package com.managesoundmode.app.ui.main

import com.managesoundmode.app.data.local.PersistedSoundPrefs
import com.managesoundmode.app.data.local.SoundPreferencesDataSource
import com.managesoundmode.app.data.local.SoundPrefsBackupPayload
import com.managesoundmode.app.data.repository.DefaultSoundModeRepository
import com.managesoundmode.app.data.system.DndPermissionRequiredException
import com.managesoundmode.app.data.system.SoundSystemController
import com.managesoundmode.app.domain.model.AccessControlConfig
import com.managesoundmode.app.domain.model.AppAccessRole
import com.managesoundmode.app.domain.model.AppPermission
import com.managesoundmode.app.domain.model.BaseAudioMode
import com.managesoundmode.app.domain.model.CalendarRule
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SavedStateSnapshot
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.SoundState
import com.managesoundmode.app.domain.model.StreamSortMode
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.TransitionLogEntry
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.system.BackgroundGeofenceManager
import com.managesoundmode.app.system.FakeVolumePreviewPlayer
import com.managesoundmode.app.system.SystemZenRuleManager
import com.managesoundmode.app.system.VolumePreviewKind
import com.managesoundmode.app.system.VolumePreviewScope
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppLanguage
import com.managesoundmode.app.ui.common.AppTestTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var fakeSystemController: FakeSoundSystemController
  private lateinit var fakePreferences: FakeSoundPreferencesDataSource
  private lateinit var repository: DefaultSoundModeRepository
  private lateinit var viewModel: MainScreenViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    fakeSystemController = FakeSoundSystemController()
    fakePreferences = FakeSoundPreferencesDataSource()
    repository = DefaultSoundModeRepository(
      systemController = fakeSystemController,
      preferencesDataSource = fakePreferences,
    )
    viewModel = MainScreenViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun uiState_whenSubscribed_emitsReadyWithSystemDefaults() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val state = viewModel.uiState.value
    assertTrue(state is MainScreenUiState.Ready)
    val ready = state as MainScreenUiState.Ready
    assertEquals(SoundMode.NORMAL, ready.currentMode)
    assertEquals(SoundProfile.defaultPresets.size, ready.profiles.size)
    assertEquals(ScheduleRule.defaultSchedules.size, ready.scheduleRules.size)
    assertEquals(LocationRule.defaultLocations.size, ready.locationRules.size)
  }

  @Test
  fun onSelectMode_vibrate_updatesModeToVibrate() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSelectMode(SoundMode.VIBRATE)

    val ready = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, ready.currentMode)
    assertEquals("Modo alterado para Vibrar", ready.statusMessage)
  }

  @Test
  fun onSelectMode_silentWithoutDndPermission_keepsPreviousModeAndShowsErrorMessage() = runTest {
    fakeSystemController.dndGranted = false
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSelectMode(SoundMode.SILENT)

    val ready = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.NORMAL, ready.currentMode)
    assertNotNull(ready.statusMessage)
    assertTrue(ready.statusMessage!!.contains("Não Perturbe"))
  }

  @Test
  fun onStartTimer_andCancelTimer_restoresPreviousMode() = runTest {
    // Desabilita horários padrão para testar isolamento do timer
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onStartTimer(targetMode = SoundMode.VIBRATE, durationMinutes = 30)
    val duringTimer = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, duringTimer.currentMode)
    assertNotNull(duringTimer.activeTimer)
    assertEquals(30, duringTimer.activeTimer?.durationMinutes)

    viewModel.onCancelTimer()
    val afterCancel = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.NORMAL, afterCancel.currentMode)
    assertNull(afterCancel.activeTimer)
  }

  @Test
  fun onSaveCurrentAsProfile_addsCustomProfileAndActivatesIt() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSaveCurrentAsProfile("Estudo Profundo", "Foco para leitura")
    val ready = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundProfile.defaultPresets.size + 1, ready.profiles.size)
    assertTrue(ready.profiles.any { it.name == "Estudo Profundo" && !it.isPreset })
    assertNotNull(ready.activeProfileId)
  }

  @Test
  fun onToggleLocationPresence_activatesLocationRuleProfile() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
    viewModel.onToggleLocationRule(collegeRule, enabled = true)
    viewModel.onToggleLocationPresence("loc_college", inside = true)

    val ready = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, ready.currentMode)
    assertTrue(ready.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
  }

  @Test
  fun onSetRestorePolicy_updatesPolicyInUiState() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSetRestorePolicy(RestorePolicy.SNAPSHOT)
    val ready = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(RestorePolicy.SNAPSHOT, ready.restorePolicy)
  }

  @Test
  fun onSetThemeMode_andUpsertProfile_updatesThemeAndEditsPresetProfile() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSetThemeMode(ThemeMode.LIGHT)
    val afterTheme = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(ThemeMode.LIGHT, afterTheme.themeMode)

    val workPreset = afterTheme.profiles.first { it.id == "preset_work_high" }
    val editedWork = workPreset.copy(
      streams = workPreset.streams.copy(ringPercent = 75, mediaPercent = 15),
    )
    viewModel.onUpsertProfile(editedWork, applyImmediately = true)

    val afterEdit = viewModel.uiState.value as MainScreenUiState.Ready
    val updatedPreset = afterEdit.profiles.first { it.id == "preset_work_high" }
    assertEquals(75, updatedPreset.streams.ringPercent)
    assertEquals(15, updatedPreset.streams.mediaPercent)
    assertEquals("preset_work_high", afterEdit.activeProfileId)
  }

  @Test
  fun onUpsertProfile_whenEditingActiveProfileWithoutApplyNow_stillSyncsHardwareStreams() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val initialReady = viewModel.uiState.value as MainScreenUiState.Ready
    val workPreset = initialReady.profiles.first { it.id == "preset_work_high" }
    viewModel.onActivateProfile(workPreset)

    val editedWork = workPreset.copy(
      streams = workPreset.streams.copy(ringPercent = 64, mediaPercent = 22),
    )
    viewModel.onUpsertProfile(editedWork, applyImmediately = false)

    val afterEdit = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals("preset_work_high", afterEdit.activeProfileId)
    assertEquals(64, afterEdit.streams.ringPercent)
    assertEquals(22, afterEdit.streams.mediaPercent)
  }

  @Test
  fun onSetMuteMediaOnSilent_whileInVibrate_immediatelyMutesAndRestoresMedia() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // Start in NORMAL with muteMediaOnSilent disabled and media at 60%
    viewModel.onSetMuteMediaOnSilent(false)
    viewModel.onStreamVolumeChange(VolumeStreamType.MEDIA, 60)
    viewModel.onSelectMode(SoundMode.VIBRATE)

    val beforeToggle = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, beforeToggle.currentMode)
    assertEquals(60, beforeToggle.streams.mediaPercent)

    // Enable muteMediaOnSilent while already in VIBRATE -> media should immediately drop to 0%
    viewModel.onSetMuteMediaOnSilent(true)
    val afterEnable = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(0, afterEnable.streams.mediaPercent)

    // Disable muteMediaOnSilent while still in VIBRATE -> media should restore to 60%
    viewModel.onSetMuteMediaOnSilent(false)
    val afterDisable = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(60, afterDisable.streams.mediaPercent)
  }

  @Test
  fun onStreamVolumeChange_ringAboveZeroInVibrate_switchesModeToNormal() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSelectMode(SoundMode.VIBRATE)
    assertEquals(SoundMode.VIBRATE, (viewModel.uiState.value as MainScreenUiState.Ready).currentMode)

    // Com silentPreAdjustEnabled = true (padrão SPEC-005), preserva VIBRATE e guarda pré-ajuste pendente
    viewModel.onStreamVolumeChange(VolumeStreamType.RING, 70)
    val duringSilentPreAdjust = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, duringSilentPreAdjust.currentMode)
    assertEquals(70, duringSilentPreAdjust.displayedStreamPercent(VolumeStreamType.RING))

    // Quando silentPreAdjustEnabled = false, ajustar RING > 0% desarma VIBRATE e muda para NORMAL
    viewModel.onSetSilentPreAdjustEnabled(false)
    viewModel.onStreamVolumeChange(VolumeStreamType.RING, 70)
    val afterRing = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.NORMAL, afterRing.currentMode)
    assertEquals(70, afterRing.streams.ringPercent)
  }

  @Test
  fun handleExternalAudioChange_whenRaisingMediaVolumeWhileAlreadyInVibrate_neverRevertsToZero() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // Enter VIBRATE -> media is auto-muted to 0%
    viewModel.onSelectMode(SoundMode.VIBRATE)
    assertEquals(0, (viewModel.uiState.value as MainScreenUiState.Ready).streams.mediaPercent)

    // User presses physical Volume Up button while in VIBRATE to raise media to 40%
    fakeSystemController.streams = fakeSystemController.streams.copy(mediaPercent = 40)
    fakeSystemController.externalFlag = true
    repository.handleExternalAudioChange()

    // Volume MUST stay at 40% and never revert to 0%
    val afterHardwareVolumeUp = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, afterHardwareVolumeUp.currentMode)
    assertEquals(40, afterHardwareVolumeUp.streams.mediaPercent)
  }

  @Test
  fun onRefreshSystemState_afterIncreasingVolumeDuringActiveRule_neverRevertsToRuleVolume() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
    viewModel.onToggleLocationRule(collegeRule, enabled = true)
    viewModel.onToggleLocationPresence("loc_college", inside = true)
    assertEquals(0, (viewModel.uiState.value as MainScreenUiState.Ready).streams.mediaPercent)

    // User increases media volume to 65%
    viewModel.onStreamVolumeChange(VolumeStreamType.MEDIA, 65)
    assertEquals(65, (viewModel.uiState.value as MainScreenUiState.Ready).streams.mediaPercent)

    // Simulate onResume calling onRefreshSystemState() -> evaluateAndApplyRules()
    viewModel.onRefreshSystemState()

    val afterResume = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(65, afterResume.streams.mediaPercent)
  }

  @Test
  fun onSetPersistentQuickBarEnabled_updatesPreferenceAndTriggersRefresh() = runTest {
    var surfaceRefreshCount = 0
    val repoWithSurfaceRefresh = DefaultSoundModeRepository(
      systemController = fakeSystemController,
      preferencesDataSource = fakePreferences,
      onRefreshExternalSurfaces = { surfaceRefreshCount++ },
    )
    val vm = MainScreenViewModel(repoWithSurfaceRefresh)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      vm.uiState.collect()
    }

    assertTrue((vm.uiState.value as MainScreenUiState.Ready).persistentQuickBarEnabled)

    vm.onSetPersistentQuickBarEnabled(false)
    val afterDisable = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(false, afterDisable.persistentQuickBarEnabled)
    assertEquals("Gadget removido da barra de notificações", afterDisable.statusMessage)
    assertEquals(1, surfaceRefreshCount)

    vm.onSetPersistentQuickBarEnabled(true)
    val afterEnable = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(true, afterEnable.persistentQuickBarEnabled)
    assertEquals("Gadget fixado na barra de notificações", afterEnable.statusMessage)
    assertEquals(2, surfaceRefreshCount)
  }

  @Test
  fun onSetQuickTileSlotProfileId_andToggleQuickMediaMute_workCorrectly() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSetQuickTileSlotProfileId(1, "preset_home")
    viewModel.onSetQuickTileSlotProfileId(2, "preset_meeting")

    val readyAfterSlots = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals("preset_home", readyAfterSlots.quickTileSlot1ProfileId)
    assertEquals("preset_meeting", readyAfterSlots.quickTileSlot2ProfileId)

    // Media starts at 50%; toggleQuickMediaMute should mute to 0% and save 50%
    viewModel.onStreamVolumeChange(VolumeStreamType.MEDIA, 64)
    val mutedPercent = repository.toggleQuickMediaMute().getOrThrow()
    assertEquals(0, mutedPercent)
    assertEquals(0, (viewModel.uiState.value as MainScreenUiState.Ready).streams.mediaPercent)

    // Second toggle should restore back to 64%
    val restoredPercent = repository.toggleQuickMediaMute().getOrThrow()
    assertEquals(64, restoredPercent)
    assertEquals(64, (viewModel.uiState.value as MainScreenUiState.Ready).streams.mediaPercent)
  }

  @Test
  fun onDeleteCustomProfile_canDeletePresetProfile_andAutoSaveUpdatesWithoutStatusMessage() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val initialProfiles = (viewModel.uiState.value as MainScreenUiState.Ready).profiles
    assertTrue(initialProfiles.any { it.id == "preset_meeting" })

    viewModel.onDeleteCustomProfile("preset_meeting")
    val afterDelete = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(afterDelete.profiles.none { it.id == "preset_meeting" })
    assertEquals("Perfil removido", afterDelete.statusMessage)
    viewModel.onDismissMessage()

    val sleepProfile = afterDelete.profiles.first { it.id == "preset_sleep" }
    viewModel.onUpsertProfile(
      sleepProfile.copy(name = "Dormir Profundo"),
      applyImmediately = false,
      showFeedback = false,
    )
    val afterAutoSave = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals("Dormir Profundo", afterAutoSave.profiles.first { it.id == "preset_sleep" }.name)
    assertNull(afterAutoSave.statusMessage)
  }

  @Test
  fun onSyncLocationsWithGps_activatesGeofenceWhenInsideRadius_andDeactivatesWhenOutside() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
    viewModel.onToggleLocationRule(collegeRule, enabled = true)

    // Coordinates ~30m away from loc_college (-23.5505, -46.6333, radius 200m)
    viewModel.onSyncLocationsWithGps(latitude = -23.5506, longitude = -46.6334)
    val insideState = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(insideState.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    assertEquals(SoundMode.VIBRATE, insideState.currentMode)

    // Coordinates ~5km away -> outside all geofences
    viewModel.onSyncLocationsWithGps(latitude = -23.6000, longitude = -46.7000)
    val outsideState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(false, outsideState.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    assertEquals(SoundMode.NORMAL, outsideState.currentMode)
  }

  @Test
  fun appLabelsAndAppTestTags_provideBilingualTranslationsAndDeterministicTestIds() {
    try {
      AppLabels.currentLanguage = AppLanguage.PT_BR
      assertEquals("Modos de Som", AppLabels.Tabs.nowTopBarTitle)
      assertEquals("Agora", AppLabels.Tabs.nowLabel)
      assertEquals("Som Ativo", SoundMode.NORMAL.title)
      assertEquals("Som + Vibrar", SoundState(BaseAudioMode.SOUND, true).title)
      assertEquals("Som (Sem Vibrar)", SoundState(BaseAudioMode.SOUND, false).title)
      assertEquals("Silêncio + Vibrar", SoundState(BaseAudioMode.SILENT, true).title)
      assertEquals("Silêncio Total", SoundState(BaseAudioMode.SILENT, false).title)
      assertEquals("Toque", VolumeStreamType.RING.label)
      assertEquals("Ligações recebidas", VolumeStreamType.RING.microDescription)
      assertEquals("Sistema", VolumeStreamType.SYSTEM.label)
      assertEquals("Chamada", VolumeStreamType.VOICE_CALL.label)
      assertEquals("Acessibilidade", VolumeStreamType.ACCESSIBILITY.label)
      assertEquals("Mais usados", StreamSortMode.MOST_USED.label)
      assertEquals(
        "Outros volumes (Sistema, Chamada, Acessibilidade)",
        AppLabels.Now.secondaryDrawerCollapsedLabel(VolumeStreamType.secondaryDefaultStreams),
      )

      AppLabels.currentLanguage = AppLanguage.EN
      assertEquals("Sound Modes", AppLabels.Tabs.nowTopBarTitle)
      assertEquals("Now", AppLabels.Tabs.nowLabel)
      assertEquals("Active Sound", SoundMode.NORMAL.title)
      assertEquals("Sound + Vibrate", SoundState(BaseAudioMode.SOUND, true).title)
      assertEquals("Sound (No Vibrate)", SoundState(BaseAudioMode.SOUND, false).title)
      assertEquals("Silent + Vibrate", SoundState(BaseAudioMode.SILENT, true).title)
      assertEquals("Total Silence", SoundState(BaseAudioMode.SILENT, false).title)
      assertEquals("Ring", VolumeStreamType.RING.label)
      assertEquals("Incoming calls", VolumeStreamType.RING.microDescription)
      assertEquals("System", VolumeStreamType.SYSTEM.label)
      assertEquals("Call", VolumeStreamType.VOICE_CALL.label)
      assertEquals("Accessibility", VolumeStreamType.ACCESSIBILITY.label)
      assertEquals("Most used", StreamSortMode.MOST_USED.label)
      assertEquals("Locations", AppLabels.Tabs.locationsLabel)

      assertEquals("tab_now", AppTestTags.Tabs.NOW)
      assertEquals("audio_mode_sound_button", AppTestTags.Now.AUDIO_MODE_SOUND_BUTTON)
      assertEquals("audio_mode_silent_button", AppTestTags.Now.AUDIO_MODE_SILENT_BUTTON)
      assertEquals("vibrate_toggle_button", AppTestTags.Now.VIBRATE_TOGGLE_BUTTON)
      assertEquals("write_settings_banner", AppTestTags.Now.WRITE_SETTINGS_BANNER)
      assertEquals("editor_audio_sound_button", AppTestTags.Profiles.EDITOR_AUDIO_SOUND_BUTTON)
      assertEquals("editor_audio_silent_button", AppTestTags.Profiles.EDITOR_AUDIO_SILENT_BUTTON)
      assertEquals("editor_vibrate_toggle", AppTestTags.Profiles.EDITOR_VIBRATE_TOGGLE)
      assertEquals("mode_button_VIBRATE", AppTestTags.Now.modeButton(SoundMode.VIBRATE))
      assertEquals("profile_card_preset_sleep", AppTestTags.Profiles.card("preset_sleep"))
      assertEquals("schedule_switch_sched_work", AppTestTags.Schedules.switch("sched_work"))
      assertEquals("location_card_loc_office", AppTestTags.Locations.card("loc_office"))
      assertEquals("language_button_EN", AppTestTags.Settings.languageButton(AppLanguage.EN))
      assertEquals("topbar_overflow_menu_button", AppTestTags.TopBar.OVERFLOW_MENU_BUTTON)
      assertEquals(SoundState(BaseAudioMode.SOUND, true), SoundState(BaseAudioMode.SOUND, true))
      assertEquals("settings_notification_permission_banner", AppTestTags.Settings.NOTIFICATION_PERMISSION_BANNER)
      assertEquals(
        "settings_authorize_notification_permission_button",
        AppTestTags.Settings.AUTHORIZE_NOTIFICATION_PERMISSION_BUTTON,
      )
      assertEquals("settings_category_appearance", AppTestTags.Settings.CATEGORY_APPEARANCE)
    } finally {
      AppLabels.currentLanguage = AppLanguage.PT_BR
    }
  }

  @Test
  fun onSetAutomationsPaused_pausesAndResumesActiveRuleEvaluation() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
    viewModel.onToggleLocationRule(collegeRule, enabled = true)
    viewModel.onToggleLocationPresence("loc_college", inside = true)
    assertEquals(SoundMode.VIBRATE, (viewModel.uiState.value as MainScreenUiState.Ready).currentMode)

    // Enable Vacation Mode (Pause Automations) -> should restore to Default Day Profile (NORMAL)
    viewModel.onSetAutomationsPaused(true)
    val pausedState = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(pausedState.automationsPaused)
    assertEquals(SoundMode.NORMAL, pausedState.currentMode)

    // Resume Automations -> should immediately re-evaluate and re-apply the active location rule (VIBRATE)
    viewModel.onSetAutomationsPaused(false)
    val resumedState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(false, resumedState.automationsPaused)
    assertEquals(SoundMode.VIBRATE, resumedState.currentMode)
  }

  @Test
  fun onResetFactoryDefaults_andDuplicateItems_andPersistLanguage_workCorrectly() = runTest {
    try {
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect()
      }

      // Delete a preset profile and change language to EN
      viewModel.onDeleteCustomProfile("preset_meeting")
      viewModel.onSetAppLanguage(AppLanguage.EN)
      viewModel.onSetDefaultTimerMinutes(60)

      val modifiedState = viewModel.uiState.value as MainScreenUiState.Ready
      assertTrue(modifiedState.profiles.none { it.id == "preset_meeting" })
      assertEquals("EN", modifiedState.appLanguageCode)
      assertEquals(AppLanguage.EN, AppLabels.currentLanguage)
      assertEquals(60, modifiedState.defaultTimerMinutes)

      // Duplicate a profile and a schedule rule
      val sleepProfile = modifiedState.profiles.first { it.id == "preset_sleep" }
      viewModel.onDuplicateProfile(sleepProfile)
      val firstSchedule = modifiedState.scheduleRules.first()
      viewModel.onDuplicateScheduleRule(firstSchedule)

      val afterDuplicate = viewModel.uiState.value as MainScreenUiState.Ready
      assertTrue(afterDuplicate.profiles.any { it.name.contains("(Cópia)") })
      assertEquals(ScheduleRule.defaultSchedules.size + 1, afterDuplicate.scheduleRules.size)

      // Reset factory defaults -> recovers deleted preset_meeting and resets schedules & timer
      viewModel.onResetFactoryDefaults()
      val afterReset = viewModel.uiState.value as MainScreenUiState.Ready
      assertTrue(afterReset.profiles.any { it.id == "preset_meeting" })
      assertEquals(SoundProfile.defaultPresets.size, afterReset.profiles.size)
      assertEquals(ScheduleRule.defaultSchedules.size, afterReset.scheduleRules.size)
      assertEquals(30, afterReset.defaultTimerMinutes)
    } finally {
      AppLabels.currentLanguage = AppLanguage.PT_BR
    }
  }

  @Test
  fun onCompleteOnboarding_andReopenOnboarding_persistOnboardingFlag() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val initialState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(false, initialState.hasCompletedOnboarding)

    viewModel.onCompleteOnboarding()
    val completedState = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(completedState.hasCompletedOnboarding)

    viewModel.onReopenOnboarding()
    val reopenedState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(false, reopenedState.hasCompletedOnboarding)
  }

  @Test
  fun backupExportAndImport_roundtripAndInvalidJsonHandling_workCorrectly() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // Customize preferences and rules before export
    viewModel.onSetThemeMode(ThemeMode.LIGHT)
    viewModel.onSetRestorePolicy(RestorePolicy.SNAPSHOT)
    viewModel.onSetDefaultDayProfileId("preset_home")
    viewModel.onSetDefaultTimerMinutes(120)
    viewModel.onSaveCurrentAsProfile("Foco Extremo", "Modo de concentração")

    var exportedJson = ""
    viewModel.onExportBackupJson { json -> exportedJson = json }
    assertTrue(exportedJson.contains("Foco Extremo"))
    assertEquals("Backup exportado com sucesso", (viewModel.uiState.value as MainScreenUiState.Ready).statusMessage)

    // Reset factory defaults to wipe custom profile and settings
    viewModel.onResetFactoryDefaults()
    val afterReset = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(afterReset.profiles.none { it.name == "Foco Extremo" })
    assertEquals(RestorePolicy.DEFAULT_PROFILE, afterReset.restorePolicy)
    assertEquals(30, afterReset.defaultTimerMinutes)

    // Import valid JSON backup -> restores custom profile and preferences
    viewModel.onImportBackupJson(exportedJson)
    val afterImport = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(afterImport.profiles.any { it.name == "Foco Extremo" })
    assertEquals(ThemeMode.LIGHT, afterImport.themeMode)
    assertEquals(RestorePolicy.SNAPSHOT, afterImport.restorePolicy)
    assertEquals("preset_home", afterImport.defaultDayProfileId)
    assertEquals(120, afterImport.defaultTimerMinutes)
    assertEquals("Backup importado e aplicado com sucesso", afterImport.statusMessage)

    // Import invalid JSON -> emits clear error message without crashing
    viewModel.onImportBackupJson("{invalid_json")
    val afterInvalid = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals("JSON de backup inválido. Verifique o conteúdo colado.", afterInvalid.statusMessage)
  }

  @Test
  fun restorePolicy_snapshotVsDefaultProfile_restoresExpectedStateWhenRuleEnds() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // Set custom volume 42% and SNAPSHOT restore policy before entering location geofence
    viewModel.onStreamVolumeChange(VolumeStreamType.RING, 42)
    viewModel.onSetRestorePolicy(RestorePolicy.SNAPSHOT)

    val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
    viewModel.onToggleLocationRule(collegeRule, enabled = true)
    viewModel.onToggleLocationPresence("loc_college", inside = true)
    assertEquals(SoundMode.VIBRATE, (viewModel.uiState.value as MainScreenUiState.Ready).currentMode)

    // Leave geofence -> SNAPSHOT restores NORMAL mode and 42% ring volume
    viewModel.onToggleLocationPresence("loc_college", inside = false)
    val afterSnapshotRestore = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.NORMAL, afterSnapshotRestore.currentMode)
    assertEquals(42, afterSnapshotRestore.streams.ringPercent)
  }

  @Test
  fun timerSession_startExtendAndCancel_restoresPreviousModeAndVolumes() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onStreamVolumeChange(VolumeStreamType.MEDIA, 55)
    viewModel.onStartTimer(SoundMode.VIBRATE, 30)

    val timerStarted = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, timerStarted.currentMode)
    assertNotNull(timerStarted.activeTimer)
    assertEquals(30, timerStarted.activeTimer?.durationMinutes)

    viewModel.onExtendTimer(30)
    val timerExtended = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(60, timerExtended.activeTimer?.durationMinutes)

    viewModel.onCancelTimer()
    val timerCancelled = viewModel.uiState.value as MainScreenUiState.Ready
    assertNull(timerCancelled.activeTimer)
    assertEquals(SoundMode.NORMAL, timerCancelled.currentMode)
    assertEquals(55, timerCancelled.streams.mediaPercent)
  }

  @Test
  fun allSettingsPreferences_locationCrud_andQuickTileCycle_updateCorrectly() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onAcceptLocationDisclosure()
    viewModel.onSetUseDynamicColor(false)
    viewModel.onSetHapticFeedbackEnabled(false)
    viewModel.onSetNotifyOnAutoTransition(false)
    viewModel.onSetPreserveMediaOnBluetooth(false)
    viewModel.onSetSmartVolumeTriggerEnabled(false)

    val updatedPrefs = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(updatedPrefs.hasAcceptedLocationDisclosure)
    assertEquals(false, updatedPrefs.useDynamicColor)
    assertEquals(false, updatedPrefs.hapticFeedbackEnabled)
    assertEquals(false, updatedPrefs.notifyOnAutoTransition)
    assertEquals(false, updatedPrefs.preserveMediaOnBluetooth)
    assertEquals(false, updatedPrefs.smartVolumeTriggerEnabled)

    // Upsert and delete location rule
    val customLocation = LocationRule(
      id = "loc_custom_gym",
      name = "Academia",
      latitude = -23.56,
      longitude = -46.65,
      radiusMeters = 150,
      targetProfileId = "preset_work",
      enabled = true,
    )
    viewModel.onUpsertLocationRule(customLocation, showFeedback = true)
    assertTrue((viewModel.uiState.value as MainScreenUiState.Ready).locationRules.any { it.id == "loc_custom_gym" })

    viewModel.onDeleteLocationRule("loc_custom_gym")
    assertTrue((viewModel.uiState.value as MainScreenUiState.Ready).locationRules.none { it.id == "loc_custom_gym" })

    // Cycle sound mode from Quick Tile: NORMAL -> VIBRATE -> SILENT -> NORMAL
    assertEquals(SoundMode.VIBRATE, repository.cycleSoundModeFromQuickTile().getOrThrow())
    assertEquals(SoundMode.SILENT, repository.cycleSoundModeFromQuickTile().getOrThrow())
    assertEquals(SoundMode.NORMAL, repository.cycleSoundModeFromQuickTile().getOrThrow())

    // Clear transition logs
    assertTrue((viewModel.uiState.value as MainScreenUiState.Ready).transitionLogs.isNotEmpty())
    viewModel.onClearTransitionLogs()
    assertTrue((viewModel.uiState.value as MainScreenUiState.Ready).transitionLogs.isEmpty())
  }

  @Test
  fun internalRbacAccessControl_enforcesRolesGranularPermissionsAndPinUnlock() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // 1. Default role is ADMIN with all 7 permissions
    val initialState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(AppAccessRole.ADMIN, initialState.accessControlConfig.activeRole)
    assertEquals(AppPermission.entries.size, initialState.accessControlConfig.grantedPermissions.size)

    // 2. Configure 4-digit PIN ("2580")
    viewModel.onSetAccessPin("2580")
    val withPin = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(withPin.accessControlConfig.isPinRequired)
    assertEquals("2580", withPin.accessControlConfig.pinCode)

    // 3. Switch to OPERATOR -> allows sound/timer changes, blocks profile/schedule/settings edits
    viewModel.onSetAccessRole(AppAccessRole.OPERATOR)
    val operatorState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(AppAccessRole.OPERATOR, operatorState.accessControlConfig.activeRole)

    // Sound mode switch is allowed for OPERATOR
    viewModel.onSelectMode(SoundMode.VIBRATE)
    assertEquals(SoundMode.VIBRATE, (viewModel.uiState.value as MainScreenUiState.Ready).currentMode)

    // Profile creation and Settings changes are blocked for OPERATOR
    val profilesCountBefore = (viewModel.uiState.value as MainScreenUiState.Ready).profiles.size
    viewModel.onSaveCurrentAsProfile("Perfil Bloqueado", "Desc")
    val afterBlockedProfile = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(profilesCountBefore, afterBlockedProfile.profiles.size)
    assertEquals(
      AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_PROFILES),
      afterBlockedProfile.statusMessage,
    )

    viewModel.onSetThemeMode(ThemeMode.LIGHT)
    val afterBlockedSettings = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(ThemeMode.DARK, afterBlockedSettings.themeMode)
    assertEquals(
      AppLabels.AccessControl.permissionDeniedMessage(AppPermission.ACCESS_SETTINGS),
      afterBlockedSettings.statusMessage,
    )

    // 4. Switch to VIEWER -> even sound mode changes are blocked
    viewModel.onSetAccessRole(AppAccessRole.VIEWER)
    viewModel.onSelectMode(SoundMode.NORMAL)
    val afterBlockedSound = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, afterBlockedSound.currentMode)
    assertEquals(
      AppLabels.AccessControl.permissionDeniedMessage(AppPermission.CHANGE_SOUND_AND_TIMER),
      afterBlockedSound.statusMessage,
    )

    // 5. Enable a single granular permission -> transitions role to CUSTOM
    viewModel.onToggleAccessPermission(AppPermission.PAUSE_AUTOMATIONS, enabled = true)
    val customState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(AppAccessRole.CUSTOM, customState.accessControlConfig.activeRole)
    viewModel.onSetAutomationsPaused(true)
    assertTrue((viewModel.uiState.value as MainScreenUiState.Ready).automationsPaused)

    // 6. Wrong PIN rejected, right PIN ("2580") unlocks ADMIN
    var unlockedCallbackInvoked = false
    viewModel.onUnlockAdminWithPin("0000") { unlockedCallbackInvoked = true }
    assertEquals(false, unlockedCallbackInvoked)
    assertEquals(AppAccessRole.CUSTOM, (viewModel.uiState.value as MainScreenUiState.Ready).accessControlConfig.activeRole)

    viewModel.onUnlockAdminWithPin("2580") { unlockedCallbackInvoked = true }
    assertTrue(unlockedCallbackInvoked)
    assertEquals(AppAccessRole.ADMIN, (viewModel.uiState.value as MainScreenUiState.Ready).accessControlConfig.activeRole)
  }

  @Test
  fun auto04SystemZenRules_andGeo01BackgroundGeofencingWithDwellAndWifiSsid_workCorrectly() = runTest {
    fakePreferences.clearSchedules()
    BackgroundGeofenceManager.clearDwellCandidates()
    var syncedZenRulesCount = 0
    var syncedGeofencesCount = 0
    var scheduledDwellDelayMillis: Long? = null

    val repoWithCallbacks = DefaultSoundModeRepository(
      systemController = fakeSystemController,
      preferencesDataSource = fakePreferences,
      onSyncSystemZenRules = { profiles, _, enabled ->
        if (enabled) {
          syncedZenRulesCount = profiles.size
          profiles.size
        } else {
          syncedZenRulesCount = 0
          0
        }
      },
      onSyncGeofenceRegistrations = { rules, paused ->
        syncedGeofencesCount = if (paused) 0 else rules.count { it.enabled && it.hasCoordinates }
        syncedGeofencesCount
      },
      onScheduleGeofenceDwellCheck = { delayMillis ->
        scheduledDwellDelayMillis = delayMillis
      },
    )
    val vm = MainScreenViewModel(repoWithCallbacks)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      vm.uiState.collect()
    }

    // 1. [AUTO-04] Condition URI round-trip & Zen Rules toggle + manual sync
    val conditionUriString = SystemZenRuleManager.buildConditionUriString("preset_work_high")
    assertEquals(
      "preset_work_high",
      SystemZenRuleManager.extractProfileIdFromConditionUri(conditionUriString),
    )

    vm.onSyncSystemZenRulesNow()
    assertEquals(SoundProfile.defaultPresets.size, syncedZenRulesCount)

    vm.onSetSystemZenRulesEnabled(false)
    assertEquals(false, (vm.uiState.value as MainScreenUiState.Ready).systemZenRulesEnabled)
    assertEquals(0, syncedZenRulesCount)

    vm.onSetSystemZenRulesEnabled(true)
    assertEquals(true, (vm.uiState.value as MainScreenUiState.Ready).systemZenRulesEnabled)
    assertEquals(SoundProfile.defaultPresets.size, syncedZenRulesCount)

    // 2. [GEO-01] Enable "Faculdade" location rule (-23.5505, -46.6333, Campus-WiFi)
    val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
    vm.onToggleLocationRule(collegeRule, enabled = true)
    vm.onSyncBackgroundGeofencesNow()
    assertEquals(2, syncedGeofencesCount)

    // Step 2a: Enter geofence WITHOUT matching Wi-Fi -> Pending 2-minute Dwell (does NOT activate yet)
    val t0 = 1_700_000_000_000L
    scheduledDwellDelayMillis = null
    repoWithCallbacks.processBackgroundGeofenceSignal(
      latitude = collegeRule.latitude,
      longitude = collegeRule.longitude,
      connectedWifiSsid = "Rede-Visitante",
      nowEpochMillis = t0,
    )
    val afterPendingDwell = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(BackgroundGeofenceManager.DEFAULT_DWELL_DURATION_MILLIS, scheduledDwellDelayMillis)
    assertEquals(false, afterPendingDwell.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    assertEquals(SoundMode.NORMAL, afterPendingDwell.currentMode)

    // Step 2b: After 2 minutes (120_000 ms) inside the radius -> Dwell confirmed, switches to VIBRATE
    scheduledDwellDelayMillis = null
    repoWithCallbacks.processBackgroundGeofenceSignal(
      latitude = collegeRule.latitude,
      longitude = collegeRule.longitude,
      connectedWifiSsid = "Rede-Visitante",
      nowEpochMillis = t0 + BackgroundGeofenceManager.DEFAULT_DWELL_DURATION_MILLIS,
    )
    val afterConfirmedDwell = vm.uiState.value as MainScreenUiState.Ready
    assertNull(scheduledDwellDelayMillis)
    assertEquals(true, afterConfirmedDwell.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    assertEquals(SoundMode.VIBRATE, afterConfirmedDwell.currentMode)

    // Step 2c: Exit geofence -> ConfirmedOutside, restores NORMAL mode
    repoWithCallbacks.processBackgroundGeofenceSignal(
      latitude = -23.6000,
      longitude = -46.7000,
      connectedWifiSsid = null,
      nowEpochMillis = t0 + 300_000L,
    )
    val afterExit = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(false, afterExit.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    assertEquals(SoundMode.NORMAL, afterExit.currentMode)

    // Step 2d: Re-enter with matching Wi-Fi SSID ("Campus-WiFi") -> Instant confirmation (0 dwell wait)
    repoWithCallbacks.processBackgroundGeofenceSignal(
      latitude = collegeRule.latitude,
      longitude = collegeRule.longitude,
      connectedWifiSsid = "\"Campus-WiFi\"",
      nowEpochMillis = t0 + 400_000L,
    )
    val afterInstantWifiConfirm = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(true, afterInstantWifiConfirm.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    assertEquals(SoundMode.VIBRATE, afterInstantWifiConfirm.currentMode)
  }

  @Test
  fun uc33_toggleVibrateInSoundMode_preservesSoundDoesNotMuteMediaAndPromptsWriteSettingsJustInTime() = runTest {
    fakePreferences.clearSchedules()
    fakeSystemController.canWriteSettings = false
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onStreamVolumeChange(VolumeStreamType.MEDIA, 65)
    val initial = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(BaseAudioMode.SOUND, initial.audioMode)
    assertEquals(true, initial.vibrateEnabled)
    assertEquals(65, initial.streams.mediaPercent)
    assertEquals(false, initial.showWriteSettingsPrompt)

    // Alternar Vibrar em Som com canWriteSettings = false:
    // Mantém SOUND, alterna vibrateEnabled -> false, NÃO zera mídia, NÃO abre timer e solicita WRITE_SETTINGS Just-in-Time
    viewModel.onToggleVibrate()
    val afterToggleOff = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(BaseAudioMode.SOUND, afterToggleOff.audioMode)
    assertEquals(false, afterToggleOff.vibrateEnabled)
    assertEquals(65, afterToggleOff.streams.mediaPercent)
    assertNull(afterToggleOff.activeTimer)
    assertEquals(true, afterToggleOff.showWriteSettingsPrompt)
    assertEquals(AppLabels.Now.writeSettingsPromptMessage, afterToggleOff.statusMessage)

    viewModel.onDismissWriteSettingsPrompt()
    assertEquals(false, (viewModel.uiState.value as MainScreenUiState.Ready).showWriteSettingsPrompt)

    // Concede WRITE_SETTINGS: sincroniza imediatamente e alterna sem banner
    fakeSystemController.canWriteSettings = true
    viewModel.onRefreshSystemState()
    assertEquals(false, fakeSystemController.vibrateWhenRingingSetting)

    viewModel.onToggleVibrate()
    val afterToggleOn = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(BaseAudioMode.SOUND, afterToggleOn.audioMode)
    assertEquals(true, afterToggleOn.vibrateEnabled)
    assertEquals(true, fakeSystemController.vibrateWhenRingingSetting)
    assertEquals(false, afterToggleOn.showWriteSettingsPrompt)
  }

  @Test
  fun uc34_toggleVibrateInSilentMode_andSwitchAudioModePreservingTactilePreference() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onStreamVolumeChange(VolumeStreamType.MEDIA, 60)
    viewModel.onSetVibrateEnabled(false)
    val soundNoVib = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundState(BaseAudioMode.SOUND, false), soundNoVib.currentSoundState)
    assertEquals(60, soundNoVib.streams.mediaPercent)

    // Trocar de Som -> Silêncio preservando vibrateEnabled = false (Silêncio Total / DND) e zerando mídia
    viewModel.onSelectAudioMode(BaseAudioMode.SILENT)
    val silentNoVib = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundState(BaseAudioMode.SILENT, false), silentNoVib.currentSoundState)
    assertEquals(SoundMode.SILENT, silentNoVib.currentMode)
    assertEquals(0, silentNoVib.streams.mediaPercent)

    // Alternar Vibrar dentro de Silêncio -> Silêncio + Vibrar (RINGER_MODE_VIBRATE)
    viewModel.onToggleVibrate()
    val silentVib = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundState(BaseAudioMode.SILENT, true), silentVib.currentSoundState)
    assertEquals(SoundMode.VIBRATE, silentVib.currentMode)

    // Sem permissão DND, tentar desligar Vibrar dentro de Silêncio bloqueia e mantém Silêncio + Vibrar
    fakeSystemController.dndGranted = false
    viewModel.onToggleVibrate()
    val blockedDnd = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundState(BaseAudioMode.SILENT, true), blockedDnd.currentSoundState)
    assertTrue(blockedDnd.statusMessage?.contains("Não Perturbe") == true)
    fakeSystemController.dndGranted = true

    // Voltar para Som preserva vibrateEnabled = true e restaura mídia de 60%
    viewModel.onSelectAudioMode(BaseAudioMode.SOUND)
    val backToSoundVib = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundState(BaseAudioMode.SOUND, true), backToSoundVib.currentSoundState)
    assertEquals(60, backToSoundVib.streams.mediaPercent)
  }

  @Test
  fun uc35_v1BackupJsonMigration_normalizesLegacyProfilesInto2x2Matrix() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val legacyV1Json = """
      {
        "version": 1,
        "customProfiles": [
          {
            "id": "legacy_normal",
            "name": "Legado Normal",
            "description": "v1",
            "mode": "NORMAL",
            "streams": {"ringPercent": 80, "notificationPercent": 70, "mediaPercent": 50, "alarmPercent": 90},
            "isPreset": false
          },
          {
            "id": "legacy_vibrate",
            "name": "Legado Vibrar",
            "description": "v1",
            "mode": "VIBRATE",
            "streams": {"ringPercent": 0, "notificationPercent": 0, "mediaPercent": 0, "alarmPercent": 85},
            "isPreset": false
          },
          {
            "id": "legacy_silent",
            "name": "Legado Silêncio",
            "description": "v1",
            "mode": "SILENT",
            "streams": {"ringPercent": 0, "notificationPercent": 0, "mediaPercent": 0, "alarmPercent": 100},
            "isPreset": false
          }
        ]
      }
    """.trimIndent()

    viewModel.onImportBackupJson(legacyV1Json)
    val ready = viewModel.uiState.value as MainScreenUiState.Ready

    val pNormal = ready.profiles.first { it.id == "legacy_normal" }
    val pVibrate = ready.profiles.first { it.id == "legacy_vibrate" }
    val pSilent = ready.profiles.first { it.id == "legacy_silent" }

    assertEquals(SoundState(BaseAudioMode.SOUND, true), pNormal.resolvedSoundState)
    assertEquals(SoundState(BaseAudioMode.SILENT, true), pVibrate.resolvedSoundState)
    assertEquals(SoundState(BaseAudioMode.SILENT, false), pSilent.resolvedSoundState)
  }

  @Test
  fun spec006_preSilenceSnapshot_timerFromSilentRestoresRealActiveSoundAndPreMuteMedia() = runTest {
    fakePreferences.clearSchedules()
    val smartPrompts = mutableListOf<Pair<SoundMode, Boolean>>()
    val scheduledExactAlarmTimes = mutableListOf<Long>()
    var cancelledExactAlarmCount = 0

    val repoS3 = DefaultSoundModeRepository(
      systemController = fakeSystemController,
      preferencesDataSource = fakePreferences,
      onScheduleExactTimerAlarm = { endsAt -> scheduledExactAlarmTimes.add(endsAt) },
      onCancelExactTimerAlarm = { cancelledExactAlarmCount++ },
      onSmartRestorePrompt = { mode, autoMuted -> smartPrompts.add(mode to autoMuted) },
      clockNowMillis = { 1_800_000_000_000L },
    )
    val vm = MainScreenViewModel(repoS3)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      vm.uiState.collect()
    }

    // 1. Configure Som + Vibrar with Ring 74% and Media 62%
    vm.onStreamVolumeChange(VolumeStreamType.RING, 74)
    vm.onStreamVolumeChange(VolumeStreamType.MEDIA, 62)
    assertEquals(SoundState(BaseAudioMode.SOUND, true), (vm.uiState.value as MainScreenUiState.Ready).currentSoundState)

    // 2. Simulate hardware Volume Down silencing to Vibrate (Android zeroes STREAM_RING in RINGER_MODE_VIBRATE)
    fakeSystemController.soundState = SoundState(BaseAudioMode.SILENT, true)
    fakeSystemController.streams = fakeSystemController.streams.copy(ringPercent = 0, notificationPercent = 0)
    fakeSystemController.externalFlag = true
    repoS3.handleExternalAudioChange()

    assertEquals(listOf(SoundMode.VIBRATE to true), smartPrompts)
    assertEquals(0, fakeSystemController.streams.mediaPercent)

    // 3. Simulate hardware toggle Vibrar -> Silêncio Total while already in Silent -> updates prompt 3101
    fakeSystemController.soundState = SoundState(BaseAudioMode.SILENT, false)
    fakeSystemController.externalFlag = true
    repoS3.handleExternalAudioChange()
    assertEquals(SoundMode.SILENT to true, smartPrompts.last())

    // 4. Start 30m timer while ALREADY in Silêncio Total -> must resolve return target to Som + Vibrar (Ring 74%, Media 62%)
    val session = repoS3.startTimedStateSession(
      targetState = SoundState(BaseAudioMode.SILENT, false),
      durationMinutes = 30,
      nowEpochMillis = 1_800_000_000_000L,
    ).getOrThrow()

    assertEquals(SoundState(BaseAudioMode.SOUND, true), session.resolvedPreviousSoundState)
    assertEquals(74, session.previousStreams.ringPercent)
    assertEquals(62, session.previousStreams.mediaPercent)
    assertEquals(listOf(1_800_000_000_000L + 30 * 60_000L), scheduledExactAlarmTimes)

    // 5. Extend timer by +30m -> reschedules exact alarm
    val extended = repoS3.extendTimedSession(
      additionalMinutes = 30,
      nowEpochMillis = 1_800_000_000_000L,
    ).getOrThrow()
    assertEquals(1_800_000_000_000L + 60 * 60_000L, extended.endsAtEpochMillis)
    assertEquals(extended.endsAtEpochMillis, scheduledExactAlarmTimes.last())

    // 6. Exact alarm expires -> restores Som + Vibrar, Ring 74% & Media 62%; second call (worker safety net) is idempotent
    repoS3.cancelOrRestoreTimedSession().getOrThrow()
    assertEquals(SoundState(BaseAudioMode.SOUND, true), fakeSystemController.soundState)
    assertEquals(74, fakeSystemController.streams.ringPercent)
    assertEquals(62, fakeSystemController.streams.mediaPercent)
    assertTrue(cancelledExactAlarmCount >= 1)

    // Modify media after restoration and ensure redundant worker execution does NOT overwrite user's state
    vm.onStreamVolumeChange(VolumeStreamType.MEDIA, 33)
    repoS3.cancelOrRestoreTimedSession().getOrThrow()
    assertEquals(33, fakeSystemController.streams.mediaPercent)
  }

  @Test
  fun spec006_zombieTimerElimination_manualModeAndProfileSwitchesCancelActiveTimerImmediately() = runTest {
    fakePreferences.clearSchedules()
    var cancelWorkerCount = 0
    var cancelExactAlarmCount = 0
    val syncedTimerSessions = mutableListOf<TimedModeSession?>()

    val repoS3 = DefaultSoundModeRepository(
      systemController = fakeSystemController,
      preferencesDataSource = fakePreferences,
      onCancelAutoRestore = { cancelWorkerCount++ },
      onCancelExactTimerAlarm = { cancelExactAlarmCount++ },
      onSyncActiveTimerNotification = { session -> syncedTimerSessions.add(session) },
    )
    val vm = MainScreenViewModel(repoS3)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      vm.uiState.collect()
    }

    // 1. Start 60m Silent timer -> manually switch audio mode to SOUND -> cancels activeTimer immediately
    vm.onStartStateTimer(SoundState(BaseAudioMode.SILENT, true), 60)
    assertNotNull((vm.uiState.value as MainScreenUiState.Ready).activeTimer)

    vm.onSelectAudioMode(BaseAudioMode.SOUND)
    val afterManualSound = vm.uiState.value as MainScreenUiState.Ready
    assertNull(afterManualSound.activeTimer)
    assertEquals(1, cancelWorkerCount)
    assertEquals(1, cancelExactAlarmCount)
    assertNull(syncedTimerSessions.last())

    // 2. Start 60m Silent timer -> manually activate a profile -> cancels activeTimer immediately
    vm.onStartStateTimer(SoundState(BaseAudioMode.SILENT, true), 60)
    assertNotNull((vm.uiState.value as MainScreenUiState.Ready).activeTimer)

    val homeProfile = afterManualSound.profiles.first { it.id == "preset_home" }
    vm.onActivateProfile(homeProfile)
    val afterProfile = vm.uiState.value as MainScreenUiState.Ready
    assertNull(afterProfile.activeTimer)
    assertEquals(2, cancelWorkerCount)
    assertEquals(2, cancelExactAlarmCount)
    assertNull(syncedTimerSessions.last())

    // 3. SoundProfile.mergeProfiles prioritizes edited preset names in customProfiles over defaultPresets
    val workPreset = afterProfile.profiles.first { it.id == "preset_work_high" }
    val renamedWork = workPreset.copy(name = "Trabalho Executivo")
    val merged = SoundProfile.mergeProfiles(savedProfiles = listOf(renamedWork))
    assertEquals("Trabalho Executivo", merged.first { it.id == "preset_work_high" }.name)
  }

  @Test
  fun spec006_autoRoutineTransition5thChannel_andBilingualLabelsAndLanguageResync() = runTest {
    try {
      fakePreferences.clearSchedules()
      val autoTransitions = mutableListOf<Triple<String, String, SoundState>>()
      var languageChangedCount = 0
      var surfaceRefreshCount = 0

      val repoS3 = DefaultSoundModeRepository(
        systemController = fakeSystemController,
        preferencesDataSource = fakePreferences,
        onAutoRoutineTransitionAlert = { title, subtitle, state ->
          autoTransitions.add(Triple(title, subtitle, state))
        },
        onLanguageChanged = { languageChangedCount++ },
        onRefreshExternalSurfaces = { surfaceRefreshCount++ },
      )
      val vm = MainScreenViewModel(repoS3)
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        vm.uiState.collect()
      }

      // 1. With notifyOnAutoTransition = true, entering and leaving a Location routine emits 5th-channel alerts
      val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
      vm.onToggleLocationRule(collegeRule, enabled = true)
      vm.onToggleLocationPresence("loc_college", inside = true)
      assertEquals(1, autoTransitions.size)
      assertTrue(autoTransitions.last().first.contains("Faculdade"))
      assertEquals(SoundState(BaseAudioMode.SILENT, true), autoTransitions.last().third)

      vm.onToggleLocationPresence("loc_college", inside = false)
      assertEquals(2, autoTransitions.size)
      assertEquals(AppLabels.Notifications.autoRoutineEndedTitle, autoTransitions.last().first)

      // 2. With notifyOnAutoTransition = false, automatic routines do NOT emit 5th-channel alerts
      vm.onSetNotifyOnAutoTransition(false)
      vm.onToggleLocationPresence("loc_college", inside = true)
      assertEquals(2, autoTransitions.size)

      // 3. Language switch re-syncs channels & surfaces and exposes 100% bilingual labels without frozen timer minutes
      vm.onSetAppLanguage(AppLanguage.EN)
      assertEquals(1, languageChangedCount)
      assertTrue(surfaceRefreshCount >= 1)
      assertEquals("Automatic Routine Transition", AppLabels.Notifications.autoTransitionChannelName)
      val quickBarTitleEn = AppLabels.Notifications.quickBarTitle(
        state = SoundState(BaseAudioMode.SILENT, true),
        activeProfileName = "Work",
      )
      assertEquals("Silent + Vibrate • Profile Work", quickBarTitleEn)
      assertTrue(!quickBarTitleEn.contains("Timer ("))
      assertTrue(
        AppLabels.Settings.gadgetsSummary(
          quickBarEnabled = true,
          slot1Name = "Work",
          slot2Name = "Sleep",
          hasNotificationPermission = false,
        ).contains("Permission pending"),
      )

      vm.onSetAppLanguage(AppLanguage.PT_BR)
      assertTrue(
        AppLabels.Settings.gadgetsSummary(
          quickBarEnabled = true,
          slot1Name = "Trabalho",
          slot2Name = "Dormir",
          hasNotificationPermission = false,
        ).contains("Permissão pendente"),
      )
    } finally {
      AppLabels.currentLanguage = AppLanguage.PT_BR
    }
  }

  @Test
  fun spec007_ac01_ac02_sevenStreamsMinVolumeFloorAndRingNotificationInlineLinkSync() = runTest {
    fakePreferences.clearSchedules()
    fakeSystemController.streamMinPercents = mapOf(
      VolumeStreamType.ALARM to 14,
      VolumeStreamType.VOICE_CALL to 17,
    )
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }
    viewModel.onRefreshSystemState()

    // 1. AC-01: All 7 streams apply cleanly and respect getStreamMinVolume floor
    val initial = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(14, initial.minPercentFor(VolumeStreamType.ALARM))
    assertEquals(17, initial.minPercentFor(VolumeStreamType.VOICE_CALL))
    assertEquals(0, initial.minPercentFor(VolumeStreamType.MEDIA))

    viewModel.onStreamVolumeChange(VolumeStreamType.ALARM, 0)
    viewModel.onStreamVolumeChange(VolumeStreamType.VOICE_CALL, 0)
    viewModel.onStreamVolumeChange(VolumeStreamType.SYSTEM, 35)
    viewModel.onStreamVolumeChange(VolumeStreamType.ACCESSIBILITY, 55)

    val afterFloorCheck = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(14, afterFloorCheck.streams.alarmPercent)
    assertEquals(17, afterFloorCheck.streams.voiceCallPercent)
    assertEquals(35, afterFloorCheck.streams.systemPercent)
    assertEquals(55, afterFloorCheck.streams.accessibilityPercent)

    // 2. AC-02: Factory default ringNotificationLinked = true syncs RING <-> NOTIFICATION bidirectionally
    assertTrue(afterFloorCheck.ringNotificationLinked)
    viewModel.onStreamVolumeChange(VolumeStreamType.RING, 62)
    val afterLinkedRing = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(62, afterLinkedRing.streams.ringPercent)
    assertEquals(62, afterLinkedRing.streams.notificationPercent)

    viewModel.onStreamVolumeChange(VolumeStreamType.NOTIFICATION, 38)
    val afterLinkedNotif = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(38, afterLinkedNotif.streams.ringPercent)
    assertEquals(38, afterLinkedNotif.streams.notificationPercent)

    // 3. Inline toggle unlinks RING and NOTIFICATION -> independent adjustments
    viewModel.onToggleRingNotificationLink()
    val unlinked = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(false, unlinked.ringNotificationLinked)

    viewModel.onStreamVolumeChange(VolumeStreamType.RING, 85)
    viewModel.onStreamVolumeChange(VolumeStreamType.NOTIFICATION, 25)
    val afterIndependent = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(85, afterIndependent.streams.ringPercent)
    assertEquals(25, afterIndependent.streams.notificationPercent)

    // 4. Re-linking aligns NOTIFICATION to RING immediately in SOUND mode
    viewModel.onToggleRingNotificationLink()
    val relinked = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(relinked.ringNotificationLinked)
    assertEquals(85, relinked.streams.ringPercent)
    assertEquals(85, relinked.streams.notificationPercent)
  }

  @Test
  fun spec007_ac03_ac04_antiJumpMostUsedOrderingVisibleFixedBars1To7AndManualOrder() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // 1. AC-03: Default 4 fixed bars (MEDIA, RING, NOTIFICATION, ALARM) and 3 hidden in drawer
    val initial = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(4, initial.visibleFixedStreamsCount)
    assertEquals(false, initial.secondaryDrawerExpanded)
    assertEquals(
      listOf(
        VolumeStreamType.MEDIA,
        VolumeStreamType.RING,
        VolumeStreamType.NOTIFICATION,
        VolumeStreamType.ALARM,
      ),
      initial.fixedVisibleStreams,
    )
    assertEquals(
      listOf(
        VolumeStreamType.SYSTEM,
        VolumeStreamType.VOICE_CALL,
        VolumeStreamType.ACCESSIBILITY,
      ),
      initial.drawerHiddenStreams,
    )

    // 2. AC-04: Adjusting VOICE_CALL 3x and SYSTEM 2x increments usage silently WITHOUT jumping mid-session
    repeat(3) { step ->
      viewModel.onStreamVolumeChange(VolumeStreamType.VOICE_CALL, 60 + step)
    }
    repeat(2) { step ->
      viewModel.onStreamVolumeChange(VolumeStreamType.SYSTEM, 40 + step)
    }

    val duringSession = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(3, duringSession.streamUsageCounts[VolumeStreamType.VOICE_CALL])
    assertEquals(2, duringSession.streamUsageCounts[VolumeStreamType.SYSTEM])
    assertEquals(VolumeStreamType.defaultOrder, duringSession.orderedStreams)

    // 3. On next screen resume/entry, latched order refreshes and promotes VOICE_CALL & SYSTEM
    viewModel.onRefreshSystemState()
    val afterResume = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(VolumeStreamType.VOICE_CALL, afterResume.fixedVisibleStreams[0])
    assertEquals(VolumeStreamType.SYSTEM, afterResume.fixedVisibleStreams[1])

    // 4. Configurable 1..7 fixed bars (2 -> 2 fixed + 5 drawer; 7 -> 7 fixed + 0 drawer)
    viewModel.onSetVisibleFixedStreamsCount(2)
    val twoFixed = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(2, twoFixed.fixedVisibleStreams.size)
    assertEquals(5, twoFixed.drawerHiddenStreams.size)

    viewModel.onSetVisibleFixedStreamsCount(7)
    val sevenFixed = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(7, sevenFixed.fixedVisibleStreams.size)
    assertTrue(sevenFixed.drawerHiddenStreams.isEmpty())

    // 5. Manual order mode + move up/down
    viewModel.onSetStreamSortMode(StreamSortMode.MANUAL)
    viewModel.onMoveStreamInManualOrder(VolumeStreamType.RING, moveUp = true)
    val manualMoved = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(StreamSortMode.MANUAL, manualMoved.streamSortMode)
    assertEquals(VolumeStreamType.RING, manualMoved.orderedStreams[0])
    assertEquals(VolumeStreamType.MEDIA, manualMoved.orderedStreams[1])

    viewModel.onSetSecondaryDrawerExpanded(true)
    assertTrue((viewModel.uiState.value as MainScreenUiState.Ready).secondaryDrawerExpanded)
  }

  @Test
  fun spec007_ac05_profileOptionalSecondaryOverridesAndLegacyV1BackupCompatibility() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // 1. Set baseline secondary stream volumes on the device
    viewModel.onStreamVolumeChange(VolumeStreamType.SYSTEM, 44)
    viewModel.onStreamVolumeChange(VolumeStreamType.VOICE_CALL, 77)
    viewModel.onStreamVolumeChange(VolumeStreamType.ACCESSIBILITY, 66)

    // 2. Activating a preset profile (null secondary overrides) preserves SYSTEM, VOICE_CALL, ACCESSIBILITY
    val workPreset = (viewModel.uiState.value as MainScreenUiState.Ready)
      .profiles.first { it.id == "preset_work_high" }
    viewModel.onActivateProfile(workPreset)
    val afterPreset = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(44, afterPreset.streams.systemPercent)
    assertEquals(77, afterPreset.streams.voiceCallPercent)
    assertEquals(66, afterPreset.streams.accessibilityPercent)

    // 3. Upsert and activate a profile that explicitly overrides SYSTEM (20%) & VOICE_CALL (92%), leaving ACCESSIBILITY null
    val customSecondaryProfile = SoundProfile(
      id = "custom_call_focus",
      name = "Ligação Clara",
      description = "Voz alta e sistema discreto",
      audioMode = BaseAudioMode.SOUND,
      vibrateEnabled = true,
      streams = VolumeStreams(
        ringPercent = 90,
        notificationPercent = 60,
        mediaPercent = 30,
        alarmPercent = 85,
        systemPercent = 20,
        voiceCallPercent = 92,
        accessibilityPercent = 10,
      ),
      isPreset = false,
      overrideSystemPercent = 20,
      overrideVoiceCallPercent = 92,
      overrideAccessibilityPercent = null,
    )
    viewModel.onUpsertProfile(customSecondaryProfile, applyImmediately = true)
    val afterCustom = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(20, afterCustom.streams.systemPercent)
    assertEquals(92, afterCustom.streams.voiceCallPercent)
    assertEquals(66, afterCustom.streams.accessibilityPercent)

    // 4. Backup/Restore round-trip of SPEC-007 settings + legacy v1 compatibility
    viewModel.onSetRingNotificationLinked(false)
    viewModel.onSetVisibleFixedStreamsCount(5)
    viewModel.onSetStreamSortMode(StreamSortMode.MANUAL)
    var exportedSpec007Json = ""
    viewModel.onExportBackupJson { exportedSpec007Json = it }

    val legacyV1WithoutSpec007Fields = """
      {"version":1,"customProfiles":[{"id":"legacy_p","name":"Perfil V1","description":"v1","mode":"NORMAL","streams":{"ringPercent":80,"notificationPercent":70,"mediaPercent":50,"alarmPercent":90},"isPreset":false}]}
    """.trimIndent()
    viewModel.onImportBackupJson(legacyV1WithoutSpec007Fields)
    val afterLegacyImport = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(afterLegacyImport.ringNotificationLinked)
    assertEquals(4, afterLegacyImport.visibleFixedStreamsCount)
    assertEquals(StreamSortMode.MOST_USED, afterLegacyImport.streamSortMode)
    val decodedLegacy = afterLegacyImport.profiles.first { it.id == "legacy_p" }
    assertNull(decodedLegacy.overrideSystemPercent)
    assertNull(decodedLegacy.overrideVoiceCallPercent)
    assertNull(decodedLegacy.overrideAccessibilityPercent)

    viewModel.onImportBackupJson(exportedSpec007Json)
    val afterSpec007Restore = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(false, afterSpec007Restore.ringNotificationLinked)
    assertEquals(5, afterSpec007Restore.visibleFixedStreamsCount)
    assertEquals(StreamSortMode.MANUAL, afterSpec007Restore.streamSortMode)
  }

  @Test
  fun spec005_ac02_ac03_volumePreviewPlayer_singleTapLoopLiveUpdateStopAndAllSevenStreams() = runTest {
    fakePreferences.clearSchedules()
    val fakePreview = FakeVolumePreviewPlayer()
    val vm = MainScreenViewModel(repository = repository, previewPlayer = fakePreview)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      vm.uiState.collect()
    }

    // 1. One-shot preview works across all 7 VolumeStreamTypes and toggles off on second tap
    VolumeStreamType.entries.forEachIndexed { idx, type ->
      val target = 40 + idx * 5
      vm.onToggleStreamPreview(type = type, percent = target, scope = VolumePreviewScope.NOW_TAB)
      val playingState = vm.uiState.value as MainScreenUiState.Ready
      assertEquals(type, playingState.activeVolumePreview?.streamType)
      assertEquals(VolumePreviewKind.ONE_SHOT, playingState.activeVolumePreview?.kind)
      assertEquals(target, playingState.activeVolumePreview?.percent)

      // Tapping same stream again stops immediately
      vm.onToggleStreamPreview(type = type, percent = target, scope = VolumePreviewScope.NOW_TAB)
      assertNull((vm.uiState.value as MainScreenUiState.Ready).activeVolumePreview)
    }
    assertEquals(7, fakePreview.playHistory.size)

    // 2. Hold-loop preview + live drag percent updates linked RING & NOTIFICATION in real time
    vm.onStartStreamLoopPreview(
      type = VolumeStreamType.RING,
      percent = 55,
      scope = VolumePreviewScope.NOW_TAB,
    )
    assertEquals(
      VolumePreviewKind.HOLD_LOOP,
      (vm.uiState.value as MainScreenUiState.Ready).activeVolumePreview?.kind,
    )

    vm.onUpdateStreamLoopPercent(
      type = VolumeStreamType.RING,
      percent = 82,
      scope = VolumePreviewScope.NOW_TAB,
    )
    assertEquals(82, (vm.uiState.value as MainScreenUiState.Ready).activeVolumePreview?.percent)
    assertEquals(82, fakeSystemController.streams.ringPercent)
    assertEquals(82, fakeSystemController.streams.notificationPercent)

    vm.onStopVolumePreview()
    assertNull((vm.uiState.value as MainScreenUiState.Ready).activeVolumePreview)

    // 3. Quick mute toggle chip & useSystemSoundsForPreview setting
    vm.onSetUseSystemSoundsForPreview(false)
    vm.onToggleStreamPreview(VolumeStreamType.ALARM, 75)
    assertEquals(false, fakePreview.lastUseSystemSounds)

    vm.onToggleVolumePreviewQuick()
    val mutedPreviewState = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(false, mutedPreviewState.volumePreviewEnabled)
    assertNull(mutedPreviewState.activeVolumePreview)

    // While muted on Now tab, tapping preview does not fire new samples
    val historyCountBefore = fakePreview.playHistory.size
    vm.onToggleStreamPreview(VolumeStreamType.MEDIA, 60)
    assertEquals(historyCountBefore, fakePreview.playHistory.size)
  }

  @Test
  fun spec005_ac04_silentPreAdjustment_preservesSilentAndVibrateRespectsLinkAndAppliesOnReturnToSound() = runTest {
    fakePreferences.clearSchedules()
    val fakePreview = FakeVolumePreviewPlayer()
    val vm = MainScreenViewModel(repository = repository, previewPlayer = fakePreview)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      vm.uiState.collect()
    }

    // 1. Enter Silêncio + Vibrar (audioMode = SILENT, vibrateEnabled = true) with hardware RING = 0
    vm.onSelectAudioMode(BaseAudioMode.SILENT)
    fakeSystemController.streams = fakeSystemController.streams.copy(
      ringPercent = 0,
      notificationPercent = 0,
      systemPercent = 0,
    )
    vm.onRefreshSystemState()

    val inSilent = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundState(BaseAudioMode.SILENT, true), inSilent.currentSoundState)

    // 2. Pre-adjust RING to 78% while linked -> OS stays in SILENT + Vibrar and both RING & NOTIFICATION become pending
    vm.onStreamVolumeChange(VolumeStreamType.RING, 78)
    vm.onStreamVolumeChange(VolumeStreamType.SYSTEM, 45)
    // Independent channels (MEDIA, ALARM, VOICE_CALL, ACCESSIBILITY) update hardware directly without disarming SILENT
    vm.onStreamVolumeChange(VolumeStreamType.MEDIA, 55)
    vm.onStreamVolumeChange(VolumeStreamType.VOICE_CALL, 72)

    val afterPreAdjust = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundState(BaseAudioMode.SILENT, true), afterPreAdjust.currentSoundState)
    assertEquals(SoundState(BaseAudioMode.SILENT, true), fakeSystemController.soundState)
    assertEquals(0, fakeSystemController.streams.ringPercent)
    assertEquals(55, fakeSystemController.streams.mediaPercent)
    assertEquals(72, fakeSystemController.streams.voiceCallPercent)
    assertEquals(78, afterPreAdjust.displayedStreamPercent(VolumeStreamType.RING))
    assertEquals(78, afterPreAdjust.displayedStreamPercent(VolumeStreamType.NOTIFICATION))
    assertEquals(45, afterPreAdjust.displayedStreamPercent(VolumeStreamType.SYSTEM))
    assertTrue(afterPreAdjust.isStreamPendingForSound(VolumeStreamType.RING))
    assertTrue(afterPreAdjust.isStreamPendingForSound(VolumeStreamType.NOTIFICATION))
    assertTrue(afterPreAdjust.isStreamPendingForSound(VolumeStreamType.SYSTEM))
    assertEquals(false, afterPreAdjust.isStreamPendingForSound(VolumeStreamType.MEDIA))

    // 3. Unlink RING/NOTIFICATION while in SILENT and set NOTIFICATION independently to 34%
    vm.onToggleRingNotificationLink()
    vm.onStreamVolumeChange(VolumeStreamType.NOTIFICATION, 34)
    val unlinkedPending = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(78, unlinkedPending.displayedStreamPercent(VolumeStreamType.RING))
    assertEquals(34, unlinkedPending.displayedStreamPercent(VolumeStreamType.NOTIFICATION))

    // 4. Authorized temporary preview in SILENT mode plays sample and restores SILENT + Vibrar right after
    vm.onToggleStreamPreview(
      type = VolumeStreamType.RING,
      percent = 78,
      scope = VolumePreviewScope.NOW_TAB,
      authorizedSilentOverride = true,
    )
    assertEquals(BaseAudioMode.SOUND, fakeSystemController.soundState.audioMode)
    assertEquals(78, fakeSystemController.streams.ringPercent)
    // UI snapshot remains flicker-free in SILENT during isolated preview
    assertEquals(BaseAudioMode.SILENT, (vm.uiState.value as MainScreenUiState.Ready).audioMode)

    fakePreview.finishCurrentOneShotNaturally()
    assertEquals(SoundState(BaseAudioMode.SILENT, true), fakeSystemController.soundState)
    assertEquals(0, fakeSystemController.streams.ringPercent)

    // 5. Transition back to SOUND -> automatically applies pending RING=78, NOTIFICATION=34, SYSTEM=45 and clears pending state
    vm.onSelectAudioMode(BaseAudioMode.SOUND)
    val backInSound = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(BaseAudioMode.SOUND, backInSound.audioMode)
    assertEquals(78, backInSound.streams.ringPercent)
    assertEquals(34, backInSound.streams.notificationPercent)
    assertEquals(45, backInSound.streams.systemPercent)
    assertTrue(backInSound.pendingSoundStreams.isEmpty())
    assertEquals(false, backInSound.isStreamPendingForSound(VolumeStreamType.RING))
  }

  @Test
  fun spec005_ac05_profileEditorIsolatedPreview_restoresHardwareVolumesAndRingerEvenAcrossRapidSwitches() = runTest {
    fakePreferences.clearSchedules()
    val fakePreview = FakeVolumePreviewPlayer()
    val vm = MainScreenViewModel(repository = repository, previewPlayer = fakePreview)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      vm.uiState.collect()
    }

    // Set baseline hardware state: SILENT Total (vibrateEnabled = false), MEDIA = 20%, ALARM = 80%, ACCESSIBILITY = 60%
    vm.onSetVibrateEnabled(false)
    vm.onSelectAudioMode(BaseAudioMode.SILENT)
    vm.onStreamVolumeChange(VolumeStreamType.MEDIA, 20)
    vm.onStreamVolumeChange(VolumeStreamType.ALARM, 80)
    vm.onStreamVolumeChange(VolumeStreamType.ACCESSIBILITY, 60)

    assertEquals(SoundState(BaseAudioMode.SILENT, false), fakeSystemController.soundState)
    assertEquals(20, fakeSystemController.streams.mediaPercent)

    // 1. Preview MEDIA at 90% in PROFILE_EDITOR -> hardware temporarily changes to 90%, UI stays SILENT Total / 20%
    vm.onToggleStreamPreview(
      type = VolumeStreamType.MEDIA,
      percent = 90,
      scope = VolumePreviewScope.PROFILE_EDITOR,
      authorizedSilentOverride = true,
    )
    assertEquals(90, fakeSystemController.streams.mediaPercent)
    assertEquals(20, (vm.uiState.value as MainScreenUiState.Ready).streams.mediaPercent)

    // 2. Rapidly switch to Hold-Loop on ACCESSIBILITY at 95% before stopping MEDIA -> MEDIA restores to 20%, ACCESSIBILITY goes to 95%
    vm.onStartStreamLoopPreview(
      type = VolumeStreamType.ACCESSIBILITY,
      percent = 95,
      scope = VolumePreviewScope.PROFILE_EDITOR,
      authorizedSilentOverride = true,
    )
    assertEquals(20, fakeSystemController.streams.mediaPercent)
    assertEquals(95, fakeSystemController.streams.accessibilityPercent)

    vm.onUpdateStreamLoopPercent(
      type = VolumeStreamType.ACCESSIBILITY,
      percent = 84,
      scope = VolumePreviewScope.PROFILE_EDITOR,
    )
    assertEquals(84, fakeSystemController.streams.accessibilityPercent)

    // 3. Stop preview (e.g. sheet dismiss) -> restores exact SILENT Total + MEDIA 20%, ALARM 80%, ACCESSIBILITY 60%
    vm.onStopVolumePreview()
    assertEquals(SoundState(BaseAudioMode.SILENT, false), fakeSystemController.soundState)
    assertEquals(20, fakeSystemController.streams.mediaPercent)
    assertEquals(80, fakeSystemController.streams.alarmPercent)
    assertEquals(60, fakeSystemController.streams.accessibilityPercent)

    // 4. Backup/Restore round-trip of SPEC-005 settings + bilingual labels check
    vm.onSetVolumePreviewEnabled(false)
    vm.onSetUseSystemSoundsForPreview(false)
    vm.onSetSilentPreAdjustEnabled(false)
    var exportedJson = ""
    vm.onExportBackupJson { exportedJson = it }

    vm.onResetFactoryDefaults()
    val resetState = vm.uiState.value as MainScreenUiState.Ready
    assertTrue(resetState.volumePreviewEnabled)
    assertTrue(resetState.useSystemSoundsForPreview)
    assertTrue(resetState.silentPreAdjustEnabled)

    vm.onImportBackupJson(exportedJson)
    val importedState = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(false, importedState.volumePreviewEnabled)
    assertEquals(false, importedState.useSystemSoundsForPreview)
    assertEquals(false, importedState.silentPreAdjustEnabled)

    try {
      AppLabels.currentLanguage = AppLanguage.PT_BR
      assertEquals("Pré-ajuste p/ Som", AppLabels.Now.pendingForSoundBadge)
      assertEquals("Aparelho em modo silencioso", AppLabels.Now.silentPreviewDialogTitle)
      AppLabels.currentLanguage = AppLanguage.EN
      assertEquals("Pre-set for Sound", AppLabels.Now.pendingForSoundBadge)
      assertEquals("Device in silent mode", AppLabels.Now.silentPreviewDialogTitle)
    } finally {
      AppLabels.currentLanguage = AppLanguage.PT_BR
    }
  }

  @Test
  fun calendarRules_crudAndDuplicate_updatesPreferencesAndUiState() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val readyInitial = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(readyInitial.calendarRules.isEmpty())

    val rule = CalendarRule(
      id = "cal_1",
      name = "Reunião de Trabalho",
      calendarIds = setOf(1L),
      busyOnly = true,
      ignoreAllDayEvents = true,
      keywordFilter = "Daily",
      targetProfileId = "preset_silent",
      highPriority = false,
    )
    viewModel.onUpsertCalendarRule(rule)

    val stateAfterAdd = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(1, stateAfterAdd.calendarRules.size)
    assertEquals("Reunião de Trabalho", stateAfterAdd.calendarRules[0].name)
    assertTrue(stateAfterAdd.calendarRules[0].enabled)

    // Toggle
    viewModel.onToggleCalendarRule(stateAfterAdd.calendarRules[0], false)
    val stateAfterToggle = viewModel.uiState.value as MainScreenUiState.Ready
    assertFalse(stateAfterToggle.calendarRules[0].enabled)

    // Duplicate
    viewModel.onDuplicateCalendarRule(stateAfterToggle.calendarRules[0])
    val stateAfterDuplicate = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(2, stateAfterDuplicate.calendarRules.size)
    val duplicated = stateAfterDuplicate.calendarRules.first { it.id != "cal_1" }
    assertTrue(duplicated.name.contains("Reunião de Trabalho"))

    // Delete
    viewModel.onDeleteCalendarRule("cal_1")
    val stateAfterDelete = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(1, stateAfterDelete.calendarRules.size)
    assertEquals(duplicated.id, stateAfterDelete.calendarRules[0].id)
  }

  @Test
  fun calendarRules_rbacCheck_manageSchedulesRestrictedBlocksMutations() = runTest {
    // Papel VIEWER sem MANAGE_SCHEDULES
    val restrictedConfig = AccessControlConfig(
      activeRole = AppAccessRole.VIEWER,
      grantedPermissions = emptySet(),
    )
    fakePreferences.setAccessControlConfig(restrictedConfig)

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val ready = viewModel.uiState.value as MainScreenUiState.Ready
    assertFalse(ready.accessControlConfig.hasPermission(AppPermission.MANAGE_SCHEDULES))

    val rule = CalendarRule(
      id = "blocked_cal",
      name = "Não deve salvar",
      targetProfileId = "preset_silent",
    )
    viewModel.onUpsertCalendarRule(rule)

    // Feedback de permissão negada exibido e nenhuma regra adicionada
    val stateWithFeedback = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(stateWithFeedback.calendarRules.isEmpty())
    assertEquals(
      AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_SCHEDULES),
      stateWithFeedback.statusMessage,
    )
  }
}

private class FakeSoundSystemController : SoundSystemController {
  private val changes = MutableStateFlow(Unit)
  var soundState: SoundState = SoundState.DEFAULT
  var mode: SoundMode
    get() = soundState.toLegacyApproximation()
    set(value) {
      soundState = SoundState.fromLegacyMode(value)
    }
  var streams: VolumeStreams = VolumeStreams()
  var streamMinPercents: Map<VolumeStreamType, Int> = emptyMap()
  var dndGranted: Boolean = true
  var canWriteSettings: Boolean = true
  var vibrateWhenRingingSetting: Boolean = true
  var externalFlag: Boolean = false

  override val systemChanges: Flow<Unit> = changes
  override val lastBroadcastWasExternal: Boolean
    get() = externalFlag

  override fun clearExternalBroadcastFlag() {
    externalFlag = false
  }

  override fun readCurrentMode(): SoundMode = soundState.toLegacyApproximation()

  override fun readSoundState(): SoundState = soundState

  override fun readVolumeStreams(): VolumeStreams = streams

  override fun readStreamMinPercent(type: VolumeStreamType): Int =
    (streamMinPercents[type] ?: 0).coerceIn(0, 100)

  override fun hasDndPermission(): Boolean = dndGranted

  override fun canWriteSystemSettings(): Boolean = canWriteSettings

  override fun syncFallbackSoundVibrate(enabled: Boolean) {
    if (soundState.audioMode == BaseAudioMode.SOUND) {
      soundState = soundState.copy(vibrateEnabled = enabled)
      if (canWriteSettings) {
        vibrateWhenRingingSetting = enabled
      }
    }
  }

  override fun applySoundMode(mode: SoundMode): Result<SoundMode> =
    applySoundState(SoundState.fromLegacyMode(mode)).map { it.toLegacyApproximation() }

  override fun applySoundState(state: SoundState): Result<SoundState> = runCatching {
    val targetNeedsDnd = state.audioMode == BaseAudioMode.SILENT && !state.vibrateEnabled
    val currentlyNeedsDnd = soundState.audioMode == BaseAudioMode.SILENT && !soundState.vibrateEnabled
    if ((targetNeedsDnd || currentlyNeedsDnd) && !dndGranted) {
      throw DndPermissionRequiredException()
    }
    soundState = state
    if (state.audioMode == BaseAudioMode.SOUND && canWriteSettings) {
      vibrateWhenRingingSetting = state.vibrateEnabled
    }
    soundState
  }

  override fun applyStreamVolume(type: VolumeStreamType, percent: Int): Result<VolumeStreams> = runCatching {
    streams = streams.withUpdatedStream(type, percent, minPercent = readStreamMinPercent(type))
    streams
  }

  override fun applyAllStreams(streams: VolumeStreams): Result<VolumeStreams> = runCatching {
    var updated = this.streams
    VolumeStreamType.entries.forEach { type ->
      updated = updated.withUpdatedStream(
        type = type,
        percent = streams.percentFor(type),
        minPercent = readStreamMinPercent(type),
      )
    }
    this.streams = updated
    this.streams
  }
}

private class FakeSoundPreferencesDataSource : SoundPreferencesDataSource {
  private val state = MutableStateFlow(PersistedSoundPrefs())
  override val prefsFlow: Flow<PersistedSoundPrefs> = state

  fun clearSchedules() {
    state.value = state.value.copy(scheduleRules = emptyList())
  }

  override suspend fun setActiveProfileId(profileId: String?) {
    state.value = state.value.copy(activeProfileId = profileId)
  }

  override suspend fun setActiveTimer(session: TimedModeSession?) {
    state.value = state.value.copy(activeTimer = session)
  }

  override suspend fun upsertCustomProfile(profile: SoundProfile) {
    val updated = state.value.customProfiles.filterNot { it.id == profile.id } + profile
    state.value = state.value.copy(
      customProfiles = updated,
      deletedProfileIds = state.value.deletedProfileIds - profile.id,
    )
  }

  override suspend fun deleteCustomProfile(profileId: String) {
    val updated = state.value.customProfiles.filterNot { it.id == profileId }
    state.value = state.value.copy(
      customProfiles = updated,
      deletedProfileIds = state.value.deletedProfileIds + profileId,
    )
  }

  override suspend fun upsertScheduleRule(rule: ScheduleRule) {
    val existing = state.value.scheduleRules.toMutableList()
    val idx = existing.indexOfFirst { it.id == rule.id }
    if (idx >= 0) existing[idx] = rule else existing.add(rule)
    state.value = state.value.copy(scheduleRules = existing)
  }

  override suspend fun deleteScheduleRule(ruleId: String) {
    state.value = state.value.copy(
      scheduleRules = state.value.scheduleRules.filterNot { it.id == ruleId },
    )
  }

  override suspend fun upsertLocationRule(rule: LocationRule) {
    val existing = state.value.locationRules.toMutableList()
    val idx = existing.indexOfFirst { it.id == rule.id }
    if (idx >= 0) existing[idx] = rule else existing.add(rule)
    state.value = state.value.copy(locationRules = existing)
  }

  override suspend fun deleteLocationRule(ruleId: String) {
    state.value = state.value.copy(
      locationRules = state.value.locationRules.filterNot { it.id == ruleId },
    )
  }

  override suspend fun upsertCalendarRule(rule: CalendarRule) {
    val existing = state.value.calendarRules.toMutableList()
    val idx = existing.indexOfFirst { it.id == rule.id }
    if (idx >= 0) existing[idx] = rule else existing.add(rule)
    state.value = state.value.copy(calendarRules = existing)
  }

  override suspend fun deleteCalendarRule(ruleId: String) {
    state.value = state.value.copy(
      calendarRules = state.value.calendarRules.filterNot { it.id == ruleId },
    )
  }

  override suspend fun setRestorePolicy(policy: RestorePolicy) {
    state.value = state.value.copy(restorePolicy = policy)
  }

  override suspend fun setDefaultDayProfileId(profileId: String) {
    state.value = state.value.copy(defaultDayProfileId = profileId)
  }

  override suspend fun setSavedSnapshot(snapshot: SavedStateSnapshot?) {
    state.value = state.value.copy(savedSnapshot = snapshot)
  }

  override suspend fun setLastActiveSoundSnapshot(snapshot: SavedStateSnapshot?) {
    state.value = state.value.copy(lastActiveSoundSnapshot = snapshot)
  }

  override suspend fun setLastAppliedRuleSessionKey(sessionKey: String?) {
    state.value = state.value.copy(lastAppliedRuleSessionKey = sessionKey)
  }

  override suspend fun setOverriddenSessionKey(sessionKey: String?) {
    state.value = state.value.copy(overriddenSessionKey = sessionKey)
  }

  override suspend fun appendTransitionLog(entry: TransitionLogEntry) {
    state.value = state.value.copy(
      transitionLogs = listOf(entry) + state.value.transitionLogs,
    )
  }

  override suspend fun clearTransitionLogs() {
    state.value = state.value.copy(transitionLogs = emptyList())
  }

  override suspend fun setUseDynamicColor(enabled: Boolean) {
    state.value = state.value.copy(useDynamicColor = enabled)
  }

  override suspend fun setThemeMode(mode: ThemeMode) {
    state.value = state.value.copy(themeMode = mode)
  }

  override suspend fun setLocationDisclosureAccepted(accepted: Boolean) {
    state.value = state.value.copy(hasAcceptedLocationDisclosure = accepted)
  }

  override suspend fun setMuteMediaOnSilent(enabled: Boolean) {
    state.value = state.value.copy(muteMediaOnSilent = enabled)
  }

  override suspend fun setPreserveMediaOnBluetooth(enabled: Boolean) {
    state.value = state.value.copy(preserveMediaOnBluetooth = enabled)
  }

  override suspend fun setSmartVolumeTriggerEnabled(enabled: Boolean) {
    state.value = state.value.copy(smartVolumeTriggerEnabled = enabled)
  }

  override suspend fun setPersistentQuickBarEnabled(enabled: Boolean) {
    state.value = state.value.copy(persistentQuickBarEnabled = enabled)
  }

  override suspend fun setQuickTileSlot1ProfileId(profileId: String) {
    state.value = state.value.copy(quickTileSlot1ProfileId = profileId)
  }

  override suspend fun setQuickTileSlot2ProfileId(profileId: String) {
    state.value = state.value.copy(quickTileSlot2ProfileId = profileId)
  }

  override suspend fun setSavedMediaBeforeMute(percent: Int?) {
    state.value = state.value.copy(savedMediaBeforeMute = percent)
  }

  override suspend fun setAppLanguageCode(languageCode: String) {
    state.value = state.value.copy(appLanguageCode = languageCode)
  }

  override suspend fun setAutomationsPaused(paused: Boolean) {
    state.value = state.value.copy(automationsPaused = paused)
  }

  override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
    state.value = state.value.copy(hapticFeedbackEnabled = enabled)
  }

  override suspend fun setNotifyOnAutoTransition(enabled: Boolean) {
    state.value = state.value.copy(notifyOnAutoTransition = enabled)
  }

  override suspend fun setDefaultTimerMinutes(minutes: Int) {
    state.value = state.value.copy(defaultTimerMinutes = minutes)
  }

  override suspend fun setOnboardingCompleted(completed: Boolean) {
    state.value = state.value.copy(hasCompletedOnboarding = completed)
  }

  override suspend fun setAccessControlConfig(config: AccessControlConfig) {
    state.value = state.value.copy(accessControlConfig = config)
  }

  override suspend fun setSystemZenRulesEnabled(enabled: Boolean) {
    state.value = state.value.copy(systemZenRulesEnabled = enabled)
  }

  override suspend fun setSoundVibrateEnabledFallback(enabled: Boolean) {
    state.value = state.value.copy(soundVibrateEnabledFallback = enabled)
  }

  override suspend fun setRingNotificationLinked(linked: Boolean) {
    state.value = state.value.copy(ringNotificationLinked = linked)
  }

  override suspend fun setVisibleFixedStreamsCount(count: Int) {
    state.value = state.value.copy(
      visibleFixedStreamsCount = count.coerceIn(1, VolumeStreamType.entries.size),
    )
  }

  override suspend fun setStreamSortMode(mode: StreamSortMode) {
    state.value = state.value.copy(streamSortMode = mode)
  }

  override suspend fun incrementStreamUsageCount(type: VolumeStreamType) {
    val currentCounts = state.value.streamUsageCounts.toMutableMap()
    currentCounts[type] = (currentCounts[type] ?: 0).coerceAtLeast(0) + 1
    state.value = state.value.copy(streamUsageCounts = currentCounts)
  }

  override suspend fun setManualStreamOrder(order: List<VolumeStreamType>) {
    state.value = state.value.copy(
      manualStreamOrder = VolumeStreamType.normalizeManualStreamOrder(order),
    )
  }

  override suspend fun setSecondaryDrawerExpanded(expanded: Boolean) {
    state.value = state.value.copy(secondaryDrawerExpanded = expanded)
  }

  override suspend fun setVolumePreviewEnabled(enabled: Boolean) {
    state.value = state.value.copy(volumePreviewEnabled = enabled)
  }

  override suspend fun setUseSystemSoundsForPreview(enabled: Boolean) {
    state.value = state.value.copy(useSystemSoundsForPreview = enabled)
  }

  override suspend fun setSilentPreAdjustEnabled(enabled: Boolean) {
    state.value = state.value.copy(silentPreAdjustEnabled = enabled)
  }

  override suspend fun setPendingSoundStreams(pending: Map<VolumeStreamType, Int>) {
    state.value = state.value.copy(pendingSoundStreams = pending)
  }

  override suspend fun resetFactoryDefaults() {
    state.value = PersistedSoundPrefs(
      themeMode = state.value.themeMode,
      appLanguageCode = state.value.appLanguageCode,
    )
  }

  override suspend fun exportBackupJson(): String {
    val current = state.value
    val payload = SoundPrefsBackupPayload(
      version = 3,
      customProfiles = current.customProfiles.map { it.normalized() },
      deletedProfileIds = current.deletedProfileIds,
      scheduleRules = current.scheduleRules,
      locationRules = current.locationRules,
      calendarRules = current.calendarRules,
      restorePolicy = current.restorePolicy,
      defaultDayProfileId = current.defaultDayProfileId,
      useDynamicColor = current.useDynamicColor,
      themeMode = current.themeMode,
      appLanguageCode = current.appLanguageCode,
      automationsPaused = current.automationsPaused,
      hapticFeedbackEnabled = current.hapticFeedbackEnabled,
      notifyOnAutoTransition = current.notifyOnAutoTransition,
      defaultTimerMinutes = current.defaultTimerMinutes,
      muteMediaOnSilent = current.muteMediaOnSilent,
      preserveMediaOnBluetooth = current.preserveMediaOnBluetooth,
      smartVolumeTriggerEnabled = current.smartVolumeTriggerEnabled,
      persistentQuickBarEnabled = current.persistentQuickBarEnabled,
      quickTileSlot1ProfileId = current.quickTileSlot1ProfileId,
      quickTileSlot2ProfileId = current.quickTileSlot2ProfileId,
      accessControlConfig = current.accessControlConfig,
      systemZenRulesEnabled = current.systemZenRulesEnabled,
      soundVibrateEnabledFallback = current.soundVibrateEnabledFallback,
      ringNotificationLinked = current.ringNotificationLinked,
      visibleFixedStreamsCount = current.visibleFixedStreamsCount,
      streamSortMode = current.streamSortMode,
      streamUsageCounts = current.streamUsageCounts,
      manualStreamOrder = current.manualStreamOrder,
      secondaryDrawerExpanded = current.secondaryDrawerExpanded,
      volumePreviewEnabled = current.volumePreviewEnabled,
      useSystemSoundsForPreview = current.useSystemSoundsForPreview,
      silentPreAdjustEnabled = current.silentPreAdjustEnabled,
    )
    return Json.encodeToString(payload)
  }

  override suspend fun importBackupJson(rawJson: String) {
    val json = Json { ignoreUnknownKeys = true }
    val payload = json.decodeFromString<SoundPrefsBackupPayload>(rawJson.trim())
    state.value = state.value.copy(
      customProfiles = payload.customProfiles.map { it.normalized() },
      deletedProfileIds = payload.deletedProfileIds,
      scheduleRules = payload.scheduleRules,
      locationRules = payload.locationRules,
      calendarRules = payload.calendarRules,
      restorePolicy = payload.restorePolicy,
      defaultDayProfileId = payload.defaultDayProfileId,
      useDynamicColor = payload.useDynamicColor,
      themeMode = payload.themeMode,
      appLanguageCode = payload.appLanguageCode,
      automationsPaused = payload.automationsPaused,
      hapticFeedbackEnabled = payload.hapticFeedbackEnabled,
      notifyOnAutoTransition = payload.notifyOnAutoTransition,
      defaultTimerMinutes = payload.defaultTimerMinutes,
      muteMediaOnSilent = payload.muteMediaOnSilent,
      preserveMediaOnBluetooth = payload.preserveMediaOnBluetooth,
      smartVolumeTriggerEnabled = payload.smartVolumeTriggerEnabled,
      persistentQuickBarEnabled = payload.persistentQuickBarEnabled,
      quickTileSlot1ProfileId = payload.quickTileSlot1ProfileId,
      quickTileSlot2ProfileId = payload.quickTileSlot2ProfileId,
      accessControlConfig = payload.accessControlConfig,
      systemZenRulesEnabled = payload.systemZenRulesEnabled,
      soundVibrateEnabledFallback = payload.soundVibrateEnabledFallback,
      ringNotificationLinked = payload.ringNotificationLinked,
      visibleFixedStreamsCount = payload.visibleFixedStreamsCount.coerceIn(1, VolumeStreamType.entries.size),
      streamSortMode = payload.streamSortMode,
      streamUsageCounts = payload.streamUsageCounts.mapValues { (_, v) -> v.coerceAtLeast(0) },
      manualStreamOrder = VolumeStreamType.normalizeManualStreamOrder(payload.manualStreamOrder),
      secondaryDrawerExpanded = payload.secondaryDrawerExpanded,
      volumePreviewEnabled = payload.volumePreviewEnabled,
      useSystemSoundsForPreview = payload.useSystemSoundsForPreview,
      silentPreAdjustEnabled = payload.silentPreAdjustEnabled,
    )
  }
}

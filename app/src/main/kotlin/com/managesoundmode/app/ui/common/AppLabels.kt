package com.managesoundmode.app.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.managesoundmode.app.domain.model.AppAccessRole
import com.managesoundmode.app.domain.model.AppPermission
import com.managesoundmode.app.domain.model.BaseAudioMode
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundState
import com.managesoundmode.app.domain.model.StreamSortMode
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.domain.model.Weekday
import com.managesoundmode.app.domain.model.ZenithAtmosphericPalette
import java.util.Locale

enum class AppLanguage(
  val code: String,
  val displayName: String,
  val shortLabel: String,
) {
  PT_BR("pt-BR", "Português (Brasil)", "Português"),
  EN("en", "English", "English"),
}

/**
 * Catálogo centralizado de Internacionalização (i18n) e Labels de UI do aplicativo.
 * Mantém todos os textos visíveis ao usuário organizados por domínio em um único arquivo,
 * com suporte nativo e reativo a Português (PT-BR) e Inglês (EN).
 */
object AppLabels {

  var currentLanguage: AppLanguage by mutableStateOf(AppLanguage.PT_BR)

  private fun tr(pt: String, en: String): String =
    when (currentLanguage) {
      AppLanguage.PT_BR -> pt
      AppLanguage.EN -> en
    }

  object Domain {
    fun baseAudioModeTitle(audioMode: BaseAudioMode): String =
      when (audioMode) {
        BaseAudioMode.SOUND -> tr("Som", "Sound")
        BaseAudioMode.SILENT -> tr("Silêncio", "Silent")
      }

    fun soundStateTitle(state: SoundState): String =
      when (state.audioMode) {
        BaseAudioMode.SOUND ->
          if (state.vibrateEnabled) {
            tr("Som + Vibrar", "Sound + Vibrate")
          } else {
            tr("Som (Sem Vibrar)", "Sound (No Vibrate)")
          }
        BaseAudioMode.SILENT ->
          if (state.vibrateEnabled) {
            tr("Silêncio + Vibrar", "Silent + Vibrate")
          } else {
            tr("Silêncio Total", "Total Silence")
          }
      }

    fun soundStateSubtitle(state: SoundState): String =
      when (state.audioMode) {
        BaseAudioMode.SOUND ->
          if (state.vibrateEnabled) {
            tr("Toque audível com vibração ativa", "Audible ring with vibration active")
          } else {
            tr("Toque audível sem vibração", "Audible ring without vibration")
          }
        BaseAudioMode.SILENT ->
          if (state.vibrateEnabled) {
            tr("Silêncio sonoro, pulso tátil discreto", "Silent audio, discreet haptic pulse")
          } else {
            tr("Foco profundo, zero interrupções", "Deep focus, zero interruptions")
          }
      }

    fun profileSummaryLabel(state: SoundState, streams: VolumeStreams): String =
      when (state.audioMode) {
        BaseAudioMode.SOUND ->
          if (state.vibrateEnabled) {
            tr(
              "Som + Vibrar • Toque ${streams.ringPercent}% • Mídia ${streams.mediaPercent}%",
              "Sound + Vibrate • Ring ${streams.ringPercent}% • Media ${streams.mediaPercent}%",
            )
          } else {
            tr(
              "Som (Sem Vibrar) • Toque ${streams.ringPercent}% • Mídia ${streams.mediaPercent}%",
              "Sound (No Vibrate) • Ring ${streams.ringPercent}% • Media ${streams.mediaPercent}%",
            )
          }
        BaseAudioMode.SILENT ->
          if (state.vibrateEnabled) {
            tr(
              "Silêncio + Vibrar • Mídia ${streams.mediaPercent}%",
              "Silent + Vibrate • Media ${streams.mediaPercent}%",
            )
          } else {
            tr(
              "Silêncio Total • Alarme ${streams.alarmPercent}%",
              "Total Silence • Alarm ${streams.alarmPercent}%",
            )
          }
      }

    fun soundModeTitle(mode: SoundMode): String =
      when (mode) {
        SoundMode.NORMAL -> tr("Som Ativo", "Active Sound")
        SoundMode.VIBRATE -> tr("Vibrar", "Vibrate")
        SoundMode.SILENT -> tr("Silencioso", "Silent")
      }

    fun soundModeSubtitle(mode: SoundMode): String =
      when (mode) {
        SoundMode.NORMAL -> tr("Toque e notificações audíveis", "Audible ring and notifications")
        SoundMode.VIBRATE -> tr("Silêncio sonoro, alerta tátil", "Silent audio, haptic alert")
        SoundMode.SILENT -> tr("Foco total, zero interrupções", "Total focus, zero interruptions")
      }

    fun streamTypeLabel(type: VolumeStreamType): String =
      when (type) {
        VolumeStreamType.MEDIA -> tr("Mídia", "Media")
        VolumeStreamType.RING -> tr("Toque", "Ring")
        VolumeStreamType.NOTIFICATION -> tr("Notificações", "Notifications")
        VolumeStreamType.ALARM -> tr("Alarme", "Alarm")
        VolumeStreamType.SYSTEM -> tr("Sistema", "System")
        VolumeStreamType.VOICE_CALL -> tr("Chamada", "Call")
        VolumeStreamType.ACCESSIBILITY -> tr("Acessibilidade", "Accessibility")
      }

    fun streamTypeMicroDescription(type: VolumeStreamType): String =
      when (type) {
        VolumeStreamType.MEDIA -> tr("Músicas, vídeos e jogos", "Music, videos, and games")
        VolumeStreamType.RING -> tr("Ligações recebidas", "Incoming calls")
        VolumeStreamType.NOTIFICATION -> tr("Mensagens e alertas de apps", "App messages and alerts")
        VolumeStreamType.ALARM -> tr("Despertadores e timers", "Alarms and timers")
        VolumeStreamType.SYSTEM ->
          tr("Teclado, toques na tela e bloqueio", "Keyboard, touch, and lock sounds")
        VolumeStreamType.VOICE_CALL ->
          tr("Voz da pessoa na ligação", "In-call voice audio")
        VolumeStreamType.ACCESSIBILITY ->
          tr("Leitor de tela (TalkBack)", "Screen reader (TalkBack)")
      }

    fun streamSortModeLabel(mode: StreamSortMode): String =
      when (mode) {
        StreamSortMode.MOST_USED -> tr("Mais usados", "Most used")
        StreamSortMode.MANUAL -> tr("Ordem manual", "Manual order")
      }

    fun weekdayShortLabel(day: Weekday): String =
      when (day) {
        Weekday.SUN -> tr("D", "S")
        Weekday.MON -> tr("S", "M")
        Weekday.TUE -> tr("T", "T")
        Weekday.WED -> tr("Q", "W")
        Weekday.THU -> tr("Q", "T")
        Weekday.FRI -> tr("S", "F")
        Weekday.SAT -> tr("S", "S")
      }

    fun weekdayFullLabel(day: Weekday): String =
      when (day) {
        Weekday.SUN -> tr("Dom", "Sun")
        Weekday.MON -> tr("Seg", "Mon")
        Weekday.TUE -> tr("Ter", "Tue")
        Weekday.WED -> tr("Qua", "Wed")
        Weekday.THU -> tr("Qui", "Thu")
        Weekday.FRI -> tr("Sex", "Fri")
        Weekday.SAT -> tr("Sáb", "Sat")
      }

    fun daysSummary(daysOfWeek: Set<Weekday>): String {
      if (daysOfWeek.size == 7) return tr("Todos os dias", "Every day")
      if (daysOfWeek == Weekday.weekdays) return tr("Seg a Sex", "Mon to Fri")
      if (daysOfWeek == Weekday.sleepWeeknights) return tr("Dom a Qui", "Sun to Thu")
      if (daysOfWeek == setOf(Weekday.SAT, Weekday.SUN)) return tr("Fim de semana", "Weekends")
      return Weekday.entries.filter { it in daysOfWeek }.joinToString(", ") { it.fullLabel }
    }

    fun themeModeLabel(mode: ThemeMode): String =
      when (mode) {
        ThemeMode.SYSTEM -> tr("Sistema", "System")
        ThemeMode.LIGHT -> tr("Claro", "Light")
        ThemeMode.DARK -> tr("Escuro", "Dark")
      }

    fun restorePolicyTitle(policy: RestorePolicy): String =
      when (policy) {
        RestorePolicy.DEFAULT_PROFILE -> tr("Perfil Padrão do Dia", "Default Day Profile")
        RestorePolicy.SNAPSHOT -> tr("Último Estado Manual (Snapshot)", "Last Manual State (Snapshot)")
      }

    fun restorePolicySubtitle(policy: RestorePolicy): String =
      when (policy) {
        RestorePolicy.DEFAULT_PROFILE ->
          tr(
            "Garante retorno ao perfil base (Som Ativo) ao encerrar qualquer período",
            "Guarantees return to the base profile (Active Sound) when any schedule ends",
          )
        RestorePolicy.SNAPSHOT ->
          tr(
            "Restaura exatamente o modo e os volumes que estavam antes da regra iniciar",
            "Restores the exact mode and volumes that were active before the rule started",
          )
      }

    fun triggerSourceBadge(source: TransitionTriggerSource): String =
      when (source) {
        TransitionTriggerSource.MANUAL -> tr("Manual", "Manual")
        TransitionTriggerSource.TIMER -> tr("Timer", "Timer")
        TransitionTriggerSource.SCHEDULE -> tr("Horário", "Schedule")
        TransitionTriggerSource.CALENDAR -> tr("Agenda", "Calendar")
        TransitionTriggerSource.LOCATION -> tr("Local", "Location")
        TransitionTriggerSource.RESTORE -> tr("Auto-Restore", "Auto-Restore")
        TransitionTriggerSource.BOOT -> tr("Sistema / Boot", "System / Boot")
        TransitionTriggerSource.QUICK_TILE -> tr("Atalho Rápido", "Quick Tile")
        TransitionTriggerSource.NOTIFICATION -> tr("Notificação", "Notification")
        TransitionTriggerSource.WIDGET -> tr("Widget", "Widget")
        TransitionTriggerSource.HARDWARE_BUTTON -> tr("Botão de Volume", "Volume Button")
      }
  }

  object Common {
    val back: String get() = tr("Voltar", "Back")
    val create: String get() = tr("Criar", "Create")
    val cancel: String get() = tr("Cancelar", "Cancel")
    val ok: String get() = tr("OK", "OK")
    val restore: String get() = tr("Restaurar", "Restore")
    val allow: String get() = tr("Permitir", "Allow")
    val authorize: String get() = tr("Autorizar", "Authorize")
    val enable: String get() = tr("Ativar", "Enable")
    val clear: String get() = tr("Limpar", "Clear")
    val autoSavedSubtitle: String get() = tr("Salvo automaticamente", "Saved automatically")
  }

  object Tabs {
    val nowLabel: String get() = tr("Agora", "Now")
    val nowTopBarTitle: String get() = tr("Modos de Som", "Sound Modes")
    val profilesLabel: String get() = tr("Perfis", "Profiles")
    val profilesTopBarTitle: String get() = tr("Perfis", "Profiles")
    val schedulesLabel: String get() = tr("Horários", "Schedules")
    val schedulesTopBarTitle: String get() = tr("Horários", "Schedules")
    val locationsLabel: String get() = tr("Locais", "Locations")
    val locationsTopBarTitle: String get() = tr("Locais", "Locations")
    val settingsLabel: String get() = tr("Configurações", "Settings")
    val settingsTopBarTitle: String get() = tr("Configurações", "Settings")
  }

  object TopBar {
    val enableDarkMode: String get() = tr("Ativar Modo Escuro", "Enable Dark Mode")
    val enableLightMode: String get() = tr("Ativar Modo Claro", "Enable Light Mode")
    val overflowMenuDescription: String get() = tr("Menu de opções", "More options")
    val openSettingsMenu: String get() = tr("Configurações", "Settings")
    val pauseAutomationsMenu: String
      get() = tr("Pausar Automações (Modo Férias)", "Pause Automations (Vacation Mode)")
    val resumeAutomationsMenu: String
      get() = tr("Retomar Automações", "Resume Automations")
    val transitionHistoryMenu: String
      get() = tr("Histórico de Transições", "Transition History")
    val permissionsDiagnosticMenu: String
      get() = tr("Diagnóstico de Permissões", "Permissions Diagnostic")
    val restoreFactoryDefaultsMenu: String
      get() = tr("Restaurar Padrões de Fábrica", "Restore Factory Defaults")
    val aboutAndPrivacyMenu: String
      get() = tr("Sobre & Privacidade", "About & Privacy")
    val setupWizardMenu: String
      get() = tr("Assistente de Setup & Permissões", "Setup & Permissions Wizard")
    val accessControlMenu: String
      get() = tr("Controle de Acesso & PIN", "Access Control & PIN")
    val proUnlockMenu: String
      get() = tr("Modos de Som Pro", "Sound Modes Pro")
  }

  object Fab {
    val newProfile: String get() = tr("Novo Perfil", "New Profile")
    val newSchedule: String get() = tr("Novo Horário", "New Schedule")
    val newCalendarRule: String get() = tr("Nova Regra de Agenda", "New Calendar Rule")
    val newLocation: String get() = tr("Novo Local", "New Location")
  }

  object Now {
    val keepForLabel: String get() = tr("Manter por:", "Keep for:")
    val extend30MinChip: String get() = "+30m"
    val volumesTitle: String get() = tr("Volumes", "Volumes")
    val fourChannelsBadge: String get() = tr("4 CANAIS", "4 CHANNELS")
    val secondaryDrawerExpandedLabel: String
      get() = tr("Ocultar volumes secundários", "Hide secondary volumes")
    val ringNotificationLinkedMessage: String
      get() = tr("Toque e Notificações vinculados", "Ring and Notifications linked")
    val ringNotificationUnlinkedMessage: String
      get() = tr("Toque e Notificações separados", "Ring and Notifications unlinked")
    val previewChipActiveLabel: String
      get() = tr("Ícone • 2× • Segure", "Icon • 2× • Hold")
    val previewChipMutedLabel: String
      get() = tr("Ajuste mudo", "Muted adjust")
    val previewChipSilentProtectedLabel: String
      get() = tr("Silêncio protegido", "Silence protected")
    val previewPlayingSubtitle: String
      get() = tr("Tocando...", "Playing...")
    val previewLoopingSubtitle: String
      get() = tr("Em loop • solte p/ parar", "Looping • release to stop")
    val previewCueSubtitle: String
      get() = tr("Toque p/ ouvir • segure em loop", "Tap to listen • hold to loop")
    val pendingForSoundBadge: String
      get() = tr("Pré-ajuste p/ Som", "Pre-set for Sound")
    val silentPreviewDialogTitle: String
      get() = tr("Aparelho em modo silencioso", "Device in silent mode")
    val silentPreviewDialogBody: String
      get() = tr(
        "Deseja reproduzir uma amostra rápida deste canal agora? O modo silencioso será restaurado imediatamente ao terminar.",
        "Play a quick sample of this channel now? Silent mode will be restored immediately afterward.",
      )
    val silentPreviewKeepMutedButton: String
      get() = tr("Manter mudo", "Keep muted")
    val silentPreviewListenNowButton: String
      get() = tr("Ouvir agora", "Listen now")
    val volumePreviewEnabledFeedback: String
      get() = tr("Preview sonoro por gesto ativado", "Gesture sound preview enabled")
    val volumePreviewMutedFeedback: String
      get() = tr("Ajuste mudo ativado (preview pausado)", "Muted adjustment active (preview paused)")

    fun streamPreviewIconContentDescription(label: String, isPlaying: Boolean): String =
      if (isPlaying) {
        tr("Parar preview de $label", "Stop $label preview")
      } else {
        tr("Ouvir preview de $label (segure para loop)", "Preview $label volume (hold to loop)")
      }

    fun channelsBadge(fixedCount: Int, totalCount: Int = 7): String =
      if (fixedCount >= totalCount) {
        tr("$totalCount CANAIS", "$totalCount CHANNELS")
      } else {
        tr("$fixedCount DE $totalCount FIXOS", "$fixedCount OF $totalCount PINNED")
      }

    fun secondaryDrawerCollapsedLabel(hiddenStreams: List<VolumeStreamType>): String {
      val names = hiddenStreams.joinToString(", ") { it.label }
      return tr("Outros volumes ($names)", "Other volumes ($names)")
    }

    fun ringNotificationLinkContentDescription(linked: Boolean): String =
      if (linked) {
        tr(
          "Toque e Notificações vinculados • Toque para separar",
          "Ring and Notifications linked • Tap to unlink",
        )
      } else {
        tr(
          "Toque e Notificações separados • Toque para vincular",
          "Ring and Notifications unlinked • Tap to link",
        )
      }
    val manualOverrideTitle: String get() = tr("Ajustado manualmente", "Manually adjusted")
    val manualOverrideSubtitle: String
      get() = tr("Retoma a rotina no próximo horário", "Resumes routine at next schedule")
    val reapplyButton: String get() = tr("Reaplicar", "Reapply")
    val dndBannerTitle: String
      get() = tr("Permissão Não Perturbe", "Do Not Disturb Permission")
    val dndBannerSubtitle: String
      get() = tr("Necessária para o modo Silencioso", "Required for Silent mode")
    val writeSettingsBannerTitle: String
      get() = tr("Modificar Configurações do Sistema", "Modify System Settings")
    val writeSettingsBannerSubtitle: String
      get() = tr(
        "Autorize para sincronizar a vibração em chamadas no modo Som",
        "Authorize to sync call vibration while in Sound mode",
      )
    val writeSettingsPromptMessage: String
      get() = tr(
        "Preferência salva! Autorize 'Modificar configurações do sistema' para controlar a vibração no modo Som.",
        "Preference saved! Authorize 'Modify system settings' to control vibration in Sound mode.",
      )
    val vibrateShortPill: String get() = tr("Vibrar", "Vibrate")
    val noVibrateBadge: String get() = tr("Sem vibrar", "No vibrate")

    fun audioModeShortPill(audioMode: BaseAudioMode): String =
      when (audioMode) {
        BaseAudioMode.SOUND -> tr("Som", "Sound")
        BaseAudioMode.SILENT -> tr("Silêncio", "Silent")
      }

    fun audioModeContentDescription(audioMode: BaseAudioMode): String =
      tr("Modo ${audioMode.title}", "Mode ${audioMode.title}")

    fun vibrateToggleContentDescription(enabled: Boolean): String =
      if (enabled) {
        tr("Vibrar ativado", "Vibrate enabled")
      } else {
        tr("Vibrar desativado", "Vibrate disabled")
      }

    fun modeChangedFeedback(state: SoundState): String =
      tr("Modo alterado para ${state.title}", "Mode changed to ${state.title}")

    fun vibrateToggledFeedback(enabled: Boolean, state: SoundState): String =
      if (enabled) {
        tr("Vibração ativada • ${state.title}", "Vibration enabled • ${state.title}")
      } else {
        tr("Vibração desativada • ${state.title}", "Vibration disabled • ${state.title}")
      }

    fun timerStartedFeedback(state: SoundState, minutes: Int): String =
      tr("${state.title} ativado por $minutes min", "${state.title} enabled for $minutes min")

    fun modeShortPill(mode: SoundMode): String =
      when (mode) {
        SoundMode.NORMAL -> tr("Som", "Sound")
        SoundMode.VIBRATE -> tr("Vibrar", "Vibrate")
        SoundMode.SILENT -> tr("Silêncio", "Silent")
      }

    fun modeContentDescription(mode: SoundMode): String =
      tr("Modo ${mode.title}", "Mode ${mode.title}")

    fun remainingTimerLabel(remainingMinutes: Int): String =
      tr("Volta em ${remainingMinutes}m", "Restores in ${remainingMinutes}m")

    fun modeWithMediaSubtitle(subtitle: String, mediaPercent: Int): String =
      tr("$subtitle • Mídia $mediaPercent%", "$subtitle • Media $mediaPercent%")
  }

  object Profiles {
    val listHint: String
      get() = tr(
        "Toque para ativar • Pressione e segure para editar ou excluir",
        "Tap to activate • Press and hold to edit or delete",
      )
    val defaultReturnSectionTitle: String
      get() = tr("Perfil Padrão de Retorno", "Default Return Profile")
    val defaultReturnSectionSubtitle: String
      get() = tr(
        "Aplicado automaticamente quando uma rotina de horário ou local chega ao fim.",
        "Applied automatically when a schedule or location routine ends.",
      )
    val activeBadge: String get() = tr("Ativo agora", "Active now")
    val defaultReturnBadge: String get() = tr("Padrão de retorno", "Default return")
    val editProfileMenu: String get() = tr("Editar perfil", "Edit profile")
    val duplicateProfileMenu: String get() = tr("Duplicar perfil", "Duplicate profile")
    val setDefaultReturnMenu: String
      get() = tr("Definir como padrão de retorno", "Set as default return profile")
    val deleteProfileMenu: String get() = tr("Excluir perfil", "Delete profile")
    val editProfileTitle: String get() = tr("Editar Perfil", "Edit Profile")
    val newProfileTitle: String get() = tr("Novo Perfil", "New Profile")
    val profileNameLabel: String get() = tr("Nome do perfil", "Profile name")
    val baseSoundModeSectionTitle: String
      get() = tr("Modo de toque", "Ringer mode")
    val channelsVolumeSectionTitle: String
      get() = tr("Volumes salvos neste perfil", "Volumes saved in this profile")
    val preserveMediaTitle: String
      get() = tr("Preservar mídia se música estiver tocando", "Preserve media if music is playing")
    val skipBluetoothTitle: String
      get() = tr("Não alterar mídia com fone Bluetooth", "Do not change media on Bluetooth headphones")
    val applyNowButton: String get() = tr("Ativar Este Perfil Agora", "Activate This Profile Now")
    val saveAndActivateButton: String get() = tr("Criar e Ativar", "Create & Activate")
    val createOnlyButton: String get() = tr("Criar Perfil", "Create Profile")
    val secondaryChannelsSectionTitle: String
      get() = tr("Canais secundários opcionais (Sistema, Chamada, Acessibilidade)", "Optional secondary channels (System, Call, Accessibility)")
    val secondaryChannelsSectionSubtitle: String
      get() = tr(
        "Desativados por padrão para preservar o volume atual do aparelho ao ativar este perfil.",
        "Disabled by default so current device volumes are preserved when activating this profile.",
      )
    val keepDeviceVolumeLabel: String
      get() = tr("Manter atual", "Keep current")

    fun secondaryOverridesBadge(count: Int): String =
      tr("+$count secundário(s)", "+$count secondary")

    fun iconOptions(): List<Pair<String, String>> = listOf(
      "work" to tr("Trabalho", "Work"),
      "bedtime" to tr("Dormir", "Sleep"),
      "vibration" to tr("Reunião", "Meeting"),
      "home" to tr("Casa", "Home"),
      "sunny" to tr("Dia", "Day"),
      "shield" to tr("Foco", "Focus"),
    )

    fun defaultStreamsDescription(streams: VolumeStreams): String =
      tr(
        "Toque ${streams.ringPercent}% • Mídia ${streams.mediaPercent}%",
        "Ring ${streams.ringPercent}% • Media ${streams.mediaPercent}%",
      )

    fun streamsSummaryLine(streams: VolumeStreams): String =
      tr(
        "Toque ${streams.ringPercent}% • Notif ${streams.notificationPercent}% • Mídia ${streams.mediaPercent}% • Alarme ${streams.alarmPercent}%",
        "Ring ${streams.ringPercent}% • Notif ${streams.notificationPercent}% • Media ${streams.mediaPercent}% • Alarm ${streams.alarmPercent}%",
      )

    fun optionsContentDescription(name: String): String =
      tr("Opções do perfil $name", "Options for profile $name")
  }

  object Schedules {
    val segmentFixed: String get() = tr("Horários Fixos", "Fixed Schedules")
    val segmentCalendar: String get() = tr("Agenda", "Calendar")
    val exactAlarmBannerTitle: String
      get() = tr(
        "Autorize alarmes exatos para pontualidade",
        "Allow exact alarms for punctuality",
      )
    val listHint: String
      get() = tr(
        "Toque para editar • Pressione e segure para opções ou excluir",
        "Tap to edit • Press and hold for options or delete",
      )
    val highPriorityBadge: String get() = tr("Prioritário", "High Priority")
    val editScheduleMenu: String get() = tr("Editar horário", "Edit schedule")
    val duplicateScheduleMenu: String get() = tr("Duplicar horário", "Duplicate schedule")
    val enableScheduleMenu: String get() = tr("Ativar horário", "Enable schedule")
    val disableScheduleMenu: String get() = tr("Desativar horário", "Disable schedule")
    val deleteScheduleMenu: String get() = tr("Excluir horário", "Delete schedule")
    val editScheduleTitle: String get() = tr("Editar Horário", "Edit Schedule")
    val newScheduleTitle: String get() = tr("Novo Horário", "New Schedule")
    val defaultScheduleName: String get() = tr("Rotina Agendada", "Scheduled Routine")
    val scheduleNameLabel: String
      get() = tr("Nome da rotina (ex.: Dormir, Trabalho)", "Schedule name (e.g. Sleep, Work)")
    val startTimeLabel: String get() = tr("INÍCIO", "START")
    val endTimeLabel: String get() = tr("FIM", "END")
    val daysOfWeekSectionTitle: String get() = tr("Repetir nos dias", "Repeat on days")
    val targetProfileSectionTitle: String
      get() = tr("Perfil de som aplicado", "Applied sound profile")
    val highPriorityTitle: String
      get() = tr("Alta Prioridade (Sobrepor outros)", "High Priority (Override others)")
    val highPrioritySubtitle: String
      get() = tr(
        "Vence conflitos mesmo quando configurado para Som Alto",
        "Wins conflicts even when configured for Loud Sound",
      )
    val createScheduleButton: String get() = tr("Criar Horário", "Create Schedule")
    val selectStartTimeTitle: String
      get() = tr("Horário de Início", "Start Time")
    val selectEndTimeTitle: String
      get() = tr("Horário de Término", "End Time")

    fun optionsContentDescription(name: String): String =
      tr("Opções do horário $name", "Options for schedule $name")
  }

  object CalendarRules {
    val tabHint: String
      get() = tr(
        "Silencie ou ajuste o som automaticamente conforme eventos da sua agenda local",
        "Silence or adjust sound automatically according to local calendar events",
      )
    val permissionCardTitle: String
      get() = tr("Automação por Agenda (100% On-Device)", "Calendar Automation (100% On-Device)")
    val permissionCardSubtitle: String
      get() = tr(
        "Silencie o aparelho automaticamente durante reuniões e compromissos marcados como ocupados. O aplicativo lê os eventos diretamente do seu aparelho, sem enviar nenhum dado para servidores externos.",
        "Automatically silence your device during meetings and busy events. The app reads events directly on-device without sending data to external servers.",
      )
    val grantPermissionButton: String
      get() = tr("Autorizar Agenda", "Grant Calendar Access")
    val disclosureTitle: String
      get() = tr("Permissão de Leitura de Agenda", "Calendar Read Permission")
    val disclosureMessage: String
      get() = tr(
        "Para silenciar o som durante compromissos e reuniões, o Manage Sound Mode precisa ler os horários e títulos dos eventos do seu calendário local.\n\nPrivacidade 100% On-Device: nenhum evento é enviado para fora do dispositivo.",
        "To silence audio during meetings and appointments, Manage Sound Mode needs to read event times and titles from your local calendar.\n\n100% On-Device Privacy: no event data ever leaves your device.",
      )
    val disclosureContinue: String
      get() = tr("Continuar", "Continue")
    val disclosureDismiss: String
      get() = tr("Agora não", "Not now")
    val emptyListTitle: String
      get() = tr("Nenhuma regra de agenda", "No calendar rules")
    val emptyListSubtitle: String
      get() = tr(
        "Crie regras para silenciar o telefone automaticamente em reuniões, aulas ou consultas médicas.",
        "Create rules to automatically silence your phone during meetings, classes, or appointments.",
      )
    val editorTitleNew: String
      get() = tr("Nova Regra de Agenda", "New Calendar Rule")
    val editorTitleEdit: String
      get() = tr("Editar Regra de Agenda", "Edit Calendar Rule")
    val nameLabel: String
      get() = tr("Nome da Regra", "Rule Name")
    val namePlaceholder: String
      get() = tr("Ex.: Reuniões de Trabalho, Aulas...", "e.g., Work Meetings, Classes...")
    val calendarsSectionTitle: String
      get() = tr("Agendas do Dispositivo", "Device Calendars")
    val allCalendarsOption: String
      get() = tr("Todas as agendas visíveis", "All visible calendars")
    val busyOnlyTitle: String
      get() = tr("Apenas eventos \"Ocupado\" (Busy)", "Only \"Busy\" events")
    val busyOnlySubtitle: String
      get() = tr("Ignora eventos livres ou informativos", "Ignores free or tentative events")
    val ignoreAllDayTitle: String
      get() = tr("Ignorar eventos de dia inteiro", "Ignore all-day events")
    val ignoreAllDaySubtitle: String
      get() = tr("Evita silenciar o dia inteiro em aniversários ou feriados", "Avoids silencing all day for birthdays or holidays")
    val keywordFilterLabel: String
      get() = tr("Filtro por palavra-chave (opcional)", "Keyword filter (optional)")
    val keywordFilterPlaceholder: String
      get() = tr("Ex.: Reunião, Daily, Consulta (separadas por vírgula)", "e.g., Meeting, Daily, Doctor (comma-separated)")
    val targetProfileTitle: String
      get() = tr("Perfil de Som Aplicado", "Applied Sound Profile")
    val highPriorityTitle: String
      get() = tr("Sobrepor outras regras (Alta prioridade)", "Override other rules (High priority)")
    val highPrioritySubtitle: String
      get() = tr("Prevalece sobre horários fixos e locais comuns", "Prevails over standard schedules and locations")
    val confirmSave: String
      get() = tr("Salvar Regra", "Save Rule")
    val chipBusyOnly: String
      get() = tr("Apenas Ocupado", "Busy Only")
    val chipAnyAvailability: String
      get() = tr("Qualquer disponibilidade", "Any Availability")
    val chipAllCalendars: String
      get() = tr("Todas as agendas", "All Calendars")
    fun chipCalendarsCount(count: Int): String =
      tr("$count agendas", "$count calendars")
    fun chipKeyword(filter: String): String =
      tr("Filtro: \"$filter\"", "Filter: \"$filter\"")
    val chipHighPriority: String
      get() = tr("Alta Prioridade", "High Priority")
    val chipIgnoreAllDay: String
      get() = tr("Ignora dia inteiro", "Ignores all-day")
    val ruleDeletedSnackbar: String
      get() = tr("Regra de agenda excluída", "Calendar rule deleted")
    val ruleSavedSnackbar: String
      get() = tr("Regra de agenda salva", "Calendar rule saved")
    val editRuleMenu: String get() = tr("Editar regra", "Edit rule")
    val duplicateRuleMenu: String get() = tr("Duplicar regra", "Duplicate rule")
    val enableRuleMenu: String get() = tr("Ativar regra", "Enable rule")
    val disableRuleMenu: String get() = tr("Desativar regra", "Disable rule")
    val deleteRuleMenu: String get() = tr("Excluir regra", "Delete rule")

    fun optionsContentDescription(name: String): String =
      tr("Opções da regra $name", "Options for rule $name")
  }

  object Locations {
    val geofenceHeaderTitle: String
      get() = tr("Cercas Geográficas (OpenStreetMap)", "Geofences (OpenStreetMap)")
    val geofenceHeaderSubtitle: String
      get() = tr(
        "Toque em um local para ajustar o pino e o raio no mapa ou pressione e segure para excluir.",
        "Tap a location to adjust its map pin and radius, or press and hold to delete.",
      )
    val readingGpsButton: String get() = tr("Lendo GPS...", "Reading GPS...")
    val verifyGpsButton: String get() = tr("Verificar pelo GPS", "Verify via GPS")
    val gpsReadErrorMessage: String
      get() = tr(
        "Não foi possível ler o GPS agora. Verifique se a localização do aparelho está ativa.",
        "Could not read GPS right now. Please check if device location is enabled.",
      )
    val gpsPermissionRequiredMessage: String
      get() = tr(
        "Permissão de localização necessária para verificar cercas pelo GPS.",
        "Location permission is required to verify geofences via GPS.",
      )
    val insideBadge: String get() = tr("No local", "At location")
    val editLocationMenu: String get() = tr("Editar local e mapa", "Edit location & map")
    val markOutsideMenu: String
      get() = tr("Marcar como fora deste local", "Mark as outside this location")
    val simulateInsideMenu: String
      get() = tr("Simular presença neste local", "Simulate presence at this location")
    val enableLocationMenu: String get() = tr("Ativar local", "Enable location")
    val disableLocationMenu: String get() = tr("Desativar local", "Disable location")
    val deleteLocationMenu: String get() = tr("Excluir local", "Delete location")
    val editLocationTitle: String get() = tr("Editar Local", "Edit Location")
    val newLocationTitle: String get() = tr("Novo Local", "New Location")
    val defaultLocationName: String get() = tr("Local Personalizado", "Custom Location")
    val locationNameLabel: String
      get() = tr(
        "Nome do local (ex.: Escritório, Faculdade)",
        "Location name (e.g. Office, College)",
      )
    val radiusSectionTitle: String
      get() = tr("Raio da Cerca Geográfica", "Geofence Radius")
    val wifiSsidLabel: String
      get() = tr(
        "Rede Wi-Fi opcional (confirmação indoor)",
        "Optional Wi-Fi SSID (indoor confirmation)",
      )
    val targetProfileSectionTitle: String
      get() = tr("Perfil ao permanecer no local", "Profile while at this location")
    val testPresenceButton: String
      get() = tr("Testar presença neste local agora", "Test presence at this location now")
    val createLocationButton: String get() = tr("Criar Local", "Create Location")

    fun radiusLabel(meters: Int): String = tr("Raio ${meters}m", "Radius ${meters}m")
    fun metersValue(meters: Int): String = tr("$meters metros", "$meters meters")
    fun optionsContentDescription(name: String): String =
      tr("Opções do local $name", "Options for location $name")

    val geofenceStatusTitle: String
      get() = tr("Geofencing Passivo em 2º Plano", "Passive Background Geofencing")
    fun geofenceStatusSubtitle(activeCount: Int): String =
      tr(
        "$activeCount cerca(s) ativa(s) • Dwell 2 min + Confirmação Wi-Fi SSID",
        "$activeCount active geofence(s) • 2-min Dwell + Wi-Fi SSID confirmation",
      )
    val syncGeofencesButton: String
      get() = tr("Sincronizar Cercas", "Sync Geofences")
    fun geofencesSyncedMessage(count: Int): String =
      tr(
        "Geofencing em 2º plano sincronizado ($count cerca(s) ativa(s))",
        "Background geofencing synced ($count active geofence(s))",
      )
  }

  object GeofenceMap {
    val mapCardTitle: String
      get() = tr("Mapa da Cerca Geográfica", "Geofence Map")
    val mapCardSubtitle: String
      get() = tr(
        "Toque no mapa, arraste o pino ou busque o endereço para posicionar o perímetro.",
        "Tap the map, drag the pin, or search an address to position the perimeter.",
      )
    val fetchingGpsLabel: String get() = tr("Obtendo GPS...", "Getting GPS...")
    val useCurrentGpsLabel: String get() = tr("Usar GPS atual", "Use current GPS")
    val searchPlaceholder: String
      get() = tr(
        "Buscar rua, bairro, empresa ou cidade...",
        "Search street, neighborhood, place or city...",
      )
    val searchContentDescription: String get() = tr("Buscar endereço", "Search address")
    val searchResultsHint: String
      get() = tr(
        "Toque em um endereço para posicionar no mapa:",
        "Tap an address to position on the map:",
      )
    val zoomInContentDescription: String get() = tr("Aproximar mapa", "Zoom in")
    val zoomOutContentDescription: String get() = tr("Afastar mapa", "Zoom out")
    val recenterContentDescription: String get() = tr("Centralizar pino", "Recenter pin")

    fun coordinatesAndRadiusBadge(lat: Double, lng: Double, radiusMeters: Int): String =
      String.format(
        Locale.ROOT,
        tr("%.4f, %.4f • Raio %dm", "%.4f, %.4f • Radius %dm"),
        lat,
        lng,
        radiusMeters,
      )
  }

  object Disclosure {
    val title: String
      get() = tr(
        "Automação por Localização (100% On-Device)",
        "Location Automation (100% On-Device)",
      )
    val body: String
      get() = tr(
        "O Modos de Som coleta dados de localização em segundo plano para ativar e desativar automaticamente seus perfis de som (Silencioso, Vibrar ou Volume Alto) quando você entra ou sai dos locais configurados, mesmo quando o aplicativo está fechado ou não está em uso.",
        "Sound Modes collects location data in the background to automatically enable and disable your sound profiles (Silent, Vibrate, or High Volume) when you enter or leave configured locations, even when the app is closed or not in use.",
      )
    val privacyHighlight: String
      get() = tr(
        "Privacidade Total: Suas coordenadas ficam salvas exclusivamente no seu aparelho. Nenhum dado de GPS é enviado para servidores.",
        "Total Privacy: Your coordinates are stored exclusively on your device. No GPS data is ever sent to external servers.",
      )
    val acceptButton: String get() = tr("Entendi e Continuar", "I Understand & Continue")
    val dismissButton: String get() = tr("Agora não", "Not now")
  }

  object Settings {
    val appearanceSection: String get() = tr("Aparência", "Appearance")
    val appThemeTitle: String get() = tr("Tema do Aplicativo", "App Theme")
    val appLanguageTitle: String get() = tr("Idioma do Aplicativo", "App Language")
    val dynamicColorsTitle: String
      get() = tr("Cores Dinâmicas (Material You)", "Dynamic Colors (Material You)")
    val dynamicColorsSubtitle: String
      get() = tr(
        "Adapta as cores ao papel de parede do Android",
        "Adapts colors to your Android wallpaper",
      )
    val volumeNotificationsMediaSection: String
      get() = tr("Botões de Volume, Notificações & Mídia", "Volume Buttons, Notifications & Media")
    val suggestTimerTitle: String
      get() = tr("Sugerir Timer ao Silenciar", "Suggest Timer When Silencing")
    val suggestTimerSubtitle: String
      get() = tr(
        "Exibe notificação com [30m] [1h] [2h] quando você silencia pelo botão físico",
        "Shows notification with [30m] [1h] [2h] when silencing via hardware button",
      )
    val testSmartNotificationButton: String
      get() = tr("Testar notificação inteligente", "Test smart notification")
    val testSmartNotificationSentMessage: String
      get() = tr(
        "Sugestão pós-volume enviada! Confira o alerta no topo da tela.",
        "Post-volume prompt sent! Check the heads-up alert at the top.",
      )
    val notificationPermissionBannerTitle: String
      get() = tr(
        "Permissão de Notificações Pendente",
        "Notification Permission Pending",
      )
    val notificationPermissionBannerSubtitle: String
      get() = tr(
        "Autorize notificações no Android para exibir a Barra Rápida 1-Toque, o Cronômetro e a Sugestão Pós-Volume.",
        "Allow Android notifications to display the 1-Tap Quick Bar, Timer and Post-Volume Prompt.",
      )
    val notificationPermissionRequiredMessage: String
      get() = tr(
        "Autorize a permissão de notificações para exibir alertas na gaveta.",
        "Allow notification permission to display alerts in the drawer.",
      )
    val persistentQuickBarPendingPermissionSubtitle: String
      get() = tr(
        "Permissão pendente • Toque em Autorizar acima para exibir a barra 1-toque na gaveta",
        "Permission pending • Tap Authorize above to show the 1-tap bar in the drawer",
      )
    val muteMediaOnSilentTitle: String
      get() = tr("Zerar Mídia ao Silenciar", "Mute Media When Silencing")
    val muteMediaOnSilentSubtitle: String
      get() = tr(
        "Zera vídeos/jogos ao entrar em Vibrar ou Silencioso e devolve ao voltar",
        "Mutes videos/games when entering Vibrate or Silent and restores on return",
      )
    val preserveBluetoothTitle: String
      get() = tr("Preservar Mídia no Bluetooth", "Preserve Media on Bluetooth")
    val preserveBluetoothSubtitle: String
      get() = tr(
        "Mantém o volume do fone ou som automotivo conectado",
        "Keeps volume on connected headphones or car audio",
      )
    val widgetsAndGadgetsSection: String
      get() = tr("Widgets & Gadgets do Sistema", "System Widgets & Gadgets")
    val persistentQuickBarTitle: String
      get() = tr("Gadget Fixo na Barra de Notificações", "Pinned Notification Bar Gadget")
    val persistentQuickBarSubtitle: String
      get() = tr(
        "Exibe barra silenciosa com botões [Som] [Silêncio] e toggle [Vibrar] nas notificações",
        "Shows silent bar with [Sound] [Silent] buttons and [Vibrate] toggle in notifications",
      )
    val headerModeTilesTitle: String
      get() = tr("Blocos de Modo no Header (Trio ou Ciclo)", "Header Mode Tiles (Trio or Cycle)")
    val headerModeTilesSubtitle: String
      get() = tr(
        "Adicione os 3 blocos independentes (apenas o modo atual fica colorido no topo e qualquer troca leva 1 toque) ou o bloco único de ciclo.",
        "Add the 3 independent tiles (only the active mode is highlighted and switching takes 1 tap) or the single cycle tile.",
      )
    val addTileNormalButton: String get() = tr("+ Som", "+ Sound")
    val addTileVibrateButton: String get() = tr("+ Vibrar", "+ Vibrate")
    val addTileSilentButton: String get() = tr("+ Silêncio", "+ Silent")
    val addCycleTileButton: String
      get() = tr(
        "Adicionar Bloco Único (Ciclo: Som → Vibrar → Silêncio)",
        "Add Single Tile (Cycle: Sound → Vibrate → Silent)",
      )
    val favoriteProfileTilesTitle: String
      get() = tr("Blocos de Perfis Favoritos no Header", "Favorite Profile Tiles in Header")
    val favoriteProfileTilesSubtitle: String
      get() = tr(
        "Escolha até 2 perfis para acionar no topo do Android aplicando os 4 canais de volume em 1 toque.",
        "Choose up to 2 profiles to trigger from the Android header, applying all 4 volume channels in 1 tap.",
      )
    val utilityTilesTitle: String
      get() = tr("Blocos de Timer Rápido & Zero Mídia", "Quick Timer & Zero Media Tiles")
    val utilityTilesSubtitle: String
      get() = tr(
        "Atalhos de 1 toque no Header para iniciar Timer de Silêncio (30m/1h/2h) ou zerar a Mídia (0%) antes de abrir vídeos.",
        "1-tap header shortcuts to start Silent Timer (30m/1h/2h) or mute Media (0%) before opening videos.",
      )
    val addTimerTileButton: String get() = tr("+ Timer (30m/1h)", "+ Timer (30m/1h)")
    val addMediaMuteTileButton: String get() = tr("+ Zero Mídia (0%)", "+ Zero Media (0%)")
    val homeWidgetTitle: String
      get() = tr("Widget Material You (2x1, 4x1 e 4x2)", "Material You Widget (2x1, 4x1 & 4x2)")
    val homeWidgetSubtitle: String
      get() = tr(
        "Troque entre Som, Vibrar, Silêncio e Perfis em 1 toque direto na tela inicial ou segure o ícone do app para ver os 4 atalhos rápidos.",
        "Switch between Sound, Vibrate, Silent and Profiles in 1 tap on your home screen or long-press the app icon for 4 quick shortcuts.",
      )
    val pinHomeWidgetButton: String
      get() = tr("Fixar Widget na Tela Inicial", "Pin Widget to Home Screen")
    val restorePolicySection: String
      get() = tr("Ao Encerrar um Período", "When a Schedule Ends")
    val systemPermissionsSection: String
      get() = tr("Permissões do Sistema", "System Permissions")
    val dndPermissionTitle: String
      get() = tr("Acesso ao Não Perturbe (DND)", "Do Not Disturb Access (DND)")
    val grantedStatus: String get() = tr("Concedido", "Granted")
    val pendingTapToAuthorizeStatus: String
      get() = tr("Pendente • Toque para autorizar", "Pending • Tap to authorize")
    val smartNotificationsTitle: String
      get() = tr("Notificações Inteligentes", "Smart Notifications")
    val smartNotificationsGrantedSubtitle: String
      get() = tr("Concedido • Timer e sugestões ativos", "Granted • Timer and suggestions active")
    val smartNotificationsPendingSubtitle: String
      get() = tr("Pendente • Toque para permitir notificações", "Pending • Tap to allow notifications")
    val exactAlarmsTitle: String get() = tr("Alarmes Exatos", "Exact Alarms")
    val writeSettingsPermissionTitle: String
      get() = tr(
        "Modificar Configurações do Sistema",
        "Modify System Settings",
      )
    val writeSettingsPermissionGrantedSubtitle: String
      get() = tr(
        "Concedido • Alternância de vibração em Som ativa",
        "Granted • Vibration toggle in Sound active",
      )
    val writeSettingsPermissionPendingSubtitle: String
      get() = tr(
        "Sob demanda • Toque para autorizar vibração no modo Som",
        "Just-in-time • Tap to authorize vibration in Sound mode",
      )
    val onDeviceStorageTitle: String
      get() = tr("Armazenamento 100% On-Device", "100% On-Device Storage")
    val onDeviceStorageSubtitle: String
      get() = tr("Ativo • Zero dados enviados à nuvem", "Active • Zero data sent to the cloud")
    val recentHistorySection: String get() = tr("Histórico Recente", "Recent History")
    val emptyHistoryMessage: String
      get() = tr("Nenhuma transição registrada ainda.", "No transitions recorded yet.")

    // --- Categorized Settings Hub & New Sub-Screens ---
    val masterAutomationTitle: String
      get() = tr("Automação Inteligente", "Smart Automation")
    val masterAutomationActiveSubtitle: String
      get() = tr(
        "Ativa • Horários e locais funcionando automaticamente",
        "Active • Schedules and locations running automatically",
      )
    val masterAutomationPausedSubtitle: String
      get() = tr(
        "Modo Férias ativo • Todas as rotinas automáticas pausadas",
        "Vacation Mode active • All automatic routines paused",
      )
    val hubPreferencesGroupTitle: String
      get() = tr("Preferências e Comportamento", "Preferences & Behavior")
    val hubSystemGroupTitle: String
      get() = tr("Sistema, Dados e Informações", "System, Data & Info")

    val categoryAppearanceTitle: String
      get() = tr("Aparência, Tema & Idioma", "Appearance, Theme & Language")
    val categoryAutomationTitle: String
      get() = tr("Automação & Retorno de Rotina", "Automation & Routine Return")
    val categoryAudioMediaTitle: String
      get() = tr("Botões de Volume, Mídia & Timer", "Volume Buttons, Media & Timer")
    val categoryGadgetsWidgetsTitle: String
      get() = tr("Atalhos no Header & Widgets", "Header Shortcuts & Widgets")
    val categoryPermissionsTitle: String
      get() = tr("Permissões & Diagnóstico", "Permissions & Diagnostics")
    val categoryAccessControlTitle: String
      get() = tr("Controle de Acesso & PIN (RBAC)", "Access Control & PIN (RBAC)")
    val categoryProUnlockTitle: String
      get() = tr("Modos de Som Pro", "Sound Modes Pro")
    fun categoryProUnlockSubtitle(isProUnlocked: Boolean): String =
      if (isProUnlocked) {
        tr("Vitalício Ativo • Recursos Ilimitados", "Lifetime Active • Unlimited Features")
      } else {
        tr("Compra única vitalícia • 100% Sem anúncios", "One-time lifetime purchase • 100% Ad-free")
      }
    val categoryHistoryTitle: String
      get() = tr("Histórico de Transições", "Transition History")
    val categoryBackupResetTitle: String
      get() = tr("Backup & Restaurar Padrões", "Backup & Factory Reset")
    val categoryAboutTitle: String
      get() = tr("Sobre o Modos de Som & Privacidade", "About Sound Modes & Privacy")

    val hapticFeedbackTitle: String
      get() = tr("Vibração Tátil nos Controles", "Haptic Feedback on Controls")
    val hapticFeedbackSubtitle: String
      get() = tr(
        "Retorno tátil suave ao alternar modos, perfis e sliders",
        "Gentle tactile feedback when switching modes, profiles and sliders",
      )
    val notifyOnAutoTransitionTitle: String
      get() = tr("Notificar ao Ativar Rotina Automática", "Notify on Automatic Routine")
    val notifyOnAutoTransitionSubtitle: String
      get() = tr(
        "Exibe aviso silencioso quando um horário ou local mudar o modo de som",
        "Shows silent alert when a schedule or location changes the sound mode",
      )
    val defaultTimerSectionTitle: String
      get() = tr("Duração Padrão do Timer Rápido", "Default Quick Timer Duration")
    val defaultTimerSectionSubtitle: String
      get() = tr(
        "Usada ao iniciar o timer pelo atalho rápido do Header ou Widget",
        "Used when starting a timer from the Header quick tile or Widget",
      )

    val locationPermissionTitle: String
      get() = tr("Localização para Cercas (GPS)", "Location for Geofences (GPS)")
    val locationPermissionGrantedSubtitle: String
      get() = tr("Concedido • Cercas geográficas ativas", "Granted • Geofences active")
    val locationPermissionPendingSubtitle: String
      get() = tr("Opcional • Conceda na aba Locais se usar cercas", "Optional • Grant in Locations tab if using geofences")

    val calendarPermissionTitle: String
      get() = tr("Agenda / Calendário (Opcional)", "Calendar (Optional)")
    val optionalCalendarPermissionSubtitle: String
      get() = tr("Opcional (Não solicitado)", "Optional (Not requested)")

    val filterAll: String get() = tr("Todos", "All")
    val filterManual: String get() = tr("Manual", "Manual")
    val filterSchedule: String get() = tr("Horário", "Schedule")
    val filterCalendar: String get() = tr("Agenda", "Calendar")
    val filterLocation: String get() = tr("Local", "Location")
    val filterShortcuts: String get() = tr("Atalhos", "Shortcuts")

    val backupSectionTitle: String
      get() = tr("Exportar & Importar Configurações (JSON)", "Export & Import Settings (JSON)")
    val backupSectionSubtitle: String
      get() = tr(
        "Salve uma cópia dos seus perfis, horários, locais e preferências ou restaure em outro aparelho.",
        "Save a copy of your profiles, schedules, locations and preferences or restore on another device.",
      )
    val exportBackupButton: String
      get() = tr("Copiar Backup JSON", "Copy JSON Backup")
    val importBackupButton: String
      get() = tr("Importar JSON", "Import JSON")
    val importBackupInputLabel: String
      get() = tr("Cole aqui o JSON de backup exportado", "Paste exported backup JSON here")
    val backupCopiedMessage: String
      get() = tr("Backup JSON copiado para a área de transferência", "Backup JSON copied to clipboard")

    val factoryResetSectionTitle: String
      get() = tr("Restaurar Padrões de Fábrica", "Restore Factory Defaults")
    val factoryResetSectionSubtitle: String
      get() = tr(
        "Recria os 5 perfis originais (Trabalho, Reunião, Dormir, Casa e Padrão), restaura horários/locais de fábrica e redefine preferências.",
        "Recreates the 5 original profiles (Work, Meeting, Sleep, Home and Default), restores default schedules/locations and resets preferences.",
      )
    val factoryResetButton: String
      get() = tr("Restaurar Perfis e Regras de Fábrica", "Restore Factory Profiles & Rules")
    val factoryResetDialogTitle: String
      get() = tr("Restaurar padrões de fábrica?", "Restore factory defaults?")
    val factoryResetDialogMessage: String
      get() = tr(
        "Todos os perfis predefinidos excluídos serão recuperados e suas regras de horário e local voltarão para a configuração original.",
        "All deleted preset profiles will be recovered and your schedule and location rules will return to their original configuration.",
      )

    val aboutVersionSubtitle: String
      get() = tr("Versão 0.1.0 (MCP1) • Android 8 a 16", "Version 0.1.0 (MCP1) • Android 8 to 16")
    val aboutTagline: String
      get() = tr(
        "Controle inteligente de som, perfis multicanal, horários e cercas geográficas sem nunca perder chamadas importantes.",
        "Smart sound control, multi-channel profiles, schedules and geofences without ever missing important calls.",
      )
    val priorityHierarchyTitle: String
      get() = tr("Como Funciona a Prioridade de Regras", "How Rule Priority Works")
    val priorityStep1: String
      get() = tr(
        "1. Ajuste Manual ou Timer Temporário: Sua ação manual sempre tem prioridade imediata.",
        "1. Manual Adjustment or Temporary Timer: Your manual action always takes immediate priority.",
      )
    val priorityStep2: String
      get() = tr(
        "2. Regra com Alta Prioridade: Vence conflitos entre regras simultâneas.",
        "2. High Priority Rule: Wins conflicts between simultaneous rules.",
      )
    val priorityStep3: String
      get() = tr(
        "3. Mais Restritiva Vence: Silêncio Total > Silêncio + Vibrar > Som (Sem Vibrar) > Som + Vibrar.",
        "3. Most Restrictive Wins: Total Silence > Silent + Vibrate > Sound (No Vibrate) > Sound + Vibrate.",
      )
    val priorityStep4: String
      get() = tr(
        "4. Retorno Seguro: Ao fim da rotina, restaura seu Perfil Padrão ou Snapshot.",
        "4. Safe Return: When a routine ends, restores your Default Profile or Snapshot.",
      )
    val privacyCommitmentTitle: String
      get() = tr("Compromisso de Privacidade (100% On-Device)", "Privacy Commitment (100% On-Device)")
    val privacyCommitmentBody: String
      get() = tr(
        "Zero servidores, zero rastreadores e zero anúncios. Todos os seus perfis, horários, coordenadas de cercas geográficas e logs de transição permanecem armazenados exclusivamente na memória local do seu dispositivo.",
        "Zero servers, zero trackers and zero ads. All your profiles, schedules, geofence coordinates and transition logs remain stored exclusively in your device's local storage.",
      )

    val slot3TileTitle: String get() = tr("Perfil do Bloco 3 (Pro)", "Slot 3 Tile Profile (Pro)")
    val slot3TileDesc: String get() = tr("Escolha a cena ativada pelo terceiro Quick Settings Tile.", "Choose the scene activated by the third Quick Settings Tile.")
    val zenithPaletteTitle: String get() = tr("Paleta Atmosférica ZENITH", "ZENITH Atmospheric Palette")
    val zenithPaletteDesc: String get() = tr("Personalize as cores dos orbes e containers com paletas atmosféricas.", "Customize orb and container colors with atmospheric palettes.")
    val adminAutoRelockTitle: String get() = tr("Auto-Relock da Sessão Admin", "Admin Session Auto-Relock")
    val adminAutoRelockDesc: String get() = tr("Bloqueia novamente o acesso de administrador após inatividade.", "Relocks admin access automatically after inactivity.")

    fun appearanceSummary(themeLabel: String, languageLabel: String, dynamicColor: Boolean): String =
      tr(
        "Tema: $themeLabel • $languageLabel${if (dynamicColor) " • Material You" else ""}",
        "Theme: $themeLabel • $languageLabel${if (dynamicColor) " • Material You" else ""}",
      )

    fun automationSummary(policyTitle: String, defaultProfileName: String, paused: Boolean): String =
      if (paused) {
        tr("Pausada (Modo Férias) • Padrão: $defaultProfileName", "Paused (Vacation Mode) • Default: $defaultProfileName")
      } else {
        tr("Retorno: $policyTitle ($defaultProfileName)", "Return: $policyTitle ($defaultProfileName)")
      }

    fun audioMediaSummary(
      muteMedia: Boolean,
      preserveBt: Boolean,
      defaultTimerMinutes: Int,
      smartTriggerEnabled: Boolean = false,
      hasNotificationPermission: Boolean = true,
    ): String =
      if (smartTriggerEnabled && !hasNotificationPermission) {
        tr(
          "Permissão pendente • Zerar mídia: ${if (muteMedia) "Sim" else "Não"} • Timer ${defaultTimerMinutes}m",
          "Permission pending • Mute media: ${if (muteMedia) "On" else "Off"} • Timer ${defaultTimerMinutes}m",
        )
      } else {
        tr(
          "Zerar mídia: ${if (muteMedia) "Sim" else "Não"} • BT: ${if (preserveBt) "Protegido" else "Normal"} • Timer ${defaultTimerMinutes}m",
          "Mute media: ${if (muteMedia) "On" else "Off"} • BT: ${if (preserveBt) "Protected" else "Normal"} • Timer ${defaultTimerMinutes}m",
        )
      }

    fun gadgetsSummary(
      quickBarEnabled: Boolean,
      slot1Name: String,
      slot2Name: String,
      hasNotificationPermission: Boolean = true,
    ): String {
      val barStatusPt = when {
        !quickBarEnabled -> "Inativa"
        !hasNotificationPermission -> "Permissão pendente"
        else -> "Ativa"
      }
      val barStatusEn = when {
        !quickBarEnabled -> "Off"
        !hasNotificationPermission -> "Permission pending"
        else -> "Active"
      }
      return tr(
        "Barra: $barStatusPt • Favoritos: $slot1Name, $slot2Name",
        "Bar: $barStatusEn • Favorites: $slot1Name, $slot2Name",
      )
    }

    fun permissionsSummary(grantedCount: Int, totalCount: Int): String =
      tr(
        "$grantedCount de $totalCount permissões essenciais concedidas",
        "$grantedCount of $totalCount essential permissions granted",
      )

    fun historySummary(count: Int): String =
      if (count == 0) {
        tr("Nenhuma transição registrada ainda", "No transitions recorded yet")
      } else {
        tr("$count transições recentes registradas", "$count recent transitions recorded")
      }

    fun backupSummary(profilesCount: Int, schedulesCount: Int, locationsCount: Int): String =
      tr(
        "$profilesCount perfis • $schedulesCount horários • $locationsCount locais salvos",
        "$profilesCount profiles • $schedulesCount schedules • $locationsCount saved locations",
      )

    fun slotLabel(slotNumber: Int, profileName: String?): String =
      tr(
        "Slot $slotNumber: ${profileName ?: "Nenhum"}",
        "Slot $slotNumber: ${profileName ?: "None"}",
      )

    fun addSlotButton(slotNumber: Int): String =
      tr("+ Adicionar Slot $slotNumber", "+ Add Slot $slotNumber")

    fun quickProfileFallbackName(slotNumber: Int): String =
      tr("Perfil Rápido $slotNumber", "Quick Profile $slotNumber")

    val reopenOnboardingButton: String
      get() = tr("Abrir Assistente de Setup Inicial", "Open Initial Setup Wizard")

    fun accessControlSummary(
      roleTitle: String,
      grantedCount: Int,
      totalCount: Int,
      hasPin: Boolean,
    ): String =
      tr(
        "$roleTitle • $grantedCount/$totalCount ações liberadas • PIN: ${if (hasPin) "Ativo" else "Desativado"}",
        "$roleTitle • $grantedCount/$totalCount actions allowed • PIN: ${if (hasPin) "Active" else "Off"}",
      )

    val systemZenRulesTitle: String
      get() = tr(
        "Modos do Sistema Android 15+ (AutomaticZenRule)",
        "Android 15+ System Modes (AutomaticZenRule)",
      )
    val systemZenRulesSubtitle: String
      get() = tr(
        "Registra seus perfis de som no menu nativo 'Modos' do Android com ativação bidirecional.",
        "Registers your sound profiles in Android's native 'Modes' menu with bidirectional activation.",
      )
    val syncZenRulesButton: String
      get() = tr("Sincronizar Modos com o Sistema", "Sync Modes with System")
    fun zenRulesSyncedMessage(count: Int): String =
      tr(
        "$count modo(s) sincronizado(s) com o sistema Android",
        "$count mode(s) synced with Android system",
      )

    val volumeChannelsSectionTitle: String
      get() = tr("Canais de Volume & Gaveta Inteligente", "Volume Channels & Smart Drawer")
    val volumePreviewSectionTitle: String
      get() = tr("Preview Acústico & Pré-Ajuste Silencioso", "Acoustic Preview & Silent Pre-Adjust")
    val volumePreviewEnabledTitle: String
      get() = tr("Preview Sonoro sob Comando", "On-Demand Volume Preview")
    val volumePreviewEnabledSubtitle: String
      get() = tr(
        "Toque no ícone (1×), dê duplo toque (2×) ou segure a barra para ouvir o volume real",
        "Tap the icon (1×), double-tap (2×), or press and hold a bar to hear actual volume",
      )
    val useSystemSoundsPreviewTitle: String
      get() = tr("Usar Sons Reais do Aparelho", "Use Device System Sounds")
    val useSystemSoundsPreviewSubtitle: String
      get() = tr(
        "Reproduz seu toque, notificação e alarme atuais (ou acorde acústico Zenith quando desligado)",
        "Plays your current ringtone, notification, and alarm tones (or Zenith acoustic chord when off)",
      )
    val silentPreAdjustTitle: String
      get() = tr("Pré-Ajuste Silencioso de Canais", "Silent Channel Pre-Adjustment")
    val silentPreAdjustSubtitle: String
      get() = tr(
        "Permite ajustar Toque, Notificações e Sistema sem desarmar o modo Silencioso ou Vibrar",
        "Adjust Ring, Notifications, and System without disarming Silent or Vibrate mode",
      )
    val ringNotificationLinkTitle: String
      get() = tr("Vincular Toque e Notificações", "Link Ring & Notifications")
    val ringNotificationLinkSubtitle: String
      get() = tr(
        "Sincroniza ambos os sliders em tempo real mantendo as duas barras visíveis",
        "Syncs both sliders in real time while keeping both bars visible",
      )
    val visibleFixedStreamsTitle: String
      get() = tr("Barras Fixas na Tela Inicial", "Pinned Bars on Home Screen")
    val visibleFixedStreamsSubtitle: String
      get() = tr(
        "Escolha quantos canais ficam visíveis fora da gaveta expansível (1 a 7)",
        "Choose how many channels remain visible outside the expandable drawer (1 to 7)",
      )
    val streamSortModeTitle: String
      get() = tr("Ordenação dos Canais de Volume", "Volume Channel Ordering")
    val streamSortModeMostUsedSubtitle: String
      get() = tr(
        "Prioriza silenciosamente os canais mais ajustados ao abrir a tela, sem pular durante o uso",
        "Silently prioritizes your most-adjusted channels on screen entry without mid-session jumps",
      )
    val streamSortModeManualSubtitle: String
      get() = tr(
        "Use as setas para definir a ordem exata das barras fixas e da gaveta",
        "Use the arrows to define the exact order of pinned and drawer bars",
      )
    val pinnedStreamTag: String
      get() = tr("Fixa", "Pinned")
    val drawerStreamTag: String
      get() = tr("Gaveta", "Drawer")

    fun moveStreamUpContentDesc(label: String): String =
      tr("Mover $label para cima", "Move $label up")

    fun moveStreamDownContentDesc(label: String): String =
      tr("Mover $label para baixo", "Move $label down")
  }

  object AccessControl {
    val sectionRoleTitle: String
      get() = tr("Papel de Acesso Ativo (Role)", "Active Access Role")
    val sectionRoleSubtitle: String
      get() = tr(
        "Escolha um nível predefinido de acesso ou personalize cada permissão individualmente abaixo.",
        "Choose a preset access level or customize each individual permission below.",
      )
    val sectionPinTitle: String
      get() = tr("Proteção por PIN (Opcional)", "PIN Protection (Optional)")
    val sectionPinSubtitle: String
      get() = tr(
        "Exige um PIN de 4 dígitos para voltar ao modo Administrador ou alterar permissões quando o app estiver em modo restrito.",
        "Requires a 4-digit PIN to return to Administrator mode or modify permissions while restricted.",
      )
    val pinInputLabel: String
      get() = tr("PIN numérico (4 dígitos)", "Numeric PIN (4 digits)")
    val savePinButton: String
      get() = tr("Salvar PIN", "Save PIN")
    val removePinButton: String
      get() = tr("Remover PIN", "Remove PIN")
    val pinActiveBadge: String
      get() = tr("PIN Ativo", "PIN Active")
    val pinInactiveBadge: String
      get() = tr("Sem PIN", "No PIN")
    val pinSavedMessage: String
      get() = tr("PIN de controle de acesso atualizado com sucesso", "Access control PIN updated")
    val pinRemovedMessage: String
      get() = tr("Proteção por PIN removida", "PIN protection removed")
    val pinInvalidLengthMessage: String
      get() = tr("Informe um PIN numérico de exatamente 4 dígitos", "Enter a 4-digit numeric PIN")
    val sectionMatrixTitle: String
      get() = tr("Matriz Granular de Permissões (RBAC)", "Granular Permissions Matrix (RBAC)")
    val sectionMatrixSubtitle: String
      get() = tr(
        "Ative ou desative cada ação permitida no aplicativo:",
        "Enable or disable each allowed action in the app:",
      )
    val pinChallengeDialogTitle: String
      get() = tr("Autorização Necessária (PIN)", "Authorization Required (PIN)")
    val pinChallengeDialogSubtitle: String
      get() = tr(
        "Digite o PIN de 4 dígitos para desbloquear o Controle de Acesso ou restaurar o papel de Administrador.",
        "Enter the 4-digit PIN to unlock Access Control or restore Administrator role.",
      )
    val pinChallengeInputLabel: String
      get() = tr("Digite o PIN de 4 dígitos", "Enter 4-digit PIN")
    val pinChallengeUnlockButton: String
      get() = tr("Desbloquear", "Unlock")
    val pinChallengeWrongError: String
      get() = tr("PIN incorreto. Tente novamente.", "Incorrect PIN. Please try again.")
    val quickUnlockAdminButton: String
      get() = tr("Desbloquear Administrador", "Unlock Administrator")
    val unlockedAsAdminMessage: String
      get() = tr("Acesso Administrador restaurado", "Administrator access restored")
    val adminAutoRelockedMessage: String
      get() = tr("Sessão de Administrador expirou e foi bloqueada automaticamente.", "Admin session expired and was automatically locked.")

    fun roleTitle(role: AppAccessRole): String =
      when (role) {
        AppAccessRole.ADMIN -> tr("Administrador (Acesso Total)", "Administrator (Full Access)")
        AppAccessRole.OPERATOR -> tr("Operador (Uso Rápido)", "Operator (Quick Use)")
        AppAccessRole.VIEWER -> tr("Somente Leitura (Bloqueado)", "View Only (Locked)")
        AppAccessRole.CUSTOM -> tr("Personalizado (Matriz Granular)", "Custom (Granular Matrix)")
      }

    fun roleSubtitle(role: AppAccessRole): String =
      when (role) {
        AppAccessRole.ADMIN ->
          tr(
            "Libera todas as funções de som, perfis, horários, locais, configurações e backup",
            "Unlocks all sound, profile, schedule, location, settings and backup actions",
          )
        AppAccessRole.OPERATOR ->
          tr(
            "Permite apenas trocar modo/volumes e usar o Timer na aba Agora; bloqueia edições e exclusões",
            "Allows switching modes/volumes and Timer in Now tab; blocks editing and deletion",
          )
        AppAccessRole.VIEWER ->
          tr(
            "Bloqueia qualquer alteração manual de som, perfis, rotinas e configurações",
            "Blocks all manual changes to sound, profiles, routines and settings",
          )
        AppAccessRole.CUSTOM ->
          tr(
            "Permissões selecionadas manualmente na matriz de controle de acesso",
            "Permissions manually selected in the access control matrix",
          )
      }

    fun roleShortBadge(role: AppAccessRole): String =
      when (role) {
        AppAccessRole.ADMIN -> tr("Admin", "Admin")
        AppAccessRole.OPERATOR -> tr("Operador", "Operator")
        AppAccessRole.VIEWER -> tr("Leitura", "Read-Only")
        AppAccessRole.CUSTOM -> tr("Restrito", "Restricted")
      }

    fun permissionTitle(permission: AppPermission): String =
      when (permission) {
        AppPermission.CHANGE_SOUND_AND_TIMER ->
          tr("Alternar Modos, Volumes e Timer", "Switch Modes, Volumes & Timer")
        AppPermission.MANAGE_PROFILES ->
          tr("Criar, Editar e Excluir Perfis", "Create, Edit & Delete Profiles")
        AppPermission.MANAGE_SCHEDULES ->
          tr("Criar, Editar e Excluir Horários", "Create, Edit & Delete Schedules")
        AppPermission.MANAGE_LOCATIONS ->
          tr("Criar, Editar e Excluir Locais", "Create, Edit & Delete Locations")
        AppPermission.PAUSE_AUTOMATIONS ->
          tr("Pausar Automações (Modo Férias)", "Pause Automations (Vacation Mode)")
        AppPermission.ACCESS_SETTINGS ->
          tr("Alterar Configurações do App", "Change App Settings")
        AppPermission.BACKUP_AND_FACTORY_RESET ->
          tr("Backup JSON & Restaurar Padrões", "JSON Backup & Factory Reset")
      }

    fun permissionSubtitle(permission: AppPermission): String =
      when (permission) {
        AppPermission.CHANGE_SOUND_AND_TIMER ->
          tr(
            "Trocar entre Som, Vibrar e Silêncio, mover sliders de volume, ativar perfil e usar o Timer na aba Agora",
            "Switch between Sound, Vibrate and Silent, adjust volume sliders, activate profiles and use Timer",
          )
        AppPermission.MANAGE_PROFILES ->
          tr(
            "Cadastrar novos perfis, duplicar, editar canais, definir padrão de retorno ou remover perfis",
            "Create new profiles, duplicate, edit streams, set default return or delete profiles",
          )
        AppPermission.MANAGE_SCHEDULES ->
          tr(
            "Criar, editar, duplicar, ativar/desativar ou excluir rotinas de horário",
            "Create, edit, duplicate, toggle or delete schedule routines",
          )
        AppPermission.MANAGE_LOCATIONS ->
          tr(
            "Criar, editar mapa/raio, ativar/desativar, simular presença ou excluir locais",
            "Create, edit map/radius, toggle, simulate presence or delete locations",
          )
        AppPermission.PAUSE_AUTOMATIONS ->
          tr(
            "Suspender ou retomar todas as rotinas automáticas pelo Modo Férias",
            "Suspend or resume all automatic routines via Vacation Mode",
          )
        AppPermission.ACCESS_SETTINGS ->
          tr(
            "Modificar tema, idioma, política de retorno, proteção de mídia, gadgets e limpar histórico",
            "Modify theme, language, restore policy, media protection, gadgets and clear history",
          )
        AppPermission.BACKUP_AND_FACTORY_RESET ->
          tr(
            "Exportar ou importar configurações via JSON e executar restauração de fábrica",
            "Export or import settings via JSON and execute factory reset",
          )
      }

    fun permissionDeniedMessage(permission: AppPermission): String =
      tr(
        "Acesso bloqueado pelo Controle de Acesso: '${permission.title}' não está habilitado no papel atual.",
        "Blocked by Access Control: '${permission.title}' is not enabled for the current role.",
      )
  }

  object Onboarding {
    val skipButton: String get() = tr("Pular", "Skip")
    val nextButton: String get() = tr("Continuar", "Continue")
    val finishButton: String get() = tr("Concluir e Começar", "Finish & Get Started")

    fun stepIndicator(current: Int, total: Int): String =
      tr("Passo $current de $total", "Step $current of $total")

    // Passo 1: Boas-vindas, Idioma & Tema
    val step1Badge: String
      get() = tr("Boas-vindas • 100% On-Device", "Welcome • 100% On-Device")
    val step1Title: String
      get() = tr("Controle inteligente de som sem perder chamadas", "Smart sound control without missing calls")
    val step1Subtitle: String
      get() = tr(
        "Configure seu idioma, aparência e permissões em menos de 1 minuto para automatizar sua rotina com segurança.",
        "Set up your language, appearance and permissions in under 1 minute to safely automate your routine.",
      )
    val feature1Title: String
      get() = tr("Proteção de Mídia & Timer Rápido", "Media Protection & Quick Timer")
    val feature1Body: String
      get() = tr(
        "Zera a mídia automaticamente ao silenciar e restaura seu som ao fim de reuniões ou aulas.",
        "Automatically mutes media when silencing and restores your sound after meetings or classes.",
      )
    val feature2Title: String
      get() = tr("Rotinas de Horário e Locais (GPS + Wi-Fi)", "Schedule & Location Routines (GPS + Wi-Fi)")
    val feature2Body: String
      get() = tr(
        "Ativa perfis ao chegar no trabalho ou dormir e respeita alterações manuais nos botões físicos.",
        "Activates profiles when arriving at work or sleeping and respects manual volume button changes.",
      )
    val feature3Title: String
      get() = tr("Privacidade Absoluta (Zero Nuvem)", "Absolute Privacy (Zero Cloud)")
    val feature3Body: String
      get() = tr(
        "Seus horários, locais e histórico nunca saem do aparelho.",
        "Your schedules, locations and history never leave your device.",
      )

    // Passo 2: Setup Rápido de Permissões
    val step2Badge: String
      get() = tr("Permissões do Sistema", "System Permissions")
    val step2Title: String
      get() = tr("Libere os controles essenciais do Android", "Enable essential Android controls")
    val step2Subtitle: String
      get() = tr(
        "O Android exige autorização explícita para alternar o modo Não Perturbe e disparar rotinas no horário exato.",
        "Android requires explicit authorization to toggle Do Not Disturb and trigger routines on time.",
      )
    val permissionGrantedBadge: String get() = tr("Ativo", "Active")
    val permissionRequiredTag: String get() = tr("Essencial", "Essential")
    val permissionRecommendedTag: String get() = tr("Recomendado", "Recommended")
    val permissionOptionalTag: String get() = tr("Opcional", "Optional")

    val dndCardTitle: String
      get() = tr("Acesso ao Não Perturbe (DND)", "Do Not Disturb Access (DND)")
    val dndCardBody: String
      get() = tr(
        "Indispensável para ativar o Modo Silencioso e controlar o volume de toque e notificações.",
        "Required to activate Silent Mode and control ring and notification volumes.",
      )
    val notificationsCardTitle: String
      get() = tr("Notificações Inteligentes & Gadget", "Smart Notifications & Gadget")
    val notificationsCardBody: String
      get() = tr(
        "Permite sugerir timer ao silenciar pelos botões físicos e exibir a barra rápida no topo.",
        "Allows suggesting a timer when silencing via hardware buttons and showing the top quick bar.",
      )
    val exactAlarmCardTitle: String
      get() = tr("Alarmes Exatos para Horários", "Exact Alarms for Schedules")
    val exactAlarmCardBody: String
      get() = tr(
        "Garante que suas rotinas de Trabalho e Sono comecem e terminem no minuto exato.",
        "Ensures your Work and Sleep routines start and end at the exact minute.",
      )
    val locationCardTitle: String
      get() = tr("Localização para Cercas (GPS)", "Location for Geofences (GPS)")
    val locationCardBody: String
      get() = tr(
        "Usada apenas se você quiser mudar o modo de som ao chegar ou sair de locais salvos.",
        "Used only if you want to switch sound modes when arriving at or leaving saved places.",
      )

    fun permissionsProgressLabel(grantedCount: Int, totalCount: Int): String =
      tr(
        "$grantedCount de $totalCount permissões ativas no aparelho",
        "$grantedCount of $totalCount permissions active on device",
      )

    // Passo 3: Perfil Padrão & Preferências Iniciais
    val step3Badge: String
      get() = tr("Toque Final", "Final Touch")
    val step3Title: String
      get() = tr("Como o app deve se comportar no dia a dia?", "How should the app behave day to day?")
    val step3Subtitle: String
      get() = tr(
        "Escolha o perfil para o qual seu celular deve voltar quando timers ou compromissos terminarem.",
        "Choose the profile your phone should return to when timers or appointments finish.",
      )
    val defaultProfileSectionTitle: String
      get() = tr("Perfil Padrão de Retorno", "Default Return Profile")
    val smartBehaviorsSectionTitle: String
      get() = tr("Automações Protetoras Ativas", "Active Protective Automations")
  }

  object Notifications {
    val promptChannelName: String
      get() = tr("Sugestão de Timer ao Silenciar", "Timer Suggestion When Silencing")
    val promptChannelDescription: String
      get() = tr(
        "Pergunta em pop-up silencioso se você deseja restaurar o som ao silenciar pelo botão de volume",
        "Asks via silent heads-up prompt whether to restore sound when silencing via volume button",
      )
    val timerChannelName: String
      get() = tr("Timer de Restauração Ativo", "Active Auto-Restore Timer")
    val timerChannelDescription: String
      get() = tr(
        "Mostra a contagem regressiva nativa enquanto um modo temporizado está ativo",
        "Shows native countdown while a timed mode is active",
      )
    val overrideChannelName: String
      get() = tr("Aviso de Pausa em Rotinas", "Routine Override Alert")
    val overrideChannelDescription: String
      get() = tr(
        "Permite reativar em 1 toque uma rotina alterada pelos botões físicos",
        "Allows re-enabling in 1 tap a routine overridden by volume buttons",
      )
    val quickBarChannelName: String
      get() = tr("Gadget de Controle Rápido no Header", "Header Quick Control Gadget")
    val quickBarChannelDescription: String
      get() = tr(
        "Barra rápida persistente em 1 toque nas notificações para controlar Som, Silêncio e Vibrar",
        "Persistent 1-tap quick bar in notifications to control Sound, Silent and Vibrate",
      )
    val autoTransitionChannelName: String
      get() = tr("Transição de Rotina Automática", "Automatic Routine Transition")
    val autoTransitionChannelDescription: String
      get() = tr(
        "Exibe aviso discreto e silencioso quando um horário ou local ativa ou encerra uma rotina",
        "Shows a discreet silent alert when a schedule or location starts or ends a routine",
      )

    fun quickBarCompactTitle(
      state: SoundState,
      activeProfileName: String?,
    ): String = if (!activeProfileName.isNullOrBlank()) {
      "${state.title} • $activeProfileName"
    } else {
      state.title
    }

    fun quickBarTitle(
      state: SoundState,
      activeProfileName: String?,
    ): String = if (!activeProfileName.isNullOrBlank()) {
      tr("${state.title} • Perfil $activeProfileName", "${state.title} • Profile $activeProfileName")
    } else {
      tr("Estado Atual: ${state.title}", "Current State: ${state.title}")
    }

    fun quickBarCompactSubtitle(state: SoundState, streams: VolumeStreams): String =
      when (state.audioMode) {
        BaseAudioMode.SOUND ->
          tr(
            "Toque ${streams.ringPercent}% • Mídia ${streams.mediaPercent}%",
            "Ring ${streams.ringPercent}% • Media ${streams.mediaPercent}%",
          )
        BaseAudioMode.SILENT ->
          if (state.vibrateEnabled) {
            tr(
              "Vibrar ON • Mídia ${streams.mediaPercent}%",
              "Vibrate ON • Media ${streams.mediaPercent}%",
            )
          } else {
            tr(
              "Silêncio total • Alarme ${streams.alarmPercent}%",
              "Total silence • Alarm ${streams.alarmPercent}%",
            )
          }
      }

    fun quickBarSubtitle(state: SoundState, streams: VolumeStreams): String =
      when (state.audioMode) {
        BaseAudioMode.SOUND ->
          tr(
            "Toque ${streams.ringPercent}% • Notif ${streams.notificationPercent}% • Mídia ${streams.mediaPercent}%",
            "Ring ${streams.ringPercent}% • Notif ${streams.notificationPercent}% • Media ${streams.mediaPercent}%",
          )
        BaseAudioMode.SILENT ->
          if (state.vibrateEnabled) {
            tr(
              "Vibração ativa • Mídia ${streams.mediaPercent}% • Alarme ${streams.alarmPercent}%",
              "Vibration on • Media ${streams.mediaPercent}% • Alarm ${streams.alarmPercent}%",
            )
          } else {
            tr(
              "Silêncio total • Mídia ${streams.mediaPercent}% • Alarme ${streams.alarmPercent}%",
              "Total silence • Media ${streams.mediaPercent}% • Alarm ${streams.alarmPercent}%",
            )
          }
      }

    val quickBarSoundPillLabel: String get() = tr("Som", "Sound")
    val quickBarSilentPillLabel: String get() = tr("Silêncio", "Silent")

    fun quickBarVibratePillLabel(vibrateEnabled: Boolean): String =
      if (vibrateEnabled) tr("Vibrar", "Vibrate") else tr("Sem vibrar", "No vibrate")

    fun quickBarSoundContentDesc(isActive: Boolean): String =
      if (isActive) tr("Som ativo", "Sound active") else tr("Ativar Som", "Enable Sound")

    fun quickBarSilentContentDesc(isActive: Boolean, needsDnd: Boolean): String =
      when {
        needsDnd -> tr("Autorizar Não Perturbe para Silêncio", "Authorize Do Not Disturb for Silent")
        isActive -> tr("Silêncio ativo", "Silent active")
        else -> tr("Ativar Silêncio", "Enable Silent")
      }

    fun quickBarVibrateContentDesc(vibrateEnabled: Boolean, needsDnd: Boolean): String =
      when {
        needsDnd -> tr("Autorizar Não Perturbe para Silêncio Total", "Authorize Do Not Disturb for Total Silence")
        vibrateEnabled -> tr("Vibrar ativado • Toque para desligar", "Vibrate on • Tap to disable")
        else -> tr("Vibrar desativado • Toque para ligar", "Vibrate off • Tap to enable")
      }

    fun smartPromptTitle(stateTitle: String): String =
      tr("$stateTitle ativado", "$stateTitle enabled")

    fun smartPromptSubtitle(stateTitle: String, mediaAutoMuted: Boolean): String =
      if (mediaAutoMuted) {
        tr("Mídia zerada automaticamente • Restaurar som depois?", "Media muted automatically • Restore sound later?")
      } else {
        tr("Por quanto tempo deseja manter em $stateTitle?", "How long would you like to keep $stateTitle?")
      }

    val action30Min: String get() = tr("30 min", "30 min")
    val action1Hour: String get() = tr("1 hora", "1 hour")
    val action2Hours: String get() = tr("2 horas", "2 hours")
    val actionExtend30Min: String get() = "+30 min"
    val actionRestoreNow: String get() = tr("Restaurar agora", "Restore now")
    val actionReapplyRule: String get() = tr("Reativar rotina", "Reapply routine")

    fun activeTimerTitle(targetTitle: String): String =
      tr("$targetTitle temporizado ativo", "Timed $targetTitle active")

    fun activeTimerSubtitle(previousTitle: String): String =
      tr("Voltará para $previousTitle automaticamente", "Will return to $previousTitle automatically")

    fun ruleOverrideTitle(ruleName: String): String =
      tr("Rotina '$ruleName' pausada manualmente", "Routine '$ruleName' paused manually")

    val ruleOverrideSubtitle: String
      get() = tr("Você alterou o som pelos botões físicos.", "You changed sound via hardware buttons.")

    fun autoRoutineActivatedTitle(ruleName: String): String =
      tr("Rotina '$ruleName' iniciada", "Routine '$ruleName' started")

    fun autoRoutineActivatedSubtitle(profileName: String, stateTitle: String): String =
      tr("Perfil $profileName aplicado ($stateTitle)", "Profile $profileName applied ($stateTitle)")

    val autoRoutineEndedTitle: String
      get() = tr("Rotina automática encerrada", "Automatic routine ended")

    fun autoRoutineRestoredSnapshotSubtitle(stateTitle: String): String =
      tr("Estado anterior restaurado ($stateTitle)", "Previous state restored ($stateTitle)")

    fun autoRoutineRestoredProfileSubtitle(profileName: String, stateTitle: String): String =
      tr("Retornou para o perfil $profileName ($stateTitle)", "Returned to profile $profileName ($stateTitle)")

    val dndRequiredAlertTitle: String
      get() = tr("Permissão Não Perturbe necessária", "Do Not Disturb permission required")

    val dndRequiredAlertSubtitle: String
      get() = tr(
        "Toque para autorizar o Não Perturbe e ativar o Silêncio Total.",
        "Tap to authorize Do Not Disturb and enable Total Silence.",
      )

    val actionOpenDndSettings: String
      get() = tr("Autorizar DND", "Authorize DND")
  }

  object Widget {
    val profilesFallback: String get() = tr("Perfis", "Profiles")
    val manualFallback: String get() = tr("Manual", "Manual")

    fun heroSubtitle(
      state: SoundState,
      streams: VolumeStreams,
      remainingTimerMinutes: Int?,
    ): String = when {
      remainingTimerMinutes != null ->
        tr("Timer: ${remainingTimerMinutes}m • Mídia ${streams.mediaPercent}%", "Timer: ${remainingTimerMinutes}m • Media ${streams.mediaPercent}%")
      state.audioMode == BaseAudioMode.SOUND ->
        tr("Toque ${streams.ringPercent}% • Mídia ${streams.mediaPercent}%", "Ring ${streams.ringPercent}% • Media ${streams.mediaPercent}%")
      state.vibrateEnabled ->
        tr("Pulso tátil • Mídia ${streams.mediaPercent}%", "Haptic pulse • Media ${streams.mediaPercent}%")
      else ->
        tr("Zero ruído • Alarme ${streams.alarmPercent}%", "Zero noise • Alarm ${streams.alarmPercent}%")
    }

    fun compactSubtitle(profileName: String?, mediaPercent: Int): String =
      tr(
        "${profileName ?: manualFallback} • Mídia $mediaPercent%",
        "${profileName ?: manualFallback} • Media $mediaPercent%",
      )
  }

  object Tiles {
    val cycleTitle: String get() = tr("Modo de Som (Ciclo)", "Sound Mode (Cycle)")
    val cycleSubtitle: String
      get() = tr("Alterna Som, Silêncio + Vibrar e Silêncio Total", "Cycles Sound, Silent + Vibrate and Total Silence")
    val normalTitle: String get() = tr("Modo: Som", "Mode: Sound")
    val normalDesc: String
      get() = tr("Fica aceso em Som • Preserva preferência de vibração", "Active in Sound • Preserves vibration preference")
    val vibrateTitle: String get() = tr("Vibrar", "Vibrate")
    val vibrateDesc: String
      get() = tr("Toggle independente • Ativa/desativa vibração em Som ou Silêncio", "Independent toggle • Toggles vibration in Sound or Silent")
    val silentTitle: String get() = tr("Modo: Silêncio", "Mode: Silent")
    val silentDesc: String
      get() = tr("Fica aceso em Silêncio • Preserva preferência de vibração", "Active in Silent • Preserves vibration preference")
    val slot1Title: String get() = tr("Perfil Rápido 1", "Quick Profile 1")
    val slot1Desc: String
      get() = tr("Ativa os 4 volumes e estado do Perfil 1 em 1 toque", "Activates 4 volumes and state of Profile 1 in 1 tap")
    val slot2Title: String get() = tr("Perfil Rápido 2", "Quick Profile 2")
    val slot2Desc: String
      get() = tr("Ativa os 4 volumes e estado do Perfil 2 em 1 toque", "Activates 4 volumes and state of Profile 2 in 1 tap")
    val slot3Title: String get() = tr("Perfil Rápido 3", "Quick Profile 3")
    val slot3Desc: String
      get() = tr("Ativa os 4 volumes e estado do Perfil 3 em 1 toque (Pro)", "Activates 4 volumes and state of Profile 3 in 1 tap (Pro)")
    val quickTimerTitle: String get() = tr("Timer Silêncio (30m/1h/2h)", "Silent Timer (30m/1h/2h)")
    val quickTimerDesc: String
      get() = tr("Inicia timer de silêncio com auto-restore ou cancela", "Starts silent timer with auto-restore or cancels")
    val mediaMuteTitle: String get() = tr("Zero Mídia (Mudo Rápido)", "Zero Media (Quick Mute)")
    val mediaMuteDesc: String
      get() = tr("Fica aceso quando Mídia está em 0% • Zera ou restaura", "Active when Media is at 0% • Mutes or restores")

    val soundLabel: String get() = tr("Som", "Sound")
    val silentLabel: String get() = tr("Silêncio", "Silent")
    val vibrateLabel: String get() = tr("Vibrar", "Vibrate")
    val authorizeDndSubtitle: String get() = tr("Autorizar DND", "Authorize DND")
    val authorizeWriteSettingsSubtitle: String get() = tr("Autorizar Sistema", "Authorize System")
    val tapToActivateSubtitle: String get() = tr("Toque p/ ativar", "Tap to activate")
    val tapToSilenceSubtitle: String get() = tr("Toque p/ silenciar", "Tap to silence")
    val configureInAppSubtitle: String get() = tr("Configure no app", "Configure in app")
    val quickTimerInactiveLabel: String get() = tr("Timer Silêncio", "Silent Timer")
    val quickTimerInactiveSubtitle: String get() = tr("Toque: 30m • 1h • 2h", "Tap: 30m • 1h • 2h")
    val mediaMutedLabel: String get() = tr("Mídia Zerada", "Media Muted")
    val zeroMediaLabel: String get() = tr("Zero Mídia", "Zero Media")

    fun cycleLabel(stateTitle: String): String = tr("Som: $stateTitle", "Sound: $stateTitle")
    fun soundActiveSubtitle(ringPercent: Int, vibrateEnabled: Boolean): String =
      if (vibrateEnabled) {
        tr("Ativo + Vibrar • Toque $ringPercent%", "Active + Vib • Ring $ringPercent%")
      } else {
        tr("Ativo • Toque $ringPercent%", "Active • Ring $ringPercent%")
      }
    fun silentActiveSubtitle(alarmPercent: Int, vibrateEnabled: Boolean): String =
      if (vibrateEnabled) {
        tr("Com Vibrar • Alarme $alarmPercent%", "With Vib • Alarm $alarmPercent%")
      } else {
        tr("Total • Alarme $alarmPercent%", "Total • Alarm $alarmPercent%")
      }
    fun vibrateTileSubtitle(enabled: Boolean, audioMode: BaseAudioMode): String =
      if (enabled) {
        tr("ON • ${audioMode.pillLabel}", "ON • ${audioMode.pillLabel}")
      } else {
        tr("OFF • ${audioMode.pillLabel}", "OFF • ${audioMode.pillLabel}")
      }
    fun profileActiveSubtitle(stateTitle: String): String =
      tr("Ativo • $stateTitle", "Active • $stateTitle")
    fun profileSlotFallbackLabel(slotIndex: Int): String =
      tr("Perfil $slotIndex", "Profile $slotIndex")
    fun timerActiveLabel(remainingMin: Int): String =
      tr("Timer • ${remainingMin}m", "Timer • ${remainingMin}m")
    fun timerActiveSubtitle(stateTitle: String): String =
      tr("$stateTitle • Toque: +30m / Fim", "$stateTitle • Tap: +30m / End")
    fun mediaMutedSubtitle(restorePercent: Int): String =
      tr("0% • Toque restaura $restorePercent%", "0% • Tap restores $restorePercent%")
    fun mediaActiveSubtitle(currentPercent: Int): String =
      tr("Mídia $currentPercent% • Toque p/ 0%", "Media $currentPercent% • Tap for 0%")

    fun tileAddedMessage(title: String): String =
      tr("Bloco '$title' adicionado ao topo das notificações!", "Tile '$title' added to Quick Settings!")
    fun tileAlreadyAddedMessage(title: String): String =
      tr("O bloco '$title' já está ativo no topo das notificações.", "Tile '$title' is already in Quick Settings.")
    fun tileCancelledMessage(title: String): String =
      tr("Adição de '$title' cancelada.", "Adding '$title' cancelled.")
    fun tileManualEditHint(title: String): String =
      tr(
        "Puxe a barra de notificações e toque em Editar para posicionar '$title'.",
        "Pull down Quick Settings and tap Edit to place '$title'.",
      )
  }

  object Shortcuts {
    val silence1hShort: String get() = tr("Silêncio + Vibrar 1h", "Silent + Vibrate 1h")
    val silence1hLong: String get() = tr("Silêncio com vibração por 1 hora", "Silent with vibration for 1 hour")
    val workShort: String get() = tr("Perfil Trabalho", "Work Profile")
    val workLong: String get() = tr("Ativar perfil Trabalho", "Activate Work profile")
    val sleepShort: String get() = tr("Perfil Dormir", "Sleep Profile")
    val sleepLong: String get() = tr("Ativar perfil Dormir", "Activate Sleep profile")
    val normalShort: String get() = tr("Som Padrão", "Default Sound")
    val normalLong: String get() = tr("Restaurar perfil Padrão (Dia)", "Restore Default (Day) profile")
  }

  object Pro {
    val title: String get() = tr("Modos de Som Pro", "Sound Modes Pro")
    val subtitle: String get() = tr("Desbloqueio vitalício • 100% Sem anúncios", "Lifetime unlock • 100% Ad-free")
    val activeLifetimeBannerTitle: String get() = tr("Vitalício Ativo", "Lifetime Active")
    val activeLifetimeBannerDesc: String
      get() = tr(
        "Seu acesso vitalício aos Modos de Som Pro está ativo neste dispositivo. Todos os recursos avançados estão liberados.",
        "Your lifetime access to Sound Modes Pro is active on this device. All advanced features are unlocked.",
      )
    val freePlanBannerTitle: String get() = tr("Plano Gratuito", "Free Plan")
    val freePlanBannerDesc: String
      get() = tr(
        "Aproveite todos os recursos básicos gratuitamente. Desbloqueie o Pro para cotas ilimitadas e personalização total.",
        "Enjoy all core features for free. Unlock Pro for unlimited quotas and complete personalization.",
      )
    val pendingBannerTitle: String get() = tr("Pagamento em Processamento", "Payment Processing")
    val pendingBannerDesc: String
      get() = tr(
        "Aguardando confirmação do pagamento pela Google Play Store. O Pro será ativado automaticamente assim que compensar.",
        "Waiting for payment confirmation from Google Play Store. Pro will activate automatically once approved.",
      )

    val benefitUnlimitedProfilesTitle: String get() = tr("Perfis Customizados Ilimitados", "Unlimited Custom Profiles")
    val benefitUnlimitedProfilesDesc: String get() = tr("Crie quantas cenas acústicas desejar além dos 5 do plano gratuito.", "Create as many custom scenes as you wish beyond the 5 on the free plan.")

    val benefitUnlimitedSchedulesTitle: String get() = tr("Horários Fixos Ilimitados", "Unlimited Schedules")
    val benefitUnlimitedSchedulesDesc: String get() = tr("Configure regras de tempo sem restrição de quantidade.", "Set up schedule rules with no quantity limits.")

    val benefitUnlimitedLocationsTitle: String get() = tr("Cercas e Wi-Fi Ilimitados", "Unlimited Geofences & Wi-Fi")
    val benefitUnlimitedLocationsDesc: String get() = tr("Automatize seus locais de trabalho, estudo e lazer sem limite.", "Automate work, study and home locations without limits.")

    val benefitUnlimitedCalendarTitle: String get() = tr("Automações de Agenda Ilimitadas", "Unlimited Calendar Automations")
    val benefitUnlimitedCalendarDesc: String get() = tr("Sincronize múltiplos calendários e palavras-chave de eventos.", "Sync multiple calendars and event keywords.")

    val benefitSlot3Title: String get() = tr("3º Bloco de Perfil Rápido", "3rd Quick Profile Tile")
    val benefitSlot3Desc: String get() = tr("Adicione um terceiro perfil favorito no header de notificações do Android.", "Add a third favorite profile to your Android notification header.")

    val benefitAutoRelockTitle: String get() = tr("Auto-Relock de Sessão Admin", "Admin Session Auto-Relock")
    val benefitAutoRelockDesc: String get() = tr("Bloqueio automático temporizado (5m, 15m, 30m) para segurança total.", "Timed automatic relock (5m, 15m, 30m) for maximum security.")

    val benefitPalettesTitle: String get() = tr("+2 Paletas Atmosféricas ZENITH", "+2 ZENITH Atmospheric Palettes")
    val benefitPalettesDesc: String get() = tr("Aurora Esmeralda e Oceano Abissal nos modos Dark e Light.", "Emerald Aurora and Abyssal Ocean in Dark and Light modes.")

    val benefitEthicalTitle: String get() = tr("100% Sem Anúncios e Sem Assinaturas", "100% Ad-Free & Subscription-Free")
    val benefitEthicalDesc: String get() = tr("Compra única vitalícia. Sem mensalidades, sem anúncios e zero rastreadores.", "One-time lifetime purchase. No subscriptions, no ads, zero trackers.")

    fun purchaseButton(price: String?): String =
      if (price != null) {
        tr("Desbloquear Modos de Som Pro • $price", "Unlock Sound Modes Pro • $price")
      } else {
        tr("Desbloquear Modos de Som Pro", "Unlock Sound Modes Pro")
      }

    val restorePurchasesButton: String get() = tr("Restaurar compras", "Restore purchases")
    val restorePurchasesHint: String
      get() = tr(
        "Já comprou em outro dispositivo ou reinstalou o app? Toque para restaurar seu acesso vitalício.",
        "Already purchased on another device or reinstalled? Tap to restore your lifetime access.",
      )

    val purchaseSuccessMessage: String
      get() = tr("Modos de Som Pro desbloqueado com sucesso! Aproveite todos os recursos vitalícios.", "Sound Modes Pro unlocked successfully! Enjoy all lifetime features.")
    val restoreSuccessMessage: String
      get() = tr("Compras restauradas com sucesso! Modos de Som Pro ativo.", "Purchases restored successfully! Sound Modes Pro active.")
    val noPurchaseFoundMessage: String
      get() = tr("Nenhuma compra ativa encontrada na sua conta Google Play.", "No active purchases found in your Google Play account.")
    val purchasePendingMessage: String
      get() = tr("Pagamento em processamento. Seu acesso Pro será liberado assim que confirmado.", "Payment processing. Your Pro access will be unlocked once confirmed.")
    val purchaseCancelledMessage: String
      get() = tr("Compra cancelada.", "Purchase cancelled.")
    val alreadyOwnedMessage: String
      get() = tr("Você já possui o Modos de Som Pro! Acesso ativado.", "You already own Sound Modes Pro! Access activated.")
    fun purchaseErrorMessage(msg: String): String =
      tr("Falha no faturamento: $msg", "Billing failed: $msg")

    val upsellProfilesQuota: String
      get() = tr(
        "Cota gratuita atingida (máximo de 5 perfis customizados). Desbloqueie o Pro para criar perfis ilimitados!",
        "Free quota reached (maximum 5 custom profiles). Unlock Pro to create unlimited profiles!",
      )
    val upsellSchedulesQuota: String
      get() = tr(
        "Cota gratuita atingida (máximo de 3 horários). Desbloqueie o Pro para horários ilimitados!",
        "Free quota reached (maximum 3 schedules). Unlock Pro for unlimited schedules!",
      )
    val upsellLocationsQuota: String
      get() = tr(
        "Cota gratuita atingida (máximo de 3 locais). Desbloqueie o Pro para locais ilimitados!",
        "Free quota reached (maximum 3 locations). Unlock Pro for unlimited locations!",
      )
    val upsellCalendarQuota: String
      get() = tr(
        "Cota gratuita atingida (máximo de 1 regra de agenda). Desbloqueie o Pro para regras de agenda ilimitadas!",
        "Free quota reached (maximum 1 calendar rule). Unlock Pro for unlimited calendar rules!",
      )
    val upsellPalettesFeature: String
      get() = tr(
        "As paletas Aurora Esmeralda e Oceano Abissal são exclusivas do Modos de Som Pro.",
        "Emerald Aurora and Abyssal Ocean palettes are exclusive to Sound Modes Pro.",
      )
    val upsellSlot3Feature: String
      get() = tr(
        "O 3º Bloco de Perfil Rápido é exclusivo do Modos de Som Pro.",
        "The 3rd Quick Profile Tile is exclusive to Sound Modes Pro.",
      )
    val upsellAutoRelockFeature: String
      get() = tr(
        "O Auto-Relock de Sessão Admin é exclusivo do Modos de Som Pro.",
        "Admin Session Auto-Relock is exclusive to Sound Modes Pro.",
      )

    fun paletteName(palette: ZenithAtmosphericPalette): String =
      when (palette) {
        ZenithAtmosphericPalette.BOREAL_CLASSIC -> tr("Noite Boreal (Padrão)", "Boreal Classic (Default)")
        ZenithAtmosphericPalette.EMERALD_AURORA -> tr("Aurora Esmeralda (Pro)", "Emerald Aurora (Pro)")
        ZenithAtmosphericPalette.ABYSSAL_OCEAN -> tr("Oceano Abissal (Pro)", "Abyssal Ocean (Pro)")
      }

    fun autoRelockOption(minutes: Int?): String =
      when (minutes) {
        null, 0 -> tr("Desativado", "Disabled")
        5 -> tr("5 minutos", "5 minutes")
        15 -> tr("15 minutos", "15 minutes")
        30 -> tr("30 minutos", "30 minutes")
        else -> tr("$minutes minutos", "$minutes minutes")
      }
  }
}

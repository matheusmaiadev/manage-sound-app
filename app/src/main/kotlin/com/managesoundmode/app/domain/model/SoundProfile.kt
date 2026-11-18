package com.managesoundmode.app.domain.model

import com.managesoundmode.app.ui.common.AppLabels
import kotlinx.serialization.Serializable

@Serializable
data class SoundProfile(
  val id: String,
  val name: String,
  val description: String,
  val mode: SoundMode = SoundMode.NORMAL,
  val streams: VolumeStreams,
  val isPreset: Boolean = true,
  val iconKey: String = "tune",
  val preserveMediaIfPlaying: Boolean = true,
  val skipMediaOnBluetooth: Boolean = true,
  val audioMode: BaseAudioMode? = null,
  val vibrateEnabled: Boolean? = null,
  val overrideSystemPercent: Int? = null,
  val overrideVoiceCallPercent: Int? = null,
  val overrideAccessibilityPercent: Int? = null,
) {
  init {
    require(overrideSystemPercent == null || overrideSystemPercent in 0..100) {
      "overrideSystemPercent must be null or between 0 and 100"
    }
    require(overrideVoiceCallPercent == null || overrideVoiceCallPercent in 0..100) {
      "overrideVoiceCallPercent must be null or between 0 and 100"
    }
    require(overrideAccessibilityPercent == null || overrideAccessibilityPercent in 0..100) {
      "overrideAccessibilityPercent must be null or between 0 and 100"
    }
  }

  val hasSecondaryOverrides: Boolean
    get() = overrideSystemPercent != null ||
      overrideVoiceCallPercent != null ||
      overrideAccessibilityPercent != null

  val resolvedAudioMode: BaseAudioMode
    get() = audioMode ?: SoundState.fromLegacyMode(mode).audioMode

  val resolvedVibrateEnabled: Boolean
    get() = vibrateEnabled ?: SoundState.fromLegacyMode(mode).vibrateEnabled

  val resolvedSoundState: SoundState
    get() = SoundState(
      audioMode = resolvedAudioMode,
      vibrateEnabled = resolvedVibrateEnabled,
    )

  /**
   * Normaliza o perfil para o schema atual populando explicitamente [audioMode], [vibrateEnabled]
   * e alinhando [mode] à aproximação legada correspondente.
   */
  fun normalized(): SoundProfile {
    val state = resolvedSoundState
    return copy(
      mode = state.toLegacyApproximation(),
      audioMode = state.audioMode,
      vibrateEnabled = state.vibrateEnabled,
      overrideSystemPercent = overrideSystemPercent?.coerceIn(0, 100),
      overrideVoiceCallPercent = overrideVoiceCallPercent?.coerceIn(0, 100),
      overrideAccessibilityPercent = overrideAccessibilityPercent?.coerceIn(0, 100),
    )
  }

  fun summaryLabel(): String = AppLabels.Domain.profileSummaryLabel(resolvedSoundState, streams)

  companion object {
    const val DEFAULT_DAY_PROFILE_ID = "preset_default_day"

    /**
     * Combina perfis predefinidos e customizados priorizando edições salvas em [savedProfiles]
     * sobre [defaultPresets], garantindo consistência em UI, Repositório e Barra Rápida.
     */
    fun mergeProfiles(
      savedProfiles: List<SoundProfile>,
      deletedIds: Set<String> = emptySet(),
    ): List<SoundProfile> {
      val savedById = savedProfiles.map { it.normalized() }.associateBy { it.id }
      val presetIds = defaultPresets.map { it.id }.toSet()
      val resolvedPresets = defaultPresets.map { preset ->
        savedById[preset.id]?.copy(isPreset = true) ?: preset
      }
      val extraCustom = savedProfiles.map { it.normalized() }.filterNot { it.id in presetIds }
      val merged = (resolvedPresets + extraCustom).filterNot { it.id in deletedIds }
      return merged.ifEmpty { listOf(defaultPresets.first()) }
    }

    val defaultPresets: List<SoundProfile> = listOf(
      SoundProfile(
        id = DEFAULT_DAY_PROFILE_ID,
        name = "Padrão (Dia)",
        description = "Equilibrado para o dia a dia",
        mode = SoundMode.NORMAL,
        audioMode = BaseAudioMode.SOUND,
        vibrateEnabled = true,
        iconKey = "sunny",
        streams = VolumeStreams(
          ringPercent = 80,
          notificationPercent = 75,
          mediaPercent = 50,
          alarmPercent = 90,
        ),
      ),
      SoundProfile(
        id = "preset_work_high",
        name = "Trabalho",
        description = "Toque alto e mídia zerada",
        mode = SoundMode.NORMAL,
        audioMode = BaseAudioMode.SOUND,
        vibrateEnabled = true,
        iconKey = "work",
        streams = VolumeStreams(
          ringPercent = 100,
          notificationPercent = 45,
          mediaPercent = 0,
          alarmPercent = 90,
        ),
      ),
      SoundProfile(
        id = "preset_work",
        name = "Reunião",
        description = "Apenas vibração discreta",
        mode = SoundMode.VIBRATE,
        audioMode = BaseAudioMode.SILENT,
        vibrateEnabled = true,
        iconKey = "vibration",
        streams = VolumeStreams(
          ringPercent = 0,
          notificationPercent = 0,
          mediaPercent = 0,
          alarmPercent = 85,
        ),
      ),
      SoundProfile(
        id = "preset_sleep",
        name = "Dormir",
        description = "Silêncio com alarme em 100%",
        mode = SoundMode.SILENT,
        audioMode = BaseAudioMode.SILENT,
        vibrateEnabled = false,
        iconKey = "bedtime",
        streams = VolumeStreams(
          ringPercent = 0,
          notificationPercent = 0,
          mediaPercent = 0,
          alarmPercent = 100,
        ),
      ),
      SoundProfile(
        id = "preset_home",
        name = "Casa",
        description = "Som ativo para chamadas e mídia",
        mode = SoundMode.NORMAL,
        audioMode = BaseAudioMode.SOUND,
        vibrateEnabled = true,
        iconKey = "home",
        streams = VolumeStreams(
          ringPercent = 85,
          notificationPercent = 80,
          mediaPercent = 60,
          alarmPercent = 90,
        ),
      ),
      SoundProfile(
        id = "preset_meeting",
        name = "Foco Total",
        description = "Zero interrupções sonoras",
        mode = SoundMode.SILENT,
        audioMode = BaseAudioMode.SILENT,
        vibrateEnabled = false,
        iconKey = "shield",
        streams = VolumeStreams(
          ringPercent = 0,
          notificationPercent = 0,
          mediaPercent = 0,
          alarmPercent = 75,
        ),
      ),
    )
  }
}

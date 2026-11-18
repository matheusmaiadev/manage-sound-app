package com.managesoundmode.app.domain.model

import com.managesoundmode.app.ui.common.AppLabels
import kotlinx.serialization.Serializable

/**
 * Eixo sonoro primário da Matriz Acústica Ortogonal 2×2 (SPEC-004).
 */
@Serializable
enum class BaseAudioMode {
  SOUND,
  SILENT,
  ;

  val title: String
    get() = AppLabels.Domain.baseAudioModeTitle(this)

  val pillLabel: String
    get() = AppLabels.Now.audioModeShortPill(this)
}

/**
 * Estado acústico completo combinando o eixo sonoro ([audioMode]) e o eixo tátil ([vibrateEnabled]).
 *
 * Matriz determinística de precedência ([restrictivenessRank]):
 * 1. `SOUND + Vibrar ON` (menos restritivo)
 * 2. `SOUND + Vibrar OFF`
 * 3. `SILENT + Vibrar ON`
 * 4. `SILENT + Vibrar OFF` (mais restritivo)
 */
@Serializable
data class SoundState(
  val audioMode: BaseAudioMode,
  val vibrateEnabled: Boolean,
) {
  val restrictivenessRank: Int
    get() = when (audioMode) {
      BaseAudioMode.SOUND -> if (vibrateEnabled) 1 else 2
      BaseAudioMode.SILENT -> if (vibrateEnabled) 3 else 4
    }

  val title: String
    get() = AppLabels.Domain.soundStateTitle(this)

  val subtitle: String
    get() = AppLabels.Domain.soundStateSubtitle(this)

  fun toLegacyApproximation(): SoundMode = when (audioMode) {
    BaseAudioMode.SOUND -> SoundMode.NORMAL
    BaseAudioMode.SILENT -> if (vibrateEnabled) SoundMode.VIBRATE else SoundMode.SILENT
  }

  companion object {
    val DEFAULT = SoundState(
      audioMode = BaseAudioMode.SOUND,
      vibrateEnabled = true,
    )

    fun fromLegacyMode(mode: SoundMode): SoundState = when (mode) {
      SoundMode.NORMAL -> SoundState(audioMode = BaseAudioMode.SOUND, vibrateEnabled = true)
      SoundMode.VIBRATE -> SoundState(audioMode = BaseAudioMode.SILENT, vibrateEnabled = true)
      SoundMode.SILENT -> SoundState(audioMode = BaseAudioMode.SILENT, vibrateEnabled = false)
    }
  }
}

/**
 * Enumeração legada mantida para 100% de retrocompatibilidade na desserialização JSON (v1 -> v2).
 */
@Serializable
enum class SoundMode(
  val restrictivenessRank: Int,
) {
  NORMAL(restrictivenessRank = 1),
  VIBRATE(restrictivenessRank = 2),
  SILENT(restrictivenessRank = 3),
  ;

  val title: String
    get() = AppLabels.Domain.soundModeTitle(this)

  val subtitle: String
    get() = AppLabels.Domain.soundModeSubtitle(this)
}

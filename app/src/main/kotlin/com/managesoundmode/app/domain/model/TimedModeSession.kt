package com.managesoundmode.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TimedModeSession(
  val targetMode: SoundMode,
  val previousMode: SoundMode,
  val previousStreams: VolumeStreams,
  val durationMinutes: Int,
  val startedAtEpochMillis: Long,
  val endsAtEpochMillis: Long,
  val audioMode: BaseAudioMode? = null,
  val vibrateEnabled: Boolean? = null,
  val previousAudioMode: BaseAudioMode? = null,
  val previousVibrateEnabled: Boolean? = null,
) {
  val resolvedAudioMode: BaseAudioMode
    get() = audioMode ?: SoundState.fromLegacyMode(targetMode).audioMode

  val resolvedVibrateEnabled: Boolean
    get() = vibrateEnabled ?: SoundState.fromLegacyMode(targetMode).vibrateEnabled

  val resolvedSoundState: SoundState
    get() = SoundState(
      audioMode = resolvedAudioMode,
      vibrateEnabled = resolvedVibrateEnabled,
    )

  val resolvedPreviousAudioMode: BaseAudioMode
    get() = previousAudioMode ?: SoundState.fromLegacyMode(previousMode).audioMode

  val resolvedPreviousVibrateEnabled: Boolean
    get() = previousVibrateEnabled ?: SoundState.fromLegacyMode(previousMode).vibrateEnabled

  val resolvedPreviousSoundState: SoundState
    get() = SoundState(
      audioMode = resolvedPreviousAudioMode,
      vibrateEnabled = resolvedPreviousVibrateEnabled,
    )

  fun remainingMinutes(nowEpochMillis: Long): Int {
    val diffMillis = (endsAtEpochMillis - nowEpochMillis).coerceAtLeast(0L)
    return ((diffMillis + 59_999L) / 60_000L).toInt()
  }
}

data class DeviceSoundSnapshot(
  val currentMode: SoundMode,
  val streams: VolumeStreams,
  val hasDndPermission: Boolean,
  val currentSoundState: SoundState = SoundState.fromLegacyMode(currentMode),
  val canWriteSystemSettings: Boolean = true,
  val canScheduleExactAlarms: Boolean = true,
  val hasLocationPermission: Boolean = false,
  val hasBackgroundLocationPermission: Boolean = false,
  val hasNotificationPermission: Boolean = true,
  val isMusicActive: Boolean = false,
  val isBluetoothHeadsetConnected: Boolean = false,
  val activeProfileId: String? = null,
  val activeTimer: TimedModeSession? = null,
  val activeRuleTitle: String? = null,
  val activeRuleSubtitle: String? = null,
  val activeRuleSessionKey: String? = null,
  val isManuallyOverridden: Boolean = false,
) {
  val audioMode: BaseAudioMode
    get() = currentSoundState.audioMode

  val vibrateEnabled: Boolean
    get() = currentSoundState.vibrateEnabled
}

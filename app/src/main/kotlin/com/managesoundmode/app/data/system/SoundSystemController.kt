package com.managesoundmode.app.data.system

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.managesoundmode.app.domain.model.BaseAudioMode
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundState
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class DndPermissionRequiredException :
  IllegalStateException("Permissão 'Não Perturbe' (DND) necessária para alterar para o modo Silencioso.")

interface SoundSystemController {
  val systemChanges: Flow<Unit>
  val lastBroadcastWasExternal: Boolean
  fun clearExternalBroadcastFlag()
  fun readCurrentMode(): SoundMode
  fun readSoundState(): SoundState = SoundState.fromLegacyMode(readCurrentMode())
  fun readVolumeStreams(): VolumeStreams
  fun readStreamMinPercent(type: VolumeStreamType): Int = 0
  fun readStreamMinPercents(): Map<VolumeStreamType, Int> =
    VolumeStreamType.entries.associateWith { readStreamMinPercent(it) }
  fun hasDndPermission(): Boolean
  fun canWriteSystemSettings(): Boolean = true
  fun canScheduleExactAlarms(): Boolean = true
  fun hasLocationPermission(): Boolean = false
  fun hasBackgroundLocationPermission(): Boolean = false
  fun hasNotificationPermission(): Boolean = true
  fun hasReadCalendarPermission(): Boolean = false
  fun isMusicActive(): Boolean = false
  fun isBluetoothHeadsetConnected(): Boolean = false
  fun syncFallbackSoundVibrate(enabled: Boolean) {}
  fun applySoundMode(mode: SoundMode): Result<SoundMode>
  fun applySoundState(state: SoundState): Result<SoundState> =
    applySoundMode(state.toLegacyApproximation()).map { state }
  fun applyStreamVolume(type: VolumeStreamType, percent: Int): Result<VolumeStreams>
  fun applyAllStreams(streams: VolumeStreams): Result<VolumeStreams>
}

class AndroidSoundSystemController(
  private val appContext: Context,
) : SoundSystemController {

  private val audioManager: AudioManager? =
    appContext.getSystemService(AudioManager::class.java)

  private val notificationManager: NotificationManager? =
    appContext.getSystemService(NotificationManager::class.java)

  private val alarmManager: AlarmManager? =
    appContext.getSystemService(AlarmManager::class.java)

  private val lastAppChangeElapsedRealtime = AtomicLong(0L)
  private val externalChangeDetected = AtomicBoolean(false)
  private val fallbackSoundVibrate = AtomicBoolean(readVibrateWhenRingingSetting() ?: true)
  private val lastSystemVibrateWriteSucceeded = AtomicBoolean(false)

  override val lastBroadcastWasExternal: Boolean
    get() = externalChangeDetected.get()

  override fun clearExternalBroadcastFlag() {
    externalChangeDetected.set(false)
  }

  override val systemChanges: Flow<Unit> = callbackFlow {
    trySend(Unit)
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        val elapsedSinceAppWrite = SystemClock.elapsedRealtime() - lastAppChangeElapsedRealtime.get()
        if (lastAppChangeElapsedRealtime.get() > 0L && elapsedSinceAppWrite <= INTERNAL_CHANGE_WINDOW_MS) {
          externalChangeDetected.set(false)
        } else if (intent?.action != null) {
          externalChangeDetected.set(true)
        }
        trySend(Unit)
      }
    }
    val vibrateObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean) {
        val elapsedSinceAppWrite = SystemClock.elapsedRealtime() - lastAppChangeElapsedRealtime.get()
        if (lastAppChangeElapsedRealtime.get() <= 0L || elapsedSinceAppWrite > INTERNAL_CHANGE_WINDOW_MS) {
          readVibrateWhenRingingSetting()?.let { fallbackSoundVibrate.set(it) }
        }
        trySend(Unit)
      }
    }
    val filter = IntentFilter().apply {
      addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
      addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
      addAction(VOLUME_CHANGED_ACTION)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        addAction(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
      }
    }
    ContextCompat.registerReceiver(
      appContext,
      receiver,
      filter,
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    runCatching {
      appContext.contentResolver.registerContentObserver(
        Settings.System.getUriFor(VIBRATE_WHEN_RINGING_KEY),
        false,
        vibrateObserver,
      )
    }.onFailure { Log.w(TAG, "Falha ao registrar ContentObserver de vibrate_when_ringing", it) }

    awaitClose {
      runCatching { appContext.unregisterReceiver(receiver) }
        .onFailure { Log.w(TAG, "Falha ao desregistrar receiver de áudio", it) }
      runCatching { appContext.contentResolver.unregisterContentObserver(vibrateObserver) }
        .onFailure { Log.w(TAG, "Falha ao desregistrar ContentObserver de vibração", it) }
    }
  }

  override fun readCurrentMode(): SoundMode {
    val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
    return when (ringerMode) {
      AudioManager.RINGER_MODE_SILENT -> SoundMode.SILENT
      AudioManager.RINGER_MODE_VIBRATE -> SoundMode.VIBRATE
      else -> SoundMode.NORMAL
    }
  }

  override fun readSoundState(): SoundState {
    val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
    return when (ringerMode) {
      AudioManager.RINGER_MODE_SILENT ->
        SoundState(audioMode = BaseAudioMode.SILENT, vibrateEnabled = false)
      AudioManager.RINGER_MODE_VIBRATE ->
        SoundState(audioMode = BaseAudioMode.SILENT, vibrateEnabled = true)
      else -> {
        val vibrateInSound = if (canWriteSystemSettings() && lastSystemVibrateWriteSucceeded.get()) {
          readVibrateWhenRingingSetting() ?: fallbackSoundVibrate.get()
        } else {
          fallbackSoundVibrate.get()
        }
        SoundState(audioMode = BaseAudioMode.SOUND, vibrateEnabled = vibrateInSound)
      }
    }
  }

  override fun readVolumeStreams(): VolumeStreams {
    val manager = audioManager ?: return VolumeStreams()
    return VolumeStreams(
      ringPercent = readStreamPercent(manager, AudioManager.STREAM_RING),
      notificationPercent = readStreamPercent(manager, AudioManager.STREAM_NOTIFICATION),
      mediaPercent = readStreamPercent(manager, AudioManager.STREAM_MUSIC),
      alarmPercent = readStreamPercent(manager, AudioManager.STREAM_ALARM),
      systemPercent = readStreamPercent(manager, AudioManager.STREAM_SYSTEM),
      voiceCallPercent = readStreamPercent(manager, AudioManager.STREAM_VOICE_CALL),
      accessibilityPercent = readStreamPercent(manager, AudioManager.STREAM_ACCESSIBILITY),
    )
  }

  override fun readStreamMinPercent(type: VolumeStreamType): Int {
    val manager = audioManager ?: return 0
    val streamConstant = type.toAndroidStream()
    val max = manager.getStreamMaxVolume(streamConstant).coerceAtLeast(1)
    val min = readStreamMinIndex(manager, streamConstant, max)
    return ((min.toFloat() / max.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
  }

  override fun hasDndPermission(): Boolean {
    return notificationManager?.isNotificationPolicyAccessGranted ?: false
  }

  override fun canWriteSystemSettings(): Boolean {
    return runCatching { Settings.System.canWrite(appContext) }
      .onFailure { Log.w(TAG, "Falha ao consultar Settings.System.canWrite", it) }
      .getOrDefault(false)
  }

  override fun canScheduleExactAlarms(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      alarmManager?.canScheduleExactAlarms() ?: true
    } else {
      true
    }
  }

  override fun hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(
      appContext,
      Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
      appContext,
      Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  override fun hasBackgroundLocationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      hasLocationPermission()
    }
  }

  override fun hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  override fun hasReadCalendarPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      appContext,
      Manifest.permission.READ_CALENDAR,
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun isMusicActive(): Boolean {
    return audioManager?.isMusicActive ?: false
  }

  override fun isBluetoothHeadsetConnected(): Boolean {
    val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: return false
    return devices.any { device ->
      device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
          (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
    }
  }

  override fun syncFallbackSoundVibrate(enabled: Boolean) {
    fallbackSoundVibrate.set(enabled)
    if (canWriteSystemSettings() && readCurrentMode() == SoundMode.NORMAL) {
      writeVibrateWhenRingingSafely(enabled)
    }
  }

  override fun applySoundMode(mode: SoundMode): Result<SoundMode> =
    applySoundState(SoundState.fromLegacyMode(mode)).map { it.toLegacyApproximation() }

  override fun applySoundState(state: SoundState): Result<SoundState> = runCatching {
    val manager = requireNotNull(audioManager) { "AudioManager indisponível no dispositivo" }
    val targetNeedsSilentDnd = state.audioMode == BaseAudioMode.SILENT && !state.vibrateEnabled
    val currentlySilentDnd = manager.ringerMode == AudioManager.RINGER_MODE_SILENT
    if ((targetNeedsSilentDnd || currentlySilentDnd) && !hasDndPermission()) {
      throw DndPermissionRequiredException()
    }

    markInternalWrite()
    when (state.audioMode) {
      BaseAudioMode.SOUND -> {
        fallbackSoundVibrate.set(state.vibrateEnabled)
        if (manager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
          manager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
        if (canWriteSystemSettings()) {
          writeVibrateWhenRingingSafely(state.vibrateEnabled)
        } else {
          lastSystemVibrateWriteSucceeded.set(false)
        }
      }

      BaseAudioMode.SILENT -> {
        val targetRinger = if (state.vibrateEnabled) {
          AudioManager.RINGER_MODE_VIBRATE
        } else {
          AudioManager.RINGER_MODE_SILENT
        }
        manager.ringerMode = targetRinger
      }
    }
    readSoundState()
  }.onFailure { error ->
    Log.e(TAG, "Erro ao aplicar estado de som 2x2: $state", error)
  }

  override fun applyStreamVolume(type: VolumeStreamType, percent: Int): Result<VolumeStreams> = runCatching {
    val manager = requireNotNull(audioManager) { "AudioManager indisponível no dispositivo" }
    markInternalWrite()
    val streamConstant = type.toAndroidStream()
    setStreamPercent(manager, streamConstant, percent)
    readVolumeStreams()
  }.onFailure { error ->
    Log.e(TAG, "Erro ao ajustar canal $type para $percent%", error)
  }

  override fun applyAllStreams(streams: VolumeStreams): Result<VolumeStreams> = runCatching {
    val manager = requireNotNull(audioManager) { "AudioManager indisponível no dispositivo" }
    markInternalWrite()
    setStreamPercent(manager, AudioManager.STREAM_RING, streams.ringPercent)
    setStreamPercent(manager, AudioManager.STREAM_NOTIFICATION, streams.notificationPercent)
    setStreamPercent(manager, AudioManager.STREAM_MUSIC, streams.mediaPercent)
    setStreamPercent(manager, AudioManager.STREAM_ALARM, streams.alarmPercent)
    setStreamPercent(manager, AudioManager.STREAM_SYSTEM, streams.systemPercent)
    setStreamPercent(manager, AudioManager.STREAM_VOICE_CALL, streams.voiceCallPercent)
    setStreamPercent(manager, AudioManager.STREAM_ACCESSIBILITY, streams.accessibilityPercent)
    readVolumeStreams()
  }.onFailure { error ->
    Log.e(TAG, "Erro ao aplicar múltiplos canais de volume", error)
  }

  private fun readVibrateWhenRingingSetting(): Boolean? =
    runCatching {
      Settings.System.getInt(appContext.contentResolver, VIBRATE_WHEN_RINGING_KEY) != 0
    }.onFailure { error ->
      Log.d(TAG, "vibrate_when_ringing não encontrado em Settings.System; usando fallback", error)
    }.getOrNull()

  private fun writeVibrateWhenRingingSafely(enabled: Boolean) {
    runCatching {
      val wrote = Settings.System.putInt(
        appContext.contentResolver,
        VIBRATE_WHEN_RINGING_KEY,
        if (enabled) 1 else 0,
      )
      val verified = wrote && (readVibrateWhenRingingSetting() == enabled)
      lastSystemVibrateWriteSucceeded.set(verified)
    }.onFailure { error ->
      lastSystemVibrateWriteSucceeded.set(false)
      Log.w(TAG, "Falha ao atualizar vibrate_when_ringing no Settings.System; mantendo fallback local", error)
    }
  }

  private fun markInternalWrite() {
    lastAppChangeElapsedRealtime.set(SystemClock.elapsedRealtime())
    externalChangeDetected.set(false)
  }

  private fun readStreamMinIndex(manager: AudioManager, streamType: Int, max: Int): Int {
    return runCatching { manager.getStreamMinVolume(streamType) }
      .getOrDefault(0)
      .coerceIn(0, max)
  }

  private fun readStreamPercent(manager: AudioManager, streamType: Int): Int {
    val max = manager.getStreamMaxVolume(streamType).coerceAtLeast(1)
    val current = manager.getStreamVolume(streamType).coerceIn(0, max)
    return ((current.toFloat() / max.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
  }

  private fun setStreamPercent(manager: AudioManager, streamType: Int, percent: Int) {
    val max = manager.getStreamMaxVolume(streamType).coerceAtLeast(1)
    val min = readStreamMinIndex(manager, streamType, max)
    val minPercent = ((min.toFloat() / max.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
    val clampedPercent = percent.coerceIn(minPercent, 100)
    val currentIndex = manager.getStreamVolume(streamType).coerceIn(min, max)
    val currentPercent = ((currentIndex.toFloat() / max.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
    var targetIndex = ((clampedPercent / 100f) * max).roundToInt().coerceIn(min, max)
    if (clampedPercent > currentPercent && targetIndex <= currentIndex) {
      targetIndex = (currentIndex + 1).coerceAtMost(max)
    } else if (clampedPercent < currentPercent && targetIndex >= currentIndex) {
      targetIndex = (currentIndex - 1).coerceAtLeast(min)
    }
    manager.setStreamVolume(streamType, targetIndex, 0)
  }

  private fun VolumeStreamType.toAndroidStream(): Int = when (this) {
    VolumeStreamType.RING -> AudioManager.STREAM_RING
    VolumeStreamType.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
    VolumeStreamType.MEDIA -> AudioManager.STREAM_MUSIC
    VolumeStreamType.ALARM -> AudioManager.STREAM_ALARM
    VolumeStreamType.SYSTEM -> AudioManager.STREAM_SYSTEM
    VolumeStreamType.VOICE_CALL -> AudioManager.STREAM_VOICE_CALL
    VolumeStreamType.ACCESSIBILITY -> AudioManager.STREAM_ACCESSIBILITY
  }

  private companion object {
    const val TAG = "AndroidSoundController"
    const val INTERNAL_CHANGE_WINDOW_MS = 400L
    const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    const val VIBRATE_WHEN_RINGING_KEY = "vibrate_when_ringing"
  }
}

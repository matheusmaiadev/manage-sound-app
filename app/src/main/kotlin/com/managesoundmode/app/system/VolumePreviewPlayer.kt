package com.managesoundmode.app.system

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.provider.Settings
import android.util.Log
import com.managesoundmode.app.domain.model.VolumeStreamType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class VolumePreviewKind {
  ONE_SHOT,
  HOLD_LOOP,
}

enum class VolumePreviewScope {
  NOW_TAB,
  PROFILE_EDITOR,
}

data class ActiveVolumePreview(
  val streamType: VolumeStreamType,
  val percent: Int,
  val kind: VolumePreviewKind,
  val scope: VolumePreviewScope = VolumePreviewScope.NOW_TAB,
)

/**
 * Motor de preview acústico sob comando (SPEC-005 / TASK-22) compatível com os 7 canais Android.
 */
interface VolumePreviewPlayer {
  val previewState: StateFlow<ActiveVolumePreview?>

  fun playSingle(
    streamType: VolumeStreamType,
    percent: Int,
    useSystemSounds: Boolean = true,
    scope: VolumePreviewScope = VolumePreviewScope.NOW_TAB,
    onStopped: (() -> Unit)? = null,
  )

  fun toggleSingle(
    streamType: VolumeStreamType,
    percent: Int,
    useSystemSounds: Boolean = true,
    scope: VolumePreviewScope = VolumePreviewScope.NOW_TAB,
    onStopped: (() -> Unit)? = null,
  )

  fun startLoop(
    streamType: VolumeStreamType,
    percent: Int,
    useSystemSounds: Boolean = true,
    scope: VolumePreviewScope = VolumePreviewScope.NOW_TAB,
    onStopped: (() -> Unit)? = null,
  )

  fun updateLivePercent(percent: Int)

  fun stop()
}

/**
 * Implementação Android nativa (API 29–36, zero permissões extras):
 * - Usa [RingtoneManager] (`TYPE_RINGTONE`, `TYPE_NOTIFICATION`, `TYPE_ALARM`) para os sons reais
 *   cadastrados pelo usuário no sistema, com `isLooping = true` no modo Hold-Loop.
 * - Usa síntese harmônica suave via [AudioTrack] (`MODE_STATIC`) roteada para o respectivo
 *   [AudioAttributes] em `MEDIA`, `SYSTEM`, `VOICE_CALL`, `ACCESSIBILITY` e como fallback automático
 *   quando o aparelho não possui URI padrão ou o usuário desativa sons do sistema.
 */
class AndroidVolumePreviewPlayer(
  private val appContext: Context,
) : VolumePreviewPlayer {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val lock = Any()

  private val _previewState = MutableStateFlow<ActiveVolumePreview?>(null)
  override val previewState: StateFlow<ActiveVolumePreview?> = _previewState.asStateFlow()

  private var activeRingtone: Ringtone? = null
  private var activeAudioTrack: AudioTrack? = null
  private var autoStopJob: Job? = null
  private var activeStopCallback: (() -> Unit)? = null

  override fun toggleSingle(
    streamType: VolumeStreamType,
    percent: Int,
    useSystemSounds: Boolean,
    scope: VolumePreviewScope,
    onStopped: (() -> Unit)?,
  ) {
    val current = _previewState.value
    if (current != null && current.streamType == streamType && current.scope == scope) {
      stop()
    } else {
      playSingle(
        streamType = streamType,
        percent = percent,
        useSystemSounds = useSystemSounds,
        scope = scope,
        onStopped = onStopped,
      )
    }
  }

  override fun playSingle(
    streamType: VolumeStreamType,
    percent: Int,
    useSystemSounds: Boolean,
    scope: VolumePreviewScope,
    onStopped: (() -> Unit)?,
  ) {
    startPlaybackInternal(
      streamType = streamType,
      percent = percent,
      kind = VolumePreviewKind.ONE_SHOT,
      useSystemSounds = useSystemSounds,
      previewScope = scope,
      onStopped = onStopped,
    )
  }

  override fun startLoop(
    streamType: VolumeStreamType,
    percent: Int,
    useSystemSounds: Boolean,
    scope: VolumePreviewScope,
    onStopped: (() -> Unit)?,
  ) {
    startPlaybackInternal(
      streamType = streamType,
      percent = percent,
      kind = VolumePreviewKind.HOLD_LOOP,
      useSystemSounds = useSystemSounds,
      previewScope = scope,
      onStopped = onStopped,
    )
  }

  override fun updateLivePercent(percent: Int) {
    val clamped = percent.coerceIn(0, 100)
    val gain = if (clamped <= 0) 0f else 1f
    synchronized(lock) {
      val current = _previewState.value ?: return
      _previewState.value = current.copy(percent = clamped)
      runCatching { activeRingtone?.volume = gain }
        .onFailure { Log.w(TAG, "Falha ao atualizar ganho do Ringtone", it) }
      runCatching { activeAudioTrack?.setVolume(gain) }
        .onFailure { Log.w(TAG, "Falha ao atualizar ganho do AudioTrack", it) }
    }
  }

  override fun stop() {
    var callbackToInvoke: (() -> Unit)? = null
    synchronized(lock) {
      autoStopJob?.cancel()
      autoStopJob = null
      releaseActivePlayersLocked()
      _previewState.value = null
      callbackToInvoke = activeStopCallback
      activeStopCallback = null
    }
    callbackToInvoke?.let { callback ->
      runCatching { callback() }
        .onFailure { Log.w(TAG, "Erro ao executar callback pós-preview", it) }
    }
  }

  private fun startPlaybackInternal(
    streamType: VolumeStreamType,
    percent: Int,
    kind: VolumePreviewKind,
    useSystemSounds: Boolean,
    previewScope: VolumePreviewScope,
    onStopped: (() -> Unit)?,
  ) {
    val clampedPercent = percent.coerceIn(0, 100)
    val isLoop = kind == VolumePreviewKind.HOLD_LOOP
    val gain = if (clampedPercent <= 0) 0f else 1f

    var previousStopCallback: (() -> Unit)? = null
    synchronized(lock) {
      autoStopJob?.cancel()
      autoStopJob = null
      releaseActivePlayersLocked()
      previousStopCallback = activeStopCallback
      activeStopCallback = null
    }
    // Se havia outro preview ativo, notifica encerramento anterior antes de iniciar o novo
    previousStopCallback?.let { prev ->
      runCatching { prev() }
        .onFailure { Log.w(TAG, "Erro ao encerrar callback anterior de preview", it) }
    }

    val audioAttributes = streamType.toPreviewAudioAttributes()
    var startedWithRingtone = false

    synchronized(lock) {
      activeStopCallback = onStopped
      _previewState.value = ActiveVolumePreview(
        streamType = streamType,
        percent = clampedPercent,
        kind = kind,
        scope = previewScope,
      )

      if (useSystemSounds && streamType.supportsSystemRingtoneUri()) {
        startedWithRingtone = tryStartSystemRingtoneLocked(
          streamType = streamType,
          audioAttributes = audioAttributes,
          isLoop = isLoop,
          gain = gain,
        )
      }

      if (!startedWithRingtone) {
        tryStartSynthesizedTrackLocked(
          streamType = streamType,
          audioAttributes = audioAttributes,
          isLoop = isLoop,
          gain = gain,
        )
      }

      if (kind == VolumePreviewKind.ONE_SHOT) {
        val expectedPreview = _previewState.value
        autoStopJob = this.scope.launch {
          val pollStepMs = 150L
          var elapsedMs = 0L
          val maxDurationMs = if (startedWithRingtone) ONE_SHOT_MAX_DURATION_MS else SYNTH_ONE_SHOT_DURATION_MS
          while (elapsedMs < maxDurationMs) {
            delay(pollStepMs)
            elapsedMs += pollStepMs
            val finishedEarly = synchronized(lock) {
              val ringtone = activeRingtone
              ringtone != null && elapsedMs >= 450L && !runCatching { ringtone.isPlaying }.getOrDefault(false)
            }
            if (finishedEarly) break
          }
          val shouldStop = synchronized(lock) { _previewState.value == expectedPreview }
          if (shouldStop) {
            stop()
          }
        }
      }
    }
  }

  private fun tryStartSystemRingtoneLocked(
    streamType: VolumeStreamType,
    audioAttributes: AudioAttributes,
    isLoop: Boolean,
    gain: Float,
  ): Boolean {
    val (ringtoneType, fallbackUri) = when (streamType) {
      VolumeStreamType.RING -> RingtoneManager.TYPE_RINGTONE to Settings.System.DEFAULT_RINGTONE_URI
      VolumeStreamType.NOTIFICATION ->
        RingtoneManager.TYPE_NOTIFICATION to Settings.System.DEFAULT_NOTIFICATION_URI
      VolumeStreamType.ALARM -> RingtoneManager.TYPE_ALARM to Settings.System.DEFAULT_ALARM_ALERT_URI
      else -> return false
    }
    val resolvedUri = runCatching {
      RingtoneManager.getActualDefaultRingtoneUri(appContext, ringtoneType)
    }.onFailure { error ->
      Log.w(TAG, "Falha ao consultar URI padrão de $streamType; usando fallback", error)
    }.getOrElse { fallbackUri } ?: return false

    return runCatching {
      val ringtone = RingtoneManager.getRingtone(appContext, resolvedUri) ?: return false
      ringtone.audioAttributes = audioAttributes
      ringtone.isLooping = isLoop
      ringtone.volume = gain
      ringtone.play()
      activeRingtone = ringtone
      true
    }.onFailure { error ->
      Log.w(TAG, "Falha ao reproduzir Ringtone do sistema para $streamType; usando síntese PCM", error)
    }.getOrDefault(false)
  }

  private fun tryStartSynthesizedTrackLocked(
    streamType: VolumeStreamType,
    audioAttributes: AudioAttributes,
    isLoop: Boolean,
    gain: Float,
  ) {
    runCatching {
      val samples = buildZenithAcousticSamples(streamType = streamType, loopable = isLoop)
      val byteBuffer = ShortArray(samples.size) { idx -> samples[idx] }
      val bufferBytes = byteBuffer.size * Short.SIZE_BYTES

      val format = AudioFormat.Builder()
        .setSampleRate(SAMPLE_RATE_HZ)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

      val track = AudioTrack.Builder()
        .setAudioAttributes(audioAttributes)
        .setAudioFormat(format)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .setBufferSizeInBytes(bufferBytes)
        .build()

      track.write(byteBuffer, 0, byteBuffer.size)
      track.setVolume(gain)
      if (isLoop) {
        track.setLoopPoints(0, byteBuffer.size, -1)
      }
      track.play()
      activeAudioTrack = track
    }.onFailure { error ->
      Log.e(TAG, "Falha ao reproduzir acorde sintetizado PCM para $streamType", error)
    }
  }

  private fun releaseActivePlayersLocked() {
    activeRingtone?.let { ringtone ->
      runCatching {
        ringtone.isLooping = false
        if (ringtone.isPlaying) {
          ringtone.stop()
        }
      }.onFailure { Log.w(TAG, "Falha ao parar Ringtone de preview", it) }
    }
    activeRingtone = null

    activeAudioTrack?.let { track ->
      runCatching {
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
          track.pause()
          track.flush()
        }
        track.release()
      }.onFailure { Log.w(TAG, "Falha ao liberar AudioTrack de preview", it) }
    }
    activeAudioTrack = null
  }

  private fun VolumeStreamType.supportsSystemRingtoneUri(): Boolean =
    this == VolumeStreamType.RING ||
      this == VolumeStreamType.NOTIFICATION ||
      this == VolumeStreamType.ALARM

  private fun VolumeStreamType.toPreviewAudioAttributes(): AudioAttributes {
    val (usage, contentType) = when (this) {
      VolumeStreamType.RING ->
        AudioAttributes.USAGE_NOTIFICATION_RINGTONE to AudioAttributes.CONTENT_TYPE_SONIFICATION
      VolumeStreamType.NOTIFICATION ->
        AudioAttributes.USAGE_NOTIFICATION to AudioAttributes.CONTENT_TYPE_SONIFICATION
      VolumeStreamType.ALARM ->
        AudioAttributes.USAGE_ALARM to AudioAttributes.CONTENT_TYPE_SONIFICATION
      VolumeStreamType.MEDIA ->
        AudioAttributes.USAGE_MEDIA to AudioAttributes.CONTENT_TYPE_MUSIC
      VolumeStreamType.SYSTEM ->
        AudioAttributes.USAGE_ASSISTANCE_SONIFICATION to AudioAttributes.CONTENT_TYPE_SONIFICATION
      VolumeStreamType.VOICE_CALL ->
        AudioAttributes.USAGE_VOICE_COMMUNICATION to AudioAttributes.CONTENT_TYPE_SPEECH
      VolumeStreamType.ACCESSIBILITY ->
        AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY to AudioAttributes.CONTENT_TYPE_SPEECH
    }
    return AudioAttributes.Builder()
      .setUsage(usage)
      .setContentType(contentType)
      .build()
  }

  /**
   * Gera um acorde acústico harmônico suave (Zenith Boreal Chord) específico para cada canal,
   * com envelope sem cliques nas bordas para permitir tanto One-Shot quanto Loop contínuo limpo.
   */
  private fun buildZenithAcousticSamples(
    streamType: VolumeStreamType,
    loopable: Boolean,
  ): ShortArray {
    val durationSeconds = if (loopable) 1.20 else 1.45
    val totalSamples = (SAMPLE_RATE_HZ * durationSeconds).toInt().coerceAtLeast(1)
    val frequencies = when (streamType) {
      VolumeStreamType.MEDIA -> doubleArrayOf(261.63, 329.63, 392.00, 493.88) // Cmaj7 musical chord
      VolumeStreamType.RING -> doubleArrayOf(440.00, 554.37, 659.25) // A major chime
      VolumeStreamType.NOTIFICATION -> doubleArrayOf(587.33, 880.00) // Crisp D5-A5 bell
      VolumeStreamType.ALARM -> doubleArrayOf(523.25, 659.25, 783.99) // Clear C5 triad
      VolumeStreamType.SYSTEM -> doubleArrayOf(783.99, 1174.66) // Subtle tactile sonification
      VolumeStreamType.VOICE_CALL -> doubleArrayOf(329.63, 440.00, 659.25) // Warm mid-band voice ref
      VolumeStreamType.ACCESSIBILITY -> doubleArrayOf(493.88, 739.99) // Distinct clear beacon
    }

    val fadeSamples = (SAMPLE_RATE_HZ * 0.035).toInt().coerceAtLeast(1)
    val output = ShortArray(totalSamples)
    val invFreqCount = 1.0 / frequencies.size

    for (i in 0 until totalSamples) {
      val t = i.toDouble() / SAMPLE_RATE_HZ
      var sum = 0.0
      for (fIdx in frequencies.indices) {
        val freq = frequencies[fIdx]
        val noteOnset = if (loopable) 0.0 else fIdx * 0.07
        if (t >= noteOnset) {
          val localT = t - noteOnset
          val harmonic = sin(2.0 * PI * freq * localT) + 0.25 * sin(4.0 * PI * freq * localT)
          val decay = if (loopable) {
            0.78 + 0.22 * sin(2.0 * PI * (i.toDouble() / totalSamples))
          } else {
            exp(-1.9 * localT)
          }
          sum += harmonic * decay
        }
      }

      val edgeWindow = when {
        i < fadeSamples -> 0.5 * (1.0 - cos(PI * i / fadeSamples))
        i > totalSamples - fadeSamples -> 0.5 * (1.0 - cos(PI * (totalSamples - i) / fadeSamples))
        else -> 1.0
      }

      val normalized = (sum * invFreqCount * edgeWindow * 0.62).coerceIn(-1.0, 1.0)
      output[i] = (normalized * Short.MAX_VALUE).toInt().toShort()
    }
    return output
  }

  private companion object {
    const val TAG = "AndroidVolumePreview"
    const val SAMPLE_RATE_HZ = 24_000
    const val ONE_SHOT_MAX_DURATION_MS = 2_800L
    const val SYNTH_ONE_SHOT_DURATION_MS = 1_550L
  }
}

/**
 * Implementação determinística em memória para testes unitários JVM e testes de UI Compose.
 */
class FakeVolumePreviewPlayer : VolumePreviewPlayer {
  private val _previewState = MutableStateFlow<ActiveVolumePreview?>(null)
  override val previewState: StateFlow<ActiveVolumePreview?> = _previewState.asStateFlow()

  private var activeStopCallback: (() -> Unit)? = null

  val playHistory = mutableListOf<ActiveVolumePreview>()
  val livePercentUpdates = mutableListOf<Int>()
  var stopCount: Int = 0
    private set
  var lastUseSystemSounds: Boolean = true
    private set

  override fun toggleSingle(
    streamType: VolumeStreamType,
    percent: Int,
    useSystemSounds: Boolean,
    scope: VolumePreviewScope,
    onStopped: (() -> Unit)?,
  ) {
    val current = _previewState.value
    if (current != null && current.streamType == streamType && current.scope == scope) {
      stop()
    } else {
      playSingle(
        streamType = streamType,
        percent = percent,
        useSystemSounds = useSystemSounds,
        scope = scope,
        onStopped = onStopped,
      )
    }
  }

  override fun playSingle(
    streamType: VolumeStreamType,
    percent: Int,
    useSystemSounds: Boolean,
    scope: VolumePreviewScope,
    onStopped: (() -> Unit)?,
  ) {
    activeStopCallback?.invoke()
    activeStopCallback = onStopped
    lastUseSystemSounds = useSystemSounds
    val active = ActiveVolumePreview(
      streamType = streamType,
      percent = percent.coerceIn(0, 100),
      kind = VolumePreviewKind.ONE_SHOT,
      scope = scope,
    )
    playHistory.add(active)
    _previewState.value = active
  }

  override fun startLoop(
    streamType: VolumeStreamType,
    percent: Int,
    useSystemSounds: Boolean,
    scope: VolumePreviewScope,
    onStopped: (() -> Unit)?,
  ) {
    activeStopCallback?.invoke()
    activeStopCallback = onStopped
    lastUseSystemSounds = useSystemSounds
    val active = ActiveVolumePreview(
      streamType = streamType,
      percent = percent.coerceIn(0, 100),
      kind = VolumePreviewKind.HOLD_LOOP,
      scope = scope,
    )
    playHistory.add(active)
    _previewState.value = active
  }

  override fun updateLivePercent(percent: Int) {
    val clamped = percent.coerceIn(0, 100)
    livePercentUpdates.add(clamped)
    val current = _previewState.value ?: return
    _previewState.value = current.copy(percent = clamped)
  }

  override fun stop() {
    stopCount++
    _previewState.value = null
    val callback = activeStopCallback
    activeStopCallback = null
    callback?.invoke()
  }

  fun finishCurrentOneShotNaturally() {
    if (_previewState.value?.kind == VolumePreviewKind.ONE_SHOT) {
      stop()
    }
  }
}

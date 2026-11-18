package com.managesoundmode.app.domain.model

import com.managesoundmode.app.ui.common.AppLabels
import kotlinx.serialization.Serializable

@Serializable
enum class StreamSortMode {
  MOST_USED,
  MANUAL,
  ;

  val label: String
    get() = AppLabels.Domain.streamSortModeLabel(this)
}

@Serializable
enum class VolumeStreamType {
  MEDIA,
  RING,
  NOTIFICATION,
  ALARM,
  SYSTEM,
  VOICE_CALL,
  ACCESSIBILITY,
  ;

  val label: String
    get() = AppLabels.Domain.streamTypeLabel(this)

  val microDescription: String
    get() = AppLabels.Domain.streamTypeMicroDescription(this)

  companion object {
    val defaultOrder: List<VolumeStreamType> = listOf(
      MEDIA,
      RING,
      NOTIFICATION,
      ALARM,
      SYSTEM,
      VOICE_CALL,
      ACCESSIBILITY,
    )

    val primaryDefaultStreams: List<VolumeStreamType> = listOf(
      MEDIA,
      RING,
      NOTIFICATION,
      ALARM,
    )

    val secondaryDefaultStreams: List<VolumeStreamType> = listOf(
      SYSTEM,
      VOICE_CALL,
      ACCESSIBILITY,
    )

    fun normalizeManualStreamOrder(order: List<VolumeStreamType>): List<VolumeStreamType> {
      val distinct = order.distinct()
      return distinct + defaultOrder.filterNot { it in distinct }
    }

    fun resolveOrderedStreams(
      sortMode: StreamSortMode,
      usageCounts: Map<VolumeStreamType, Int>,
      manualOrder: List<VolumeStreamType>,
    ): List<VolumeStreamType> {
      return when (sortMode) {
        StreamSortMode.MANUAL -> normalizeManualStreamOrder(manualOrder)
        StreamSortMode.MOST_USED -> {
          defaultOrder.sortedWith(
            compareByDescending<VolumeStreamType> { (usageCounts[it] ?: 0).coerceAtLeast(0) }
              .thenBy { defaultOrder.indexOf(it) },
          )
        }
      }
    }
  }
}

@Serializable
data class VolumeStreams(
  val ringPercent: Int = 80,
  val notificationPercent: Int = 75,
  val mediaPercent: Int = 50,
  val alarmPercent: Int = 90,
  val systemPercent: Int = 50,
  val voiceCallPercent: Int = 80,
  val accessibilityPercent: Int = 70,
) {
  init {
    require(ringPercent in 0..100) { "ringPercent must be between 0 and 100" }
    require(notificationPercent in 0..100) { "notificationPercent must be between 0 and 100" }
    require(mediaPercent in 0..100) { "mediaPercent must be between 0 and 100" }
    require(alarmPercent in 0..100) { "alarmPercent must be between 0 and 100" }
    require(systemPercent in 0..100) { "systemPercent must be between 0 and 100" }
    require(voiceCallPercent in 0..100) { "voiceCallPercent must be between 0 and 100" }
    require(accessibilityPercent in 0..100) { "accessibilityPercent must be between 0 and 100" }
  }

  fun percentFor(type: VolumeStreamType): Int = when (type) {
    VolumeStreamType.MEDIA -> mediaPercent
    VolumeStreamType.RING -> ringPercent
    VolumeStreamType.NOTIFICATION -> notificationPercent
    VolumeStreamType.ALARM -> alarmPercent
    VolumeStreamType.SYSTEM -> systemPercent
    VolumeStreamType.VOICE_CALL -> voiceCallPercent
    VolumeStreamType.ACCESSIBILITY -> accessibilityPercent
  }

  fun withUpdatedStream(
    type: VolumeStreamType,
    percent: Int,
    minPercent: Int = 0,
  ): VolumeStreams {
    val floor = minPercent.coerceIn(0, 100)
    val clamped = percent.coerceIn(floor, 100)
    return when (type) {
      VolumeStreamType.MEDIA -> copy(mediaPercent = clamped)
      VolumeStreamType.RING -> copy(ringPercent = clamped)
      VolumeStreamType.NOTIFICATION -> copy(notificationPercent = clamped)
      VolumeStreamType.ALARM -> copy(alarmPercent = clamped)
      VolumeStreamType.SYSTEM -> copy(systemPercent = clamped)
      VolumeStreamType.VOICE_CALL -> copy(voiceCallPercent = clamped)
      VolumeStreamType.ACCESSIBILITY -> copy(accessibilityPercent = clamped)
    }
  }
}

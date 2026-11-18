package com.managesoundmode.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.managesoundmode.app.domain.model.BaseAudioMode
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.SoundState
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.system.ActiveVolumePreview
import com.managesoundmode.app.system.VolumePreviewKind
import com.managesoundmode.app.system.VolumePreviewScope
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppTestTags
import com.managesoundmode.app.ui.theme.onActivePillColor
import com.managesoundmode.app.ui.theme.onTonalContainerColor
import com.managesoundmode.app.ui.theme.tonalAccentColor
import com.managesoundmode.app.ui.theme.tonalContainerColor
import com.managesoundmode.app.ui.theme.zenithOnActivePillColor
import com.managesoundmode.app.ui.theme.zenithVibrateAccentColor
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Formato de cartão orgânico ZENITH (cantos suaves atmosféricos).
 */
fun groupedCardShape(index: Int, totalCount: Int): RoundedCornerShape {
  if (totalCount <= 1) return RoundedCornerShape(24.dp)
  return when (index) {
    0 -> RoundedCornerShape(
      topStart = 24.dp,
      topEnd = 24.dp,
      bottomStart = 16.dp,
      bottomEnd = 16.dp,
    )
    totalCount - 1 -> RoundedCornerShape(
      topStart = 16.dp,
      topEnd = 16.dp,
      bottomStart = 24.dp,
      bottomEnd = 24.dp,
    )
    else -> RoundedCornerShape(16.dp)
  }
}

fun profileIconForKey(iconKey: String): ImageVector = when (iconKey) {
  "sunny" -> Icons.Default.WbSunny
  "work" -> Icons.Default.Work
  "vibration" -> Icons.Default.Vibration
  "bedtime" -> Icons.Default.Bedtime
  "home" -> Icons.Default.Home
  "shield" -> Icons.Default.Shield
  else -> Icons.Default.Tune
}

fun SoundMode.modeIcon(): ImageVector = when (this) {
  SoundMode.NORMAL -> Icons.Default.NotificationsActive
  SoundMode.VIBRATE -> Icons.Default.Vibration
  SoundMode.SILENT -> Icons.Default.DoNotDisturbOn
}

fun BaseAudioMode.audioModeIcon(): ImageVector = when (this) {
  BaseAudioMode.SOUND -> Icons.Default.NotificationsActive
  BaseAudioMode.SILENT -> Icons.Default.DoNotDisturbOn
}

fun VolumeStreamType.streamIcon(): ImageVector = when (this) {
  VolumeStreamType.MEDIA -> Icons.Default.MusicNote
  VolumeStreamType.RING -> Icons.Default.RingVolume
  VolumeStreamType.NOTIFICATION -> Icons.Default.Notifications
  VolumeStreamType.ALARM -> Icons.Default.Alarm
  VolumeStreamType.SYSTEM -> Icons.Default.TouchApp
  VolumeStreamType.VOICE_CALL -> Icons.Default.Call
  VolumeStreamType.ACCESSIBILITY -> Icons.Default.AccessibilityNew
}

/**
 * Componente Split Capsule `2 + 1` do Design System ZENITH (SPEC-004):
 * - Cápsula esquerda (`weight(2f)`): Segmented exclusivo `[ Som | Silêncio ]`
 * - Cápsula direita (`weight(1f)`): Toggle ortogonal independente `[ Vibrar ]`
 */
@Composable
fun ZenithSplitModeAndVibrateRow(
  audioMode: BaseAudioMode,
  vibrateEnabled: Boolean,
  onSelectAudioMode: (BaseAudioMode) -> Unit,
  onToggleVibrate: () -> Unit,
  soundButtonTag: String,
  silentButtonTag: String,
  vibrateButtonTag: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // 1. Cápsula Esquerda — Segmented Audio Pill (weight 2f)
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.weight(2f),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BaseAudioMode.entries.forEach { mode ->
          val isSelected = audioMode == mode
          val modeAccent = mode.tonalAccentColor()
          val pillBg by animateColorAsState(
            targetValue = if (isSelected) modeAccent else Color.Transparent,
            animationSpec = tween(180),
            label = "zenithSplitAudioBg_${mode.name}",
          )
          val pillContent by animateColorAsState(
            targetValue = if (isSelected) {
              zenithOnActivePillColor()
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
            animationSpec = tween(180),
            label = "zenithSplitAudioContent_${mode.name}",
          )
          val buttonTag = if (mode == BaseAudioMode.SOUND) soundButtonTag else silentButtonTag

          Surface(
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 44.dp)
              .clip(RoundedCornerShape(19.dp))
              .clickable { onSelectAudioMode(mode) }
              .semantics {
                selected = isSelected
                contentDescription = AppLabels.Now.audioModeContentDescription(mode)
              }
              .testTag(buttonTag),
            shape = RoundedCornerShape(19.dp),
            color = pillBg,
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = mode.audioModeIcon(),
                contentDescription = null,
                tint = pillContent,
                modifier = Modifier.size(17.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = AppLabels.Now.audioModeShortPill(mode),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = pillContent,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }

    // 2. Cápsula Direita — Orthogonal Vibrate Toggle Pill (weight 1f)
    val vibrateAccent = zenithVibrateAccentColor()
    val vibrateBg by animateColorAsState(
      targetValue = if (vibrateEnabled) {
        vibrateAccent
      } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
      },
      animationSpec = tween(180),
      label = "zenithSplitVibrateBg",
    )
    val vibrateBorder by animateColorAsState(
      targetValue = if (vibrateEnabled) {
        vibrateAccent
      } else {
        MaterialTheme.colorScheme.outlineVariant
      },
      animationSpec = tween(180),
      label = "zenithSplitVibrateBorder",
    )
    val vibrateContent by animateColorAsState(
      targetValue = if (vibrateEnabled) {
        zenithOnActivePillColor()
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      },
      animationSpec = tween(180),
      label = "zenithSplitVibrateContent",
    )

    Surface(
      modifier = Modifier
        .weight(1f)
        .heightIn(min = 54.dp)
        .clip(RoundedCornerShape(24.dp))
        .clickable { onToggleVibrate() }
        .semantics {
          selected = vibrateEnabled
          contentDescription = AppLabels.Now.vibrateToggleContentDescription(vibrateEnabled)
        }
        .testTag(vibrateButtonTag),
      shape = RoundedCornerShape(24.dp),
      color = vibrateBg,
      border = BorderStroke(1.dp, vibrateBorder),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Default.Vibration,
          contentDescription = null,
          tint = vibrateContent,
          modifier = Modifier.size(17.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = AppLabels.Now.vibrateShortPill,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = if (vibrateEnabled) FontWeight.Bold else FontWeight.Medium,
          color = vibrateContent,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/**
 * Switch ZENITH com ícone `Check` quando ativo e feedback tátil.
 */
@Composable
fun M3CheckSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  Switch(
    checked = checked,
    onCheckedChange = { next ->
      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
      onCheckedChange(next)
    },
    colors = SwitchDefaults.colors(
      checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
      checkedTrackColor = MaterialTheme.colorScheme.primary,
      uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
      uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    ),
    thumbContent = if (checked) {
      {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          modifier = Modifier.size(SwitchDefaults.IconSize),
        )
      }
    } else {
      null
    },
    modifier = modifier,
  )
}

private const val PREVIEW_HOLD_THRESHOLD_MS = 300L
private const val PREVIEW_DOUBLE_TAP_WINDOW_MS = 320L
private const val PREVIEW_PLAY_CUE_DURATION_MS = 4_500L

/**
 * Cápsula Líquida de Volume ZENITH (`Zenith Capsule Slider`).
 * Exibe preenchimento líquido proporcional ao volume dentro de uma cápsula arredondada,
 * com badge circular interativo (`1×` sample, `Press & Hold` loop ao vivo), suporte a `2×` duplo
 * toque e `Press & Hold` na cápsula sem atrasar o arraste normal mudo, além do Cue `PlayArrow` por 4,5s.
 */
@Composable
fun PillVolumeSliderRow(
  type: VolumeStreamType,
  percent: Int,
  onValueChangeFinished: (Int) -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector = type.streamIcon(),
  accentColor: Color = MaterialTheme.colorScheme.primary,
  minPercent: Int = 0,
  microDescription: String? = type.microDescription,
  isLinked: Boolean? = null,
  onToggleLink: (() -> Unit)? = null,
  isPendingForSound: Boolean = false,
  activePreviewKind: VolumePreviewKind? = null,
  onSingleTapPreview: ((Int) -> Unit)? = null,
  onDoubleTapPreview: ((Int) -> Unit)? = null,
  onStartHoldPreview: ((Int) -> Unit)? = null,
  onHoldLivePercentChange: ((Int) -> Unit)? = null,
  onStopHoldPreview: ((Int, Boolean) -> Unit)? = null,
  externalPreviewPercent: Int? = null,
  onLivePreviewChange: ((Int?) -> Unit)? = null,
  capsuleTestTag: String = AppTestTags.Now.streamCapsule(type),
  sliderTestTag: String = AppTestTags.Now.streamSlider(type),
  iconButtonTestTag: String = AppTestTags.Now.streamIconButton(type),
  playCueTestTag: String = AppTestTags.Now.streamPlayCue(type),
  previewActiveBadgeTestTag: String = AppTestTags.Now.streamPreviewActiveBadge(type),
  pendingBadgeTestTag: String = AppTestTags.Now.streamPendingBadge(type),
  linkButtonTestTag: String = AppTestTags.Now.streamLinkButton(type),
) {
  val haptics = LocalHapticFeedback.current
  val clampedMin = minPercent.coerceIn(0, 99)
  val floorFloat = clampedMin.toFloat()
  var sliderValue by remember {
    mutableFloatStateOf(percent.coerceIn(clampedMin, 100).toFloat())
  }
  var isDragging by remember { mutableStateOf(false) }
  var isHoldLooping by remember { mutableStateOf(false) }
  var playCueTriggerCount by remember { mutableIntStateOf(0) }
  var showPlayCue by remember { mutableStateOf(false) }

  var capsuleRootPos by remember { mutableStateOf(Offset.Zero) }
  var capsuleWidthPx by remember { mutableFloatStateOf(0f) }
  var iconRootRect by remember { mutableStateOf(Rect.Zero) }
  var linkRootRect by remember { mutableStateOf(Rect.Zero) }

  LaunchedEffect(percent, clampedMin) {
    if (!isDragging && !isHoldLooping) {
      sliderValue = percent.coerceIn(clampedMin, 100).toFloat()
    }
  }

  LaunchedEffect(playCueTriggerCount) {
    if (playCueTriggerCount > 0) {
      showPlayCue = true
      delay(PREVIEW_PLAY_CUE_DURATION_MS)
      showPlayCue = false
    }
  }

  val displayValue = if (!isDragging && !isHoldLooping && externalPreviewPercent != null) {
    externalPreviewPercent.coerceIn(clampedMin, 100).toFloat()
  } else {
    sliderValue.coerceIn(floorFloat, 100f)
  }

  val currentPercent = displayValue.roundToInt().coerceIn(clampedMin, 100)
  val isPreviewPlaying = activePreviewKind != null
  val isCueVisible = showPlayCue && !isPreviewPlaying

  val latestPercent by rememberUpdatedState(currentPercent)
  val latestClampedMin by rememberUpdatedState(clampedMin)
  val latestFloorFloat by rememberUpdatedState(floorFloat)
  val latestOnSingleTap by rememberUpdatedState(onSingleTapPreview)
  val latestOnDoubleTap by rememberUpdatedState(onDoubleTapPreview)
  val latestOnStartHold by rememberUpdatedState(onStartHoldPreview)
  val latestOnHoldLivePercent by rememberUpdatedState(onHoldLivePercentChange)
  val latestOnStopHold by rememberUpdatedState(onStopHoldPreview)
  val latestOnLivePreviewChange by rememberUpdatedState(onLivePreviewChange)
  val latestOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

  val liquidAlphaScale = if (isPendingForSound) 0.55f else 1f
  val animatedFillFraction by animateFloatAsState(
    targetValue = (currentPercent / 100f).coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 150),
    label = "zenithCapsuleFill_${type.name}",
  )

  val capsuleBorderColor by animateColorAsState(
    targetValue = when {
      isPreviewPlaying -> accentColor
      isCueVisible -> accentColor.copy(alpha = 0.65f)
      else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    },
    animationSpec = tween(durationMillis = 180),
    label = "zenithCapsuleBorder_${type.name}",
  )

  val capsuleShape = RoundedCornerShape(20.dp)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .onGloballyPositioned { coords ->
        capsuleRootPos = coords.positionInRoot()
        capsuleWidthPx = coords.size.width.toFloat()
      }
      .clip(capsuleShape)
      .background(MaterialTheme.colorScheme.surfaceContainerHigh)
      .border(
        width = if (isPreviewPlaying) 1.5.dp else 1.dp,
        color = capsuleBorderColor,
        shape = capsuleShape,
      )
      .pointerInput(type) {
        val touchSlop = viewConfiguration.touchSlop
        val childHitPaddingPx = 4.dp.toPx()
        var lastTapUpTimeMillis = 0L
        var lastTapPosition = Offset.Zero

        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
          val rootDown = capsuleRootPos + down.position
          val hitIcon = iconRootRect != Rect.Zero && iconRootRect.inflate(childHitPaddingPx).contains(rootDown)
          val hitLink = linkRootRect != Rect.Zero && linkRootRect.inflate(childHitPaddingPx).contains(rootDown)
          if (hitIcon || hitLink) {
            return@awaitEachGesture
          }

          val downTimeMillis = down.uptimeMillis
          val downPos = down.position
          val isSecondTapCandidate =
            (downTimeMillis - lastTapUpTimeMillis) in 1L..PREVIEW_DOUBLE_TAP_WINDOW_MS &&
              (downPos - lastTapPosition).getDistance() <= touchSlop * 3.2f

          var exceededSlop = false
          var releasedCleanly = false
          var upTimeMillis = downTimeMillis

          val finishedBeforeHold = withTimeoutOrNull(PREVIEW_HOLD_THRESHOLD_MS) {
            while (true) {
              val event = awaitPointerEvent(PointerEventPass.Initial)
              val change = event.changes.firstOrNull { it.id == down.id }
                ?: event.changes.firstOrNull()
                ?: break
              if ((change.position - downPos).getDistance() > touchSlop) {
                exceededSlop = true
                break
              }
              if (!change.pressed) {
                releasedCleanly = true
                upTimeMillis = change.uptimeMillis
                break
              }
            }
            true
          }

          when {
            exceededSlop -> {
              lastTapUpTimeMillis = 0L
            }

            finishedBeforeHold == true && releasedCleanly -> {
              if (isSecondTapCandidate && latestOnDoubleTap != null) {
                lastTapUpTimeMillis = 0L
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                latestOnDoubleTap?.invoke(latestPercent)
              } else {
                lastTapUpTimeMillis = upTimeMillis
                lastTapPosition = downPos
              }
            }

            finishedBeforeHold == null && latestOnStartHold != null -> {
              lastTapUpTimeMillis = 0L
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              isHoldLooping = true
              var didDragInHold = false
              var lastX = downPos.x
              val usableWidth = (capsuleWidthPx - 28.dp.toPx()).coerceAtLeast(80f)

              try {
                latestOnStartHold?.invoke(latestPercent)
                while (true) {
                  val event = awaitPointerEvent(PointerEventPass.Initial)
                  val activeChange = event.changes.firstOrNull { it.id == down.id }
                    ?: event.changes.firstOrNull()
                  if (activeChange == null || !activeChange.pressed) {
                    break
                  }
                  val dx = activeChange.position.x - lastX
                  if (abs(dx) > 1f && capsuleWidthPx > 0f) {
                    lastX = activeChange.position.x
                    didDragInHold = true
                    if (!isDragging) {
                      val nextVal = (sliderValue + (dx / usableWidth) * 100f)
                        .coerceIn(latestFloorFloat, 100f)
                      sliderValue = nextVal
                      val nextInt = nextVal.roundToInt().coerceIn(latestClampedMin, 100)
                      latestOnLivePreviewChange?.invoke(nextInt)
                      latestOnHoldLivePercent?.invoke(nextInt)
                    }
                  }
                }
              } finally {
                val finalInt = sliderValue.roundToInt().coerceIn(latestClampedMin, 100)
                isHoldLooping = false
                if (!isDragging) {
                  latestOnLivePreviewChange?.invoke(null)
                }
                latestOnStopHold?.invoke(finalInt, didDragInHold)
                if (didDragInHold && !isDragging) {
                  latestOnValueChangeFinished(finalInt)
                }
              }
            }
          }
        }
      }
      .testTag(capsuleTestTag),
  ) {
    // Camada de preenchimento líquido interno (Zenith Liquid Fill)
    Box(
      modifier = Modifier
        .matchParentSize(),
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(fraction = animatedFillFraction)
          .background(
            brush = Brush.horizontalGradient(
              colors = listOf(
                accentColor.copy(alpha = 0.20f * liquidAlphaScale),
                accentColor.copy(alpha = 0.08f * liquidAlphaScale),
              ),
            ),
          ),
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier
            .weight(1f)
            .padding(end = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          val badgeBgColor by animateColorAsState(
            targetValue = when {
              isPreviewPlaying -> accentColor
              isCueVisible -> accentColor.copy(alpha = 0.26f)
              else -> accentColor.copy(alpha = 0.16f)
            },
            animationSpec = tween(durationMillis = 160),
            label = "zenithIconBadgeBg_${type.name}",
          )
          val badgeIconTint = if (isPreviewPlaying) {
            zenithOnActivePillColor()
          } else {
            accentColor
          }
          val badgeIconVector = when {
            activePreviewKind == VolumePreviewKind.HOLD_LOOP -> Icons.Default.GraphicEq
            activePreviewKind == VolumePreviewKind.ONE_SHOT -> Icons.Default.Stop
            isCueVisible -> Icons.Default.PlayArrow
            else -> icon
          }

          Box(
            modifier = Modifier
              .size(34.dp)
              .onGloballyPositioned { coords ->
                iconRootRect = Rect(coords.positionInRoot(), coords.size.toSize())
              }
              .clip(CircleShape)
              .background(badgeBgColor)
              .then(
                if (isCueVisible || isPreviewPlaying) {
                  Modifier.border(
                    width = 1.5.dp,
                    color = accentColor,
                    shape = CircleShape,
                  )
                } else {
                  Modifier
                },
              )
              .semantics(mergeDescendants = false) {
                role = Role.Button
                contentDescription = AppLabels.Now.streamPreviewIconContentDescription(
                  label = type.label,
                  isPlaying = isPreviewPlaying,
                )
                onClick {
                  latestOnSingleTap?.invoke(latestPercent)
                  true
                }
              }
              .pointerInput(type) {
                val usablePadPx = 28.dp.toPx()
                awaitEachGesture {
                  val down = awaitFirstDown(requireUnconsumed = false)
                  down.consume()

                  val tappedCleanly = withTimeoutOrNull(PREVIEW_HOLD_THRESHOLD_MS) {
                    while (true) {
                      val event = awaitPointerEvent()
                      val active = event.changes.firstOrNull { it.id == down.id }
                        ?: event.changes.firstOrNull()
                        ?: break
                      active.consume()
                      if (!active.pressed) {
                        break
                      }
                    }
                    true
                  }

                  if (tappedCleanly == true) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    latestOnSingleTap?.invoke(latestPercent)
                  } else if (latestOnStartHold != null) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    isHoldLooping = true
                    var didDragInHold = false
                    var lastX = (iconRootRect.left - capsuleRootPos.x) + down.position.x
                    val usableWidth = (capsuleWidthPx - usablePadPx).coerceAtLeast(80f)

                    try {
                      latestOnStartHold?.invoke(latestPercent)
                      while (true) {
                        val event = awaitPointerEvent()
                        val active = event.changes.firstOrNull { it.id == down.id }
                          ?: event.changes.firstOrNull()
                        if (active == null || !active.pressed) {
                          event.changes.forEach { it.consume() }
                          break
                        }
                        val currentCapsuleX = (iconRootRect.left - capsuleRootPos.x) + active.position.x
                        val dx = currentCapsuleX - lastX
                        if (abs(dx) > 1f && capsuleWidthPx > 0f) {
                          lastX = currentCapsuleX
                          didDragInHold = true
                          val nextVal = (sliderValue + (dx / usableWidth) * 100f)
                            .coerceIn(latestFloorFloat, 100f)
                          sliderValue = nextVal
                          val nextInt = nextVal.roundToInt().coerceIn(latestClampedMin, 100)
                          latestOnLivePreviewChange?.invoke(nextInt)
                          latestOnHoldLivePercent?.invoke(nextInt)
                        }
                        active.consume()
                      }
                    } finally {
                      val finalInt = sliderValue.roundToInt().coerceIn(latestClampedMin, 100)
                      isHoldLooping = false
                      latestOnLivePreviewChange?.invoke(null)
                      latestOnStopHold?.invoke(finalInt, didDragInHold)
                      if (didDragInHold) {
                        latestOnValueChangeFinished(finalInt)
                      }
                    }
                  }
                }
              }
              .testTag(iconButtonTestTag),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = badgeIconVector,
              contentDescription = null,
              tint = badgeIconTint,
              modifier = Modifier
                .size(17.dp)
                .then(
                  when {
                    isPreviewPlaying -> Modifier.testTag(previewActiveBadgeTestTag)
                    isCueVisible -> Modifier.testTag(playCueTestTag)
                    else -> Modifier
                  },
                ),
            )
          }

          Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
              text = type.label,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            val resolvedSubtitle = when {
              activePreviewKind == VolumePreviewKind.HOLD_LOOP -> AppLabels.Now.previewLoopingSubtitle
              activePreviewKind == VolumePreviewKind.ONE_SHOT -> AppLabels.Now.previewPlayingSubtitle
              isCueVisible -> AppLabels.Now.previewCueSubtitle
              else -> microDescription
            }
            if (!resolvedSubtitle.isNullOrBlank()) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                if (isPreviewPlaying) {
                  Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp),
                  )
                }
                Text(
                  text = resolvedSubtitle,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isPreviewPlaying || isCueVisible) {
                    FontWeight.SemiBold
                  } else {
                    FontWeight.Normal
                  },
                  color = if (isPreviewPlaying || isCueVisible) {
                    accentColor
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (isPendingForSound) {
            Surface(
              shape = RoundedCornerShape(50),
              color = accentColor.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f)),
              modifier = Modifier.testTag(pendingBadgeTestTag),
            ) {
              Text(
                text = AppLabels.Now.pendingForSoundBadge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }

          if (isLinked != null && onToggleLink != null) {
            Surface(
              shape = CircleShape,
              color = if (isLinked) {
                accentColor
              } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
              },
              border = if (isLinked) {
                null
              } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
              },
              modifier = Modifier
                .size(26.dp)
                .onGloballyPositioned { coords ->
                  linkRootRect = Rect(coords.positionInRoot(), coords.size.toSize())
                }
                .clip(CircleShape)
                .clickable(onClick = onToggleLink)
                .semantics {
                  selected = isLinked
                  contentDescription = AppLabels.Now.ringNotificationLinkContentDescription(isLinked)
                }
                .testTag(linkButtonTestTag),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (isLinked) Icons.Default.Link else Icons.Default.LinkOff,
                  contentDescription = null,
                  tint = if (isLinked) {
                    zenithOnActivePillColor()
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  modifier = Modifier.size(14.dp),
                )
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(50),
            color = if (currentPercent == 0) {
              MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
              accentColor.copy(alpha = if (isPendingForSound) 0.12f else 0.16f)
            },
          ) {
            Text(
              text = "$currentPercent%",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = if (currentPercent == 0) {
                MaterialTheme.colorScheme.onSurfaceVariant
              } else {
                accentColor
              },
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            )
          }
        }
      }

      Slider(
        value = displayValue,
        onValueChange = { raw ->
          val wasDragging = isDragging
          isDragging = true
          val clamped = raw.coerceIn(floorFloat, 100f)
          val nextInt = clamped.roundToInt().coerceIn(clampedMin, 100)
          val valueMoved = nextInt != sliderValue.roundToInt() || !wasDragging
          sliderValue = clamped
          if (valueMoved && !isHoldLooping) {
            playCueTriggerCount++
          }
          onLivePreviewChange?.invoke(nextInt)
          if (isHoldLooping) {
            onHoldLivePercentChange?.invoke(nextInt)
          }
        },
        onValueChangeFinished = {
          isDragging = false
          onLivePreviewChange?.invoke(null)
          onValueChangeFinished(sliderValue.roundToInt().coerceIn(clampedMin, 100))
        },
        valueRange = floorFloat..100f,
        colors = SliderDefaults.colors(
          thumbColor = accentColor.copy(alpha = if (isPendingForSound) 0.85f else 1f),
          activeTrackColor = accentColor.copy(alpha = if (isPendingForSound) 0.72f else 1f),
          inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag(sliderTestTag),
      )
    }
  }
}

/**
 * Diálogo compacto de segurança acústica quando o aparelho está em modo Silencioso/Vibrar
 * e o usuário aciona um gesto de preview sonoro (SPEC-005 / Issues #3 & #4).
 */
@Composable
fun SilentPreviewConfirmDialog(
  onKeepMuted: () -> Unit,
  onListenNow: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onKeepMuted,
    modifier = Modifier.testTag(AppTestTags.Now.SILENT_PREVIEW_CONFIRM_DIALOG),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    icon = {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(BaseAudioMode.SILENT.tonalContainerColor()),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = null,
          tint = BaseAudioMode.SILENT.tonalAccentColor(),
          modifier = Modifier.size(22.dp),
        )
      }
    },
    title = {
      Text(
        text = AppLabels.Now.silentPreviewDialogTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Text(
        text = AppLabels.Now.silentPreviewDialogBody,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    confirmButton = {
      Button(
        onClick = onListenNow,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag(AppTestTags.Now.SILENT_PREVIEW_LISTEN_NOW_BUTTON),
      ) {
        Text(AppLabels.Now.silentPreviewListenNowButton, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onKeepMuted,
        modifier = Modifier.testTag(AppTestTags.Now.SILENT_PREVIEW_KEEP_MUTED_BUTTON),
      ) {
        Text(AppLabels.Now.silentPreviewKeepMutedButton)
      }
    },
  )
}

/**
 * Tela dedicada Full-Screen ZENITH para criar ou editar qualquer Cena / Perfil de Som.
 * - Zero fechamento acidental ao arrastar sliders.
 * - Quando editando um perfil existente (initialProfile != null), salva automaticamente qualquer alteração.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileEditorBottomSheet(
  initialProfile: SoundProfile?,
  defaultMode: SoundMode,
  defaultStreams: VolumeStreams,
  defaultSoundState: SoundState = SoundState.fromLegacyMode(defaultMode),
  streamMinPercents: Map<VolumeStreamType, Int> = emptyMap(),
  ringNotificationLinkedDefault: Boolean = true,
  activeVolumePreview: ActiveVolumePreview? = null,
  onToggleEditorStreamPreview: (VolumeStreamType, Int) -> Unit = { _, _ -> },
  onStartEditorStreamLoop: (VolumeStreamType, Int) -> Unit = { _, _ -> },
  onUpdateEditorStreamLoopPercent: (VolumeStreamType, Int) -> Unit = { _, _ -> },
  onStopEditorStreamPreview: () -> Unit = {},
  onSaveProfile: (profile: SoundProfile, applyNow: Boolean) -> Unit,
  onAutoSaveProfile: (profile: SoundProfile) -> Unit = {},
  onDeleteProfile: ((profileId: String) -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  val isEditing = initialProfile != null
  var silentPreviewAuthorizedInSession by rememberSaveable { mutableStateOf(false) }
  var pendingSilentPreviewRequest by remember { mutableStateOf<Pair<VolumeStreamType, Int>?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      onStopEditorStreamPreview()
    }
  }
  var name by rememberSaveable { mutableStateOf(initialProfile?.name ?: "") }
  var iconKey by rememberSaveable { mutableStateOf(initialProfile?.iconKey ?: "work") }
  var selectedAudioMode by rememberSaveable {
    mutableStateOf(initialProfile?.resolvedAudioMode ?: defaultSoundState.audioMode)
  }
  var selectedVibrateEnabled by rememberSaveable {
    mutableStateOf(initialProfile?.resolvedVibrateEnabled ?: defaultSoundState.vibrateEnabled)
  }
  var ringPercent by rememberSaveable {
    mutableIntStateOf(initialProfile?.streams?.ringPercent ?: defaultStreams.ringPercent)
  }
  var notifPercent by rememberSaveable {
    mutableIntStateOf(
      if (initialProfile != null) {
        initialProfile.streams.notificationPercent
      } else if (ringNotificationLinkedDefault) {
        defaultStreams.ringPercent
      } else {
        defaultStreams.notificationPercent
      },
    )
  }
  var editorRingNotifLinked by rememberSaveable {
    mutableStateOf(
      if (initialProfile != null) {
        initialProfile.streams.ringPercent == initialProfile.streams.notificationPercent
      } else {
        ringNotificationLinkedDefault
      },
    )
  }
  var editorLinkedDragPreview by remember { mutableStateOf<Int?>(null) }
  var mediaPercent by rememberSaveable {
    mutableIntStateOf(initialProfile?.streams?.mediaPercent ?: defaultStreams.mediaPercent)
  }
  var alarmPercent by rememberSaveable {
    mutableIntStateOf(initialProfile?.streams?.alarmPercent ?: defaultStreams.alarmPercent)
  }
  var secondaryExpanded by rememberSaveable {
    mutableStateOf(initialProfile?.hasSecondaryOverrides == true)
  }
  var overrideSystemEnabled by rememberSaveable {
    mutableStateOf(initialProfile?.overrideSystemPercent != null)
  }
  var systemPercent by rememberSaveable {
    mutableIntStateOf(
      initialProfile?.overrideSystemPercent
        ?: initialProfile?.streams?.systemPercent
        ?: defaultStreams.systemPercent,
    )
  }
  var overrideVoiceCallEnabled by rememberSaveable {
    mutableStateOf(initialProfile?.overrideVoiceCallPercent != null)
  }
  var voiceCallPercent by rememberSaveable {
    mutableIntStateOf(
      initialProfile?.overrideVoiceCallPercent
        ?: initialProfile?.streams?.voiceCallPercent
        ?: defaultStreams.voiceCallPercent,
    )
  }
  var overrideAccessibilityEnabled by rememberSaveable {
    mutableStateOf(initialProfile?.overrideAccessibilityPercent != null)
  }
  var accessibilityPercent by rememberSaveable {
    mutableIntStateOf(
      initialProfile?.overrideAccessibilityPercent
        ?: initialProfile?.streams?.accessibilityPercent
        ?: defaultStreams.accessibilityPercent,
    )
  }
  var preserveMedia by rememberSaveable {
    mutableStateOf(initialProfile?.preserveMediaIfPlaying ?: true)
  }
  var skipBluetooth by rememberSaveable {
    mutableStateOf(initialProfile?.skipMediaOnBluetooth ?: true)
  }

  val iconOptions = AppLabels.Profiles.iconOptions()
  val activeAccent = selectedAudioMode.tonalAccentColor()

  fun minFor(type: VolumeStreamType): Int = (streamMinPercents[type] ?: 0).coerceIn(0, 100)

  fun buildProfile(): SoundProfile {
    val resolvedName = name.trim().ifEmpty {
      initialProfile?.name ?: AppLabels.Profiles.newProfileTitle
    }
    val streams = VolumeStreams(
      ringPercent = ringPercent.coerceIn(minFor(VolumeStreamType.RING), 100),
      notificationPercent = notifPercent.coerceIn(minFor(VolumeStreamType.NOTIFICATION), 100),
      mediaPercent = mediaPercent.coerceIn(minFor(VolumeStreamType.MEDIA), 100),
      alarmPercent = alarmPercent.coerceIn(minFor(VolumeStreamType.ALARM), 100),
      systemPercent = systemPercent.coerceIn(minFor(VolumeStreamType.SYSTEM), 100),
      voiceCallPercent = voiceCallPercent.coerceIn(minFor(VolumeStreamType.VOICE_CALL), 100),
      accessibilityPercent = accessibilityPercent.coerceIn(minFor(VolumeStreamType.ACCESSIBILITY), 100),
    )
    val state = SoundState(
      audioMode = selectedAudioMode,
      vibrateEnabled = selectedVibrateEnabled,
    )
    return SoundProfile(
      id = initialProfile?.id ?: "custom_${System.currentTimeMillis()}",
      name = resolvedName,
      description = initialProfile?.description
        ?: AppLabels.Domain.profileSummaryLabel(state, streams),
      mode = state.toLegacyApproximation(),
      audioMode = selectedAudioMode,
      vibrateEnabled = selectedVibrateEnabled,
      streams = streams,
      isPreset = initialProfile?.isPreset ?: false,
      iconKey = iconKey,
      preserveMediaIfPlaying = preserveMedia,
      skipMediaOnBluetooth = skipBluetooth,
      overrideSystemPercent = if (overrideSystemEnabled) {
        systemPercent.coerceIn(minFor(VolumeStreamType.SYSTEM), 100)
      } else {
        null
      },
      overrideVoiceCallPercent = if (overrideVoiceCallEnabled) {
        voiceCallPercent.coerceIn(minFor(VolumeStreamType.VOICE_CALL), 100)
      } else {
        null
      },
      overrideAccessibilityPercent = if (overrideAccessibilityEnabled) {
        accessibilityPercent.coerceIn(minFor(VolumeStreamType.ACCESSIBILITY), 100)
      } else {
        null
      },
    )
  }

  fun notifyAutoSaveIfEditing() {
    if (isEditing) {
      onAutoSaveProfile(buildProfile())
    }
  }

  fun editorPreviewKindFor(type: VolumeStreamType): VolumePreviewKind? =
    activeVolumePreview
      ?.takeIf { it.scope == VolumePreviewScope.PROFILE_EDITOR && it.streamType == type }
      ?.kind

  fun requestEditorTapPreview(type: VolumeStreamType, targetPercent: Int) {
    val isCurrentlyPlayingThis = activeVolumePreview?.scope == VolumePreviewScope.PROFILE_EDITOR &&
      activeVolumePreview.streamType == type
    if (isCurrentlyPlayingThis) {
      onStopEditorStreamPreview()
    } else if (defaultSoundState.audioMode == BaseAudioMode.SILENT && !silentPreviewAuthorizedInSession) {
      pendingSilentPreviewRequest = type to targetPercent
    } else {
      onToggleEditorStreamPreview(type, targetPercent)
    }
  }

  fun requestEditorHoldPreview(type: VolumeStreamType, targetPercent: Int) {
    if (defaultSoundState.audioMode == BaseAudioMode.SILENT && !silentPreviewAuthorizedInSession) {
      pendingSilentPreviewRequest = type to targetPercent
    } else {
      onStartEditorStreamLoop(type, targetPercent)
    }
  }

  if (pendingSilentPreviewRequest != null) {
    SilentPreviewConfirmDialog(
      onKeepMuted = { pendingSilentPreviewRequest = null },
      onListenNow = {
        val request = pendingSilentPreviewRequest
        pendingSilentPreviewRequest = null
        silentPreviewAuthorizedInSession = true
        if (request != null) {
          onToggleEditorStreamPreview(request.first, request.second)
        }
      },
    )
  }

  Dialog(
    onDismissRequest = {
      onStopEditorStreamPreview()
      notifyAutoSaveIfEditing()
      onDismiss()
    },
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnClickOutside = false,
      dismissOnBackPress = true,
    ),
  ) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      topBar = {
        TopAppBar(
          navigationIcon = {
            IconButton(
              onClick = {
                onStopEditorStreamPreview()
                notifyAutoSaveIfEditing()
                onDismiss()
              },
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = AppLabels.Common.back,
              )
            }
          },
          title = {
            Column {
              Text(
                text = if (isEditing) {
                  AppLabels.Profiles.editProfileTitle
                } else {
                  AppLabels.Profiles.newProfileTitle
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (isEditing) {
                Text(
                  text = AppLabels.Common.autoSavedSubtitle,
                  style = MaterialTheme.typography.labelSmall,
                  color = activeAccent,
                )
              }
            }
          },
          actions = {
            if (isEditing && onDeleteProfile != null) {
              IconButton(
                onClick = {
                  onStopEditorStreamPreview()
                  onDeleteProfile(initialProfile.id)
                },
                modifier = Modifier.testTag(AppTestTags.Profiles.DELETE_TOP_BUTTON),
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = AppLabels.Profiles.deleteProfileMenu,
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            } else if (!isEditing) {
              TextButton(
                onClick = {
                  onStopEditorStreamPreview()
                  onSaveProfile(buildProfile(), false)
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag(AppTestTags.Profiles.CONFIRM_SAVE_BUTTON),
              ) {
                Text(AppLabels.Common.create, fontWeight = FontWeight.Bold)
              }
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          ),
        )
      },
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Seletor de ícone ZENITH com halo semântico
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          FlowRow(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            iconOptions.forEach { (key, label) ->
              val selected = iconKey == key
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .clickable {
                    iconKey = key
                    notifyAutoSaveIfEditing()
                  }
                  .padding(6.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                      if (selected) {
                        activeAccent
                      } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                      },
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = profileIconForKey(key),
                    contentDescription = label,
                    tint = if (selected) {
                      zenithOnActivePillColor()
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp),
                  )
                }
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                  softWrap = false,
                  color = if (selected) {
                    activeAccent
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            notifyAutoSaveIfEditing()
          },
          label = { Text(AppLabels.Profiles.profileNameLabel) },
          singleLine = true,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag(AppTestTags.Profiles.NAME_INPUT),
        )

        // Seletor Split Capsule 2 + 1 ZENITH ([Som | Silêncio] + [Vibrar])
        Text(
          text = AppLabels.Profiles.baseSoundModeSectionTitle,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ZenithSplitModeAndVibrateRow(
          audioMode = selectedAudioMode,
          vibrateEnabled = selectedVibrateEnabled,
          onSelectAudioMode = { mode ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            selectedAudioMode = mode
            notifyAutoSaveIfEditing()
          },
          onToggleVibrate = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            selectedVibrateEnabled = !selectedVibrateEnabled
            notifyAutoSaveIfEditing()
          },
          soundButtonTag = AppTestTags.Profiles.EDITOR_AUDIO_SOUND_BUTTON,
          silentButtonTag = AppTestTags.Profiles.EDITOR_AUDIO_SILENT_BUTTON,
          vibrateButtonTag = AppTestTags.Profiles.EDITOR_VIBRATE_TOGGLE,
        )

        // Cápsulas dos 4 Canais do Perfil
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = AppLabels.Profiles.channelsVolumeSectionTitle,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PillVolumeSliderRow(
              type = VolumeStreamType.MEDIA,
              icon = Icons.Default.MusicNote,
              percent = mediaPercent,
              minPercent = minFor(VolumeStreamType.MEDIA),
              accentColor = activeAccent,
              activePreviewKind = editorPreviewKindFor(VolumeStreamType.MEDIA),
              onSingleTapPreview = { requestEditorTapPreview(VolumeStreamType.MEDIA, it) },
              onDoubleTapPreview = { requestEditorTapPreview(VolumeStreamType.MEDIA, it) },
              onStartHoldPreview = { requestEditorHoldPreview(VolumeStreamType.MEDIA, it) },
              onHoldLivePercentChange = {
                mediaPercent = it
                onUpdateEditorStreamLoopPercent(VolumeStreamType.MEDIA, it)
              },
              onStopHoldPreview = { _, _ -> onStopEditorStreamPreview() },
              capsuleTestTag = AppTestTags.Profiles.editorStreamCapsule(VolumeStreamType.MEDIA),
              sliderTestTag = AppTestTags.Profiles.editorStreamSlider(VolumeStreamType.MEDIA),
              iconButtonTestTag = AppTestTags.Profiles.editorStreamIconButton(VolumeStreamType.MEDIA),
              playCueTestTag = AppTestTags.Profiles.editorStreamPlayCue(VolumeStreamType.MEDIA),
              previewActiveBadgeTestTag = AppTestTags.Profiles.editorStreamPreviewActiveBadge(VolumeStreamType.MEDIA),
              onValueChangeFinished = {
                mediaPercent = it
                notifyAutoSaveIfEditing()
              },
            )
            PillVolumeSliderRow(
              type = VolumeStreamType.RING,
              icon = Icons.Default.RingVolume,
              percent = ringPercent,
              minPercent = minFor(VolumeStreamType.RING),
              accentColor = activeAccent,
              isLinked = editorRingNotifLinked,
              onToggleLink = {
                val nextLinked = !editorRingNotifLinked
                editorRingNotifLinked = nextLinked
                editorLinkedDragPreview = null
                if (nextLinked) {
                  notifPercent = ringPercent
                }
                notifyAutoSaveIfEditing()
              },
              activePreviewKind = editorPreviewKindFor(VolumeStreamType.RING),
              onSingleTapPreview = { requestEditorTapPreview(VolumeStreamType.RING, it) },
              onDoubleTapPreview = { requestEditorTapPreview(VolumeStreamType.RING, it) },
              onStartHoldPreview = { requestEditorHoldPreview(VolumeStreamType.RING, it) },
              onHoldLivePercentChange = {
                ringPercent = it
                if (editorRingNotifLinked) {
                  notifPercent = it
                }
                onUpdateEditorStreamLoopPercent(VolumeStreamType.RING, it)
              },
              onStopHoldPreview = { _, _ -> onStopEditorStreamPreview() },
              externalPreviewPercent = if (editorRingNotifLinked) editorLinkedDragPreview else null,
              onLivePreviewChange = { preview ->
                editorLinkedDragPreview = if (editorRingNotifLinked) preview else null
              },
              capsuleTestTag = AppTestTags.Profiles.editorStreamCapsule(VolumeStreamType.RING),
              sliderTestTag = AppTestTags.Profiles.editorStreamSlider(VolumeStreamType.RING),
              iconButtonTestTag = AppTestTags.Profiles.editorStreamIconButton(VolumeStreamType.RING),
              playCueTestTag = AppTestTags.Profiles.editorStreamPlayCue(VolumeStreamType.RING),
              previewActiveBadgeTestTag = AppTestTags.Profiles.editorStreamPreviewActiveBadge(VolumeStreamType.RING),
              linkButtonTestTag = AppTestTags.Profiles.editorStreamLinkButton(VolumeStreamType.RING),
              onValueChangeFinished = {
                ringPercent = it
                if (editorRingNotifLinked) {
                  notifPercent = it
                }
                notifyAutoSaveIfEditing()
              },
            )
            PillVolumeSliderRow(
              type = VolumeStreamType.NOTIFICATION,
              icon = Icons.Default.Notifications,
              percent = notifPercent,
              minPercent = minFor(VolumeStreamType.NOTIFICATION),
              accentColor = activeAccent,
              isLinked = editorRingNotifLinked,
              onToggleLink = {
                val nextLinked = !editorRingNotifLinked
                editorRingNotifLinked = nextLinked
                editorLinkedDragPreview = null
                if (nextLinked) {
                  notifPercent = ringPercent
                }
                notifyAutoSaveIfEditing()
              },
              activePreviewKind = editorPreviewKindFor(VolumeStreamType.NOTIFICATION),
              onSingleTapPreview = { requestEditorTapPreview(VolumeStreamType.NOTIFICATION, it) },
              onDoubleTapPreview = { requestEditorTapPreview(VolumeStreamType.NOTIFICATION, it) },
              onStartHoldPreview = { requestEditorHoldPreview(VolumeStreamType.NOTIFICATION, it) },
              onHoldLivePercentChange = {
                notifPercent = it
                if (editorRingNotifLinked) {
                  ringPercent = it
                }
                onUpdateEditorStreamLoopPercent(VolumeStreamType.NOTIFICATION, it)
              },
              onStopHoldPreview = { _, _ -> onStopEditorStreamPreview() },
              externalPreviewPercent = if (editorRingNotifLinked) editorLinkedDragPreview else null,
              onLivePreviewChange = { preview ->
                editorLinkedDragPreview = if (editorRingNotifLinked) preview else null
              },
              capsuleTestTag = AppTestTags.Profiles.editorStreamCapsule(VolumeStreamType.NOTIFICATION),
              sliderTestTag = AppTestTags.Profiles.editorStreamSlider(VolumeStreamType.NOTIFICATION),
              iconButtonTestTag = AppTestTags.Profiles.editorStreamIconButton(VolumeStreamType.NOTIFICATION),
              playCueTestTag = AppTestTags.Profiles.editorStreamPlayCue(VolumeStreamType.NOTIFICATION),
              previewActiveBadgeTestTag = AppTestTags.Profiles.editorStreamPreviewActiveBadge(VolumeStreamType.NOTIFICATION),
              linkButtonTestTag = AppTestTags.Profiles.editorStreamLinkButton(VolumeStreamType.NOTIFICATION),
              onValueChangeFinished = {
                notifPercent = it
                if (editorRingNotifLinked) {
                  ringPercent = it
                }
                notifyAutoSaveIfEditing()
              },
            )
            PillVolumeSliderRow(
              type = VolumeStreamType.ALARM,
              icon = Icons.Default.Alarm,
              percent = alarmPercent,
              minPercent = minFor(VolumeStreamType.ALARM),
              accentColor = activeAccent,
              activePreviewKind = editorPreviewKindFor(VolumeStreamType.ALARM),
              onSingleTapPreview = { requestEditorTapPreview(VolumeStreamType.ALARM, it) },
              onDoubleTapPreview = { requestEditorTapPreview(VolumeStreamType.ALARM, it) },
              onStartHoldPreview = { requestEditorHoldPreview(VolumeStreamType.ALARM, it) },
              onHoldLivePercentChange = {
                alarmPercent = it
                onUpdateEditorStreamLoopPercent(VolumeStreamType.ALARM, it)
              },
              onStopHoldPreview = { _, _ -> onStopEditorStreamPreview() },
              capsuleTestTag = AppTestTags.Profiles.editorStreamCapsule(VolumeStreamType.ALARM),
              sliderTestTag = AppTestTags.Profiles.editorStreamSlider(VolumeStreamType.ALARM),
              iconButtonTestTag = AppTestTags.Profiles.editorStreamIconButton(VolumeStreamType.ALARM),
              playCueTestTag = AppTestTags.Profiles.editorStreamPlayCue(VolumeStreamType.ALARM),
              previewActiveBadgeTestTag = AppTestTags.Profiles.editorStreamPreviewActiveBadge(VolumeStreamType.ALARM),
              onValueChangeFinished = {
                alarmPercent = it
                notifyAutoSaveIfEditing()
              },
            )

            // Gaveta de Canais Secundários Opcionais (Sistema, Chamada, Acessibilidade)
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { secondaryExpanded = !secondaryExpanded }
                .testTag(AppTestTags.Profiles.EDITOR_SECONDARY_DRAWER_TOGGLE),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = if (secondaryExpanded) {
                    AppLabels.Now.secondaryDrawerExpandedLabel
                  } else {
                    AppLabels.Now.secondaryDrawerCollapsedLabel(
                      VolumeStreamType.secondaryDefaultStreams,
                    )
                  },
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                )
                Icon(
                  imageVector = if (secondaryExpanded) {
                    Icons.Default.ExpandLess
                  } else {
                    Icons.Default.ExpandMore
                  },
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp),
                )
              }
            }

            AnimatedVisibility(
              visible = secondaryExpanded,
              enter = expandVertically() + fadeIn(),
              exit = shrinkVertically() + fadeOut(),
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                  text = AppLabels.Profiles.secondaryChannelsSectionSubtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ProfileOptionalSecondaryStreamControl(
                  type = VolumeStreamType.SYSTEM,
                  enabled = overrideSystemEnabled,
                  percent = systemPercent,
                  minPercent = minFor(VolumeStreamType.SYSTEM),
                  accentColor = activeAccent,
                  activePreviewKind = editorPreviewKindFor(VolumeStreamType.SYSTEM),
                  onRequestTapPreview = { requestEditorTapPreview(VolumeStreamType.SYSTEM, it) },
                  onRequestHoldPreview = { requestEditorHoldPreview(VolumeStreamType.SYSTEM, it) },
                  onHoldLivePercentChange = {
                    systemPercent = it
                    onUpdateEditorStreamLoopPercent(VolumeStreamType.SYSTEM, it)
                  },
                  onStopHoldPreview = onStopEditorStreamPreview,
                  onToggleEnabled = { enabled ->
                    overrideSystemEnabled = enabled
                    notifyAutoSaveIfEditing()
                  },
                  onPercentChanged = { next ->
                    systemPercent = next
                    notifyAutoSaveIfEditing()
                  },
                )
                ProfileOptionalSecondaryStreamControl(
                  type = VolumeStreamType.VOICE_CALL,
                  enabled = overrideVoiceCallEnabled,
                  percent = voiceCallPercent,
                  minPercent = minFor(VolumeStreamType.VOICE_CALL),
                  accentColor = activeAccent,
                  activePreviewKind = editorPreviewKindFor(VolumeStreamType.VOICE_CALL),
                  onRequestTapPreview = { requestEditorTapPreview(VolumeStreamType.VOICE_CALL, it) },
                  onRequestHoldPreview = { requestEditorHoldPreview(VolumeStreamType.VOICE_CALL, it) },
                  onHoldLivePercentChange = {
                    voiceCallPercent = it
                    onUpdateEditorStreamLoopPercent(VolumeStreamType.VOICE_CALL, it)
                  },
                  onStopHoldPreview = onStopEditorStreamPreview,
                  onToggleEnabled = { enabled ->
                    overrideVoiceCallEnabled = enabled
                    notifyAutoSaveIfEditing()
                  },
                  onPercentChanged = { next ->
                    voiceCallPercent = next
                    notifyAutoSaveIfEditing()
                  },
                )
                ProfileOptionalSecondaryStreamControl(
                  type = VolumeStreamType.ACCESSIBILITY,
                  enabled = overrideAccessibilityEnabled,
                  percent = accessibilityPercent,
                  minPercent = minFor(VolumeStreamType.ACCESSIBILITY),
                  accentColor = activeAccent,
                  activePreviewKind = editorPreviewKindFor(VolumeStreamType.ACCESSIBILITY),
                  onRequestTapPreview = { requestEditorTapPreview(VolumeStreamType.ACCESSIBILITY, it) },
                  onRequestHoldPreview = { requestEditorHoldPreview(VolumeStreamType.ACCESSIBILITY, it) },
                  onHoldLivePercentChange = {
                    accessibilityPercent = it
                    onUpdateEditorStreamLoopPercent(VolumeStreamType.ACCESSIBILITY, it)
                  },
                  onStopHoldPreview = onStopEditorStreamPreview,
                  onToggleEnabled = { enabled ->
                    overrideAccessibilityEnabled = enabled
                    notifyAutoSaveIfEditing()
                  },
                  onPercentChanged = { next ->
                    accessibilityPercent = next
                    notifyAutoSaveIfEditing()
                  },
                )
              }
            }
          }
        }

        // Proteções de Mídia e Bluetooth em Cartão ZENITH
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = AppLabels.Profiles.preserveMediaTitle,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                  .weight(1f)
                  .padding(end = 12.dp),
              )
              M3CheckSwitch(
                checked = preserveMedia,
                onCheckedChange = {
                  preserveMedia = it
                  notifyAutoSaveIfEditing()
                },
              )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = AppLabels.Profiles.skipBluetoothTitle,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                  .weight(1f)
                  .padding(end = 12.dp),
              )
              M3CheckSwitch(
                checked = skipBluetooth,
                onCheckedChange = {
                  skipBluetooth = it
                  notifyAutoSaveIfEditing()
                },
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (!isEditing) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FilledTonalButton(
              onClick = {
                onStopEditorStreamPreview()
                onSaveProfile(buildProfile(), false)
              },
              enabled = name.trim().isNotEmpty(),
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier
                .weight(1f)
                .height(52.dp),
            ) {
              Text(AppLabels.Profiles.createOnlyButton, fontWeight = FontWeight.SemiBold)
            }
            Button(
              onClick = {
                onStopEditorStreamPreview()
                onSaveProfile(buildProfile(), true)
              },
              enabled = name.trim().isNotEmpty(),
              shape = RoundedCornerShape(18.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = activeAccent,
                contentColor = zenithOnActivePillColor(),
              ),
              modifier = Modifier
                .weight(1f)
                .height(52.dp),
            ) {
              Text(AppLabels.Profiles.saveAndActivateButton, fontWeight = FontWeight.Bold)
            }
          }
        } else {
          Button(
            onClick = {
              onStopEditorStreamPreview()
              onSaveProfile(buildProfile(), true)
            },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = activeAccent,
              contentColor = zenithOnActivePillColor(),
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
          ) {
            Text(AppLabels.Profiles.applyNowButton, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileOptionalSecondaryStreamControl(
  type: VolumeStreamType,
  enabled: Boolean,
  percent: Int,
  minPercent: Int,
  accentColor: Color,
  activePreviewKind: VolumePreviewKind? = null,
  onRequestTapPreview: (Int) -> Unit = {},
  onRequestHoldPreview: (Int) -> Unit = {},
  onHoldLivePercentChange: (Int) -> Unit = {},
  onStopHoldPreview: () -> Unit = {},
  onToggleEnabled: (Boolean) -> Unit,
  onPercentChanged: (Int) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surfaceContainer,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier
            .weight(1f)
            .padding(end = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = type.streamIcon(),
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(15.dp),
            )
          }
          Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
              text = type.label,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = if (enabled) {
                type.microDescription
              } else {
                "${AppLabels.Profiles.keepDeviceVolumeLabel} • ${type.microDescription}"
              },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        M3CheckSwitch(
          checked = enabled,
          onCheckedChange = onToggleEnabled,
          modifier = Modifier.testTag(AppTestTags.Profiles.editorSecondaryOverrideSwitch(type)),
        )
      }
    }

    if (enabled) {
      PillVolumeSliderRow(
        type = type,
        percent = percent,
        minPercent = minPercent,
        accentColor = accentColor,
        activePreviewKind = activePreviewKind,
        onSingleTapPreview = onRequestTapPreview,
        onDoubleTapPreview = onRequestTapPreview,
        onStartHoldPreview = onRequestHoldPreview,
        onHoldLivePercentChange = onHoldLivePercentChange,
        onStopHoldPreview = { _, _ -> onStopHoldPreview() },
        capsuleTestTag = AppTestTags.Profiles.editorStreamCapsule(type),
        sliderTestTag = AppTestTags.Profiles.editorStreamSlider(type),
        iconButtonTestTag = AppTestTags.Profiles.editorStreamIconButton(type),
        playCueTestTag = AppTestTags.Profiles.editorStreamPlayCue(type),
        previewActiveBadgeTestTag = AppTestTags.Profiles.editorStreamPreviewActiveBadge(type),
        onValueChangeFinished = onPercentChanged,
      )
    }
  }
}

/**
 * Modal de Prominent Disclosure exigido pela política da Google Play Store no estilo ZENITH.
 */
@Composable
fun ProminentLocationDisclosureDialog(
  onAcceptAndContinue: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    icon = {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp),
        )
      }
    },
    title = {
      Text(
        text = AppLabels.Disclosure.title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = AppLabels.Disclosure.body,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = SoundMode.NORMAL.tonalContainerColor(),
          border = BorderStroke(1.dp, SoundMode.NORMAL.tonalAccentColor().copy(alpha = 0.35f)),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = SoundMode.NORMAL.tonalAccentColor(),
            )
            Text(
              text = AppLabels.Disclosure.privacyHighlight,
              style = MaterialTheme.typography.bodySmall,
              color = SoundMode.NORMAL.onTonalContainerColor(),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onAcceptAndContinue,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag(AppTestTags.Locations.ACCEPT_DISCLOSURE_BUTTON),
      ) {
        Text(AppLabels.Disclosure.acceptButton, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(AppLabels.Disclosure.dismissButton)
      }
    },
  )
}

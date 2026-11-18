package com.managesoundmode.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.managesoundmode.app.MainActivity
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.R
import com.managesoundmode.app.data.local.PersistedSoundPrefs
import com.managesoundmode.app.domain.model.AppPermission
import com.managesoundmode.app.domain.model.BaseAudioMode
import com.managesoundmode.app.domain.model.DeviceSoundSnapshot
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.SoundState
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppLanguage
import kotlinx.coroutines.flow.first

class SoundModeWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = SoundModeGlanceWidget()
}

class SoundModeWidgetActionCallback : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    val app = context.applicationContext as? ManageSoundApplication ?: return
    val repo = app.container.soundModeRepository
    val prefs = repo.preferencesStream.first()
    if (!prefs.accessControlConfig.hasPermission(AppPermission.CHANGE_SOUND_AND_TIMER)) {
      return
    }

    val command = parameters[KEY_COMMAND] ?: return
    when (command) {
      CMD_SET_AUDIO_MODE -> {
        val rawAudioMode = parameters[KEY_AUDIO_MODE] ?: return
        val audioMode = runCatching { BaseAudioMode.valueOf(rawAudioMode) }.getOrNull() ?: return
        repo.setBaseAudioMode(audioMode, source = TransitionTriggerSource.WIDGET)
      }
      CMD_TOGGLE_VIBRATE -> {
        repo.toggleVibrate(source = TransitionTriggerSource.WIDGET)
      }
      CMD_SET_MODE -> {
        val rawMode = parameters[KEY_MODE] ?: return
        val mode = runCatching { SoundMode.valueOf(rawMode) }.getOrNull() ?: return
        repo.setSoundMode(mode, source = TransitionTriggerSource.WIDGET)
      }
      CMD_CYCLE_MODE -> {
        repo.cycleSoundModeFromQuickTile()
      }
      CMD_CYCLE_PROFILE -> {
        val profiles = repo.profilesStream.first()
        if (profiles.isNotEmpty()) {
          val currentId = repo.preferencesStream.first().activeProfileId
          val currentIndex = profiles.indexOfFirst { it.id == currentId }
          val nextProfile = profiles[(currentIndex + 1) % profiles.size]
          repo.activateProfile(nextProfile, source = TransitionTriggerSource.WIDGET)
        }
      }
    }

    SoundModeGlanceWidget().updateAll(context)
  }

  companion object {
    val KEY_COMMAND = ActionParameters.Key<String>("widget_cmd")
    val KEY_MODE = ActionParameters.Key<String>("widget_mode")
    val KEY_AUDIO_MODE = ActionParameters.Key<String>("widget_audio_mode")

    const val CMD_SET_MODE = "set_mode"
    const val CMD_SET_AUDIO_MODE = "set_audio_mode"
    const val CMD_TOGGLE_VIBRATE = "toggle_vibrate"
    const val CMD_CYCLE_MODE = "cycle_mode"
    const val CMD_CYCLE_PROFILE = "cycle_profile"
  }
}

private data class WidgetPalette(
  val outerSurface: ColorProvider,
  val bannerBg: ColorProvider,
  val bannerTitle: ColorProvider,
  val bannerSubtitle: ColorProvider,
  val badgeBg: ColorProvider,
  val badgeContent: ColorProvider,
  val capsuleContainerBg: ColorProvider,
  val soundActiveBg: ColorProvider,
  val silentActiveBg: ColorProvider,
  val vibrateActiveBg: ColorProvider,
  val activeButtonContent: ColorProvider,
  val inactiveButtonContent: ColorProvider,
)

class SoundModeGlanceWidget : GlanceAppWidget() {

  override val sizeMode: SizeMode = SizeMode.Responsive(
    setOf(
      COMPACT_BAR,
      HERO_CARD_STANDARD,
      HERO_CARD_LARGE,
    ),
  )

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val app = context.applicationContext as? ManageSoundApplication
    val repo = app?.container?.soundModeRepository
    val controller = app?.container?.soundSystemController

    val initialState = controller?.readSoundState() ?: SoundState.DEFAULT
    val initialStreams = controller?.readVolumeStreams() ?: VolumeStreams()
    val initialPrefs = repo?.preferencesStream?.first() ?: PersistedSoundPrefs()
    val initialProfiles = repo?.profilesStream?.first() ?: SoundProfile.defaultPresets
    val initialSnapshot = DeviceSoundSnapshot(
      currentMode = initialState.toLegacyApproximation(),
      currentSoundState = initialState,
      streams = initialStreams,
      hasDndPermission = controller?.hasDndPermission() ?: true,
      canWriteSystemSettings = controller?.canWriteSystemSettings() ?: true,
      activeProfileId = initialPrefs.activeProfileId,
      activeTimer = initialPrefs.activeTimer,
    )

    provideContent {
      GlanceTheme {
        val snapshot by if (repo != null) {
          repo.snapshotStream.collectAsState(initial = initialSnapshot)
        } else {
          androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialSnapshot) }
        }
        val prefs by if (repo != null) {
          repo.preferencesStream.collectAsState(initial = initialPrefs)
        } else {
          androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialPrefs) }
        }
        val profiles by if (repo != null) {
          repo.profilesStream.collectAsState(initial = initialProfiles)
        } else {
          androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialProfiles) }
        }

        val resolvedLang = runCatching { AppLanguage.valueOf(prefs.appLanguageCode) }
          .getOrDefault(AppLanguage.PT_BR)
        if (AppLabels.currentLanguage != resolvedLang) {
          AppLabels.currentLanguage = resolvedLang
        }

        val activeProfile = profiles.firstOrNull { it.id == snapshot.activeProfileId }
        val remainingMinutes = prefs.activeTimer?.remainingMinutes(System.currentTimeMillis())
        val isDark = resolveDarkTheme(context, prefs.themeMode)
        val palette = rememberWidgetPalette(snapshot.currentSoundState, isDark)

        PureHeroWidgetLayout(
          context = context,
          size = LocalSize.current,
          soundState = snapshot.currentSoundState,
          streams = snapshot.streams,
          activeProfile = activeProfile,
          remainingTimerMinutes = remainingMinutes?.takeIf { it > 0 },
          palette = palette,
        )
      }
    }
  }

  @Composable
  private fun PureHeroWidgetLayout(
    context: Context,
    size: DpSize,
    soundState: SoundState,
    streams: VolumeStreams,
    activeProfile: SoundProfile?,
    remainingTimerMinutes: Int?,
    palette: WidgetPalette,
  ) {
    if (size.height < 105.dp) {
      CompactSingleRowHeroWidget(
        context = context,
        soundState = soundState,
        streams = streams,
        activeProfile = activeProfile,
        palette = palette,
      )
      return
    }

    Column(
      modifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(28.dp)
        .background(palette.outerSurface)
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      HeroTonalBanner(
        context = context,
        soundState = soundState,
        streams = streams,
        activeProfile = activeProfile,
        remainingTimerMinutes = remainingTimerMinutes,
        palette = palette,
      )

      Spacer(modifier = GlanceModifier.height(10.dp))

      // Split Capsule 2 + 1: [Som | Silêncio] + [Vibrar] independente
      HeroModesSelectorRow(
        soundState = soundState,
        palette = palette,
      )
    }
  }

  @Composable
  private fun HeroTonalBanner(
    context: Context,
    soundState: SoundState,
    streams: VolumeStreams,
    activeProfile: SoundProfile?,
    remainingTimerMinutes: Int?,
    palette: WidgetPalette,
  ) {
    val cleanSubtitle = AppLabels.Widget.heroSubtitle(
      state = soundState,
      streams = streams,
      remainingTimerMinutes = remainingTimerMinutes,
    )

    Row(
      modifier = GlanceModifier
        .fillMaxWidth()
        .cornerRadius(22.dp)
        .background(palette.bannerBg)
        .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
        .padding(horizontal = 14.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = GlanceModifier
          .size(44.dp)
          .cornerRadius(22.dp)
          .background(palette.badgeBg),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          provider = ImageProvider(audioModeDrawableRes(soundState.audioMode)),
          contentDescription = soundState.title,
          colorFilter = ColorFilter.tint(palette.badgeContent),
          modifier = GlanceModifier.size(22.dp),
        )
      }

      Spacer(modifier = GlanceModifier.width(12.dp))

      Column(modifier = GlanceModifier.defaultWeight()) {
        Text(
          text = soundState.title,
          style = TextStyle(
            color = palette.bannerTitle,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
          ),
          maxLines = 1,
        )
        Text(
          text = cleanSubtitle,
          style = TextStyle(
            color = palette.bannerSubtitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
          ),
          maxLines = 1,
        )
      }

      Spacer(modifier = GlanceModifier.width(8.dp))

      Box(
        modifier = GlanceModifier
          .cornerRadius(16.dp)
          .background(palette.badgeBg)
          .clickable(
            actionRunCallback<SoundModeWidgetActionCallback>(
              actionParametersOf(
                SoundModeWidgetActionCallback.KEY_COMMAND to SoundModeWidgetActionCallback.CMD_CYCLE_PROFILE,
              ),
            ),
          )
          .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Image(
            provider = ImageProvider(R.drawable.ic_widget_style),
            contentDescription = null,
            colorFilter = ColorFilter.tint(palette.badgeContent),
            modifier = GlanceModifier.size(14.dp),
          )
          Spacer(modifier = GlanceModifier.width(5.dp))
          Text(
            text = activeProfile?.name ?: AppLabels.Widget.profilesFallback,
            style = TextStyle(
              color = palette.badgeContent,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
          )
        }
      }
    }
  }

  /**
   * Split Capsule `2 + 1` para o Widget Hero 4x2:
   * - Cápsula esquerda (`defaultWeight()`): Segmented exclusivo `[ Som | Silêncio ]`
   * - Cápsula direita (`98.dp`): Toggle ortogonal independente `[ Vibrar ]`
   */
  @Composable
  private fun HeroModesSelectorRow(
    soundState: SoundState,
    palette: WidgetPalette,
  ) {
    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Cápsula Esquerda (2/3): [ Som | Silêncio ]
      Row(
        modifier = GlanceModifier
          .defaultWeight()
          .height(50.dp)
          .cornerRadius(24.dp)
          .background(palette.capsuleContainerBg)
          .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        HeroAudioModeButton(
          modifier = GlanceModifier.defaultWeight(),
          label = AppLabels.Now.audioModeShortPill(BaseAudioMode.SOUND),
          iconRes = R.drawable.ic_widget_volume_up,
          selected = soundState.audioMode == BaseAudioMode.SOUND,
          activeBg = palette.soundActiveBg,
          targetAudioMode = BaseAudioMode.SOUND,
          palette = palette,
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        HeroAudioModeButton(
          modifier = GlanceModifier.defaultWeight(),
          label = AppLabels.Now.audioModeShortPill(BaseAudioMode.SILENT),
          iconRes = R.drawable.ic_widget_silent,
          selected = soundState.audioMode == BaseAudioMode.SILENT,
          activeBg = palette.silentActiveBg,
          targetAudioMode = BaseAudioMode.SILENT,
          palette = palette,
        )
      }

      Spacer(modifier = GlanceModifier.width(8.dp))

      // Cápsula Direita (1/3): Toggle independente [ Vibrar ]
      val vibBg = if (soundState.vibrateEnabled) {
        palette.vibrateActiveBg
      } else {
        palette.capsuleContainerBg
      }
      val vibContent = if (soundState.vibrateEnabled) {
        palette.activeButtonContent
      } else {
        palette.inactiveButtonContent
      }

      Box(
        modifier = GlanceModifier
          .width(98.dp)
          .height(50.dp)
          .cornerRadius(24.dp)
          .background(vibBg)
          .clickable(
            actionRunCallback<SoundModeWidgetActionCallback>(
              actionParametersOf(
                SoundModeWidgetActionCallback.KEY_COMMAND to SoundModeWidgetActionCallback.CMD_TOGGLE_VIBRATE,
              ),
            ),
          )
          .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Image(
            provider = ImageProvider(R.drawable.ic_widget_vibration),
            contentDescription = AppLabels.Now.vibrateToggleContentDescription(soundState.vibrateEnabled),
            colorFilter = ColorFilter.tint(vibContent),
            modifier = GlanceModifier.size(17.dp),
          )
          Spacer(modifier = GlanceModifier.width(5.dp))
          Text(
            text = AppLabels.Now.vibrateShortPill,
            style = TextStyle(
              color = vibContent,
              fontSize = 13.sp,
              fontWeight = if (soundState.vibrateEnabled) FontWeight.Bold else FontWeight.Medium,
            ),
            maxLines = 1,
          )
        }
      }
    }
  }

  @Composable
  private fun HeroAudioModeButton(
    modifier: GlanceModifier,
    label: String,
    iconRes: Int,
    selected: Boolean,
    activeBg: ColorProvider,
    targetAudioMode: BaseAudioMode,
    palette: WidgetPalette,
  ) {
    val bgColor = if (selected) activeBg else palette.capsuleContainerBg
    val contentColor = if (selected) palette.activeButtonContent else palette.inactiveButtonContent

    Box(
      modifier = modifier
        .height(42.dp)
        .cornerRadius(19.dp)
        .background(bgColor)
        .clickable(
          actionRunCallback<SoundModeWidgetActionCallback>(
            actionParametersOf(
              SoundModeWidgetActionCallback.KEY_COMMAND to SoundModeWidgetActionCallback.CMD_SET_AUDIO_MODE,
              SoundModeWidgetActionCallback.KEY_AUDIO_MODE to targetAudioMode.name,
            ),
          ),
        )
        .padding(horizontal = 8.dp),
      contentAlignment = Alignment.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
          provider = ImageProvider(iconRes),
          contentDescription = null,
          colorFilter = ColorFilter.tint(contentColor),
          modifier = GlanceModifier.size(17.dp),
        )
        Spacer(modifier = GlanceModifier.width(5.dp))
        Text(
          text = label,
          style = TextStyle(
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          ),
          maxLines = 1,
        )
      }
    }
  }

  /**
   * Widget Compacto (`2x1` / `4x1`) com Split Capsule `2 + 1` de ícones vetoriais.
   */
  @Composable
  private fun CompactSingleRowHeroWidget(
    context: Context,
    soundState: SoundState,
    streams: VolumeStreams,
    activeProfile: SoundProfile?,
    palette: WidgetPalette,
  ) {
    Row(
      modifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(26.dp)
        .background(palette.outerSurface)
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = GlanceModifier
          .defaultWeight()
          .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = GlanceModifier
            .size(38.dp)
            .cornerRadius(19.dp)
            .background(palette.bannerBg),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            provider = ImageProvider(audioModeDrawableRes(soundState.audioMode)),
            contentDescription = soundState.title,
            colorFilter = ColorFilter.tint(palette.badgeContent),
            modifier = GlanceModifier.size(20.dp),
          )
        }
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column {
          Text(
            text = soundState.title,
            style = TextStyle(
              color = palette.bannerTitle,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
          )
          Text(
            text = AppLabels.Widget.compactSubtitle(activeProfile?.name, streams.mediaPercent),
            style = TextStyle(
              color = palette.inactiveButtonContent,
              fontSize = 11.sp,
            ),
            maxLines = 1,
          )
        }
      }

      // Segmented Audio Pill [Som | Silêncio]
      Row(
        modifier = GlanceModifier
          .cornerRadius(18.dp)
          .background(palette.capsuleContainerBg)
          .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CompactAudioIconPill(
          iconRes = R.drawable.ic_widget_volume_up,
          contentDescription = BaseAudioMode.SOUND.title,
          selected = soundState.audioMode == BaseAudioMode.SOUND,
          activeBg = palette.soundActiveBg,
          targetAudioMode = BaseAudioMode.SOUND,
          palette = palette,
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        CompactAudioIconPill(
          iconRes = R.drawable.ic_widget_silent,
          contentDescription = BaseAudioMode.SILENT.title,
          selected = soundState.audioMode == BaseAudioMode.SILENT,
          activeBg = palette.silentActiveBg,
          targetAudioMode = BaseAudioMode.SILENT,
          palette = palette,
        )
      }

      Spacer(modifier = GlanceModifier.width(6.dp))

      // Orthogonal Vibrate Toggle Pill [Vibrar]
      val vibBg = if (soundState.vibrateEnabled) {
        palette.vibrateActiveBg
      } else {
        palette.capsuleContainerBg
      }
      val vibTint = if (soundState.vibrateEnabled) {
        palette.activeButtonContent
      } else {
        palette.inactiveButtonContent
      }
      Box(
        modifier = GlanceModifier
          .size(42.dp)
          .cornerRadius(18.dp)
          .background(vibBg)
          .clickable(
            actionRunCallback<SoundModeWidgetActionCallback>(
              actionParametersOf(
                SoundModeWidgetActionCallback.KEY_COMMAND to SoundModeWidgetActionCallback.CMD_TOGGLE_VIBRATE,
              ),
            ),
          ),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          provider = ImageProvider(R.drawable.ic_widget_vibration),
          contentDescription = AppLabels.Now.vibrateToggleContentDescription(soundState.vibrateEnabled),
          colorFilter = ColorFilter.tint(vibTint),
          modifier = GlanceModifier.size(18.dp),
        )
      }
    }
  }

  @Composable
  private fun CompactAudioIconPill(
    iconRes: Int,
    contentDescription: String,
    selected: Boolean,
    activeBg: ColorProvider,
    targetAudioMode: BaseAudioMode,
    palette: WidgetPalette,
  ) {
    val bg = if (selected) activeBg else palette.capsuleContainerBg
    val tint = if (selected) palette.activeButtonContent else palette.inactiveButtonContent
    Box(
      modifier = GlanceModifier
        .size(36.dp)
        .cornerRadius(15.dp)
        .background(bg)
        .clickable(
          actionRunCallback<SoundModeWidgetActionCallback>(
            actionParametersOf(
              SoundModeWidgetActionCallback.KEY_COMMAND to SoundModeWidgetActionCallback.CMD_SET_AUDIO_MODE,
              SoundModeWidgetActionCallback.KEY_AUDIO_MODE to targetAudioMode.name,
            ),
          ),
        ),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        provider = ImageProvider(iconRes),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = GlanceModifier.size(17.dp),
      )
    }
  }

  private fun resolveDarkTheme(context: Context, themeMode: ThemeMode): Boolean {
    return when (themeMode) {
      ThemeMode.DARK -> true
      ThemeMode.LIGHT -> false
      ThemeMode.SYSTEM -> {
        val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        nightMask == Configuration.UI_MODE_NIGHT_YES
      }
    }
  }

  private fun rememberWidgetPalette(soundState: SoundState, isDark: Boolean): WidgetPalette {
    if (isDark) {
      // Zenith — Noite Boreal (Dark Mode)
      val (bannerBg, bannerTitle, bannerSub, badgeBg, badgeContent) = when (soundState.audioMode) {
        BaseAudioMode.SOUND -> Quintuple(
          Color(0xFF0E2926),
          Color(0xFFF0F4FC),
          Color(0xFF99F6E4),
          Color(0xFF133A36),
          Color(0xFF2DD4BF),
        )
        BaseAudioMode.SILENT -> Quintuple(
          Color(0xFF1D1536),
          Color(0xFFF0F4FC),
          Color(0xFFDDD6FE),
          Color(0xFF2B1F4F),
          Color(0xFFA78BFA),
        )
      }
      return WidgetPalette(
        outerSurface = ColorProvider(Color(0xFF070A10)),
        bannerBg = ColorProvider(bannerBg),
        bannerTitle = ColorProvider(bannerTitle),
        bannerSubtitle = ColorProvider(bannerSub),
        badgeBg = ColorProvider(badgeBg),
        badgeContent = ColorProvider(badgeContent),
        capsuleContainerBg = ColorProvider(Color(0xFF161F33)),
        soundActiveBg = ColorProvider(Color(0xFF2DD4BF)),
        silentActiveBg = ColorProvider(Color(0xFFA78BFA)),
        vibrateActiveBg = ColorProvider(Color(0xFFFBBF24)),
        activeButtonContent = ColorProvider(Color(0xFF070A10)),
        inactiveButtonContent = ColorProvider(Color(0xFF94A3B8)),
      )
    } else {
      // Zenith — Alvorada Nórdica (Light Mode)
      val (bannerBg, bannerTitle, bannerSub, badgeBg, badgeContent) = when (soundState.audioMode) {
        BaseAudioMode.SOUND -> Quintuple(
          Color(0xFFCCFBF1),
          Color(0xFF0B1C1A),
          Color(0xFF115E59),
          Color(0xFFFFFFFF),
          Color(0xFF0F766E),
        )
        BaseAudioMode.SILENT -> Quintuple(
          Color(0xFFEDE9FE),
          Color(0xFF1E1038),
          Color(0xFF5B21B6),
          Color(0xFFFFFFFF),
          Color(0xFF6D28D9),
        )
      }
      return WidgetPalette(
        outerSurface = ColorProvider(Color(0xFFF6F5F0)),
        bannerBg = ColorProvider(bannerBg),
        bannerTitle = ColorProvider(bannerTitle),
        bannerSubtitle = ColorProvider(bannerSub),
        badgeBg = ColorProvider(badgeBg),
        badgeContent = ColorProvider(badgeContent),
        capsuleContainerBg = ColorProvider(Color(0xFFEAE6DF)),
        soundActiveBg = ColorProvider(Color(0xFF0F766E)),
        silentActiveBg = ColorProvider(Color(0xFF6D28D9)),
        vibrateActiveBg = ColorProvider(Color(0xFFB45309)),
        activeButtonContent = ColorProvider(Color(0xFFFFFFFF)),
        inactiveButtonContent = ColorProvider(Color(0xFF475569)),
      )
    }
  }

  private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
  )

  private fun audioModeDrawableRes(audioMode: BaseAudioMode): Int = when (audioMode) {
    BaseAudioMode.SOUND -> R.drawable.ic_widget_volume_up
    BaseAudioMode.SILENT -> R.drawable.ic_widget_silent
  }

  companion object {
    private const val TAG = "SoundModeGlanceWidget"
    private val COMPACT_BAR = DpSize(180.dp, 72.dp)
    private val HERO_CARD_STANDARD = DpSize(260.dp, 130.dp)
    private val HERO_CARD_LARGE = DpSize(320.dp, 160.dp)

    suspend fun refreshAllWidgets(context: Context) {
      runCatching { SoundModeGlanceWidget().updateAll(context) }
        .onFailure { Log.w(TAG, "Falha ao atualizar widgets Glance", it) }
    }

    fun requestPinWidget(context: Context): Boolean {
      val appWidgetManager = context.getSystemService(AppWidgetManager::class.java) ?: return false
      if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
      val provider = ComponentName(context, SoundModeWidgetReceiver::class.java)
      return appWidgetManager.requestPinAppWidget(provider, null, null)
    }
  }
}

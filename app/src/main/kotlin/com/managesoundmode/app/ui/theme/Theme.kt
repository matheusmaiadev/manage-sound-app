package com.managesoundmode.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.ZenithAtmosphericPalette

private val ZenithEmeraldDarkScheme = darkColorScheme(
  primary = Color(0xFF34D399),
  onPrimary = Color(0xFF022C22),
  primaryContainer = Color(0xFF064E3B),
  onPrimaryContainer = Color(0xFFA7F3D0),
  secondary = Color(0xFF6EE7B7),
  onSecondary = Color(0xFF064E3B),
  secondaryContainer = Color(0xFF0D2E23),
  onSecondaryContainer = Color(0xFFA7F3D0),
  tertiary = ZenithVibrateAmberDark,
  onTertiary = Color(0xFF451A03),
  tertiaryContainer = ZenithVibrateContainerDark,
  onTertiaryContainer = ZenithVibrateOnContainerDark,
  error = Color(0xFFF87171),
  onError = Color(0xFF450A0A),
  errorContainer = Color(0xFF3B1219),
  onErrorContainer = Color(0xFFFECACA),
  background = Color(0xFF060E0B),
  onBackground = Color(0xFFF2FDF7),
  surface = Color(0xFF060E0B),
  onSurface = Color(0xFFF2FDF7),
  surfaceVariant = Color(0xFF13281E),
  onSurfaceVariant = Color(0xFF86A596),
  surfaceContainerLowest = Color(0xFF060E0B),
  surfaceContainerLow = Color(0xFF0D1A14),
  surfaceContainer = Color(0xFF102119),
  surfaceContainerHigh = Color(0xFF13281E),
  surfaceContainerHighest = Color(0xFF1B382B),
  outline = Color(0xFF234B3B),
  outlineVariant = Color(0xFF1B382B),
)

private val ZenithEmeraldLightScheme = lightColorScheme(
  primary = Color(0xFF047857),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD1FAE5),
  onPrimaryContainer = Color(0xFF065F46),
  secondary = Color(0xFF059669),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFA7F3D0),
  onSecondaryContainer = Color(0xFF047857),
  tertiary = ZenithVibrateAmberLight,
  onTertiary = Color.White,
  tertiaryContainer = ZenithVibrateContainerLight,
  onTertiaryContainer = ZenithVibrateOnContainerLight,
  error = Color(0xFFDC2626),
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF7F1D1D),
  background = Color(0xFFF2F7F4),
  onBackground = Color(0xFF052E16),
  surface = Color(0xFFF2F7F4),
  onSurface = Color(0xFF052E16),
  surfaceVariant = Color(0xFFDBE8E1),
  onSurfaceVariant = Color(0xFF386650),
  surfaceContainerLowest = Color(0xFFF2F7F4),
  surfaceContainerLow = Color(0xFFFFFFFF),
  surfaceContainer = Color(0xFFE5EFEA),
  surfaceContainerHigh = Color(0xFFDBE8E1),
  surfaceContainerHighest = Color(0xFFD1E1D8),
  outline = Color(0xFFB4CFC2),
  outlineVariant = Color(0xFFD1E1D8),
)

private val ZenithAbyssalDarkScheme = darkColorScheme(
  primary = Color(0xFF38BDF8),
  onPrimary = Color(0xFF082F49),
  primaryContainer = Color(0xFF0C4A6E),
  onPrimaryContainer = Color(0xFFBAE6FD),
  secondary = Color(0xFF7DD3FC),
  onSecondary = Color(0xFF0C4A6E),
  secondaryContainer = Color(0xFF0D253D),
  onSecondaryContainer = Color(0xFFBAE6FD),
  tertiary = ZenithVibrateAmberDark,
  onTertiary = Color(0xFF451A03),
  tertiaryContainer = ZenithVibrateContainerDark,
  onTertiaryContainer = ZenithVibrateOnContainerDark,
  error = Color(0xFFF87171),
  onError = Color(0xFF450A0A),
  errorContainer = Color(0xFF3B1219),
  onErrorContainer = Color(0xFFFECACA),
  background = Color(0xFF060B14),
  onBackground = Color(0xFFF0F9FF),
  surface = Color(0xFF060B14),
  onSurface = Color(0xFFF0F9FF),
  surfaceVariant = Color(0xFF142542),
  onSurfaceVariant = Color(0xFF859CB8),
  surfaceContainerLowest = Color(0xFF060B14),
  surfaceContainerLow = Color(0xFF0D1729),
  surfaceContainer = Color(0xFF101E36),
  surfaceContainerHigh = Color(0xFF142542),
  surfaceContainerHighest = Color(0xFF1D345C),
  outline = Color(0xFF28467A),
  outlineVariant = Color(0xFF1D345C),
)

private val ZenithAbyssalLightScheme = lightColorScheme(
  primary = Color(0xFF0369A1),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0F2FE),
  onPrimaryContainer = Color(0xFF075985),
  secondary = Color(0xFF0284C7),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFBAE6FD),
  onSecondaryContainer = Color(0xFF0369A1),
  tertiary = ZenithVibrateAmberLight,
  onTertiary = Color.White,
  tertiaryContainer = ZenithVibrateContainerLight,
  onTertiaryContainer = ZenithVibrateOnContainerLight,
  error = Color(0xFFDC2626),
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF7F1D1D),
  background = Color(0xFFF2F6FA),
  onBackground = Color(0xFF082F49),
  surface = Color(0xFFF2F6FA),
  onSurface = Color(0xFF082F49),
  surfaceVariant = Color(0xFFD9E4EF),
  onSurfaceVariant = Color(0xFF3A5875),
  surfaceContainerLowest = Color(0xFFF2F6FA),
  surfaceContainerLow = Color(0xFFFFFFFF),
  surfaceContainer = Color(0xFFE4ECF4),
  surfaceContainerHigh = Color(0xFFD9E4EF),
  surfaceContainerHighest = Color(0xFFCEDBEE),
  outline = Color(0xFFB4CDE4),
  outlineVariant = Color(0xFFCEDBEE),
)

private val ZenithBorealDarkScheme = darkColorScheme(
  primary = ZenithNormalTealDark,
  onPrimary = Color(0xFF042F2E),
  primaryContainer = ZenithNormalContainerDark,
  onPrimaryContainer = ZenithNormalOnContainerDark,
  secondary = ZenithSilentVioletDark,
  onSecondary = Color(0xFF1E1B4B),
  secondaryContainer = ZenithSilentContainerDark,
  onSecondaryContainer = ZenithSilentOnContainerDark,
  tertiary = ZenithVibrateAmberDark,
  onTertiary = Color(0xFF451A03),
  tertiaryContainer = ZenithVibrateContainerDark,
  onTertiaryContainer = ZenithVibrateOnContainerDark,
  error = Color(0xFFF87171),
  onError = Color(0xFF450A0A),
  errorContainer = Color(0xFF3B1219),
  onErrorContainer = Color(0xFFFECACA),
  background = ZenithBorealBg,
  onBackground = ZenithBorealTextPrimary,
  surface = ZenithBorealBg,
  onSurface = ZenithBorealTextPrimary,
  surfaceVariant = ZenithBorealElevated,
  onSurfaceVariant = ZenithBorealTextSecondary,
  surfaceContainerLowest = ZenithBorealBg,
  surfaceContainerLow = ZenithBorealCard,
  surfaceContainer = ZenithBorealNav,
  surfaceContainerHigh = ZenithBorealElevated,
  surfaceContainerHighest = ZenithBorealPillInactive,
  outline = Color(0xFF334155),
  outlineVariant = ZenithBorealBorder,
)

private val ZenithNordicLightScheme = lightColorScheme(
  primary = ZenithNormalTealLight,
  onPrimary = Color.White,
  primaryContainer = ZenithNormalContainerLight,
  onPrimaryContainer = ZenithNormalOnContainerLight,
  secondary = ZenithSilentVioletLight,
  onSecondary = Color.White,
  secondaryContainer = ZenithSilentContainerLight,
  onSecondaryContainer = ZenithSilentOnContainerLight,
  tertiary = ZenithVibrateAmberLight,
  onTertiary = Color.White,
  tertiaryContainer = ZenithVibrateContainerLight,
  onTertiaryContainer = ZenithVibrateOnContainerLight,
  error = Color(0xFFDC2626),
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF7F1D1D),
  background = ZenithNordicBg,
  onBackground = ZenithNordicTextPrimary,
  surface = ZenithNordicBg,
  onSurface = ZenithNordicTextPrimary,
  surfaceVariant = ZenithNordicElevated,
  onSurfaceVariant = ZenithNordicTextSecondary,
  surfaceContainerLowest = ZenithNordicBg,
  surfaceContainerLow = ZenithNordicCard,
  surfaceContainer = ZenithNordicNav,
  surfaceContainerHigh = ZenithNordicElevated,
  surfaceContainerHighest = ZenithNordicPillInactive,
  outline = Color(0xFFCBD5E1),
  outlineVariant = ZenithNordicBorder,
)

@Composable
fun ManageSoundModeTheme(
  themeMode: ThemeMode = ThemeMode.DARK,
  dynamicColor: Boolean = true,
  zenithPalette: ZenithAtmosphericPalette = ZenithAtmosphericPalette.BOREAL_CLASSIC,
  isProUnlocked: Boolean = false,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val isDark = when (themeMode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
  }

  val context = LocalContext.current
  val resolvedPalette = if (isProUnlocked || !zenithPalette.isPro) {
    zenithPalette
  } else {
    ZenithAtmosphericPalette.BOREAL_CLASSIC
  }

  val baseZenithScheme = when (resolvedPalette) {
    ZenithAtmosphericPalette.BOREAL_CLASSIC ->
      if (isDark) ZenithBorealDarkScheme else ZenithNordicLightScheme
    ZenithAtmosphericPalette.EMERALD_AURORA ->
      if (isDark) ZenithEmeraldDarkScheme else ZenithEmeraldLightScheme
    ZenithAtmosphericPalette.ABYSSAL_OCEAN ->
      if (isDark) ZenithAbyssalDarkScheme else ZenithAbyssalLightScheme
  }

  // Mantém a identidade atmosférica ZENITH ("Noite Boreal" / "Alvorada Nórdica") mesmo quando
  // Dynamic Color está ativo, ancorando todas as superfícies e acentos semânticos do Orbe.
  val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val dynamic = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    baseZenithScheme.copy(
      inversePrimary = dynamic.inversePrimary,
    )
  } else {
    baseZenithScheme
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window ?: return@SideEffect
      val insetsController = WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = !isDark
      insetsController.isAppearanceLightNavigationBars = !isDark
    }
  }

  CompositionLocalProvider(LocalZenithIsDark provides isDark) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content,
    )
  }
}

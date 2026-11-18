package com.managesoundmode.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ZenithAtmosphericPalette {
  BOREAL_CLASSIC,
  EMERALD_AURORA,
  ABYSSAL_OCEAN;

  val isPro: Boolean
    get() = this != BOREAL_CLASSIC
}

object FreemiumQuotas {
  const val PRO_LIFETIME_PRODUCT_ID = "modos_de_som_pro_lifetime"
  const val FREE_MAX_CUSTOM_PROFILES = 5
  const val FREE_MAX_SCHEDULE_RULES = 3
  const val FREE_MAX_LOCATION_RULES = 3
  const val FREE_MAX_CALENDAR_RULES = 1
}

enum class ProFeatureGate {
  UNLIMITED_PROFILES,
  UNLIMITED_SCHEDULES,
  UNLIMITED_LOCATIONS,
  UNLIMITED_CALENDAR_RULES,
  EXTRA_QUICK_TILE_SLOT_3,
  RBAC_AUTO_RELOCK,
  ZENITH_PRO_PALETTES,
}

sealed interface PurchaseResult {
  data object Purchased : PurchaseResult
  data object Pending : PurchaseResult
  data object UserCancelled : PurchaseResult
  data object AlreadyOwned : PurchaseResult
  data class Error(val billingResponseCode: Int, val debugMessage: String) : PurchaseResult
}

sealed interface RestorePurchasesResult {
  data object RestoredPro : RestorePurchasesResult
  data object NoActivePurchaseFound : RestorePurchasesResult
  data class Error(val debugMessage: String) : RestorePurchasesResult
}

data class ProEntitlementState(
  val isProUnlocked: Boolean = false,
  val isPurchasePending: Boolean = false,
  val formattedPrice: String? = null,
  val unlockedAtEpochMillis: Long? = null,
  val isBillingConnected: Boolean = false,
  val isProcessing: Boolean = false,
)

@Serializable
data class CachedProEntitlement(
  val isProUnlocked: Boolean = false,
  val unlockedAtEpochMillis: Long? = null,
  val purchaseTokenHash: String? = null,
  val hmacSignature: String? = null,
) {
  fun payloadToSign(): String =
    "$isProUnlocked|$unlockedAtEpochMillis|$purchaseTokenHash"
}

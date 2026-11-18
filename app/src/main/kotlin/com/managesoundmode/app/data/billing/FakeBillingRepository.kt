package com.managesoundmode.app.data.billing

import android.app.Activity
import com.managesoundmode.app.data.local.SoundPreferencesDataSource
import com.managesoundmode.app.domain.model.CachedProEntitlement
import com.managesoundmode.app.domain.model.ProEntitlementState
import com.managesoundmode.app.domain.model.PurchaseResult
import com.managesoundmode.app.domain.model.RestorePurchasesResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeBillingRepository(
  private val preferencesDataSource: SoundPreferencesDataSource? = null,
  initialUnlocked: Boolean = false,
) : BillingRepository {

  private val _entitlementFlow = MutableStateFlow(
    ProEntitlementState(
      isProUnlocked = initialUnlocked,
      formattedPrice = "R$ 19,90",
      isBillingConnected = true,
      unlockedAtEpochMillis = if (initialUnlocked) 1700000000000L else null,
    ),
  )
  override val entitlementFlow: StateFlow<ProEntitlementState> = _entitlementFlow.asStateFlow()

  var nextPurchaseResult: PurchaseResult = PurchaseResult.Purchased
  var nextRestoreResult: RestorePurchasesResult = RestorePurchasesResult.RestoredPro
  var simulateOffline: Boolean = false

  override suspend fun startConnection() {
    _entitlementFlow.update { it.copy(isBillingConnected = !simulateOffline) }
  }

  override suspend fun queryProductDetails(): Result<String?> =
    if (simulateOffline) {
      Result.failure(Exception("Offline"))
    } else {
      Result.success(_entitlementFlow.value.formattedPrice)
    }

  override suspend fun launchProPurchaseFlow(activity: Activity): PurchaseResult {
    if (simulateOffline) {
      return PurchaseResult.Error(-1, "Offline")
    }
    return when (val result = nextPurchaseResult) {
      is PurchaseResult.Purchased -> {
        setProUnlocked(true)
        result
      }
      is PurchaseResult.Pending -> {
        _entitlementFlow.update { it.copy(isPurchasePending = true) }
        result
      }
      is PurchaseResult.AlreadyOwned -> {
        setProUnlocked(true)
        result
      }
      else -> result
    }
  }

  override suspend fun restorePurchases(): RestorePurchasesResult {
    if (simulateOffline) {
      return RestorePurchasesResult.Error("Offline")
    }
    return when (val result = nextRestoreResult) {
      is RestorePurchasesResult.RestoredPro -> {
        setProUnlocked(true)
        result
      }
      is RestorePurchasesResult.NoActivePurchaseFound -> {
        setProUnlocked(false)
        result
      }
      else -> result
    }
  }

  suspend fun setProUnlocked(unlocked: Boolean) {
    val epoch = if (unlocked) System.currentTimeMillis() else null
    _entitlementFlow.update {
      it.copy(
        isProUnlocked = unlocked,
        isPurchasePending = false,
        unlockedAtEpochMillis = epoch,
      )
    }
    if (unlocked) {
      preferencesDataSource?.setCachedProEntitlement(
        CachedProEntitlement(
          isProUnlocked = true,
          unlockedAtEpochMillis = epoch,
          purchaseTokenHash = "test_purchase_token_hash",
          hmacSignature = "test_hmac_signature",
        ),
      )
    } else {
      preferencesDataSource?.setCachedProEntitlement(null)
    }
  }

  fun setFormattedPrice(price: String?) {
    _entitlementFlow.update { it.copy(formattedPrice = price) }
  }

  fun setPending(pending: Boolean) {
    _entitlementFlow.update { it.copy(isPurchasePending = pending) }
  }
}

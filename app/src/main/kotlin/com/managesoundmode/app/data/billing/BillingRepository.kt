package com.managesoundmode.app.data.billing

import android.app.Activity
import com.managesoundmode.app.domain.model.ProEntitlementState
import com.managesoundmode.app.domain.model.PurchaseResult
import com.managesoundmode.app.domain.model.RestorePurchasesResult
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {
  val entitlementFlow: StateFlow<ProEntitlementState>

  suspend fun startConnection()
  suspend fun queryProductDetails(): Result<String?>
  suspend fun launchProPurchaseFlow(activity: Activity): PurchaseResult
  suspend fun restorePurchases(): RestorePurchasesResult
}

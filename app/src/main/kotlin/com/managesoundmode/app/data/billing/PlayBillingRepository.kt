package com.managesoundmode.app.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.managesoundmode.app.data.local.SoundPreferencesDataSource
import com.managesoundmode.app.domain.model.CachedProEntitlement
import com.managesoundmode.app.domain.model.FreemiumQuotas
import com.managesoundmode.app.domain.model.ProEntitlementState
import com.managesoundmode.app.domain.model.PurchaseResult
import com.managesoundmode.app.domain.model.RestorePurchasesResult
import com.managesoundmode.app.system.InternalIntentSecurity
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class PlayBillingRepository(
  private val context: Context,
  private val preferencesDataSource: SoundPreferencesDataSource,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : BillingRepository {

  private val _entitlementFlow = MutableStateFlow(ProEntitlementState())
  override val entitlementFlow: StateFlow<ProEntitlementState> = _entitlementFlow.asStateFlow()

  @Volatile
  private var cachedProductDetails: ProductDetails? = null

  @Volatile
  private var activePurchaseDeferred: CompletableDeferred<PurchaseResult>? = null

  private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
    scope.launch {
      handlePurchasesUpdated(billingResult, purchases)
    }
  }

  private val billingClient: BillingClient = BillingClient.newBuilder(context)
    .setListener(purchasesUpdatedListener)
    .enablePendingPurchases(
      PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build(),
    )
    .build()

  init {
    scope.launch {
      preferencesDataSource.prefsFlow.collect { prefs ->
        val cached = prefs.cachedProEntitlement
        if (cached != null && cached.isProUnlocked) {
          val isValid = InternalIntentSecurity.verifyHmacSha256(
            context = context,
            message = cached.payloadToSign(),
            expectedHex = cached.hmacSignature ?: "",
          )
          if (isValid) {
            _entitlementFlow.update {
              it.copy(
                isProUnlocked = true,
                unlockedAtEpochMillis = cached.unlockedAtEpochMillis,
              )
            }
          } else {
            Log.w(TAG, "Assinatura HMAC do cache Pro inválida; revogando acesso Pro em cache")
            preferencesDataSource.setCachedProEntitlement(null)
          }
        }
      }
    }
    scope.launch {
      startConnection()
    }
  }

  private suspend fun loadCachedEntitlement() {
    runCatching {
      val prefs = preferencesDataSource.prefsFlow.first()
      val cached = prefs.cachedProEntitlement
      if (cached != null && cached.isProUnlocked) {
        val isValid = InternalIntentSecurity.verifyHmacSha256(
          context = context,
          message = cached.payloadToSign(),
          expectedHex = cached.hmacSignature ?: "",
        )
        if (isValid) {
          _entitlementFlow.update {
            it.copy(
              isProUnlocked = true,
              unlockedAtEpochMillis = cached.unlockedAtEpochMillis,
            )
          }
        } else {
          Log.w(TAG, "Assinatura HMAC do cache Pro inválida; revogando acesso Pro em cache")
          preferencesDataSource.setCachedProEntitlement(null)
        }
      }
    }.onFailure {
      Log.w(TAG, "Falha ao ler cache local de Pro Entitlement", it)
    }
  }

  override suspend fun startConnection() {
    if (billingClient.isReady) {
      _entitlementFlow.update { it.copy(isBillingConnected = true) }
      queryProductDetails()
      return
    }

    val connected = suspendCancellableCoroutine<Boolean> { continuation ->
      billingClient.startConnection(object : BillingClientStateListener {
        override fun onBillingSetupFinished(billingResult: BillingResult) {
          val isOk = billingResult.responseCode == BillingClient.BillingResponseCode.OK
          _entitlementFlow.update { it.copy(isBillingConnected = isOk) }
          if (continuation.isActive) {
            continuation.resume(isOk)
          }
        }

        override fun onBillingServiceDisconnected() {
          _entitlementFlow.update { it.copy(isBillingConnected = false) }
          if (continuation.isActive) {
            continuation.resume(false)
          }
        }
      })
    }

    if (connected) {
      queryProductDetails()
    }
  }

  override suspend fun queryProductDetails(): Result<String?> = withContext(Dispatchers.IO) {
    runCatching {
      if (!billingClient.isReady) {
        startConnection()
      }
      val productList = listOf(
        QueryProductDetailsParams.Product.newBuilder()
          .setProductId(FreemiumQuotas.PRO_LIFETIME_PRODUCT_ID)
          .setProductType(BillingClient.ProductType.INAPP)
          .build(),
      )
      val params = QueryProductDetailsParams.newBuilder()
        .setProductList(productList)
        .build()

      val result = billingClient.queryProductDetails(params)
      if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
        val details = result.productDetailsList?.firstOrNull {
          it.productId == FreemiumQuotas.PRO_LIFETIME_PRODUCT_ID
        }
        cachedProductDetails = details
        val price = details?.oneTimePurchaseOfferDetails?.formattedPrice
        _entitlementFlow.update { it.copy(formattedPrice = price, isBillingConnected = true) }
        price
      } else {
        Log.w(TAG, "Erro ao consultar ProductDetails: ${result.billingResult.debugMessage}")
        _entitlementFlow.value.formattedPrice
      }
    }
  }

  override suspend fun launchProPurchaseFlow(activity: Activity): PurchaseResult = withContext(Dispatchers.Main) {
    if (_entitlementFlow.value.isProUnlocked) {
      return@withContext PurchaseResult.AlreadyOwned
    }
    if (!billingClient.isReady) {
      startConnection()
    }
    var details = cachedProductDetails
    if (details == null) {
      queryProductDetails()
      details = cachedProductDetails
    }
    if (details == null) {
      return@withContext PurchaseResult.Error(
        billingResponseCode = BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
        debugMessage = "Detalhes do produto Pro indisponíveis na Google Play Store.",
      )
    }

    val productDetailsParamsList = listOf(
      BillingFlowParams.ProductDetailsParams.newBuilder()
        .setProductDetails(details)
        .build(),
    )
    val flowParams = BillingFlowParams.newBuilder()
      .setProductDetailsParamsList(productDetailsParamsList)
      .build()

    val deferred = CompletableDeferred<PurchaseResult>()
    activePurchaseDeferred = deferred

    val launchResult = billingClient.launchBillingFlow(activity, flowParams)
    when (launchResult.responseCode) {
      BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
        activePurchaseDeferred = null
        scope.launch { restorePurchases() }
        PurchaseResult.AlreadyOwned
      }
      BillingClient.BillingResponseCode.USER_CANCELED -> {
        activePurchaseDeferred = null
        PurchaseResult.UserCancelled
      }
      BillingClient.BillingResponseCode.OK -> {
        _entitlementFlow.update { it.copy(isProcessing = true) }
        withTimeoutOrNull(120_000L) {
          deferred.await()
        } ?: run {
          activePurchaseDeferred = null
          _entitlementFlow.update { it.copy(isProcessing = false) }
          if (_entitlementFlow.value.isProUnlocked) {
            PurchaseResult.Purchased
          } else {
            PurchaseResult.Error(
              BillingClient.BillingResponseCode.ERROR,
              "Tempo limite para conclusão do faturamento excedido.",
            )
          }
        }
      }
      else -> {
        activePurchaseDeferred = null
        PurchaseResult.Error(launchResult.responseCode, launchResult.debugMessage)
      }
    }
  }

  private suspend fun handlePurchasesUpdated(
    billingResult: BillingResult,
    purchases: List<Purchase>?,
  ) {
    val deferred = activePurchaseDeferred
    when (billingResult.responseCode) {
      BillingClient.BillingResponseCode.OK -> {
        val targetPurchase = purchases?.firstOrNull {
          it.products.contains(FreemiumQuotas.PRO_LIFETIME_PRODUCT_ID)
        }
        if (targetPurchase != null) {
          when (targetPurchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
              acknowledgeAndCachePurchase(targetPurchase)
              deferred?.complete(PurchaseResult.Purchased)
            }
            Purchase.PurchaseState.PENDING -> {
              _entitlementFlow.update { it.copy(isPurchasePending = true, isProcessing = false) }
              deferred?.complete(PurchaseResult.Pending)
            }
            else -> {
              _entitlementFlow.update { it.copy(isProcessing = false) }
              deferred?.complete(PurchaseResult.UserCancelled)
            }
          }
        } else {
          _entitlementFlow.update { it.copy(isProcessing = false) }
          deferred?.complete(PurchaseResult.UserCancelled)
        }
      }
      BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
        scope.launch { restorePurchases() }
        deferred?.complete(PurchaseResult.AlreadyOwned)
      }
      BillingClient.BillingResponseCode.USER_CANCELED -> {
        _entitlementFlow.update { it.copy(isProcessing = false) }
        deferred?.complete(PurchaseResult.UserCancelled)
      }
      else -> {
        _entitlementFlow.update { it.copy(isProcessing = false) }
        deferred?.complete(PurchaseResult.Error(billingResult.responseCode, billingResult.debugMessage))
      }
    }
    activePurchaseDeferred = null
  }

  private suspend fun acknowledgeAndCachePurchase(purchase: Purchase) {
    if (!purchase.isAcknowledged) {
      val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
        .setPurchaseToken(purchase.purchaseToken)
        .build()
      val ackResult = billingClient.acknowledgePurchase(acknowledgeParams)
      if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
        Log.w(TAG, "Falha ao reconhecer compra: ${ackResult.debugMessage}")
      }
    }

    val tokenHash = MessageDigest.getInstance("SHA-256")
      .digest(purchase.purchaseToken.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }

    val epochMillis = if (purchase.purchaseTime > 0) purchase.purchaseTime else System.currentTimeMillis()
    val payload = "true|$epochMillis|$tokenHash"
    val signature = InternalIntentSecurity.computeHmacSha256(context, payload)

    val cached = CachedProEntitlement(
      isProUnlocked = true,
      unlockedAtEpochMillis = epochMillis,
      purchaseTokenHash = tokenHash,
      hmacSignature = signature,
    )
    preferencesDataSource.setCachedProEntitlement(cached)

    _entitlementFlow.update {
      it.copy(
        isProUnlocked = true,
        isPurchasePending = false,
        isProcessing = false,
        unlockedAtEpochMillis = epochMillis,
      )
    }
  }

  override suspend fun restorePurchases(): RestorePurchasesResult = withContext(Dispatchers.IO) {
    if (!billingClient.isReady) {
      startConnection()
    }
    val params = QueryPurchasesParams.newBuilder()
      .setProductType(BillingClient.ProductType.INAPP)
      .build()

    val purchasesResult = billingClient.queryPurchasesAsync(params)
    val billingResult = purchasesResult.billingResult
    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
      val activePurchase = purchasesResult.purchasesList.firstOrNull {
        it.products.contains(FreemiumQuotas.PRO_LIFETIME_PRODUCT_ID) &&
          it.purchaseState == Purchase.PurchaseState.PURCHASED
      }
      if (activePurchase != null) {
        acknowledgeAndCachePurchase(activePurchase)
        RestorePurchasesResult.RestoredPro
      } else {
        // Play Store confirmou OK sem compra ativa -> revoga cache
        preferencesDataSource.setCachedProEntitlement(null)
        _entitlementFlow.update {
          it.copy(
            isProUnlocked = false,
            isPurchasePending = false,
            unlockedAtEpochMillis = null,
          )
        }
        RestorePurchasesResult.NoActivePurchaseFound
      }
    } else {
      Log.w(TAG, "Erro ao restaurar compras na Google Play: ${billingResult.debugMessage}")
      RestorePurchasesResult.Error(billingResult.debugMessage.ifBlank { "Erro código ${billingResult.responseCode}" })
    }
  }

  companion object {
    private const val TAG = "PlayBillingRepo"
  }
}

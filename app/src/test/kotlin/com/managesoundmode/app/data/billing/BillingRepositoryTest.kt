package com.managesoundmode.app.data.billing

import com.managesoundmode.app.data.local.PersistedSoundPrefs
import com.managesoundmode.app.domain.model.CachedProEntitlement
import com.managesoundmode.app.domain.model.FreemiumQuotas
import com.managesoundmode.app.domain.model.PurchaseResult
import com.managesoundmode.app.domain.model.RestorePurchasesResult
import com.managesoundmode.app.system.InternalIntentSecurity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingRepositoryTest {

  @Test
  fun hmacSha256_computesDeterministicSignatureAndVerifiesGenuinePayload() {
    val secretKey = "test_256_bit_secret_key_manage_sound_mode"
    val payload = "modos_de_som_pro_lifetime:1700000000000:purchase_token_abc123"

    val signature = InternalIntentSecurity.computeHmacSha256(secretKey, payload)
    assertEquals(64, signature.length)

    val valid = InternalIntentSecurity.verifyHmacSha256(secretKey, payload, signature)
    assertTrue("A assinatura gerada deve ser verificada como válida", valid)
  }

  @Test
  fun hmacSha256_tamperedPayloadOrSignature_isRejected() {
    val secretKey = "test_256_bit_secret_key_manage_sound_mode"
    val payload = "modos_de_som_pro_lifetime:1700000000000:purchase_token_abc123"
    val tamperedPayload = "modos_de_som_pro_lifetime:1700000000001:purchase_token_abc123"

    val signature = InternalIntentSecurity.computeHmacSha256(secretKey, payload)

    // Payload alterado
    val validTamperedPayload = InternalIntentSecurity.verifyHmacSha256(secretKey, tamperedPayload, signature)
    assertFalse("Payload alterado deve ser rejeitado", validTamperedPayload)

    // Assinatura corrompida
    val corruptedSignature = signature.dropLast(2) + "ff"
    val validCorruptedSignature = InternalIntentSecurity.verifyHmacSha256(secretKey, payload, corruptedSignature)
    assertFalse("Assinatura corrompida deve ser rejeitada", validCorruptedSignature)

    // Assinatura em branco
    assertFalse(InternalIntentSecurity.verifyHmacSha256(secretKey, payload, ""))
  }

  @Test
  fun fakeBillingRepository_initialState_isFreePlan() = runTest {
    val fakeBilling = FakeBillingRepository(initialUnlocked = false)
    val state = fakeBilling.entitlementFlow.first()

    assertFalse(state.isProUnlocked)
    assertFalse(state.isPurchasePending)
    assertEquals("R$ 19,90", state.formattedPrice)
    assertNull(state.unlockedAtEpochMillis)
  }

  @Test
  fun fakeBillingRepository_purchaseFlow_pendingThenPurchased_unlocksPro() = runTest {
    val fakeBilling = FakeBillingRepository(initialUnlocked = false)

    // Simula compra pendente
    fakeBilling.nextPurchaseResult = PurchaseResult.Pending
    val pendingResult = fakeBilling.launchProPurchaseFlow(android.app.Activity())
    assertTrue(pendingResult is PurchaseResult.Pending)
    assertTrue(fakeBilling.entitlementFlow.value.isPurchasePending)
    assertFalse(fakeBilling.entitlementFlow.value.isProUnlocked)

    // Simula pagamento compensado
    fakeBilling.nextPurchaseResult = PurchaseResult.Purchased
    val purchasedResult = fakeBilling.launchProPurchaseFlow(android.app.Activity())
    assertTrue(purchasedResult is PurchaseResult.Purchased)
    assertFalse(fakeBilling.entitlementFlow.value.isPurchasePending)
    assertTrue(fakeBilling.entitlementFlow.value.isProUnlocked)
    assertTrue((fakeBilling.entitlementFlow.value.unlockedAtEpochMillis ?: 0L) > 0L)
  }

  @Test
  fun fakeBillingRepository_restorePurchases_restoresProOrReturnsNoPurchaseFound() = runTest {
    val fakeBilling = FakeBillingRepository(initialUnlocked = false)

    // Restauração com compra encontrada
    fakeBilling.nextRestoreResult = RestorePurchasesResult.RestoredPro
    val restored = fakeBilling.restorePurchases()
    assertTrue(restored is RestorePurchasesResult.RestoredPro)
    assertTrue(fakeBilling.entitlementFlow.value.isProUnlocked)

    // Restauração sem compras ativas
    fakeBilling.nextRestoreResult = RestorePurchasesResult.NoActivePurchaseFound
    val notFound = fakeBilling.restorePurchases()
    assertTrue(notFound is RestorePurchasesResult.NoActivePurchaseFound)
    assertFalse(fakeBilling.entitlementFlow.value.isProUnlocked)
  }

  @Test
  fun fakeBillingRepository_offlineSimulation_failsGracefully() = runTest {
    val fakeBilling = FakeBillingRepository(initialUnlocked = false)
    fakeBilling.simulateOffline = true

    val purchaseRes = fakeBilling.launchProPurchaseFlow(android.app.Activity())
    assertTrue(purchaseRes is PurchaseResult.Error)

    val restoreRes = fakeBilling.restorePurchases()
    assertTrue(restoreRes is RestorePurchasesResult.Error)
  }

  @Test
  fun freemiumQuotas_constantsAreCompliantWithSpec009() {
    assertEquals("modos_de_som_pro_lifetime", FreemiumQuotas.PRO_LIFETIME_PRODUCT_ID)
    assertEquals(5, FreemiumQuotas.FREE_MAX_CUSTOM_PROFILES)
    assertEquals(3, FreemiumQuotas.FREE_MAX_SCHEDULE_RULES)
    assertEquals(3, FreemiumQuotas.FREE_MAX_LOCATION_RULES)
    assertEquals(1, FreemiumQuotas.FREE_MAX_CALENDAR_RULES)
  }
}

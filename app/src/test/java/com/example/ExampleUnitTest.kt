package com.example

import com.example.data.model.BillingCycle
import com.example.data.model.SubscriptionCategory
import com.example.util.CurrencyHelper
import com.example.util.RecurringChargeDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testRecurringChargeDetector_Netflix() {
    val sms = "Your card ending in 4242 was debited for $15.99 on 24-Sep-2026 for monthly Netflix subscription auto-renewal."
    val detected = RecurringChargeDetector.parse(sms)
    assertNotNull(detected)
    assertEquals("Netflix", detected!!.serviceName)
    assertEquals(15.99, detected.amount, 0.01)
    assertEquals("USD", detected.currency)
    assertEquals(BillingCycle.MONTHLY, detected.billingCycle)
    assertEquals(SubscriptionCategory.ENTERTAINMENT, detected.category)
    assertTrue(detected.paymentMethod.contains("4242"))
  }

  @Test
  fun testCurrencyHelper_Formatting() {
    val usd = CurrencyHelper.format(15.99, "USD")
    assertEquals("$15.99", usd)

    val converted = CurrencyHelper.convert(100.0, "USD", "USD")
    assertEquals(100.0, converted, 0.001)

    val inrCountry = CurrencyHelper.getCountryByCode("INR")
    assertNotNull(inrCountry)
    assertEquals("India", inrCountry!!.countryName)
    assertEquals("₹", inrCountry.currencySymbol)
    assertEquals("🇮🇳", inrCountry.flag)
  }

  @Test
  fun testBillingCycle_Calculations() {
    val monthly = BillingCycle.MONTHLY.toMonthlyAmount(10.0)
    assertEquals(10.0, monthly, 0.001)

    val yearly = BillingCycle.YEARLY.toMonthlyAmount(120.0)
    assertEquals(10.0, yearly, 0.001)
  }
}


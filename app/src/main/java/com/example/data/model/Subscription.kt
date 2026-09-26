package com.example.data.model

data class Subscription(
    val id: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val currency: String = "USD",
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val category: SubscriptionCategory = SubscriptionCategory.ENTERTAINMENT,
    val nextBillingDate: Long, // Epoch milliseconds
    val paymentMethod: String = "Credit Card",
    val isActive: Boolean = true,
    val reminderDaysBefore: Int = 3,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val monthlyEquivalent: Double
        get() = billingCycle.toMonthlyAmount(amount)

    val yearlyEquivalent: Double
        get() = billingCycle.toYearlyAmount(amount)
}

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val authProvider: String = "local", // local, email, google
    val passwordHash: String = "",
    val country: String = "United States",
    val currencyPreference: String = "USD",
    val isPremium: Boolean = true, // 100% Free App: All features unlocked for free
    val notificationTimingDays: Int = 3,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val lastGoogleDriveBackupTime: Long = 0L,
    val isDriveSyncEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

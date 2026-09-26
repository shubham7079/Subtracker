package com.example.data.repository

import com.example.data.local.SubscriptionDao
import com.example.data.local.SubscriptionEntity
import com.example.data.model.BillingCycle
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

class SubscriptionRepository(private val subscriptionDao: SubscriptionDao) {

    fun getSubscriptions(userId: String): Flow<List<Subscription>> {
        return subscriptionDao.getSubscriptionsForUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getActiveSubscriptions(userId: String): Flow<List<Subscription>> {
        return subscriptionDao.getActiveSubscriptionsForUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getSubscriptionById(id: String): Subscription? {
        return subscriptionDao.getSubscriptionById(id)?.toDomain()
    }

    suspend fun insertSubscription(subscription: Subscription) {
        subscriptionDao.insertSubscription(SubscriptionEntity.fromDomain(subscription))
    }

    suspend fun updateSubscription(subscription: Subscription) {
        subscriptionDao.updateSubscription(SubscriptionEntity.fromDomain(subscription))
    }

    suspend fun deleteSubscription(id: String) {
        subscriptionDao.deleteById(id)
    }

    suspend fun deleteAllForUser(userId: String) {
        subscriptionDao.deleteAllForUser(userId)
    }

    suspend fun preloadSampleData(userId: String) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // 1. Netflix - renews in 3 days
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, 3)
        val netflix = Subscription(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = "Netflix",
            amount = 15.99,
            currency = "USD",
            billingCycle = BillingCycle.MONTHLY,
            category = SubscriptionCategory.ENTERTAINMENT,
            nextBillingDate = cal.timeInMillis,
            paymentMethod = "Visa •• 4242",
            isActive = true,
            reminderDaysBefore = 3,
            notes = "Standard 4K UHD Plan"
        )

        // 2. Spotify - renews in 7 days
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, 7)
        val spotify = Subscription(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = "Spotify Premium",
            amount = 10.99,
            currency = "USD",
            billingCycle = BillingCycle.MONTHLY,
            category = SubscriptionCategory.ENTERTAINMENT,
            nextBillingDate = cal.timeInMillis,
            paymentMethod = "Mastercard •• 8812",
            isActive = true,
            reminderDaysBefore = 1,
            notes = "Individual student/personal plan"
        )

        // 3. ChatGPT Plus - renews in 14 days
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, 14)
        val chatgpt = Subscription(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = "ChatGPT Plus",
            amount = 20.00,
            currency = "USD",
            billingCycle = BillingCycle.MONTHLY,
            category = SubscriptionCategory.PRODUCTIVITY,
            nextBillingDate = cal.timeInMillis,
            paymentMethod = "Apple Pay",
            isActive = true,
            reminderDaysBefore = 3,
            notes = "AI Research & Writing Assistant"
        )

        // 4. AWS Cloud - renews in 20 days
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, 20)
        val aws = Subscription(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = "AWS Cloud Services",
            amount = 45.00,
            currency = "USD",
            billingCycle = BillingCycle.MONTHLY,
            category = SubscriptionCategory.CLOUD_STORAGE,
            nextBillingDate = cal.timeInMillis,
            paymentMethod = "Visa •• 4242",
            isActive = true,
            reminderDaysBefore = 3,
            notes = "EC2 & S3 server compute charges"
        )

        // 5. Gym Membership - renews in 28 days
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, 28)
        val gym = Subscription(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = "Equinox Gym",
            amount = 49.99,
            currency = "USD",
            billingCycle = BillingCycle.MONTHLY,
            category = SubscriptionCategory.HEALTH_FITNESS,
            nextBillingDate = cal.timeInMillis,
            paymentMethod = "Direct Debit",
            isActive = true,
            reminderDaysBefore = 3,
            notes = "Includes swimming pool & sauna pass"
        )

        subscriptionDao.insertSubscriptions(
            listOf(
                SubscriptionEntity.fromDomain(netflix),
                SubscriptionEntity.fromDomain(spotify),
                SubscriptionEntity.fromDomain(chatgpt),
                SubscriptionEntity.fromDomain(aws),
                SubscriptionEntity.fromDomain(gym)
            )
        )
    }
}

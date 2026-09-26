package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BillingCycle
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionCategory

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val currency: String,
    val billingCycle: String,
    val category: String,
    val nextBillingDate: Long,
    val paymentMethod: String,
    val isActive: Boolean,
    val reminderDaysBefore: Int,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Subscription {
        val cycle = try {
            BillingCycle.valueOf(billingCycle)
        } catch (_: Exception) {
            BillingCycle.MONTHLY
        }
        val cat = try {
            SubscriptionCategory.valueOf(category)
        } catch (_: Exception) {
            SubscriptionCategory.OTHER
        }

        return Subscription(
            id = id,
            userId = userId,
            name = name,
            amount = amount,
            currency = currency,
            billingCycle = cycle,
            category = cat,
            nextBillingDate = nextBillingDate,
            paymentMethod = paymentMethod,
            isActive = isActive,
            reminderDaysBefore = reminderDaysBefore,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(domain: Subscription): SubscriptionEntity {
            return SubscriptionEntity(
                id = domain.id,
                userId = domain.userId,
                name = domain.name,
                amount = domain.amount,
                currency = domain.currency,
                billingCycle = domain.billingCycle.name,
                category = domain.category.name,
                nextBillingDate = domain.nextBillingDate,
                paymentMethod = domain.paymentMethod,
                isActive = domain.isActive,
                reminderDaysBefore = domain.reminderDaysBefore,
                notes = domain.notes,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )
        }
    }
}

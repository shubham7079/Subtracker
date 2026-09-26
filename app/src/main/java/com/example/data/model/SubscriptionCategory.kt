package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class SubscriptionCategory(
    val displayName: String,
    val color: Long,
    val iconName: String
) {
    ENTERTAINMENT("Entertainment", 0xFF8B5CF6, "Movie"),
    PRODUCTIVITY("Productivity", 0xFF3B82F6, "Work"),
    CLOUD_STORAGE("Cloud & Tech", 0xFF06B6D4, "Cloud"),
    UTILITIES("Utilities & Bills", 0xFFF59E0B, "Business"),
    HEALTH_FITNESS("Health & Fitness", 0xFF10B981, "FitnessCenter"),
    EDUCATION("Education", 0xFFEC4899, "School"),
    FINANCE("Finance & Banking", 0xFF6366F1, "Payments"),
    OTHER("Other", 0xFF64748B, "Widgets");

    fun getIcon(): ImageVector {
        return when (this) {
            ENTERTAINMENT -> Icons.Default.Movie
            PRODUCTIVITY -> Icons.Default.Work
            CLOUD_STORAGE -> Icons.Default.Cloud
            UTILITIES -> Icons.Default.Business
            HEALTH_FITNESS -> Icons.Default.FitnessCenter
            EDUCATION -> Icons.Default.School
            FINANCE -> Icons.Default.Payments
            OTHER -> Icons.Default.Widgets
        }
    }

    val composeColor: Color
        get() = Color(color)
}

enum class BillingCycle(val displayName: String, val shortName: String, val monthsMultiplier: Double) {
    WEEKLY("Weekly", "wk", 4.33),
    MONTHLY("Monthly", "mo", 1.0),
    QUARTERLY("Quarterly", "qtr", 0.3333),
    YEARLY("Yearly", "yr", 0.0833);

    fun toMonthlyAmount(amount: Double): Double {
        return when (this) {
            WEEKLY -> amount * (52.0 / 12.0)
            MONTHLY -> amount
            QUARTERLY -> amount / 3.0
            YEARLY -> amount / 12.0
        }
    }

    fun toYearlyAmount(amount: Double): Double {
        return toMonthlyAmount(amount) * 12.0
    }
}

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.UserProfile

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val authProvider: String,
    val passwordHash: String = "",
    val country: String = "United States",
    val currencyPreference: String = "USD",
    val isPremium: Boolean = true,
    val notificationTimingDays: Int = 3,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val lastGoogleDriveBackupTime: Long = 0L,
    val isDriveSyncEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): UserProfile {
        return UserProfile(
            id = id,
            name = name,
            email = email,
            authProvider = authProvider,
            passwordHash = passwordHash,
            country = country,
            currencyPreference = currencyPreference,
            isPremium = isPremium,
            notificationTimingDays = notificationTimingDays,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            lastGoogleDriveBackupTime = lastGoogleDriveBackupTime,
            isDriveSyncEnabled = isDriveSyncEnabled,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(user: UserProfile): UserEntity {
            return UserEntity(
                id = user.id,
                name = user.name,
                email = user.email,
                authProvider = user.authProvider,
                passwordHash = user.passwordHash,
                country = user.country,
                currencyPreference = user.currencyPreference,
                isPremium = user.isPremium,
                notificationTimingDays = user.notificationTimingDays,
                reminderHour = user.reminderHour,
                reminderMinute = user.reminderMinute,
                lastGoogleDriveBackupTime = user.lastGoogleDriveBackupTime,
                isDriveSyncEnabled = user.isDriveSyncEnabled,
                createdAt = user.createdAt
            )
        }
    }
}

package com.example.data.repository

import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class UserRepository(
    private val userDao: UserDao,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _currentUser = MutableStateFlow(
        UserProfile(
            id = "default_user_1",
            name = "Alex Morgan",
            email = "alex.morgan@example.com",
            authProvider = "local",
            country = "United States",
            currencyPreference = "USD",
            isPremium = true, // 100% Free App: All features unlocked
            notificationTimingDays = 3,
            reminderHour = 9,
            reminderMinute = 0
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    val allUsers: Flow<List<UserProfile>> = userDao.getAllUsers().map { entities ->
        entities.map { it.toDomain().copy(isPremium = true) }
    }

    init {
        externalScope.launch {
            val existing = userDao.getAllUsers().firstOrNull()
            if (existing.isNullOrEmpty()) {
                val initialUser = _currentUser.value
                userDao.insertUser(UserEntity.fromDomain(initialUser))
            } else {
                val user = existing.first().toDomain().copy(isPremium = true)
                _currentUser.value = user
                userDao.updateUser(UserEntity.fromDomain(user))
            }
        }
    }

    fun switchUser(user: UserProfile) {
        _currentUser.value = user.copy(isPremium = true)
    }

    suspend fun createUser(
        name: String,
        email: String,
        authProvider: String = "local",
        country: String = "United States",
        currency: String = "USD"
    ): UserProfile {
        val newUser = UserProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email,
            authProvider = authProvider,
            country = country,
            currencyPreference = currency,
            isPremium = true // Free application with all features unlocked
        )
        userDao.insertUser(UserEntity.fromDomain(newUser))
        _currentUser.value = newUser
        return newUser
    }

    suspend fun updateCurrentUser(updated: UserProfile) {
        val user = updated.copy(isPremium = true)
        _currentUser.value = user
        userDao.updateUser(UserEntity.fromDomain(user))
    }

    suspend fun setCountryAndCurrency(country: String, currencyCode: String) {
        val updated = _currentUser.value.copy(
            country = country,
            currencyPreference = currencyCode,
            isPremium = true
        )
        updateCurrentUser(updated)
    }

    suspend fun setCurrency(currencyCode: String) {
        val updated = _currentUser.value.copy(
            currencyPreference = currencyCode,
            isPremium = true
        )
        updateCurrentUser(updated)
    }

    suspend fun setNotificationTiming(days: Int, hour: Int, minute: Int) {
        val updated = _currentUser.value.copy(
            notificationTimingDays = days,
            reminderHour = hour,
            reminderMinute = minute
        )
        updateCurrentUser(updated)
    }

    suspend fun deleteUser(userId: String) {
        userDao.deleteUserById(userId)
        val remaining = userDao.getAllUsers().firstOrNull()
        if (!remaining.isNullOrEmpty()) {
            _currentUser.value = remaining.first().toDomain().copy(isPremium = true)
        } else {
            createUser("Alex Morgan", "alex.morgan@example.com")
        }
    }
}

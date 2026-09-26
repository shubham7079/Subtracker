package com.example

import android.app.Application
import com.example.data.local.SubTrackDatabase
import com.example.data.repository.SubscriptionRepository
import com.example.data.repository.UserRepository
import com.example.util.NotificationHelper

class SubTrackApplication : Application() {

    lateinit var database: SubTrackDatabase
        private set

    lateinit var subscriptionRepository: SubscriptionRepository
        private set

    lateinit var userRepository: UserRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = SubTrackDatabase.getDatabase(this)
        subscriptionRepository = SubscriptionRepository(database.subscriptionDao())
        userRepository = UserRepository(database.userDao())

        // Initialize Notification Channels
        NotificationHelper.createNotificationChannel(this)
    }
}

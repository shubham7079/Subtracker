package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SubscriptionEntity::class, UserEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SubTrackDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: SubTrackDatabase? = null

        fun getDatabase(context: Context): SubTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SubTrackDatabase::class.java,
                    "subtrack_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BillingCycle
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionCategory
import com.example.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleDriveBackupHelper {

    data class BackupPayload(
        val timestamp: Long,
        val formattedDate: String,
        val userEmail: String,
        val userName: String,
        val country: String,
        val currency: String,
        val subscriptions: List<Subscription>
    )

    fun createBackupJson(user: UserProfile, subscriptions: List<Subscription>): String {
        val root = JSONObject()
        root.put("app", "SubTrack")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val userObj = JSONObject()
        userObj.put("id", user.id)
        userObj.put("name", user.name)
        userObj.put("email", user.email)
        userObj.put("country", user.country)
        userObj.put("currency", user.currencyPreference)
        root.put("user", userObj)

        val subsArray = JSONArray()
        for (sub in subscriptions) {
            val obj = JSONObject()
            obj.put("id", sub.id)
            obj.put("name", sub.name)
            obj.put("amount", sub.amount)
            obj.put("currency", sub.currency)
            obj.put("billingCycle", sub.billingCycle.name)
            obj.put("category", sub.category.name)
            obj.put("nextBillingDate", sub.nextBillingDate)
            obj.put("paymentMethod", sub.paymentMethod)
            obj.put("isActive", sub.isActive)
            obj.put("reminderDaysBefore", sub.reminderDaysBefore)
            obj.put("notes", sub.notes)
            obj.put("createdAt", sub.createdAt)
            obj.put("updatedAt", sub.updatedAt)
            subsArray.put(obj)
        }
        root.put("subscriptions", subsArray)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String, currentUserId: String): BackupPayload {
        val root = JSONObject(jsonString)
        val timestamp = root.optLong("timestamp", System.currentTimeMillis())
        val userObj = root.optJSONObject("user")
        val userEmail = userObj?.optString("email") ?: ""
        val userName = userObj?.optString("name") ?: ""
        val country = userObj?.optString("country") ?: "United States"
        val currency = userObj?.optString("currency") ?: "USD"

        val subsArray = root.optJSONArray("subscriptions") ?: JSONArray()
        val subsList = mutableListOf<Subscription>()

        for (i in 0 until subsArray.length()) {
            val obj = subsArray.getJSONObject(i)
            val cycleName = obj.optString("billingCycle", "MONTHLY")
            val cycle = runCatching { BillingCycle.valueOf(cycleName) }.getOrDefault(BillingCycle.MONTHLY)

            val catName = obj.optString("category", "ENTERTAINMENT")
            val category = runCatching { SubscriptionCategory.valueOf(catName) }.getOrDefault(SubscriptionCategory.ENTERTAINMENT)

            val sub = Subscription(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                userId = currentUserId,
                name = obj.optString("name", "Subscription"),
                amount = obj.optDouble("amount", 0.0),
                currency = obj.optString("currency", currency),
                billingCycle = cycle,
                category = category,
                nextBillingDate = obj.optLong("nextBillingDate", System.currentTimeMillis()),
                paymentMethod = obj.optString("paymentMethod", "Credit Card"),
                isActive = obj.optBoolean("isActive", true),
                reminderDaysBefore = obj.optInt("reminderDaysBefore", 3),
                notes = obj.optString("notes", ""),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            )
            subsList.add(sub)
        }

        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return BackupPayload(
            timestamp = timestamp,
            formattedDate = sdf.format(Date(timestamp)),
            userEmail = userEmail,
            userName = userName,
            country = country,
            currency = currency,
            subscriptions = subsList
        )
    }

    fun saveBackupToLocalFile(context: Context, json: String): File {
        val fileName = "subtrack_google_drive_backup.json"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { it.write(json.toByteArray()) }
        return file
    }

    fun shareToGoogleDrive(context: Context, json: String) {
        val cacheFile = File(context.cacheDir, "subtrack_backup.json")
        FileOutputStream(cacheFile).use { it.write(json.toByteArray()) }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SubTrack Google Drive Backup")
            putExtra(Intent.EXTRA_TEXT, "SubTrack Subscription Backup data for Google Drive storage.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Save to Google Drive")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun formatBackupTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Never"
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

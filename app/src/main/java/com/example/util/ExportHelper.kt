package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.Subscription
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    fun generateCsv(subscriptions: List<Subscription>): String {
        val sb = StringBuilder()
        sb.append("Name,Amount,Currency,Billing Cycle,Category,Next Billing Date,Payment Method,Status,Notes\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (sub in subscriptions) {
            val dateStr = dateFormat.format(Date(sub.nextBillingDate))
            val cleanName = escapeCsv(sub.name)
            val cleanNotes = escapeCsv(sub.notes)
            val cleanMethod = escapeCsv(sub.paymentMethod)
            val status = if (sub.isActive) "Active" else "Paused"
            sb.append("\"$cleanName\",${sub.amount},${sub.currency},${sub.billingCycle.displayName},${sub.category.displayName},$dateStr,\"$cleanMethod\",$status,\"$cleanNotes\"\n")
        }
        return sb.toString()
    }

    fun generateJson(subscriptions: List<Subscription>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val items = subscriptions.joinToString(",\n  ") { sub ->
            """{
    "id": "${sub.id}",
    "name": "${escapeJson(sub.name)}",
    "amount": ${sub.amount},
    "currency": "${sub.currency}",
    "billingCycle": "${sub.billingCycle.name}",
    "category": "${sub.category.name}",
    "nextBillingDate": "${dateFormat.format(Date(sub.nextBillingDate))}",
    "paymentMethod": "${escapeJson(sub.paymentMethod)}",
    "isActive": ${sub.isActive},
    "notes": "${escapeJson(sub.notes)}"
  }"""
        }
        return "[\n  $items\n]"
    }

    private fun escapeCsv(str: String): String {
        return str.replace("\"", "\"\"")
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }

    fun shareExport(context: Context, content: String, filename: String, mimeType: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, "SubTrack Export - $filename")
            putExtra(Intent.EXTRA_TEXT, content)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Subscriptions via")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }
}

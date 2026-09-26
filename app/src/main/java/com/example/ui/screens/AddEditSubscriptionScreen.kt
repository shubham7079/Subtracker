package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BillingCycle
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionCategory
import com.example.util.CurrencyHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PopularServicePreset(
    val name: String,
    val amount: Double,
    val cycle: BillingCycle,
    val category: SubscriptionCategory,
    val defaultPayment: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditSubscriptionScreen(
    initialSubscription: Subscription?,
    defaultCurrency: String,
    onSave: (
        id: String?,
        name: String,
        amount: Double,
        currency: String,
        cycle: BillingCycle,
        category: SubscriptionCategory,
        nextBillingDate: Long,
        paymentMethod: String,
        isActive: Boolean,
        reminderDaysBefore: Int,
        notes: String
    ) -> Unit,
    onDelete: ((Subscription) -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        PopularServicePreset("Netflix", 15.99, BillingCycle.MONTHLY, SubscriptionCategory.ENTERTAINMENT, "Visa •• 4242"),
        PopularServicePreset("Spotify", 10.99, BillingCycle.MONTHLY, SubscriptionCategory.ENTERTAINMENT, "Mastercard •• 8812"),
        PopularServicePreset("ChatGPT Plus", 20.00, BillingCycle.MONTHLY, SubscriptionCategory.PRODUCTIVITY, "Apple Pay"),
        PopularServicePreset("YouTube Premium", 13.99, BillingCycle.MONTHLY, SubscriptionCategory.ENTERTAINMENT, "Google Pay"),
        PopularServicePreset("Amazon Prime", 14.99, BillingCycle.MONTHLY, SubscriptionCategory.ENTERTAINMENT, "Visa •• 4242"),
        PopularServicePreset("iCloud+ (200GB)", 2.99, BillingCycle.MONTHLY, SubscriptionCategory.CLOUD_STORAGE, "Apple Pay"),
        PopularServicePreset("Equinox Gym", 49.99, BillingCycle.MONTHLY, SubscriptionCategory.HEALTH_FITNESS, "Direct Debit"),
        PopularServicePreset("GitHub Pro", 4.00, BillingCycle.MONTHLY, SubscriptionCategory.PRODUCTIVITY, "PayPal"),
        PopularServicePreset("Adobe Cloud", 29.99, BillingCycle.MONTHLY, SubscriptionCategory.PRODUCTIVITY, "Credit Card")
    )

    var name by remember { mutableStateOf(initialSubscription?.name ?: "") }
    var amountText by remember { mutableStateOf(initialSubscription?.amount?.toString() ?: "") }
    var currency by remember { mutableStateOf(initialSubscription?.currency ?: defaultCurrency) }
    var billingCycle by remember { mutableStateOf(initialSubscription?.billingCycle ?: BillingCycle.MONTHLY) }
    var category by remember { mutableStateOf(initialSubscription?.category ?: SubscriptionCategory.ENTERTAINMENT) }
    var paymentMethod by remember { mutableStateOf(initialSubscription?.paymentMethod ?: "Credit Card") }
    var isActive by remember { mutableStateOf(initialSubscription?.isActive ?: true) }
    var reminderDays by remember { mutableStateOf(initialSubscription?.reminderDaysBefore ?: 3) }
    var notes by remember { mutableStateOf(initialSubscription?.notes ?: "") }

    // Date management
    var billingCalendar by remember {
        val cal = Calendar.getInstance()
        if (initialSubscription != null) {
            cal.timeInMillis = initialSubscription.nextBillingDate
        } else {
            cal.add(Calendar.DAY_OF_YEAR, 30)
        }
        mutableStateOf(cal)
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialSubscription == null) "Add Subscription" else "Edit Subscription",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (initialSubscription != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(initialSubscription)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("delete_sub_button")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick preset pills if adding new
            if (initialSubscription == null) {
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (preset in presets) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                name = preset.name
                                amountText = preset.amount.toString()
                                billingCycle = preset.cycle
                                category = preset.category
                                paymentMethod = preset.defaultPayment
                            }
                        ) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("Service or Bill Name *") },
                placeholder = { Text("e.g. Netflix, Rent, Spotify, Gym") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sub_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Amount & Currency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount *") },
                    placeholder = { Text("15.99") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("sub_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Currency selector
                Column(modifier = Modifier.weight(1f)) {
                    Text("Currency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (curr in CurrencyHelper.supportedCurrencies) {
                            val flag = CurrencyHelper.getFlag(curr.code)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currency == curr.code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (currency == curr.code) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = { currency = curr.code }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = flag, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = curr.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Billing Cycle
            Column {
                Text("Billing Cycle", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (cycle in BillingCycle.values()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (billingCycle == cycle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (billingCycle == cycle) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { billingCycle = cycle },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = cycle.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (billingCycle == cycle) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Category Picker
            Column {
                Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (cat in SubscriptionCategory.values()) {
                        val isSelected = category == cat
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) cat.composeColor else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { category = cat }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = cat.getIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Renewal Date Stepper / Date Picker
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Next Renewal Date",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(billingCalendar.time),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date quick adjustments
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            onClick = {
                                val c = Calendar.getInstance()
                                c.timeInMillis = billingCalendar.timeInMillis
                                c.add(Calendar.DAY_OF_YEAR, -1)
                                billingCalendar = c
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-1 Day", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            onClick = {
                                val c = Calendar.getInstance()
                                c.timeInMillis = billingCalendar.timeInMillis
                                c.add(Calendar.DAY_OF_YEAR, 1)
                                billingCalendar = c
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+1 Day", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            onClick = {
                                val c = Calendar.getInstance()
                                c.timeInMillis = billingCalendar.timeInMillis
                                c.add(Calendar.WEEK_OF_YEAR, 1)
                                billingCalendar = c
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+1 Wk", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            onClick = {
                                val c = Calendar.getInstance()
                                c.timeInMillis = billingCalendar.timeInMillis
                                c.add(Calendar.MONTH, 1)
                                billingCalendar = c
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+1 Mo", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }

            // Payment Method Quick Chips
            Column {
                Text("Payment Method", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = { paymentMethod = it },
                    placeholder = { Text("e.g. Visa •• 4242, Apple Pay, PayPal") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_method_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickMethods = listOf("Visa •• 4242", "Mastercard •• 8812", "Apple Pay", "Google Pay", "PayPal", "Amex", "UPI", "Bank Debit")
                    for (method in quickMethods) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { paymentMethod = method }
                        ) {
                            Text(
                                text = method,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Reminder Timing & Active Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Active Subscription", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Include in spend totals and send renewal alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it },
                    modifier = Modifier.testTag("active_status_switch")
                )
            }

            // Reminder Days
            Column {
                Text("Remind Me Before Renewal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val reminderOptions = listOf(1 to "1 Day", 3 to "3 Days", 7 to "7 Days", 0 to "Same Day")
                    for ((days, label) in reminderOptions) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (reminderDays == days) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (reminderDays == days) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { reminderDays = days },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (reminderDays == days) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Login account, cancellation terms, etc.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sub_notes_input"),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (name.isBlank()) {
                        errorMessage = "Please enter a subscription name"
                        return@Button
                    }
                    if (amount == null || amount <= 0.0) {
                        errorMessage = "Please enter a valid positive amount"
                        return@Button
                    }

                    onSave(
                        initialSubscription?.id,
                        name,
                        amount,
                        currency,
                        billingCycle,
                        category,
                        billingCalendar.timeInMillis,
                        paymentMethod,
                        isActive,
                        reminderDays,
                        notes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_subscription_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialSubscription == null) "Save Subscription" else "Update Subscription",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

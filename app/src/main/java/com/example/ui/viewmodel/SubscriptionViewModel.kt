package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BillingCycle
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionCategory
import com.example.data.model.UserProfile
import com.example.data.repository.SubscriptionRepository
import com.example.data.repository.UserRepository
import com.example.util.CurrencyHelper
import com.example.util.DetectedCharge
import com.example.util.ExportHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

enum class SubscriptionSortOrder(val displayName: String) {
    RENEWAL_DATE("Renewal Date"),
    AMOUNT_DESC("Highest Spend"),
    NAME("Name A-Z")
}

data class CategorySpend(
    val category: SubscriptionCategory,
    val monthlyTotal: Double,
    val percentage: Float,
    val count: Int
)

data class DashboardMetrics(
    val monthlyTotal: Double,
    val yearlyTotal: Double,
    val activeCount: Int,
    val pausedCount: Int,
    val totalCount: Int,
    val monthlySavingsFromPaused: Double,
    val nextUpcoming: Subscription?,
    val daysUntilNextUpcoming: Int?,
    val categorySpends: List<CategorySpend>
)

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val currentUser: StateFlow<UserProfile> = userRepository.currentUser
    val allUsers: StateFlow<List<UserProfile>> = userRepository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Raw subscriptions stream reacting to current user
    val subscriptions: StateFlow<List<Subscription>> = currentUser
        .flatMapLatest { user ->
            subscriptionRepository.getSubscriptions(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filters and UI states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<SubscriptionCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedStatus = MutableStateFlow("ALL") // "ALL", "ACTIVE", "PAUSED"
    val selectedStatus = _selectedStatus.asStateFlow()

    private val _sortOrder = MutableStateFlow(SubscriptionSortOrder.RENEWAL_DATE)
    val sortOrder = _sortOrder.asStateFlow()

    // Dialog & Screen Visibility Controls
    private val _showPaywall = MutableStateFlow(false)
    val showPaywall = _showPaywall.asStateFlow()

    private val _showAutoDetect = MutableStateFlow(false)
    val showAutoDetect = _showAutoDetect.asStateFlow()

    private val _showAddEdit = MutableStateFlow(false)
    val showAddEdit = _showAddEdit.asStateFlow()

    private val _editingSubscription = MutableStateFlow<Subscription?>(null)
    val editingSubscription = _editingSubscription.asStateFlow()

    private val _showProfileDialog = MutableStateFlow(false)
    val showProfileDialog = _showProfileDialog.asStateFlow()

    private val _showCountryPicker = MutableStateFlow(false)
    val showCountryPicker = _showCountryPicker.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    private val _darkModePreference = MutableStateFlow("SYSTEM") // "SYSTEM", "DARK", "LIGHT"
    val darkModePreference = _darkModePreference.asStateFlow()

    // Filtered subscriptions list
    val filteredSubscriptions: StateFlow<List<Subscription>> = combine(
        subscriptions,
        _searchQuery,
        _selectedCategory,
        _selectedStatus,
        _sortOrder
    ) { subs, query, cat, status, sort ->
        var list = subs

        if (query.isNotBlank()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.notes.contains(query, ignoreCase = true) ||
                        it.paymentMethod.contains(query, ignoreCase = true)
            }
        }

        if (cat != null) {
            list = list.filter { it.category == cat }
        }

        when (status) {
            "ACTIVE" -> list = list.filter { it.isActive }
            "PAUSED" -> list = list.filter { !it.isActive }
        }

        when (sort) {
            SubscriptionSortOrder.RENEWAL_DATE -> list.sortedBy { it.nextBillingDate }
            SubscriptionSortOrder.AMOUNT_DESC -> list.sortedByDescending { it.monthlyEquivalent }
            SubscriptionSortOrder.NAME -> list.sortedBy { it.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard calculations
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        subscriptions,
        currentUser
    ) { subs, user ->
        val userCurrency = user.currencyPreference
        var totalMonthly = 0.0
        var totalPausedMonthly = 0.0
        var activeCount = 0
        var pausedCount = 0
        val categoryMap = mutableMapOf<SubscriptionCategory, Double>()
        val categoryCounts = mutableMapOf<SubscriptionCategory, Int>()

        val now = System.currentTimeMillis()
        var nextSub: Subscription? = null
        var minTimeDiff = Long.MAX_VALUE

        for (sub in subs) {
            val amountInUserCurrency = CurrencyHelper.convert(sub.amount, sub.currency, userCurrency)
            val monthlyInUserCurrency = sub.billingCycle.toMonthlyAmount(amountInUserCurrency)

            if (sub.isActive) {
                activeCount++
                totalMonthly += monthlyInUserCurrency

                val catTotal = categoryMap[sub.category] ?: 0.0
                categoryMap[sub.category] = catTotal + monthlyInUserCurrency
                categoryCounts[sub.category] = (categoryCounts[sub.category] ?: 0) + 1

                val diff = sub.nextBillingDate - now
                if (diff >= -86400000L && diff < minTimeDiff) {
                    minTimeDiff = diff
                    nextSub = sub
                }
            } else {
                pausedCount++
                totalPausedMonthly += monthlyInUserCurrency
            }
        }

        val totalYearly = totalMonthly * 12.0

        val categorySpends = categoryMap.map { (cat, monthlyAmt) ->
            val pct = if (totalMonthly > 0) (monthlyAmt / totalMonthly).toFloat() else 0f
            CategorySpend(
                category = cat,
                monthlyTotal = monthlyAmt,
                percentage = pct,
                count = categoryCounts[cat] ?: 1
            )
        }.sortedByDescending { it.monthlyTotal }

        val daysUntilNext = nextSub?.let {
            val diffMs = it.nextBillingDate - now
            val days = (diffMs / (1000 * 60 * 60 * 24)).toInt()
            if (days < 0) 0 else days
        }

        DashboardMetrics(
            monthlyTotal = totalMonthly,
            yearlyTotal = totalYearly,
            activeCount = activeCount,
            pausedCount = pausedCount,
            totalCount = subs.size,
            monthlySavingsFromPaused = totalPausedMonthly,
            nextUpcoming = nextSub,
            daysUntilNextUpcoming = daysUntilNext,
            categorySpends = categorySpends
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardMetrics(0.0, 0.0, 0, 0, 0, 0.0, null, null, emptyList())
    )

    init {
        // Preload sample data if current user has no subscriptions
        viewModelScope.launch {
            val user = currentUser.value
            val currentSubs = subscriptionRepository.getSubscriptions(user.id)
            currentSubs.collect { list ->
                if (list.isEmpty()) {
                    subscriptionRepository.preloadSampleData(user.id)
                }
            }
        }
    }

    // UI actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: SubscriptionCategory?) {
        _selectedCategory.value = category
    }

    fun setStatusFilter(status: String) {
        _selectedStatus.value = status
    }

    fun setSortOrder(order: SubscriptionSortOrder) {
        _sortOrder.value = order
    }

    // 100% Free App: All features completely free & unrestricted!
    fun openAddSubscription() {
        _editingSubscription.value = null
        _showAddEdit.value = true
    }

    fun openEditSubscription(subscription: Subscription) {
        _editingSubscription.value = subscription
        _showAddEdit.value = true
    }

    fun closeAddEdit() {
        _showAddEdit.value = false
        _editingSubscription.value = null
    }

    fun openPaywall() {
        _showPaywall.value = true
    }

    fun closePaywall() {
        _showPaywall.value = false
    }

    fun upgradeToPro() {
        _showPaywall.value = false
        _snackbarMessage.value = "All features are already 100% free!"
    }

    fun openAutoDetect() {
        _showAutoDetect.value = true
    }

    fun closeAutoDetect() {
        _showAutoDetect.value = false
    }

    fun openProfileDialog() {
        _showProfileDialog.value = true
    }

    fun closeProfileDialog() {
        _showProfileDialog.value = false
    }

    fun openCountryPicker() {
        _showCountryPicker.value = true
    }

    fun closeCountryPicker() {
        _showCountryPicker.value = false
    }

    fun setDarkModePreference(mode: String) {
        _darkModePreference.value = mode
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun saveSubscription(
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
    ) {
        viewModelScope.launch {
            val user = currentUser.value
            val subId = id ?: UUID.randomUUID().toString()
            val sub = Subscription(
                id = subId,
                userId = user.id,
                name = name.trim(),
                amount = amount,
                currency = currency,
                billingCycle = cycle,
                category = category,
                nextBillingDate = nextBillingDate,
                paymentMethod = paymentMethod.trim().ifEmpty { "Credit Card" },
                isActive = isActive,
                reminderDaysBefore = reminderDaysBefore,
                notes = notes.trim(),
                updatedAt = System.currentTimeMillis()
            )

            if (id == null) {
                subscriptionRepository.insertSubscription(sub)
                _snackbarMessage.value = "Added ${sub.name}"
            } else {
                subscriptionRepository.updateSubscription(sub)
                _snackbarMessage.value = "Updated ${sub.name}"
            }
            closeAddEdit()
        }
    }

    fun toggleSubscriptionActive(subscription: Subscription) {
        viewModelScope.launch {
            val updated = subscription.copy(isActive = !subscription.isActive)
            subscriptionRepository.updateSubscription(updated)
            _snackbarMessage.value = if (updated.isActive) "Resumed ${subscription.name}" else "Paused ${subscription.name}"
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            subscriptionRepository.deleteSubscription(subscription.id)
            _snackbarMessage.value = "Deleted ${subscription.name}"
        }
    }

    fun importDetectedCharge(charge: DetectedCharge) {
        viewModelScope.launch {
            val user = currentUser.value
            val cal = Calendar.getInstance()
            when (charge.billingCycle) {
                BillingCycle.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
                BillingCycle.MONTHLY -> cal.add(Calendar.MONTH, 1)
                BillingCycle.QUARTERLY -> cal.add(Calendar.MONTH, 3)
                BillingCycle.YEARLY -> cal.add(Calendar.YEAR, 1)
            }

            val newSub = Subscription(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                name = charge.serviceName,
                amount = charge.amount,
                currency = charge.currency,
                billingCycle = charge.billingCycle,
                category = charge.category,
                nextBillingDate = cal.timeInMillis,
                paymentMethod = charge.paymentMethod,
                isActive = true,
                reminderDaysBefore = 3,
                notes = "Auto-detected from receipt/SMS"
            )

            subscriptionRepository.insertSubscription(newSub)
            _showAutoDetect.value = false
            _snackbarMessage.value = "Imported ${newSub.name} successfully!"
        }
    }

    fun preloadSampleData() {
        viewModelScope.launch {
            subscriptionRepository.preloadSampleData(currentUser.value.id)
            _snackbarMessage.value = "Sample subscriptions loaded"
        }
    }

    fun setCountryAndCurrency(country: CurrencyHelper.CountryInfo) {
        viewModelScope.launch {
            userRepository.setCountryAndCurrency(country.countryName, country.currencyCode)
            _showCountryPicker.value = false
            _snackbarMessage.value = "Set to ${country.flag} ${country.countryName} (${country.currencyCode} ${country.currencySymbol})"
        }
    }

    fun setCurrency(currencyCode: String) {
        viewModelScope.launch {
            val country = CurrencyHelper.getCountryByCode(currencyCode)
            if (country != null) {
                userRepository.setCountryAndCurrency(country.countryName, currencyCode)
            } else {
                userRepository.setCurrency(currencyCode)
            }
            _snackbarMessage.value = "Display currency: $currencyCode"
        }
    }

    fun setNotificationTiming(days: Int) {
        viewModelScope.launch {
            userRepository.setNotificationTiming(days, 9, 0)
            _snackbarMessage.value = "Reminder set to $days days before renewal"
        }
    }

    fun switchUser(user: UserProfile) {
        userRepository.switchUser(user)
        _showProfileDialog.value = false
        _snackbarMessage.value = "Switched to ${user.name}"
    }

    fun createNewUser(name: String, email: String, provider: String = "local") {
        viewModelScope.launch {
            val currUser = currentUser.value
            val newUser = userRepository.createUser(
                name = name,
                email = email,
                authProvider = provider,
                country = currUser.country,
                currency = currUser.currencyPreference
            )
            // Preload sample subscriptions for new user
            subscriptionRepository.preloadSampleData(newUser.id)
            _showProfileDialog.value = false
            _snackbarMessage.value = "Created profile for $name"
        }
    }

    fun deleteAccount(userId: String) {
        viewModelScope.launch {
            subscriptionRepository.deleteAllForUser(userId)
            userRepository.deleteUser(userId)
            _showProfileDialog.value = false
            _snackbarMessage.value = "Account and data deleted"
        }
    }

    fun exportData(context: Context, format: String) {
        val subs = subscriptions.value
        if (subs.isEmpty()) {
            _snackbarMessage.value = "No subscriptions to export"
            return
        }

        if (format.equals("CSV", ignoreCase = true)) {
            val csv = ExportHelper.generateCsv(subs)
            ExportHelper.shareExport(context, csv, "subtrack_export.csv", "text/csv")
        } else {
            val json = ExportHelper.generateJson(subs)
            ExportHelper.shareExport(context, json, "subtrack_export.json", "application/json")
        }
        _snackbarMessage.value = "Export generated!"
    }

    fun sendTestRenewalNotification(context: Context) {
        val metrics = dashboardMetrics.value
        val sub = metrics.nextUpcoming ?: subscriptions.value.firstOrNull()
        if (sub != null) {
            val formatted = CurrencyHelper.format(sub.amount, sub.currency)
            val days = metrics.daysUntilNextUpcoming ?: 2
            NotificationHelper.sendRenewalNotification(
                context = context,
                notificationId = (sub.id.hashCode() and 0x7FFFFFFF),
                subscriptionName = sub.name,
                amountFormatted = formatted,
                daysRemaining = days
            )
            _snackbarMessage.value = "Sent test alert for ${sub.name}"
        } else {
            NotificationHelper.sendTestAlert(context)
            _snackbarMessage.value = "Sent test renewal alert"
        }
    }

    companion object {
        fun provideFactory(
            subscriptionRepository: SubscriptionRepository,
            userRepository: UserRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SubscriptionViewModel(subscriptionRepository, userRepository) as T
            }
        }
    }
}

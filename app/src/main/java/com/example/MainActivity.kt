package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CountryCurrencyPickerDialog
import com.example.ui.components.PaywallDialog
import com.example.ui.screens.AddEditSubscriptionScreen
import com.example.ui.screens.AutoDetectChargeScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.ProfileSwitchDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubscriptionsListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SubscriptionViewModel
import com.example.util.CurrencyHelper

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    SUBSCRIPTIONS("Subscriptions", Icons.Filled.CreditCard, Icons.Outlined.CreditCard),
    INSIGHTS("Insights", Icons.Filled.Insights, Icons.Outlined.Insights),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: SubscriptionViewModel by viewModels {
        val app = application as SubTrackApplication
        SubscriptionViewModel.provideFactory(
            subscriptionRepository = app.subscriptionRepository,
            userRepository = app.userRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkModePref by viewModel.darkModePreference.collectAsStateWithLifecycle()
            val isDarkTheme = when (darkModePref) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                SubTrackApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubTrackApp(viewModel: SubscriptionViewModel) {
    var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val showPaywall by viewModel.showPaywall.collectAsStateWithLifecycle()
    val showAutoDetect by viewModel.showAutoDetect.collectAsStateWithLifecycle()
    val showAddEdit by viewModel.showAddEdit.collectAsStateWithLifecycle()
    val editingSub by viewModel.editingSubscription.collectAsStateWithLifecycle()
    val showProfileDialog by viewModel.showProfileDialog.collectAsStateWithLifecycle()
    val showCountryPicker by viewModel.showCountryPicker.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Secondary Full Screen: Add/Edit
    if (showAddEdit) {
        BackHandler {
            viewModel.closeAddEdit()
        }
        AddEditSubscriptionScreen(
            initialSubscription = editingSub,
            defaultCurrency = currentUser.currencyPreference,
            onSave = { id, name, amount, curr, cycle, cat, nextDate, method, active, reminder, notes ->
                viewModel.saveSubscription(
                    id = id,
                    name = name,
                    amount = amount,
                    currency = curr,
                    cycle = cycle,
                    category = cat,
                    nextBillingDate = nextDate,
                    paymentMethod = method,
                    isActive = active,
                    reminderDaysBefore = reminder,
                    notes = notes
                )
            },
            onDelete = { sub ->
                viewModel.deleteSubscription(sub)
            },
            onDismiss = { viewModel.closeAddEdit() }
        )
        return
    }

    // Secondary Full Screen: Auto Detect Charge
    if (showAutoDetect) {
        BackHandler {
            viewModel.closeAutoDetect()
        }
        AutoDetectChargeScreen(
            onImport = { charge ->
                viewModel.importDetectedCharge(charge)
            },
            onDismiss = { viewModel.closeAutoDetect() }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SubTrack",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Country & Currency quick selector button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clickable { viewModel.openCountryPicker() }
                            .testTag("appbar_country_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = CurrencyHelper.getFlag(currentUser.currencyPreference),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${currentUser.currencyPreference} ${CurrencyHelper.getSymbol(currentUser.currencyPreference)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Profile button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { viewModel.openProfileDialog() }
                            .testTag("appbar_profile_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentUser.name.split(" ").firstOrNull() ?: "Profile",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                for (tab in NavigationTab.values()) {
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSubscriptions = { currentTab = NavigationTab.SUBSCRIPTIONS },
                    onNavigateToInsights = { currentTab = NavigationTab.INSIGHTS }
                )
                NavigationTab.SUBSCRIPTIONS -> SubscriptionsListScreen(
                    viewModel = viewModel
                )
                NavigationTab.INSIGHTS -> InsightsScreen(
                    viewModel = viewModel
                )
                NavigationTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    // Country & Currency Picker Dialog
    if (showCountryPicker) {
        CountryCurrencyPickerDialog(
            currentCountryName = currentUser.country,
            currentCurrencyCode = currentUser.currencyPreference,
            onSelect = { country ->
                viewModel.setCountryAndCurrency(country)
            },
            onDismiss = { viewModel.closeCountryPicker() }
        )
    }

    // Paywall Dialog
    if (showPaywall) {
        PaywallDialog(
            onDismiss = { viewModel.closePaywall() },
            onUpgrade = { viewModel.upgradeToPro() }
        )
    }

    // Profile Switcher Dialog
    if (showProfileDialog) {
        ProfileSwitchDialog(
            currentUser = currentUser,
            allUsers = allUsers,
            onSelectUser = { viewModel.switchUser(it) },
            onCreateUser = { name, email -> viewModel.createNewUser(name, email) },
            onDismiss = { viewModel.closeProfileDialog() }
        )
    }
}

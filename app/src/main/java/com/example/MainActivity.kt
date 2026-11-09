package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.ui.components.AddTransactionDialog
import com.example.ui.navigation.AppScreen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel

import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricSecurityManager

class MainActivity : FragmentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

                // Session lock: Always show login screen every time app is opened (starts false)
                var isSessionUnlocked by remember { mutableStateOf(false) }
                var lastBackgroundTime by remember { mutableLongStateOf(0L) }
                val lifecycleOwner = LocalLifecycleOwner.current

                // Function to prompt biometric unlock automatically
                val promptBiometricAutoUnlock = {
                    if (isLoggedIn && !isSessionUnlocked && userProfile?.isBiometricEnabled == true && BiometricSecurityManager.canAuthenticate(this@MainActivity)) {
                        BiometricSecurityManager.authenticate(
                            activity = this@MainActivity,
                            title = "Unlock Finance Pro",
                            subtitle = "Touch fingerprint sensor to unlock",
                            onSucceeded = {
                                isSessionUnlocked = true
                            },
                            onError = {
                                // Keep locked on failure/cancel, allowing fallback to email/password
                            }
                        )
                    }
                }

                // Automatic prompt on initial open
                LaunchedEffect(isLoggedIn, userProfile?.isBiometricEnabled) {
                    if (isLoggedIn && !isSessionUnlocked && userProfile?.isBiometricEnabled == true) {
                        promptBiometricAutoUnlock()
                    }
                }

                // Auto-lock: if backgrounded/minimized for >= 1 minute (60,000ms), lock app
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_STOP -> {
                                lastBackgroundTime = System.currentTimeMillis()
                            }
                            Lifecycle.Event.ON_START -> {
                                if (lastBackgroundTime > 0L) {
                                    val elapsed = System.currentTimeMillis() - lastBackgroundTime
                                    if (elapsed >= 60_000L) {
                                        isSessionUnlocked = false
                                        // Prompt biometric immediately upon return
                                        promptBiometricAutoUnlock()
                                    }
                                    lastBackgroundTime = 0L
                                }
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
                var showAddDialog by remember { mutableStateOf(false) }
                var dialogInitialType by remember { mutableStateOf("INCOME") }
                var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
                var showTransferDialog by remember { mutableStateOf(false) }

                val accounts by viewModel.accounts.collectAsStateWithLifecycle()
                val categories by viewModel.categories.collectAsStateWithLifecycle()

                if (!isLoggedIn || !isSessionUnlocked) {
                    AuthScreen(
                        viewModel = viewModel,
                        initialMode = AuthMode.LOGIN,
                        onAuthSuccess = {
                            isSessionUnlocked = true
                            currentScreen = AppScreen.DASHBOARD
                        }
                    )
                } else {
                    // Intercept back button to return to dashboard if on sub-screen
                    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
                        currentScreen = AppScreen.DASHBOARD
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (currentScreen != AppScreen.ADD_TRANSACTION) {
                                TopAppBar(
                                    title = {
                                        Column {
                                            Text(
                                                text = "Finance Pro",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${currentScreen.title} • ${userProfile?.fullName?.ifBlank { "User" } ?: "User"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    actions = {
                                        IconButton(
                                            onClick = { currentScreen = AppScreen.SALARY_LOANS },
                                            modifier = Modifier.testTag("topbar_salary_loans_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Payments,
                                                contentDescription = "Salary & Loans",
                                                tint = if (currentScreen == AppScreen.SALARY_LOANS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = { currentScreen = AppScreen.REPORTS },
                                            modifier = Modifier.testTag("topbar_reports_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PictureAsPdf,
                                                contentDescription = "Reports & Export",
                                                tint = if (currentScreen == AppScreen.REPORTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = { currentScreen = AppScreen.SETTINGS },
                                            modifier = Modifier.testTag("topbar_settings_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = "Profile & Settings",
                                                tint = if (currentScreen == AppScreen.SETTINGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                isSessionUnlocked = false
                                                viewModel.logout()
                                            },
                                            modifier = Modifier.testTag("topbar_logout_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Logout,
                                                contentDescription = "Log Out",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            if (currentScreen != AppScreen.ADD_TRANSACTION) {
                                NavigationBar(
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.DASHBOARD,
                                        onClick = { currentScreen = AppScreen.DASHBOARD },
                                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                        label = { Text("Dashboard") },
                                        modifier = Modifier.testTag("nav_dashboard")
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.DAILY_STATEMENT,
                                        onClick = { currentScreen = AppScreen.DAILY_STATEMENT },
                                        icon = { Icon(Icons.Default.TableChart, contentDescription = "Statement") },
                                        label = { Text("Statement") },
                                        modifier = Modifier.testTag("nav_statement")
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.ACCOUNTS,
                                        onClick = { currentScreen = AppScreen.ACCOUNTS },
                                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Accounts") },
                                        label = { Text("Accounts") },
                                        modifier = Modifier.testTag("nav_accounts")
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.PETROL_EXTRA,
                                        onClick = { currentScreen = AppScreen.PETROL_EXTRA },
                                        icon = { Icon(Icons.Default.LocalGasStation, contentDescription = "Petrol & Extra") },
                                        label = { Text("Fuel/Extra") },
                                        modifier = Modifier.testTag("nav_petrol_extra")
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.STATEMENTS,
                                        onClick = { currentScreen = AppScreen.STATEMENTS },
                                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Monthly & Annual") },
                                        label = { Text("Monthly") },
                                        modifier = Modifier.testTag("nav_statements")
                                    )
                                }
                            }
                        },
                        floatingActionButton = {
                            if (currentScreen != AppScreen.ADD_TRANSACTION) {
                                FloatingActionButton(
                                    onClick = {
                                        transactionToEdit = null
                                        dialogInitialType = "EXPENSE"
                                        currentScreen = AppScreen.ADD_TRANSACTION
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("fab_add_transaction")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                AppScreen.ADD_TRANSACTION -> AddTransactionScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        transactionToEdit = null
                                        currentScreen = AppScreen.DAILY_STATEMENT
                                    },
                                    initialType = dialogInitialType,
                                    transactionToEdit = transactionToEdit
                                )

                                AppScreen.DASHBOARD -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToStatement = { currentScreen = AppScreen.DAILY_STATEMENT },
                                    onNavigateToAccounts = { currentScreen = AppScreen.ACCOUNTS },
                                    onQuickAdd = { type ->
                                        if (type == "TRANSFER") {
                                            showTransferDialog = true
                                        } else {
                                            transactionToEdit = null
                                            dialogInitialType = type
                                            currentScreen = AppScreen.ADD_TRANSACTION
                                        }
                                    }
                                )

                                AppScreen.DAILY_STATEMENT -> DailyStatementScreen(
                                    viewModel = viewModel,
                                    onAddTransaction = {
                                        transactionToEdit = null
                                        dialogInitialType = "EXPENSE"
                                        currentScreen = AppScreen.ADD_TRANSACTION
                                    },
                                    onEditTransaction = { tx ->
                                        transactionToEdit = tx
                                        dialogInitialType = tx.type
                                        currentScreen = AppScreen.ADD_TRANSACTION
                                    }
                                )

                                AppScreen.ACCOUNTS -> AccountsScreen(
                                    viewModel = viewModel,
                                    onOpenTransfer = { showTransferDialog = true }
                                )

                                AppScreen.PETROL_EXTRA -> PetrolExtraScreen(
                                    viewModel = viewModel,
                                    onAddPetrol = {
                                        transactionToEdit = null
                                        dialogInitialType = "PETROL"
                                        currentScreen = AppScreen.ADD_TRANSACTION
                                    },
                                    onAddExtra = {
                                        transactionToEdit = null
                                        dialogInitialType = "EXTRA_EXPENSE"
                                        currentScreen = AppScreen.ADD_TRANSACTION
                                    }
                                )

                                AppScreen.STATEMENTS -> MonthlyAnnualScreen(
                                    viewModel = viewModel
                                )

                                AppScreen.SALARY_LOANS -> SalaryLoansScreen(
                                    viewModel = viewModel
                                )

                                AppScreen.REPORTS -> ReportsExportScreen(
                                    viewModel = viewModel
                                )

                                AppScreen.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }

                        // Add or Edit Transaction Dialog
                        if (showAddDialog) {
                            AddTransactionDialog(
                                initialType = dialogInitialType,
                                transactionToEdit = transactionToEdit,
                                accounts = accounts,
                                categories = categories,
                                onDismiss = {
                                    showAddDialog = false
                                    transactionToEdit = null
                                },
                                onSave = { type, amount, date, time, from, to, cat, comments, extra, liters, rate ->
                                    val editing = transactionToEdit
                                    if (editing != null) {
                                        viewModel.updateTransaction(
                                            oldTx = editing,
                                            date = date,
                                            time = time,
                                            type = type,
                                            amount = amount,
                                            fromAccount = from,
                                            toAccount = to,
                                            category = cat,
                                            comments = comments,
                                            extraItem = extra,
                                            petrolLiters = liters,
                                            petrolRate = rate
                                        ) {
                                            showAddDialog = false
                                            transactionToEdit = null
                                        }
                                    } else {
                                        viewModel.addTransaction(
                                            date = date,
                                            time = time,
                                            type = type,
                                            amount = amount,
                                            fromAccount = from,
                                            toAccount = to,
                                            category = cat,
                                            comments = comments,
                                            extraItem = extra,
                                            petrolLiters = liters,
                                            petrolRate = rate
                                        ) {
                                            showAddDialog = false
                                        }
                                    }
                                }
                            )
                        }

                        // Quick Transfer Dialog
                        if (showTransferDialog) {
                            TransferDialog(
                                accounts = accounts,
                                onDismiss = { showTransferDialog = false },
                                onTransfer = { from, to, amount, comments ->
                                    viewModel.addTransaction(
                                        type = "TRANSFER",
                                        amount = amount,
                                        fromAccount = from,
                                        toAccount = to,
                                        category = "Account Transfer",
                                        comments = comments
                                    ) {
                                        showTransferDialog = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    initialType: String = "EXPENSE",
    transactionToEdit: TransactionEntity? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Form states
    var selectedType by remember { mutableStateOf(transactionToEdit?.type ?: initialType) }
    var amountText by remember { mutableStateOf(if (transactionToEdit != null) transactionToEdit.amount.toString() else "") }
    var categoryText by remember { mutableStateOf(transactionToEdit?.category ?: "") }
    var dateText by remember { mutableStateOf(transactionToEdit?.date ?: DateUtils.getCurrentDate()) }
    val dayText by remember(dateText) {
        derivedStateOf { DateUtils.getDayOfWeek(dateText) }
    }
    var timeText by remember { mutableStateOf(transactionToEdit?.time ?: DateUtils.getCurrentTime()) }

    var fromAccount by remember {
        mutableStateOf(
            transactionToEdit?.fromAccount ?: if (selectedType == "INCOME") "Salary / Income" else "HBL"
        )
    }
    var toAccount by remember {
        mutableStateOf(
            transactionToEdit?.toAccount ?: if (selectedType == "INCOME") "HBL" else "Vendor / General"
        )
    }

    var descriptionText by remember {
        mutableStateOf(transactionToEdit?.description ?: "")
    }
    var commentsText by remember {
        mutableStateOf(transactionToEdit?.comments ?: "")
    }

    // Petrol specific
    var litersText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }

    // Extra Expense specific
    var extraItemText by remember { mutableStateOf("") }

    // Dialog state
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Handle back press
    BackHandler {
        onNavigateBack()
    }

    // Suggested categories based on selected type
    val categorySuggestions = remember(selectedType) {
        when (selectedType) {
            "INCOME" -> listOf("Salary", "Freelance", "Business Profit", "Bonus", "Rental Income", "Investments", "Cash Gift")
            "EXPENSE" -> listOf("Food & Dining", "Groceries", "Utilities", "Electricity Bill", "Shopping", "Transport", "Mobile & Internet", "Medical & Health", "Rent", "Home Supplies", "Education")
            "PETROL" -> listOf("Car Fuel", "Bike Petrol", "Generator Fuel", "Travel Petrol", "Fleet Fuel")
            "EXTRA_EXPENSE" -> listOf("Emergency Repair", "Car Service", "Home Repair", "Gadget Repair", "Medical Emergency")
            else -> listOf("General Transfer", "Bank Deposit", "Internal Transfer")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (transactionToEdit != null) "Edit Transaction" else "New Transaction",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Record dynamic ledger entry with auto account balancing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_transaction_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            amountText = ""
                            categoryText = ""
                            descriptionText = ""
                            commentsText = ""
                            litersText = ""
                            rateText = ""
                            extraItemText = ""
                            errorMessage = null
                        }
                    ) {
                        Text("Clear")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error banner if any
            errorMessage?.let { err ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 1. Transaction Type Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Transaction Type",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val types = listOf(
                        "EXPENSE" to ("Expense" to Icons.Default.ArrowUpward),
                        "INCOME" to ("Income" to Icons.Default.ArrowDownward),
                        "TRANSFER" to ("Transfer" to Icons.Default.SwapHoriz),
                        "PETROL" to ("Petrol" to Icons.Default.LocalGasStation),
                        "EXTRA_EXPENSE" to ("Extra Exp" to Icons.Default.Build)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        types.forEach { (typeKey, pair) ->
                            val (label, icon) = pair
                            val isSelected = selectedType == typeKey
                            val tintColor = when (typeKey) {
                                "INCOME" -> FinanceIncomeGreen
                                "EXPENSE" -> FinanceExpenseRed
                                "TRANSFER" -> FinanceTransferBlue
                                "PETROL" -> FinancePetrolOrange
                                else -> FinanceExtraPurple
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedType = typeKey
                                    if (typeKey == "INCOME" && fromAccount == "HBL") {
                                        fromAccount = "Salary / Income"
                                        toAccount = "HBL"
                                    } else if (typeKey == "TRANSFER") {
                                        if (fromAccount.contains("Salary") || fromAccount.isBlank()) {
                                            fromAccount = "HBL"
                                        }
                                        if (toAccount.contains("Vendor") || toAccount == fromAccount || toAccount.isBlank()) {
                                            toAccount = if (fromAccount == "HBL") "Sadapay" else "HBL"
                                        }
                                    } else if (typeKey != "INCOME" && toAccount == "HBL" && fromAccount.contains("Salary")) {
                                        fromAccount = "HBL"
                                        toAccount = "Vendor / General"
                                    }
                                    if (categorySuggestions.isNotEmpty() && (categoryText.isBlank() || categoryText in categorySuggestions)) {
                                        categoryText = categorySuggestions.first()
                                    }
                                },
                                label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else tintColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tintColor,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("type_chip_$typeKey")
                            )
                        }
                    }
                }
            }

            // 2. Amount Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Amount (PKR / Rs.) *",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            // Allow digits and single decimal point
                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                amountText = input
                                errorMessage = null
                            }
                        },
                        placeholder = { Text("0.00", style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.outlineVariant)) },
                        leadingIcon = {
                            Text(
                                text = "Rs.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            )
                        },
                        trailingIcon = {
                            if (amountText.isNotEmpty()) {
                                IconButton(onClick = { amountText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear amount")
                                }
                            }
                        },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick increment chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickAmounts = listOf(500, 1000, 2000, 5000, 10000, 25000)
                        quickAmounts.forEach { amt ->
                            SuggestionChip(
                                onClick = {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    amountText = (current + amt).toInt().toString()
                                    errorMessage = null
                                },
                                label = { Text("+${DateUtils.formatCurrency(amt.toDouble())}") }
                            )
                        }
                    }
                }
            }

            // 3. Category Input Card (Text input for category + Quick selection chips)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Category *",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Type any category name or select from popular presets below",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = categoryText,
                        onValueChange = {
                            categoryText = it
                            errorMessage = null
                        },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Food, Groceries, Utilities, Fuel...") },
                        leadingIcon = {
                            Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (categoryText.isNotEmpty()) {
                                IconButton(onClick = { categoryText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear category")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_category_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Suggestion Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categorySuggestions.forEach { cat ->
                            val isSelected = categoryText.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    categoryText = cat
                                    errorMessage = null
                                },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // 4. Date & Time Selection Card (Interactive Date Picker)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Date & Time (Asia/Karachi PKT)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Date Field with Tap-to-Open Calendar Picker
                        Box(modifier = Modifier.weight(1.2f)) {
                            OutlinedTextField(
                                value = dateText,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Date") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showDatePicker = true },
                                        modifier = Modifier.testTag("transaction_date_picker_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Select Date",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("transaction_date_input")
                            )
                            // Clickable overlay on date input
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showDatePicker = true }
                            )
                        }

                        // Day Field (read-only, auto-calculated from selected date)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = dayText,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Day") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("transaction_day_input")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Time Field with Tap-to-Open Time Picker (12-hour AM/PM)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = DateUtils.format12HourTime(timeText),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Time (12-Hour AM/PM)") },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showTimePicker = true },
                                    modifier = Modifier.testTag("transaction_time_picker_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Select Time",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transaction_time_input")
                        )
                        // Clickable overlay on time input
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTimePicker = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date human-readable info & shortcuts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${DateUtils.formatReadableDateWithDay(dateText, dayText)} • ${DateUtils.format12HourTime(timeText)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = {
                                    dateText = DateUtils.getCurrentDate()
                                    timeText = DateUtils.getCurrentTime()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Today / Now", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // 5. Account Management & Source Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Account Breakdown",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    when (selectedType) {
                        "INCOME" -> {
                            OutlinedTextField(
                                value = fromAccount,
                                onValueChange = { fromAccount = it },
                                label = { Text("Received From (Payee / Source)") },
                                placeholder = { Text("e.g. Employer, Client, Cash") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Deposited Into Account *", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            AccountSelectorGrid(
                                accounts = accounts,
                                selectedAccount = toAccount,
                                onSelectAccount = { toAccount = it }
                            )
                        }

                        "TRANSFER" -> {
                            Text(
                                text = "Transfer From (Source Bank) *",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            AccountSelectorGrid(
                                accounts = accounts,
                                selectedAccount = fromAccount,
                                tagPrefix = "transfer_from",
                                onSelectAccount = { bank ->
                                    fromAccount = bank
                                    if (bank.equals(toAccount, ignoreCase = true)) {
                                        toAccount = if (bank == "HBL") "Sadapay" else "HBL"
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Swap Accounts button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        val temp = fromAccount
                                        fromAccount = toAccount
                                        toAccount = temp
                                    },
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("screen_swap_accounts_button"),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = "Swap Banks",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Swap Accounts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Transfer To (Destination Bank) *",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FinanceTransferBlue
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            AccountSelectorGrid(
                                accounts = accounts,
                                selectedAccount = toAccount,
                                tagPrefix = "transfer_to",
                                onSelectAccount = { bank ->
                                    toAccount = bank
                                    if (bank.equals(fromAccount, ignoreCase = true)) {
                                        fromAccount = if (bank == "HBL") "Sadapay" else "HBL"
                                    }
                                }
                            )
                        }

                        else -> { // EXPENSE, PETROL, EXTRA_EXPENSE
                            Text("Paid From Account *", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            AccountSelectorGrid(
                                accounts = accounts,
                                selectedAccount = fromAccount,
                                onSelectAccount = { fromAccount = it }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = toAccount,
                                onValueChange = { toAccount = it },
                                label = { Text("Paid To / Vendor / Beneficiary") },
                                placeholder = { Text("e.g. Supermarket, Shell Petrol, Electric Supply") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 6. Conditional Fields (Petrol or Extra Expense)
            if (selectedType == "PETROL") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FinancePetrolBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = FinancePetrolOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Petrol Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = litersText,
                                onValueChange = {
                                    litersText = it
                                    val l = it.toDoubleOrNull()
                                    val r = rateText.toDoubleOrNull()
                                    if (l != null && r != null && l > 0 && r > 0) {
                                        amountText = String.format(Locale.US, "%.2f", l * r)
                                    }
                                },
                                label = { Text("Fuel Liters") },
                                placeholder = { Text("e.g. 15.5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = rateText,
                                onValueChange = {
                                    rateText = it
                                    val l = litersText.toDoubleOrNull()
                                    val r = it.toDoubleOrNull()
                                    if (l != null && r != null && l > 0 && r > 0) {
                                        amountText = String.format(Locale.US, "%.2f", l * r)
                                    }
                                },
                                label = { Text("Rate / Liter (Rs.)") },
                                placeholder = { Text("e.g. 275.50") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (selectedType == "EXTRA_EXPENSE") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FinanceExtraBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = FinanceExtraPurple)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extra Expense Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = extraItemText,
                            onValueChange = { extraItemText = it },
                            label = { Text("Item / Repair Name *") },
                            placeholder = { Text("e.g. AC Gas Refill, Water Motor Repair") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 7. Description & Comments
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Description & Notes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        label = { Text("Description / Particulars") },
                        placeholder = { Text("e.g. Monthly grocery purchase at Metro") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = commentsText,
                        onValueChange = { commentsText = it },
                        label = { Text("Comments / Reference No.") },
                        placeholder = { Text("e.g. Transaction ID, Receipt #1042") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 8. Submit / Save Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0.0) {
                        errorMessage = "Please enter a valid transaction amount greater than 0."
                        return@Button
                    }
                    if (categoryText.isBlank() && selectedType != "TRANSFER") {
                        errorMessage = "Please enter or select a category for this transaction."
                        return@Button
                    }
                    if (selectedType == "TRANSFER" && fromAccount == toAccount) {
                        errorMessage = "Source and destination accounts cannot be the same."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null

                    val categoryToSave = if (selectedType == "TRANSFER") "Account Transfer" else categoryText.trim()
                    val descToSave = descriptionText.ifBlank { categoryToSave }

                    val onSavedCallback = {
                        isSubmitting = false
                        Toast.makeText(context, "Transaction recorded successfully!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }

                    val editing = transactionToEdit
                    if (editing != null) {
                        viewModel.updateTransaction(
                            oldTx = editing,
                            date = dateText,
                            time = timeText,
                            type = selectedType,
                            amount = amount,
                            fromAccount = fromAccount,
                            toAccount = toAccount,
                            category = categoryToSave,
                            comments = commentsText,
                            extraItem = extraItemText,
                            petrolLiters = litersText.toDoubleOrNull() ?: 0.0,
                            petrolRate = rateText.toDoubleOrNull() ?: 0.0,
                            onComplete = onSavedCallback
                        )
                    } else {
                        viewModel.addTransaction(
                            date = dateText,
                            time = timeText,
                            type = selectedType,
                            amount = amount,
                            fromAccount = fromAccount,
                            toAccount = toAccount,
                            category = categoryToSave,
                            comments = commentsText,
                            description = descToSave,
                            extraItem = extraItemText,
                            petrolLiters = litersText.toDoubleOrNull() ?: 0.0,
                            petrolRate = rateText.toDoubleOrNull() ?: 0.0,
                            onComplete = onSavedCallback
                        )
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_transaction_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (transactionToEdit != null) "Update Transaction" else "Save Transaction",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Interactive Calendar DatePicker Dialog
    if (showDatePicker) {
        val initialEpochMillis = remember(dateText) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(dateText)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpochMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            dateText = sdf.format(Date(millis))
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("confirm_date_picker_button")
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Interactive TimePicker Dialog (12-hour AM/PM with digits)
    if (showTimePicker) {
        val (initHour, initMinute) = remember(timeText) {
            try {
                val canonical24 = DateUtils.to24HourTime(timeText)
                val parts = canonical24.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                Pair(h, m)
            } catch (_: Exception) {
                Pair(12, 0)
            }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initHour,
            initialMinute = initMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        timeText = String.format(
                            Locale.US,
                            "%02d:%02d:00",
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        showTimePicker = false
                    },
                    modifier = Modifier.testTag("confirm_time_picker_button")
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Select Transaction Time (PKT)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

@Composable
private fun AccountSelectorGrid(
    accounts: List<AccountEntity>,
    selectedAccount: String,
    tagPrefix: String = "account",
    onSelectAccount: (String) -> Unit
) {
    val standardBankNames = remember {
        listOf("HBL", "UBL", "Sadapay", "Easypaisa", "JazzCash", "Mashreq", "Wallet")
    }
    val effectiveAccounts = remember(accounts) {
        if (accounts.isNotEmpty()) accounts
        else standardBankNames.map { AccountEntity(id = it.lowercase(), name = it, openingBalance = 0.0, currentBalance = 0.0) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        effectiveAccounts.forEach { acc ->
            val isSelected = selectedAccount.equals(acc.name, ignoreCase = true)
            val isWallet = isWalletAccount(acc)
            val icon = if (isWallet) Icons.Default.AccountBalanceWallet else Icons.Default.AccountBalance

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isSelected)
                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                else
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectAccount(acc.name) }
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("${tagPrefix}_chip_${acc.name}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = acc.name,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = acc.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = DateUtils.formatCurrency(acc.currentBalance),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

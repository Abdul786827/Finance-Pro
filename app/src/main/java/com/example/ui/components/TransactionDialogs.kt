package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.FinanceTransferBlue
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    initialType: String = "INCOME",
    transactionToEdit: TransactionEntity? = null,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        amount: Double,
        date: String,
        time: String,
        fromAccount: String,
        toAccount: String,
        category: String,
        comments: String,
        extraItem: String,
        liters: Double,
        rate: Double
    ) -> Unit
) {
    var selectedType by remember {
        mutableStateOf(transactionToEdit?.type ?: initialType)
    }
    var amountText by remember {
        mutableStateOf(if (transactionToEdit != null) transactionToEdit.amount.toString() else "")
    }
    var dateText by remember {
        mutableStateOf(transactionToEdit?.date ?: DateUtils.getCurrentDate())
    }
    val dayText by remember(dateText) {
        derivedStateOf { DateUtils.getDayOfWeek(dateText) }
    }
    var timeText by remember {
        mutableStateOf(transactionToEdit?.time ?: DateUtils.getCurrentTime())
    }

    val standardBankNames = remember {
        listOf("HBL", "UBL", "Sadapay", "Easypaisa", "JazzCash", "Mashreq", "Wallet")
    }
    val effectiveAccounts = remember(accounts) {
        if (accounts.isNotEmpty()) accounts
        else standardBankNames.map { AccountEntity(id = it.lowercase(), name = it, openingBalance = 0.0, currentBalance = 0.0) }
    }

    var fromAccount by remember {
        mutableStateOf(transactionToEdit?.fromAccount ?: (effectiveAccounts.firstOrNull()?.name ?: "HBL"))
    }
    var toAccount by remember {
        mutableStateOf(transactionToEdit?.toAccount ?: (effectiveAccounts.getOrNull(2)?.name ?: effectiveAccounts.getOrNull(1)?.name ?: "Sadapay"))
    }
    var selectedCategory by remember {
        mutableStateOf(transactionToEdit?.category ?: (categories.firstOrNull()?.name ?: "General"))
    }
    var commentsText by remember {
        mutableStateOf(transactionToEdit?.comments ?: "")
    }
    var extraItemText by remember {
        mutableStateOf(
            if (transactionToEdit?.type == "EXTRA_EXPENSE")
                transactionToEdit.description.removePrefix("Extra: ")
            else ""
        )
    }
    var litersText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("277.5") }

    // Date & Time Picker visibility states
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val types = listOf("INCOME", "EXPENSE", "TRANSFER", "PETROL", "EXTRA_EXPENSE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (transactionToEdit != null) "Edit Transaction (#${transactionToEdit.serialNumber})" else "New Transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type selector chips
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    types.forEachIndexed { index, type ->
                        val label = when (type) {
                            "INCOME" -> "Income"
                            "EXPENSE" -> "Expense"
                            "TRANSFER" -> "Transfer"
                            "PETROL" -> "Petrol"
                            "EXTRA_EXPENSE" -> "Extra"
                            else -> type
                        }
                        SegmentedButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                            modifier = Modifier.testTag("type_button_$type")
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (PKR / Rs.)") },
                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input"),
                    singleLine = true
                )

                // Date & Day Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date field - Tapping opens Calendar DatePicker
                    Box(modifier = Modifier.weight(1.2f)) {
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Open Date Picker",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transaction_date_input"),
                            singleLine = true
                        )
                        // Clickable overlay on date text field
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }

                    // Day field - Auto-calculated, read-only
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transaction_day_input"),
                            singleLine = true
                        )
                    }
                }

                // Time field - 12-Hour format with AM/PM
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = DateUtils.format12HourTime(timeText),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time (12-Hour AM/PM)") },
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Open Time Picker",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_time_input"),
                        singleLine = true
                    )
                    // Clickable overlay on time text field
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showTimePicker = true }
                    )
                }

                Text(
                    text = "${DateUtils.formatReadableDateWithDay(dateText, dayText)} • ${DateUtils.format12HourTime(timeText)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Accounts based on type
                when (selectedType) {
                    "INCOME" -> {
                        OutlinedTextField(
                            value = fromAccount,
                            onValueChange = { fromAccount = it },
                            label = { Text("Received From (e.g. Salary, Client)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Deposited Into Account *",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        DialogAccountSelectorRow(
                            accounts = effectiveAccounts,
                            selectedAccount = toAccount,
                            onSelectAccount = { toAccount = it }
                        )
                    }

                    "EXPENSE", "PETROL", "EXTRA_EXPENSE" -> {
                        Text(
                            text = "Paid From Account *",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        DialogAccountSelectorRow(
                            accounts = effectiveAccounts,
                            selectedAccount = fromAccount,
                            onSelectAccount = { fromAccount = it }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = toAccount,
                            onValueChange = { toAccount = it },
                            label = { Text("Paid To / Vendor / Payee") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    "TRANSFER" -> {
                        Text(
                            text = "Source Account (From) *",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        DialogAccountSelectorRow(
                            accounts = effectiveAccounts,
                            selectedAccount = fromAccount,
                            onSelectAccount = { bank ->
                                fromAccount = bank
                                if (bank.equals(toAccount, ignoreCase = true)) {
                                    toAccount = effectiveAccounts.firstOrNull { !it.name.equals(bank, ignoreCase = true) }?.name ?: "Sadapay"
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Swap Banks Button
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
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp).testTag("dialog_swap_accounts_button")
                            ) {
                                Icon(Icons.Default.SwapVert, contentDescription = "Swap Banks", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Swap Accounts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Destination Account (To) *",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FinanceTransferBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        DialogAccountSelectorRow(
                            accounts = effectiveAccounts,
                            selectedAccount = toAccount,
                            onSelectAccount = { bank ->
                                toAccount = bank
                                if (bank.equals(fromAccount, ignoreCase = true)) {
                                    fromAccount = effectiveAccounts.firstOrNull { !it.name.equals(bank, ignoreCase = true) }?.name ?: "HBL"
                                }
                            }
                        )
                    }
                }

                // Category (for Income or Expense)
                if (selectedType != "TRANSFER") {
                    Text(
                        text = "Category *",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DialogCategorySelectorRow(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it }
                    )
                }

                // Specific fields for Petrol
                if (selectedType == "PETROL") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = litersText,
                            onValueChange = { litersText = it },
                            label = { Text("Liters") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = { rateText = it },
                            label = { Text("Rate / Liter") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                // Specific fields for Extra Expense
                if (selectedType == "EXTRA_EXPENSE") {
                    OutlinedTextField(
                        value = extraItemText,
                        onValueChange = { extraItemText = it },
                        label = { Text("Item / Repair Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Comments / Description
                OutlinedTextField(
                    value = commentsText,
                    onValueChange = { commentsText = it },
                    label = { Text("Comments / Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(
                            selectedType,
                            amt,
                            dateText,
                            timeText,
                            fromAccount,
                            toAccount,
                            selectedCategory,
                            commentsText,
                            extraItemText,
                            litersText.toDoubleOrNull() ?: 0.0,
                            rateText.toDoubleOrNull() ?: 0.0
                        )
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text(if (transactionToEdit != null) "Update Record" else "Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Calendar DatePicker Dialog
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
                    }
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

    // TimePicker Dialog with selectable digits (hours, minutes, AM/PM)
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
                    }
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
                Text("Select Time (12-Hour AM/PM)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
private fun DialogAccountSelectorRow(
    accounts: List<AccountEntity>,
    selectedAccount: String,
    onSelectAccount: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        accounts.forEach { acc ->
            val isSelected = selectedAccount.equals(acc.name, ignoreCase = true)
            val isWallet = isWalletAccount(acc)
            val icon = if (isWallet) Icons.Default.AccountBalanceWallet else Icons.Default.AccountBalance

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isSelected)
                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                else
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectAccount(acc.name) }
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("dialog_account_${acc.name}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                        Text(
                            text = acc.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
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

@Composable
private fun DialogCategorySelectorRow(
    categories: List<CategoryEntity>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(cat.name) },
                label = { Text(cat.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("dialog_category_${cat.name}")
            )
        }
    }
}

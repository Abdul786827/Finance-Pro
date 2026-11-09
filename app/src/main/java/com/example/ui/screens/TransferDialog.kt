package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.DateUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onTransfer: (from: String, to: String, amount: Double, comments: String) -> Unit
) {
    val standardBankNames = remember {
        listOf("HBL", "UBL", "Sadapay", "Easypaisa", "JazzCash", "Mashreq", "Wallet")
    }

    // Always guarantee all 7 accounts exist even if the database is still loading
    val effectiveAccounts = remember(accounts) {
        if (accounts.isNotEmpty()) {
            accounts
        } else {
            standardBankNames.map {
                AccountEntity(id = it.lowercase(), name = it, openingBalance = 0.0, currentBalance = 0.0)
            }
        }
    }

    var fromAccount by remember {
        mutableStateOf(effectiveAccounts.firstOrNull()?.name ?: "HBL")
    }
    var toAccount by remember {
        mutableStateOf(
            effectiveAccounts.getOrNull(2)?.name
                ?: effectiveAccounts.getOrNull(1)?.name
                ?: "Sadapay"
        )
    }
    var amountText by remember { mutableStateOf("") }
    var commentsText by remember { mutableStateOf("") }

    // Dialog pickers for full vertical list selection if user wants a full modal view
    var showFromBankPicker by remember { mutableStateOf(false) }
    var showToBankPicker by remember { mutableStateOf(false) }

    val fromAcc = effectiveAccounts.find { it.name == fromAccount }
    val toAcc = effectiveAccounts.find { it.name == toAccount }
    val amountNum = amountText.toDoubleOrNull() ?: 0.0
    val isSameAccount = fromAccount.equals(toAccount, ignoreCase = true)
    val isValid = amountNum > 0.0 && !isSameAccount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = FinanceTransferBlue.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = FinanceTransferBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Transfer Between Accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time money transfer between banks & wallets",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FinanceTransferBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = FinanceTransferBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Deducts from Source and deposits into Destination. Combined ledger balance remains unchanged.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 1. SOURCE BANK (FROM) SECTION
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transfer From (Source Bank) *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { showFromBankPicker = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp).testTag("browse_from_banks_button")
                        ) {
                            Text("View All", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Selected Bank Header Banner
                    SelectedBankBanner(
                        account = fromAcc,
                        label = "Selected Source Bank",
                        tintColor = MaterialTheme.colorScheme.primary,
                        onClick = { showFromBankPicker = true },
                        testTag = "selected_from_bank_banner"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Direct Clickable Bank Selector Row
                    Text(
                        text = "Tap a bank to select:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BankHorizontalList(
                        accounts = effectiveAccounts,
                        selectedBankName = fromAccount,
                        tagPrefix = "from_bank",
                        onSelectBank = { bankName ->
                            fromAccount = bankName
                            if (bankName.equals(toAccount, ignoreCase = true)) {
                                // Automatically shift destination to a different bank
                                toAccount = effectiveAccounts.firstOrNull { !it.name.equals(bankName, ignoreCase = true) }?.name ?: "Sadapay"
                            }
                        }
                    )
                }

                // 2. SWAP ACCOUNTS BUTTON
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            val temp = fromAccount
                            fromAccount = toAccount
                            toAccount = temp
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .testTag("swap_transfer_accounts_button")
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Swap Banks",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Swap Accounts",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. DESTINATION BANK (TO) SECTION
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transfer To (Destination Bank) *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = FinanceTransferBlue
                        )
                        TextButton(
                            onClick = { showToBankPicker = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp).testTag("browse_to_banks_button")
                        ) {
                            Text("View All", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Selected Destination Bank Banner
                    SelectedBankBanner(
                        account = toAcc,
                        label = "Selected Destination Bank",
                        tintColor = FinanceTransferBlue,
                        onClick = { showToBankPicker = true },
                        testTag = "selected_to_bank_banner"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Tap a bank to select:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BankHorizontalList(
                        accounts = effectiveAccounts,
                        selectedBankName = toAccount,
                        tagPrefix = "to_bank",
                        onSelectBank = { bankName ->
                            toAccount = bankName
                            if (bankName.equals(fromAccount, ignoreCase = true)) {
                                fromAccount = effectiveAccounts.firstOrNull { !it.name.equals(bankName, ignoreCase = true) }?.name ?: "HBL"
                            }
                        }
                    )
                }

                // Validation Warning if accounts match
                if (isSameAccount) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Source and Destination accounts cannot be the same bank.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 4. AMOUNT INPUT & QUICK CHIPS
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Transfer Amount (PKR / Rs.) *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                amountText = input
                            }
                        },
                        label = { Text("Amount (Rs.)") },
                        placeholder = { Text("0.00") },
                        leadingIcon = {
                            Text(
                                text = "Rs.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            )
                        },
                        trailingIcon = {
                            if (amountText.isNotEmpty()) {
                                IconButton(onClick = { amountText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_amount_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick fill chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickAmounts = listOf(500, 1000, 2000, 5000, 10000)
                        quickAmounts.forEach { amt ->
                            SuggestionChip(
                                onClick = {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    amountText = (current + amt).toInt().toString()
                                },
                                label = { Text("+${DateUtils.formatCurrency(amt.toDouble())}", fontSize = 11.sp) }
                            )
                        }
                        // Fill max available balance from source bank
                        fromAcc?.let { acc ->
                            if (acc.currentBalance > 0) {
                                SuggestionChip(
                                    onClick = {
                                        amountText = String.format(Locale.US, "%.2f", acc.currentBalance)
                                    },
                                    label = { Text("Full (${DateUtils.formatCurrency(acc.currentBalance)})", fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                }

                // 5. COMMENTS / REASON
                OutlinedTextField(
                    value = commentsText,
                    onValueChange = { commentsText = it },
                    label = { Text("Reason / Comments (Optional)") },
                    placeholder = { Text("e.g. Monthly Savings, Fund Transfer") },
                    modifier = Modifier.fillMaxWidth().testTag("transfer_comments_input"),
                    singleLine = true
                )

                // 6. LIVE TRANSFER PREVIEW
                if (isValid) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Transfer Preview:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Rs. ${DateUtils.formatCurrency(amountNum)}: $fromAccount ➔ $toAccount",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onTransfer(fromAccount, toAccount, amountNum, commentsText)
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("confirm_transfer_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm Transfer")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_transfer_button")
            ) {
                Text("Cancel")
            }
        }
    )

    // Dedicated Full-List Bank Picker Modal for Source Bank
    if (showFromBankPicker) {
        BankPickerModal(
            title = "Select Source Bank (From)",
            accounts = effectiveAccounts,
            currentSelected = fromAccount,
            onSelect = { selected ->
                fromAccount = selected
                if (selected.equals(toAccount, ignoreCase = true)) {
                    toAccount = effectiveAccounts.firstOrNull { !it.name.equals(selected, ignoreCase = true) }?.name ?: "Sadapay"
                }
                showFromBankPicker = false
            },
            onDismiss = { showFromBankPicker = false }
        )
    }

    // Dedicated Full-List Bank Picker Modal for Destination Bank
    if (showToBankPicker) {
        BankPickerModal(
            title = "Select Destination Bank (To)",
            accounts = effectiveAccounts,
            currentSelected = toAccount,
            onSelect = { selected ->
                toAccount = selected
                if (selected.equals(fromAccount, ignoreCase = true)) {
                    fromAccount = effectiveAccounts.firstOrNull { !it.name.equals(selected, ignoreCase = true) }?.name ?: "HBL"
                }
                showToBankPicker = false
            },
            onDismiss = { showToBankPicker = false }
        )
    }
}

/**
 * Clickable banner showing currently selected bank with real-time balance
 */
@Composable
private fun SelectedBankBanner(
    account: AccountEntity?,
    label: String,
    tintColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val name = account?.name ?: "Select Bank"
    val isWallet = isWalletAccount(name)
    val balance = account?.currentBalance ?: 0.0

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tintColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, tintColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = tintColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isWallet) Icons.Default.AccountBalanceWallet else Icons.Default.AccountBalance,
                            contentDescription = name,
                            tint = tintColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Balance: ${DateUtils.formatCurrency(balance)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = tintColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelSmall,
                    color = tintColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Clickable horizontal list of banks with large touch targets (minimum 48dp height)
 */
@Composable
private fun BankHorizontalList(
    accounts: List<AccountEntity>,
    selectedBankName: String,
    tagPrefix: String,
    onSelectBank: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        accounts.forEach { acc ->
            val isSelected = selectedBankName.equals(acc.name, ignoreCase = true)
            val isWallet = isWalletAccount(acc)
            val icon = if (isWallet) Icons.Default.AccountBalanceWallet else Icons.Default.AccountBalance

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isSelected)
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectBank(acc.name) }
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("${tagPrefix}_${acc.name}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = acc.name,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
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

/**
 * Dedicated modal dialog listing all banks vertically with large, easily clickable rows
 */
@Composable
private fun BankPickerModal(
    title: String,
    accounts: List<AccountEntity>,
    currentSelected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tap on any account below to select it:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accounts) { acc ->
                        val isSelected = currentSelected.equals(acc.name, ignoreCase = true)
                        val isWallet = isWalletAccount(acc)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected)
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else
                                null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(acc.name) }
                                .testTag("modal_bank_${acc.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isWallet) Icons.Default.AccountBalanceWallet else Icons.Default.AccountBalance,
                                                contentDescription = acc.name,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Balance: ${DateUtils.formatCurrency(acc.currentBalance)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.DateUtils

@Composable
fun AccountsScreen(
    viewModel: FinanceViewModel,
    onOpenTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    var selectedAccountName by remember { mutableStateOf<String?>("HBL") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Transfer Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "Bank & Wallet Accounts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "7 Managed Accounts • Real-time Balances",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Button(
                onClick = onOpenTransfer,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("transfer_button")
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Transfer", maxLines = 1, softWrap = false)
            }
        }

        // Account Cards Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.heightIn(max = 260.dp)
        ) {
            items(accounts) { acc ->
                val isSelected = selectedAccountName == acc.name
                AccountCardItem(
                    account = acc,
                    isSelected = isSelected,
                    onClick = { selectedAccountName = acc.name }
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // Account Details & History Section
        val activeAccount = accounts.find { it.name == selectedAccountName }
        val accountTxList = transactions.filter {
            it.fromAccount == selectedAccountName || it.toAccount == selectedAccountName
        }

        activeAccount?.let { acc ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${acc.name} Ledger Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Opening Balance: ${DateUtils.formatCurrency(acc.openingBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Current Balance",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = DateUtils.formatCurrency(acc.currentBalance),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Text(
            text = "Transaction History for ${selectedAccountName ?: "All"} (${accountTxList.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (accountTxList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions recorded for this account yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(accountTxList, key = { it.id }) { tx ->
                    AccountTxRow(tx = tx, currentAccount = selectedAccountName ?: "")
                }
            }
        }
    }
}

@Composable
private fun AccountCardItem(
    account: AccountEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("account_card_${account.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val isWallet = isWalletAccount(account)
                Icon(
                    imageVector = if (isWallet) Icons.Default.AccountBalanceWallet else Icons.Default.AccountBalance,
                    contentDescription = if (isWallet) "Wallet" else "Bank Account",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = DateUtils.formatCurrency(account.currentBalance),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "In: +${DateUtils.formatCurrency(account.totalReceived)}",
                style = MaterialTheme.typography.labelSmall,
                color = FinanceIncomeGreen
            )
            if (account.totalTransferredOut > 0) {
                Text(
                    text = "Out: -${DateUtils.formatCurrency(account.totalTransferredOut)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = FinanceExpenseRed
                )
            }
        }
    }
}

@Composable
private fun AccountTxRow(tx: TransactionEntity, currentAccount: String) {
    val isIncoming = tx.toAccount == currentAccount
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when {
                    tx.type == "UDHAAR" -> Icons.Default.Handshake
                    tx.type == "TRANSFER" -> Icons.Default.SwapHoriz
                    isIncoming -> Icons.Default.ArrowDownward
                    else -> Icons.Default.ArrowUpward
                }
                val tint = when {
                    tx.type == "TRANSFER" -> FinanceTransferBlue
                    isIncoming -> FinanceIncomeGreen
                    else -> FinanceExpenseRed
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = tx.description.ifBlank { tx.category },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val dateFormatted = DateUtils.formatReadableDateWithDay(tx.date, tx.dayOfWeek)
                    val timeFormatted = DateUtils.format12HourTime(tx.time)
                    Text(
                        text = "$dateFormatted $timeFormatted • ${tx.fromAccount} -> ${tx.toAccount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val prefix = if (tx.type == "TRANSFER") "" else if (isIncoming) "+" else "-"
                val color = if (tx.type == "TRANSFER") FinanceTransferBlue else if (isIncoming) FinanceIncomeGreen else FinanceExpenseRed

                Text(
                    text = "$prefix${DateUtils.formatCurrency(tx.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = if (tx.type == "UDHAAR") "Udhaar" else tx.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (tx.type == "UDHAAR") Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (tx.type == "UDHAAR") FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.ui.components.DailyStatementTableView
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.DateUtils
import com.example.util.StatementExportUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyStatementScreen(
    viewModel: FinanceViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedAccount by viewModel.selectedAccountFilter.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()

    var showDeleteConfirm by remember { mutableStateOf<TransactionEntity?>(null) }
    var viewMode by remember { mutableStateOf("LIST") } // "LIST" or "TABLE"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Export Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search transactions, transfers...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("statement_search_input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )

            // PDF Export button
            FilledTonalIconButton(
                onClick = {
                    StatementExportUtil.exportAndShareDailyPdf(
                        context = context,
                        title = "Daily Statement (${selectedAccount ?: "All Accounts"})",
                        transactions = filteredTransactions
                    )
                },
                modifier = Modifier.testTag("export_pdf_button")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
            }

            // CSV Export button
            FilledTonalIconButton(
                onClick = {
                    val csv = StatementExportUtil.generateDailyStatementCsv(filteredTransactions)
                    StatementExportUtil.exportAndShareCsv(
                        context = context,
                        filename = "Daily_Statement_${System.currentTimeMillis()}.csv",
                        csvContent = csv
                    )
                },
                modifier = Modifier.testTag("export_csv_button")
            ) {
                Icon(Icons.Default.TableChart, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.secondary)
            }
        }

        // Account Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedAccount == null,
                    onClick = { viewModel.selectedAccountFilter.value = null },
                    label = { Text("All Accounts") }
                )
            }
            items(accounts) { acc ->
                FilterChip(
                    selected = selectedAccount.equals(acc.name, ignoreCase = true),
                    onClick = {
                        viewModel.selectedAccountFilter.value =
                            if (selectedAccount.equals(acc.name, ignoreCase = true)) null else acc.name
                    },
                    label = { Text(acc.name) }
                )
            }
        }

        // Transaction Type Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val typeList = listOf(
                null to "All Types",
                "INCOME" to "Income",
                "EXPENSE" to "Expenses",
                "TRANSFER" to "Transfers",
                "PETROL" to "Petrol",
                "EXTRA_EXPENSE" to "Extra Expenses",
                "UDHAAR" to "Udhaar"
            )
            items(typeList) { (typeKey, typeLabel) ->
                FilterChip(
                    selected = selectedType == typeKey,
                    onClick = { viewModel.selectedTypeFilter.value = typeKey },
                    label = { Text(typeLabel) }
                )
            }
        }

        // Summary Bar with Income, Expense & Transfers
        val totalIn = filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalOut = filteredTransactions.filter { it.type in listOf("EXPENSE", "PETROL", "EXTRA_EXPENSE") }.sumOf { it.amount }
        val totalTransfers = filteredTransactions.filter { it.type == "TRANSFER" }.sumOf { it.amount }
        val totalUdhaar = filteredTransactions.filter { it.type == "UDHAAR" }.sumOf { it.amount }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredTransactions.size} Records Found",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // View Mode Switcher
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = viewMode == "LIST",
                            onClick = { viewMode = "LIST" },
                            label = { Text("List View", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.ViewList, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = viewMode == "TABLE",
                            onClick = { viewMode = "TABLE" },
                            label = { Text("Excel Sheet", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "In: +${DateUtils.formatCurrency(totalIn)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = FinanceIncomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Out: -${DateUtils.formatCurrency(totalOut)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = FinanceExpenseRed,
                        fontWeight = FontWeight.Bold
                    )
                    if (totalTransfers > 0) {
                        Text(
                            text = "Transfer: ${DateUtils.formatCurrency(totalTransfers)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = FinanceTransferBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (totalUdhaar > 0) {
                        Text(
                            text = "Udhaar: ${DateUtils.formatCurrency(totalUdhaar)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7C3AED),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Content Area: List View or Excel Table View
        if (viewMode == "TABLE") {
            DailyStatementTableView(
                transactions = filteredTransactions,
                onEdit = onEditTransaction,
                onDelete = { showDeleteConfirm = it },
                modifier = Modifier.weight(1f)
            )
        } else {
            // "Records Found" Card List View
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No records match your criteria",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        StatementRecordCard(
                            tx = tx,
                            onEdit = { onEditTransaction(tx) },
                            onDelete = { showDeleteConfirm = tx }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteConfirm?.let { tx ->
        val recordTitle = if (tx.type == "TRANSFER") "Transfer: ${tx.fromAccount} -> ${tx.toAccount}" else tx.description.ifBlank { tx.category }
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Statement Record?") },
            text = {
                Text("Are you sure you want to delete Sr# ${tx.serialNumber} ($recordTitle - ${DateUtils.formatCurrency(tx.amount)})? Account balances will automatically recalculate.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatementRecordCard(
    tx: TransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isTransfer = tx.type == "TRANSFER"
    val isUdhaar = tx.type == "UDHAAR"
    val isUdhaarIncoming = isUdhaar && (tx.description.contains("Taken", ignoreCase = true) || tx.description.contains("Repayment Received", ignoreCase = true))
    val isIncome = tx.type == "INCOME" || isUdhaarIncoming

    val icon = when (tx.type) {
        "TRANSFER" -> Icons.Default.SwapHoriz
        "INCOME" -> Icons.Default.ArrowDownward
        "PETROL" -> Icons.Default.LocalGasStation
        "EXTRA_EXPENSE" -> Icons.Default.ReceiptLong
        "UDHAAR" -> Icons.Default.Handshake
        else -> Icons.Default.ArrowUpward
    }

    val iconTint = when (tx.type) {
        "TRANSFER" -> FinanceTransferBlue
        "INCOME" -> FinanceIncomeGreen
        "PETROL" -> FinancePetrolOrange
        "EXTRA_EXPENSE" -> FinanceExtraPurple
        "UDHAAR" -> Color(0xFF7C3AED)
        else -> FinanceExpenseRed
    }

    val iconBg = iconTint.copy(alpha = 0.15f)

    val cardBg = when (tx.type) {
        "TRANSFER" -> FinanceTransferBg.copy(alpha = 0.35f)
        "INCOME" -> FinanceIncomeBg.copy(alpha = 0.25f)
        "PETROL" -> FinancePetrolBg.copy(alpha = 0.25f)
        "EXTRA_EXPENSE" -> FinanceExtraBg.copy(alpha = 0.25f)
        "UDHAAR" -> Color(0xFF7C3AED).copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tx.type,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                val title = if (isTransfer) {
                    "Transfer: ${tx.fromAccount} -> ${tx.toAccount}"
                } else {
                    tx.description.ifBlank { tx.category }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                val dayText = tx.dayOfWeek.ifBlank { DateUtils.getDayOfWeek(tx.date) }
                val dateWithDay = DateUtils.formatReadableDateWithDay(tx.date, dayText)
                val time12 = DateUtils.format12HourTime(tx.time)
                Text(
                    text = "$dateWithDay • $time12",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (tx.comments.isNotBlank() && tx.comments != title) {
                    Text(
                        text = tx.comments,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Amount, Balance and Action Buttons
            Column(horizontalAlignment = Alignment.End) {
                val amtColor = when {
                    isTransfer -> FinanceTransferBlue
                    isIncome -> FinanceIncomeGreen
                    else -> FinanceExpenseRed
                }
                val prefix = if (isTransfer) "" else if (isIncome) "+" else "-"

                Text(
                    text = "$prefix${DateUtils.formatCurrency(tx.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = amtColor
                )

                Text(
                    text = "Bal: ${DateUtils.formatCurrency(tx.totalAccountBalance)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("edit_tx_${tx.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit record",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_tx_${tx.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete record",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.theme.*
import com.example.util.DateUtils

@Composable
fun DailyStatementTableView(
    transactions: List<TransactionEntity>,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val hScrollState = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(hScrollState)
        ) {
            // Header Row (17 Excel Columns + Actions)
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeaderCell("Sr#", 50.dp)
                TableHeaderCell("Day", 80.dp)
                TableHeaderCell("Date & Time (PKT)", 150.dp)
                TableHeaderCell("Expense", 95.dp)
                TableHeaderCell("Received", 95.dp)
                TableHeaderCell("Balance", 100.dp)
                TableHeaderCell("From", 110.dp)
                TableHeaderCell("To", 110.dp)
                TableHeaderCell("Comments", 150.dp)
                TableHeaderCell("HBL", 90.dp)
                TableHeaderCell("UBL", 90.dp)
                TableHeaderCell("Sadapay", 90.dp)
                TableHeaderCell("Easypaisa", 90.dp)
                TableHeaderCell("JazzCash", 90.dp)
                TableHeaderCell("Mashreq", 90.dp)
                TableHeaderCell("Wallet", 90.dp)
                TableHeaderCell("Total Balance", 110.dp)
                TableHeaderCell("Actions", 90.dp)
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No statement records match your criteria",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 600.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        val isUdhaarIncoming = tx.type == "UDHAAR" && (tx.description.contains("Taken", ignoreCase = true) || tx.description.contains("Repayment Received", ignoreCase = true))
                        val expense = if (tx.type in listOf("EXPENSE", "PETROL", "EXTRA_EXPENSE") || (tx.type == "UDHAAR" && !isUdhaarIncoming)) tx.amount else 0.0
                        val received = if (tx.type == "INCOME" || (tx.type == "UDHAAR" && isUdhaarIncoming)) tx.amount else 0.0

                        val rowBg = when (tx.type) {
                            "INCOME" -> FinanceIncomeBg.copy(alpha = 0.5f)
                            "PETROL" -> FinancePetrolBg.copy(alpha = 0.5f)
                            "EXTRA_EXPENSE" -> FinanceExtraBg.copy(alpha = 0.5f)
                            "TRANSFER" -> FinanceTransferBg.copy(alpha = 0.4f)
                            "UDHAAR" -> Color(0xFF7C3AED).copy(alpha = 0.08f)
                            else -> Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .background(rowBg)
                                .clickable { onEdit(tx) }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayDay = tx.dayOfWeek.ifBlank { DateUtils.getDayOfWeek(tx.date) }
                            val displayTime = DateUtils.format12HourTime(tx.time)
                            TableCell("${tx.serialNumber}", 50.dp, isBold = true)
                            TableCell(displayDay, 80.dp)
                            TableCell("${DateUtils.formatReadableDateWithDay(tx.date, displayDay)}\n$displayTime", 150.dp)

                            // Expense / Transfer Out
                            TableCell(
                                text = when {
                                    tx.type == "TRANSFER" -> DateUtils.formatCurrency(tx.amount)
                                    expense > 0 -> DateUtils.formatCurrency(expense)
                                    else -> "-"
                                },
                                width = 95.dp,
                                color = if (tx.type == "TRANSFER") FinanceTransferBlue else if (expense > 0) FinanceExpenseRed else Color.Unspecified,
                                isBold = tx.type == "TRANSFER" || expense > 0
                            )

                            // Received
                            TableCell(
                                text = if (received > 0) DateUtils.formatCurrency(received) else "-",
                                width = 95.dp,
                                color = if (received > 0) FinanceIncomeGreen else Color.Unspecified,
                                isBold = received > 0
                            )

                            // Running Balance
                            TableCell(
                                text = DateUtils.formatCurrency(tx.totalAccountBalance),
                                width = 100.dp,
                                isBold = true
                            )

                            TableCell(tx.fromAccount, 110.dp)
                            TableCell(tx.toAccount, 110.dp)
                            val displayDesc = if (tx.type == "TRANSFER") {
                                if (tx.comments.isNotBlank()) "Transfer: ${tx.comments}" else "Transfer: ${tx.fromAccount} -> ${tx.toAccount}"
                            } else {
                                tx.comments.ifBlank { tx.description }
                            }
                            TableCell(displayDesc, 150.dp)

                            // 7 Account Columns from Excel
                            TableCell(DateUtils.formatCurrency(tx.hblBalance), 90.dp)
                            TableCell(DateUtils.formatCurrency(tx.ublBalance), 90.dp)
                            TableCell(DateUtils.formatCurrency(tx.sadapayBalance), 90.dp)
                            TableCell(DateUtils.formatCurrency(tx.easypaisaBalance), 90.dp)
                            TableCell(DateUtils.formatCurrency(tx.jazzCashBalance), 90.dp)
                            TableCell(DateUtils.formatCurrency(tx.mashreqBalance), 90.dp)
                            TableCell(DateUtils.formatCurrency(tx.walletBalance), 90.dp)

                            // Total Account Balance Column
                            TableCell(DateUtils.formatCurrency(tx.totalAccountBalance), 110.dp, isBold = true)

                            // Action buttons: Edit and Delete
                            Row(
                                modifier = Modifier.width(90.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onEdit(tx) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("edit_tx_${tx.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit record",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(tx) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("delete_tx_${tx.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete record",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color = Color.Unspecified,
    isBold: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = color,
        fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 2
    )
}

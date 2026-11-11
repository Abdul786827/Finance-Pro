package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.DateUtils
import com.example.util.StatementExportUtil

@Composable
fun ReportsExportScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val monthlyStatements by viewModel.monthlyStatements.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalAvailableBalance.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Statement & Audit Reports",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Generate printable PDF statements & Excel spreadsheets matching the original statement specifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Current Ledger Balance: ${DateUtils.formatCurrency(totalBalance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Text(
                text = "PDF Statements (Formatted & Print-Ready)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ExportActionCard(
                title = "Daily Statement PDF",
                description = "Generates a full PDF statement report with headers, date/time, 17-column account breakdown, and net totals.",
                icon = Icons.Default.PictureAsPdf,
                buttonLabel = "Export PDF",
                testTag = "export_daily_pdf_action",
                onClick = {
                    StatementExportUtil.exportAndShareDailyPdf(
                        context = context,
                        title = "Complete Daily Statement (${DateUtils.getCurrentDate()})",
                        transactions = transactions
                    )
                }
            )
        }

        item {
            val udhaarList by viewModel.udhaarRecords.collectAsStateWithLifecycle()
            ExportActionCard(
                title = "Udhaar (Lend & Borrow) PDF Statement",
                description = "Export a dedicated PDF statement for all Given & Taken loans and lent items, including repayments, dues, and net balance.",
                icon = Icons.Default.Handshake,
                buttonLabel = "Export Udhaar PDF",
                testTag = "export_udhaar_pdf_action",
                onClick = {
                    StatementExportUtil.exportAndShareUdhaarPdf(
                        context = context,
                        title = "Udhaar & Loans Report (${DateUtils.getCurrentDate()})",
                        records = udhaarList
                    )
                }
            )
        }

        item {
            Text(
                text = "Excel & CSV Exports (Excel Structure)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ExportActionCard(
                title = "Export Daily Transactions (CSV)",
                description = "Exports all transactions matching the exact 17 columns from Statement for app.xlsx: Sr#, Day, Date, Time, Expense, Received, Balance, From, To, Comments, HBL, UBL, Sadapay, Easypaisa, JazzCash, Mashreq, Wallet, Total.",
                icon = Icons.Default.TableChart,
                buttonLabel = "Export CSV",
                testTag = "export_daily_csv_action",
                onClick = {
                    val csv = StatementExportUtil.generateDailyStatementCsv(transactions)
                    StatementExportUtil.exportAndShareCsv(
                        context = context,
                        filename = "Daily_Statement_All_${System.currentTimeMillis()}.csv",
                        csvContent = csv
                    )
                }
            )
        }

        item {
            ExportActionCard(
                title = "Export Annual Summary",
                description = "Exports the complete 12-month Annual Summary table with Received, Expenses, Petrol, Extra, Salary, Pre-existing, Actual, Gross, Loan, and Closing Balances.",
                icon = Icons.Default.Analytics,
                buttonLabel = "Export Annual Summary",
                testTag = "export_annual_summary_action",
                onClick = {
                    val csv = StatementExportUtil.generateAnnualSummaryCsv(2026, monthlyStatements)
                    StatementExportUtil.exportAndShareCsv(
                        context = context,
                        filename = "Annual_Summary_2026.csv",
                        csvContent = csv
                    )
                }
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Timezone Asia/Karachi (PKT) is stamped on all generated exports.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    buttonLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.testTag(testTag)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(buttonLabel)
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MonthlyStatementEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.DateUtils
import com.example.util.StatementExportUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyAnnualScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val monthlyStatements by viewModel.monthlyStatements.collectAsStateWithLifecycle()
    var selectedYear by remember { mutableIntStateOf(2026) }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Complete 12 months for selected year (fill in missing months with zero records)
    val completeYearStatements = remember(monthlyStatements, selectedYear) {
        (1..12).map { monthNum ->
            val monthId = String.format("%04d-%02d", selectedYear, monthNum)
            monthlyStatements.find { it.id == monthId } ?: MonthlyStatementEntity(
                id = monthId,
                year = selectedYear,
                month = monthNum,
                monthName = "${monthNames[monthNum - 1]} $selectedYear",
                openingBalance = 0.0,
                totalReceived = 0.0,
                totalExpenses = 0.0,
                totalPetrolExpenses = 0.0,
                totalExtraExpenses = 0.0,
                totalSalary = 0.0,
                preExistingAmount = 0.0,
                actualSalary = 0.0,
                grossSalary = 0.0,
                totalLoan = 0.0,
                monthlyClosingBalance = 0.0
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tab Row
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Monthly Statements", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Annual Summary", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.TableChart, contentDescription = null) }
            )
        }

        // Year Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedYear-- }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Year")
                }
                Text(
                    text = "Fiscal Year $selectedYear",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { selectedYear++ }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
                }
            }

            // Annual CSV Export
            OutlinedButton(
                onClick = {
                    val csv = StatementExportUtil.generateAnnualSummaryCsv(selectedYear, completeYearStatements)
                    StatementExportUtil.exportAndShareCsv(
                        context = context,
                        filename = "Annual_Summary_$selectedYear.csv",
                        csvContent = csv
                    )
                },
                modifier = Modifier.testTag("export_annual_csv_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export Excel/CSV")
            }
        }

        if (selectedTab == 0) {
            // Monthly Statements Cards
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(completeYearStatements, key = { it.id }) { st ->
                    MonthlyStatementCard(
                        statement = st,
                        onToggleLock = { viewModel.toggleStatementLock(st.id) }
                    )
                }
            }
        } else {
            // Annual Summary Table matching the Excel file
            AnnualSummaryTable(statements = completeYearStatements)
        }
    }
}

@Composable
private fun MonthlyStatementCard(
    statement: MonthlyStatementEntity,
    onToggleLock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = statement.monthName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (statement.isLocked) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "LOCKED",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (statement.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock/Unlock",
                        tint = if (statement.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Financial Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Opening Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatCurrency(statement.openingBalance), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Received (Income)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+${DateUtils.formatCurrency(statement.totalReceived)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = FinanceIncomeGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("-${DateUtils.formatCurrency(statement.totalExpenses)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = FinanceExpenseRed)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Sub-categories: Petrol, Extra, Salary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Petrol Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatCurrency(statement.totalPetrolExpenses), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = FinancePetrolOrange)
                }
                Column {
                    Text("Extra Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatCurrency(statement.totalExtraExpenses), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = FinanceExtraPurple)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Monthly Closing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatCurrency(statement.monthlyClosingBalance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AnnualSummaryTable(statements: List<MonthlyStatementEntity>) {
    val hScrollState = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(hScrollState)
        ) {
            // Header Row matching Sheet2 Columns
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCell("Month", 110.dp)
                HeaderCell("Received", 95.dp)
                HeaderCell("Expenses", 95.dp)
                HeaderCell("Petrol", 85.dp)
                HeaderCell("Extra Exp", 85.dp)
                HeaderCell("Salary", 95.dp)
                HeaderCell("Pre-exist", 85.dp)
                HeaderCell("Actual Sal", 95.dp)
                HeaderCell("Gross Sal", 95.dp)
                HeaderCell("Loan", 85.dp)
                HeaderCell("Closing Bal", 105.dp)
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // 12 Monthly Rows
            LazyColumn(modifier = Modifier.heightIn(max = 450.dp)) {
                items(statements, key = { it.id }) { st ->
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DataCell(st.monthName.split(" ").first(), 110.dp, isBold = true)
                        DataCell(DateUtils.formatCurrency(st.totalReceived), 95.dp, color = if (st.totalReceived > 0) FinanceIncomeGreen else Color.Unspecified)
                        DataCell(DateUtils.formatCurrency(st.totalExpenses), 95.dp, color = if (st.totalExpenses > 0) FinanceExpenseRed else Color.Unspecified)
                        DataCell(DateUtils.formatCurrency(st.totalPetrolExpenses), 85.dp)
                        DataCell(DateUtils.formatCurrency(st.totalExtraExpenses), 85.dp)
                        DataCell(DateUtils.formatCurrency(st.totalSalary), 95.dp)
                        DataCell(DateUtils.formatCurrency(st.preExistingAmount), 85.dp)
                        DataCell(DateUtils.formatCurrency(st.actualSalary), 95.dp)
                        DataCell(DateUtils.formatCurrency(st.grossSalary), 95.dp)
                        DataCell(DateUtils.formatCurrency(st.totalLoan), 85.dp)
                        DataCell(DateUtils.formatCurrency(st.monthlyClosingBalance), 105.dp, isBold = true)
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            // Totals Row
            val totRecv = statements.sumOf { it.totalReceived }
            val totExp = statements.sumOf { it.totalExpenses }
            val totPetrol = statements.sumOf { it.totalPetrolExpenses }
            val totExtra = statements.sumOf { it.totalExtraExpenses }
            val totSal = statements.sumOf { it.totalSalary }
            val totPre = statements.sumOf { it.preExistingAmount }
            val totAct = statements.sumOf { it.actualSalary }
            val totGross = statements.sumOf { it.grossSalary }
            val totLoan = statements.sumOf { it.totalLoan }
            val finalClosing = statements.lastOrNull()?.monthlyClosingBalance ?: 0.0

            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DataCell("TOTAL (Year)", 110.dp, isBold = true)
                DataCell(DateUtils.formatCurrency(totRecv), 95.dp, isBold = true, color = FinanceIncomeGreen)
                DataCell(DateUtils.formatCurrency(totExp), 95.dp, isBold = true, color = FinanceExpenseRed)
                DataCell(DateUtils.formatCurrency(totPetrol), 85.dp, isBold = true)
                DataCell(DateUtils.formatCurrency(totExtra), 85.dp, isBold = true)
                DataCell(DateUtils.formatCurrency(totSal), 95.dp, isBold = true)
                DataCell(DateUtils.formatCurrency(totPre), 85.dp, isBold = true)
                DataCell(DateUtils.formatCurrency(totAct), 95.dp, isBold = true)
                DataCell(DateUtils.formatCurrency(totGross), 95.dp, isBold = true)
                DataCell(DateUtils.formatCurrency(totLoan), 85.dp, isBold = true)
                DataCell(DateUtils.formatCurrency(finalClosing), 105.dp, isBold = true)
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Composable
private fun DataCell(
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
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
    )
}

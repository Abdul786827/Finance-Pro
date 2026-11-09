package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LoanRecordEntity
import com.example.data.model.SalaryRecordEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryLoansScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val salaries by viewModel.salaryRecords.collectAsStateWithLifecycle()
    val loans by viewModel.loanRecords.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    var showAddSalaryDialog by remember { mutableStateOf(false) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var loanToRepay by remember { mutableStateOf<LoanRecordEntity?>(null) }

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
                text = { Text("Salary Management", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Payments, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Loan Records", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.CreditScore, contentDescription = null) }
            )
        }

        if (selectedTab == 0) {
            // Salary Formula Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SALARY CALCULATION FORMULA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Actual Salary = Gross Salary - Pre-existing Amount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { showAddSalaryDialog = true },
                            modifier = Modifier.testTag("add_salary_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Salary")
                        }
                    }
                }
            }

            Text(
                text = "Salary Logs (${salaries.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (salaries.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No salary records yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(salaries, key = { it.id }) { item ->
                        SalaryItemCard(item = item)
                    }
                }
            }
        } else {
            // Loans Top Banner
            val totalRemainingLoan = loans.filter { it.status != "Repaid" }.sumOf { it.remainingLoan }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL OUTSTANDING LOANS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = DateUtils.formatCurrency(totalRemainingLoan),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Button(
                        onClick = { showAddLoanDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("add_loan_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Loan")
                    }
                }
            }

            Text(
                text = "Loans & Borrowings (${loans.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (loans.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No loan records found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(loans, key = { it.id }) { loan ->
                        LoanItemCard(
                            loan = loan,
                            onRepay = { loanToRepay = loan }
                        )
                    }
                }
            }
        }
    }

    // Add Salary Dialog
    if (showAddSalaryDialog) {
        AddSalaryDialog(
            accounts = accounts.map { it.name },
            onDismiss = { showAddSalaryDialog = false },
            onSave = { month, gross, pre, received, acct, comments ->
                viewModel.addSalary(month, gross, pre, received, acct, comments) {
                    showAddSalaryDialog = false
                }
            }
        )
    }

    // Add Loan Dialog
    if (showAddLoanDialog) {
        AddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onSave = { date, person, amt, desc ->
                viewModel.addLoan(date, person, amt, desc) {
                    showAddLoanDialog = false
                }
            }
        )
    }

    // Repay Loan Dialog
    loanToRepay?.let { loan ->
        RepayLoanDialog(
            loan = loan,
            accounts = accounts.map { it.name },
            onDismiss = { loanToRepay = null },
            onPay = { amount, account ->
                viewModel.payLoan(loan, amount, account) {
                    loanToRepay = null
                }
            }
        )
    }
}

@Composable
private fun SalaryItemCard(item: SalaryRecordEntity) {
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
                Text(
                    text = "Salary Month: ${item.salaryDate}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Deposited to: ${item.depositedAccount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Gross Salary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatCurrency(item.grossSalary), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Pre-existing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("-${DateUtils.formatCurrency(item.preExistingAmount)}", style = MaterialTheme.typography.bodyMedium, color = FinanceExpenseRed, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Actual Salary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatCurrency(item.actualSalary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FinanceIncomeGreen)
                }
            }

            if (item.comments.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Notes: ${item.comments}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoanItemCard(
    loan: LoanRecordEntity,
    onRepay: () -> Unit
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
                Column {
                    Text(
                        text = loan.personOrSource,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Date: ${loan.date} • ${loan.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (loan.status) {
                        "Repaid" -> FinanceIncomeBg
                        "Partially Paid" -> FinancePetrolBg
                        else -> FinanceExpenseBg
                    }
                ) {
                    Text(
                        text = loan.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (loan.status) {
                            "Repaid" -> FinanceIncomeGreen
                            "Partially Paid" -> FinancePetrolOrange
                            else -> FinanceExpenseRed
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Loan: ${DateUtils.formatCurrency(loan.loanAmount)}", style = MaterialTheme.typography.bodySmall)
                    Text("Paid: ${DateUtils.formatCurrency(loan.paidAmount)}", style = MaterialTheme.typography.bodySmall, color = FinanceIncomeGreen)
                    Text("Remaining: ${DateUtils.formatCurrency(loan.remainingLoan)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }

                if (loan.remainingLoan > 0) {
                    OutlinedButton(onClick = onRepay) {
                        Text("Pay / Repay")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSalaryDialog(
    accounts: List<String>,
    onDismiss: () -> Unit,
    onSave: (month: String, gross: Double, pre: Double, received: Double, account: String, comments: String) -> Unit
) {
    var monthText by remember { mutableStateOf("2026-11") }
    var grossText by remember { mutableStateOf("200000") }
    var preText by remember { mutableStateOf("15000") }
    var receivedText by remember { mutableStateOf("185000") }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull() ?: "HBL") }
    var commentsText by remember { mutableStateOf("") }
    var accountExpanded by remember { mutableStateOf(false) }

    val gross = grossText.toDoubleOrNull() ?: 0.0
    val pre = preText.toDoubleOrNull() ?: 0.0
    val actual = gross - pre

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Salary") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = monthText,
                    onValueChange = { monthText = it },
                    label = { Text("Salary Month (YYYY-MM)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = grossText,
                    onValueChange = { grossText = it },
                    label = { Text("Gross Salary (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = preText,
                    onValueChange = { preText = it },
                    label = { Text("Pre-existing Amount Deducted (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Actual Salary: ${DateUtils.formatCurrency(actual)}",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = receivedText,
                    onValueChange = { receivedText = it },
                    label = { Text("Received Amount (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = !accountExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Deposited Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc) },
                                onClick = {
                                    selectedAccount = acc
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = commentsText,
                    onValueChange = { commentsText = it },
                    label = { Text("Comments / Deductions note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        monthText,
                        gross,
                        pre,
                        receivedText.toDoubleOrNull() ?: actual,
                        selectedAccount,
                        commentsText
                    )
                }
            ) {
                Text("Save Salary")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddLoanDialog(
    onDismiss: () -> Unit,
    onSave: (date: String, person: String, amount: Double, desc: String) -> Unit
) {
    var dateText by remember { mutableStateOf(DateUtils.getCurrentDate()) }
    var personText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Loan / Borrowing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = personText,
                    onValueChange = { personText = it },
                    label = { Text("Person / Lender / Source") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Loan Amount (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Purpose / Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && personText.isNotBlank()) {
                        onSave(dateText, personText, amt, descText)
                    }
                }
            ) {
                Text("Save Loan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepayLoanDialog(
    loan: LoanRecordEntity,
    accounts: List<String>,
    onDismiss: () -> Unit,
    onPay: (amount: Double, account: String) -> Unit
) {
    var payAmountText by remember { mutableStateOf(loan.remainingLoan.toString()) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull() ?: "HBL") }
    var acctExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Repay Loan: ${loan.personOrSource}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Remaining Outstanding: ${DateUtils.formatCurrency(loan.remainingLoan)}")

                OutlinedTextField(
                    value = payAmountText,
                    onValueChange = { payAmountText = it },
                    label = { Text("Repayment Amount (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = acctExpanded,
                    onExpandedChange = { acctExpanded = !acctExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = acctExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = acctExpanded,
                        onDismissRequest = { acctExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc) },
                                onClick = {
                                    selectedAccount = acc
                                    acctExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = payAmountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onPay(amt, selectedAccount)
                    }
                }
            ) {
                Text("Confirm Repayment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

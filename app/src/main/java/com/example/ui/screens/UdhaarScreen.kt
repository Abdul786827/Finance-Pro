package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UdhaarRecordEntity
import com.example.data.model.UdhaarRepaymentEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.DateUtils
import com.example.util.StatementExportUtil
import com.example.util.UdhaarNotificationHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdhaarScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val records by viewModel.udhaarRecords.collectAsStateWithLifecycle()
    val repayments by viewModel.udhaarRepayments.collectAsStateWithLifecycle()

    val totalToReceive by viewModel.udhaarTotalToReceive.collectAsStateWithLifecycle()
    val totalToPay by viewModel.udhaarTotalToPay.collectAsStateWithLifecycle()
    val netBalance by viewModel.udhaarNetBalance.collectAsStateWithLifecycle()

    // Local UI states
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Given, 2: Taken, 3: Settled
    val tabTitles = listOf("All", "Given", "Taken", "Settled")
    var searchQuery by remember { mutableStateOf("") }

    // Dialog states
    var showAddEditDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<UdhaarRecordEntity?>(null) }

    var recordForRepayment by remember { mutableStateOf<UdhaarRecordEntity?>(null) }
    var repaymentToEdit by remember { mutableStateOf<UdhaarRepaymentEntity?>(null) }

    var recordToDelete by remember { mutableStateOf<UdhaarRecordEntity?>(null) }
    var repaymentToDelete by remember { mutableStateOf<Pair<Long, Long>?>(null) } // (repaymentId, udhaarId)

    // Check notifications on launch
    LaunchedEffect(records) {
        UdhaarNotificationHelper.checkAndNotifyDueRecords(context, records)
    }

    // Filter records
    val filteredRecords = remember(records, selectedTab, searchQuery) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        records.filter { rec ->
            val matchesTab = when (selectedTab) {
                1 -> rec.type == "GIVEN" && rec.status != "SETTLED"
                2 -> rec.type == "TAKEN" && rec.status != "SETTLED"
                3 -> rec.status == "SETTLED"
                else -> true
            }

            val matchesSearch = searchQuery.isBlank() ||
                    rec.personName.contains(searchQuery, ignoreCase = true) ||
                    rec.phoneNumber.contains(searchQuery) ||
                    rec.itemDescription.contains(searchQuery, ignoreCase = true) ||
                    rec.note.contains(searchQuery, ignoreCase = true)

            matchesTab && matchesSearch
        }
    }

    // Group filtered records by person name
    val groupedByPerson = remember(filteredRecords) {
        filteredRecords.groupBy { it.personName }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    recordToEdit = null
                    showAddEditDialog = true
                },
                containerColor = EmeraldPrimaryLight,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_udhaar")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Udhaar Record")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Ledger Notice Banner (Clarifies zero account impact)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldPrimaryLight.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Separate Udhaar Ledger: Records and repayments never modify your bank/wallet balances or statement totals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Summary Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Given Pending (To Receive)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_udhaar_receive"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = FinanceIncomeBg)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "I Will Receive",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Given Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = FinanceIncomeGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = DateUtils.formatCurrency(totalToReceive),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FinanceIncomeGreen
                            )
                        }
                    }

                    // Taken Pending (To Pay)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_udhaar_pay"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = FinanceExpenseBg)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "I Have to Pay",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Taken Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = FinanceExpenseRed,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = DateUtils.formatCurrency(totalToPay),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FinanceExpenseRed
                            )
                        }
                    }

                    // Net Balance
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_udhaar_net"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Net Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (netBalance >= 0) "Positive (+)" else "Negative (-)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (netBalance >= 0) EmeraldPrimaryLight else FinanceExpenseRed,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = DateUtils.formatCurrency(netBalance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (netBalance >= 0) EmeraldPrimaryLight else FinanceExpenseRed
                            )
                        }
                    }
                }
            }

            // Search Bar & PDF Export Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("udhaar_search_input"),
                        placeholder = { Text("Search by person, item, or note...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    IconButton(
                        onClick = {
                            StatementExportUtil.exportAndShareUdhaarPdf(
                                context = context,
                                title = "Udhaar & Loans Report (${DateUtils.getCurrentDate()})",
                                records = records
                            )
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .background(EmeraldPrimaryLight.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .testTag("udhaar_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export Udhaar PDF",
                            tint = EmeraldPrimaryLight
                        )
                    }
                }
            }

            // Filter Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = EmeraldPrimaryLight,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                val count = when (index) {
                                    1 -> records.count { it.type == "GIVEN" && it.status != "SETTLED" }
                                    2 -> records.count { it.type == "TAKEN" && it.status != "SETTLED" }
                                    3 -> records.count { it.status == "SETTLED" }
                                    else -> records.size
                                }
                                Text("$title ($count)", fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                            },
                            modifier = Modifier.testTag("udhaar_tab_$index")
                        )
                    }
                }
            }

            // Records List (Grouped by Person)
            if (filteredRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No udhaar records match '$searchQuery'" else "No udhaar records in this category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap the '+' button below to record a loan (Money or Item).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                groupedByPerson.forEach { (person, personRecords) ->
                    item(key = "person_header_$person") {
                        val personGiven = personRecords.filter { it.type == "GIVEN" && it.category == "MONEY" && it.status != "SETTLED" }.sumOf { it.remainingAmount }
                        val personTaken = personRecords.filter { it.type == "TAKEN" && it.category == "MONEY" && it.status != "SETTLED" }.sumOf { it.remainingAmount }
                        val personNet = personGiven - personTaken

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = EmeraldPrimaryLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = person,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = " (${personRecords.size})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (personGiven > 0 || personTaken > 0) {
                                    Text(
                                        text = "Net: ${DateUtils.formatCurrency(personNet)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (personNet >= 0) FinanceIncomeGreen else FinanceExpenseRed
                                    )
                                }
                            }
                        }
                    }

                    items(personRecords, key = { it.id }) { record ->
                        val recordRepayments = remember(repayments, record.id) {
                            repayments.filter { it.udhaarId == record.id }
                        }

                        UdhaarRecordCard(
                            record = record,
                            repayments = recordRepayments,
                            onEdit = {
                                recordToEdit = record
                                showAddEditDialog = true
                            },
                            onDelete = { recordToDelete = record },
                            onAddRepayment = { recordForRepayment = record },
                            onEditRepayment = { rep ->
                                recordForRepayment = record
                                repaymentToEdit = rep
                            },
                            onDeleteRepayment = { repId ->
                                repaymentToDelete = Pair(repId, record.id)
                            },
                            onToggleItemReturned = { isReturned ->
                                viewModel.markUdhaarItemReturned(record.id, isReturned)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Add / Edit Udhaar Record Dialog
    if (showAddEditDialog) {
        UdhaarAddEditDialog(
            recordToEdit = recordToEdit,
            onDismiss = {
                showAddEditDialog = false
                recordToEdit = null
            },
            onSave = { type, name, phone, category, amount, itemDesc, date, time, dueDate, note ->
                if (recordToEdit != null) {
                    viewModel.updateUdhaar(
                        id = recordToEdit!!.id,
                        type = type,
                        personName = name,
                        phoneNumber = phone,
                        category = category,
                        amount = amount,
                        itemDescription = itemDesc,
                        date = date,
                        time = time,
                        dueDate = dueDate,
                        note = note
                    )
                } else {
                    viewModel.addUdhaar(
                        type = type,
                        personName = name,
                        phoneNumber = phone,
                        category = category,
                        amount = amount,
                        itemDescription = itemDesc,
                        date = date,
                        time = time,
                        dueDate = dueDate,
                        note = note
                    )
                }
                showAddEditDialog = false
                recordToEdit = null
            }
        )
    }

    // Add / Edit Repayment Dialog
    if (recordForRepayment != null) {
        UdhaarRepaymentDialog(
            record = recordForRepayment!!,
            repaymentToEdit = repaymentToEdit,
            onDismiss = {
                recordForRepayment = null
                repaymentToEdit = null
            },
            onSave = { amt, date, time, note ->
                val rep = repaymentToEdit
                if (rep != null) {
                    viewModel.updateUdhaarRepayment(
                        repaymentId = rep.id,
                        udhaarId = recordForRepayment!!.id,
                        amount = amt,
                        date = date,
                        time = time,
                        note = note
                    )
                } else {
                    viewModel.addUdhaarRepayment(
                        udhaarId = recordForRepayment!!.id,
                        amount = amt,
                        date = date,
                        time = time,
                        note = note
                    )
                }
                recordForRepayment = null
                repaymentToEdit = null
            }
        )
    }

    // Confirm Delete Record Dialog
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = FinanceExpenseRed) },
            title = { Text("Delete Udhaar Record?") },
            text = {
                Text(
                    text = "Are you sure you want to delete this record for ${recordToDelete!!.personName}? All recorded repayments for this entry will also be permanently deleted. (No bank accounts will be affected)."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUdhaar(recordToDelete!!.id)
                        recordToDelete = null
                        Toast.makeText(context, "Udhaar record deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Delete Repayment Dialog
    if (repaymentToDelete != null) {
        AlertDialog(
            onDismissRequest = { repaymentToDelete = null },
            title = { Text("Delete Repayment?") },
            text = { Text("Are you sure you want to delete this repayment? The remaining Udhaar amount will be automatically restored.") },
            confirmButton = {
                Button(
                    onClick = {
                        val pair = repaymentToDelete!!
                        viewModel.deleteUdhaarRepayment(pair.first, pair.second)
                        repaymentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { repaymentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UdhaarRecordCard(
    record: UdhaarRecordEntity,
    repayments: List<UdhaarRepaymentEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddRepayment: () -> Unit,
    onEditRepayment: (UdhaarRepaymentEntity) -> Unit,
    onDeleteRepayment: (Long) -> Unit,
    onToggleItemReturned: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var expandedRepayments by remember { mutableStateOf(false) }

    val isGiven = record.type == "GIVEN"
    val isMoney = record.category == "MONEY"
    val isSettled = record.status == "SETTLED" || (record.category == "ITEM" && record.isItemReturned)

    // Overdue Check
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val isOverdue = remember(record.dueDate, isSettled) {
        !isSettled && record.dueDate.isNotBlank() && record.dueDate < todayStr
    }

    val typeColor = if (isGiven) FinanceIncomeGreen else FinanceExpenseRed
    val cardBg = if (isOverdue) {
        Color(0xFFFFF1F1) // Red alert background
    } else if (isSettled) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("udhaar_record_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOverdue) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Type Badge + Due Status + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isGiven) "GIVEN (Maine Diya)" else "TAKEN (Maine Liya)",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (record.status) {
                            "SETTLED" -> FinanceIncomeGreen.copy(alpha = 0.15f)
                            "PARTIAL" -> FinancePetrolOrange.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        }
                    ) {
                        Text(
                            text = if (record.category == "ITEM" && record.isItemReturned) "RETURNED" else record.status,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (record.status) {
                                "SETTLED" -> FinanceIncomeGreen
                                "PARTIAL" -> FinancePetrolOrange
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (isOverdue) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FinanceExpenseRed
                        ) {
                            Text(
                                text = "OVERDUE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Edit & Delete Icons
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Record",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Record",
                            tint = FinanceExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Info: Person Name & Amounts / Item Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (record.phoneNumber.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${record.phoneNumber}"))
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = record.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (record.category == "ITEM") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = EmeraldPrimaryLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Item: ${record.itemDescription}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Amount or Return Button
                Column(horizontalAlignment = Alignment.End) {
                    if (isMoney) {
                        Text(
                            text = DateUtils.formatCurrency(record.remainingAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSettled) MaterialTheme.colorScheme.onSurfaceVariant else typeColor
                        )
                        if (record.originalAmount != record.remainingAmount) {
                            Text(
                                text = "Orig: ${DateUtils.formatCurrency(record.originalAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Item status button
                        Button(
                            onClick = { onToggleItemReturned(!record.isItemReturned) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (record.isItemReturned) FinanceIncomeGreen else EmeraldPrimaryLight
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = if (record.isItemReturned) Icons.Default.CheckCircle else Icons.Default.AssignmentReturn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (record.isItemReturned) "Returned" else "Mark Returned", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date, Day, Time & Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${DateUtils.formatReadableDateWithDay(record.date, record.dayOfWeek)} • ${DateUtils.format12HourTime(record.time)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (record.dueDate.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (isOverdue) FinanceExpenseRed else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Due: ${record.dueDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) FinanceExpenseRed else MaterialTheme.colorScheme.primary,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            if (record.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${record.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Repayment Section for Money records
            if (isMoney) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { expandedRepayments = !expandedRepayments },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (expandedRepayments) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Repayments (${repayments.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isSettled) {
                        OutlinedButton(
                            onClick = onAddRepayment,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_repay_${record.id}")
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Payment", fontSize = 12.sp)
                        }
                    }
                }

                // Repayment List
                AnimatedVisibility(visible = expandedRepayments) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (repayments.isEmpty()) {
                            Text(
                                text = "No repayments recorded yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            repayments.forEach { rep ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "+${DateUtils.formatCurrency(rep.amount)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = FinanceIncomeGreen
                                            )
                                            Text(
                                                text = "${rep.date} • ${DateUtils.format12HourTime(rep.time)}${if (rep.note.isNotBlank()) " • ${rep.note}" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row {
                                            IconButton(
                                                onClick = { onEditRepayment(rep) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Repayment",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteRepayment(rep.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Repayment",
                                                    tint = FinanceExpenseRed,
                                                    modifier = Modifier.size(14.dp)
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdhaarAddEditDialog(
    recordToEdit: UdhaarRecordEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        personName: String,
        phoneNumber: String,
        category: String,
        amount: Double,
        itemDescription: String,
        date: String,
        time: String,
        dueDate: String,
        note: String
    ) -> Unit
) {
    var type by remember { mutableStateOf(recordToEdit?.type ?: "GIVEN") }
    var personName by remember { mutableStateOf(recordToEdit?.personName ?: "") }
    var phoneNumber by remember { mutableStateOf(recordToEdit?.phoneNumber ?: "") }
    var category by remember { mutableStateOf(recordToEdit?.category ?: "MONEY") }
    var amountText by remember { mutableStateOf(if (recordToEdit != null && recordToEdit.category == "MONEY") recordToEdit.originalAmount.toString() else "") }
    var itemDescription by remember { mutableStateOf(recordToEdit?.itemDescription ?: "") }

    var dateText by remember { mutableStateOf(recordToEdit?.date ?: DateUtils.getCurrentDate()) }
    val dayText by remember(dateText) { derivedStateOf { DateUtils.getDayOfWeek(dateText) } }
    var timeText by remember { mutableStateOf(recordToEdit?.time ?: DateUtils.getCurrentTime()) }

    var dueDateText by remember { mutableStateOf(recordToEdit?.dueDate ?: "") }
    var noteText by remember { mutableStateOf(recordToEdit?.note ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (recordToEdit != null) "Edit Udhaar Record" else "New Udhaar Record",
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
                // Type Switcher: Given vs Taken
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "GIVEN",
                        onClick = { type = "GIVEN" },
                        label = { Text("Given (Maine Diya)") },
                        leadingIcon = {
                            if (type == "GIVEN") Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = FinanceIncomeGreen)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "TAKEN",
                        onClick = { type = "TAKEN" },
                        label = { Text("Taken (Maine Liya)") },
                        leadingIcon = {
                            if (type == "TAKEN") Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = FinanceExpenseRed)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Category Switcher: Money vs Item
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = category == "MONEY",
                        onClick = { category = "MONEY" },
                        label = { Text("Money (Paisa)") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = category == "ITEM",
                        onClick = { category = "ITEM" },
                        label = { Text("Item (Cheez)") },
                        leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Person Name (Required)
                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personName = it
                        validationError = null
                    },
                    label = { Text("Person Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("udhaar_person_input"),
                    singleLine = true
                )

                // Phone Number (Optional)
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("udhaar_phone_input"),
                    singleLine = true
                )

                // Category-specific fields
                if (category == "MONEY") {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            validationError = null
                        },
                        label = { Text("Amount (Rs.) *") },
                        leadingIcon = { Text("Rs. ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("udhaar_amount_input"),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = itemDescription,
                        onValueChange = {
                            itemDescription = it
                            validationError = null
                        },
                        label = { Text("Item Description * (e.g. Laptop, Book)") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("udhaar_item_desc_input"),
                        singleLine = true
                    )
                }

                // Date & Day Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1.3f)) {
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }

                    OutlinedTextField(
                        value = dayText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Time (12-hour format)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = DateUtils.format12HourTime(timeText),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time (12-Hour AM/PM)") },
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.Schedule, contentDescription = "Pick Time")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showTimePicker = true }
                    )
                }

                // Due Date (Optional)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (dueDateText.isNotBlank()) dueDateText else "No due date set",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Due / Return Date (Optional)") },
                        trailingIcon = {
                            Row {
                                if (dueDateText.isNotBlank()) {
                                    IconButton(onClick = { dueDateText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear Due Date")
                                    }
                                }
                                IconButton(onClick = { showDueDatePicker = true }) {
                                    Icon(Icons.Default.Event, contentDescription = "Pick Due Date")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDueDatePicker = true }
                    )
                }

                // Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Purpose (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                validationError?.let { err ->
                    Text(
                        text = err,
                        color = FinanceExpenseRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (personName.isBlank()) {
                        validationError = "Please enter person name."
                        return@Button
                    }
                    if (category == "MONEY" && (amountText.toDoubleOrNull() ?: 0.0) <= 0.0) {
                        validationError = "Please enter a valid amount greater than 0."
                        return@Button
                    }
                    if (category == "ITEM" && itemDescription.isBlank()) {
                        validationError = "Please describe the item."
                        return@Button
                    }

                    onSave(
                        type,
                        personName.trim(),
                        phoneNumber.trim(),
                        category,
                        amountText.toDoubleOrNull() ?: 0.0,
                        itemDescription.trim(),
                        dateText,
                        timeText,
                        dueDateText,
                        noteText.trim()
                    )
                },
                modifier = Modifier.testTag("udhaar_save_button")
            ) {
                Text(if (recordToEdit != null) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Calendar DatePicker for record date
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(dateText)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dateText = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Calendar DatePicker for due date
    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDateText = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }.format(Date(millis))
                        }
                        showDueDatePicker = false
                    }
                ) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // TimePicker Dialog with selectable digits
    if (showTimePicker) {
        val (initHour, initMinute) = remember(timeText) {
            try {
                val canonical24 = DateUtils.to24HourTime(timeText)
                val parts = canonical24.split(":")
                Pair(parts.getOrNull(0)?.toIntOrNull() ?: 12, parts.getOrNull(1)?.toIntOrNull() ?: 0)
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
                        timeText = String.format(Locale.US, "%02d:%02d:00", timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdhaarRepaymentDialog(
    record: UdhaarRecordEntity,
    repaymentToEdit: UdhaarRepaymentEntity? = null,
    onDismiss: () -> Unit,
    onSave: (amount: Double, date: String, time: String, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf(if (repaymentToEdit != null) repaymentToEdit.amount.toString() else record.remainingAmount.toString()) }
    var dateText by remember { mutableStateOf(repaymentToEdit?.date ?: DateUtils.getCurrentDate()) }
    var timeText by remember { mutableStateOf(repaymentToEdit?.time ?: DateUtils.getCurrentTime()) }
    var noteText by remember { mutableStateOf(repaymentToEdit?.note ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (repaymentToEdit != null) "Edit Repayment" else "Add Repayment",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Record for ${record.personName} • Remaining: ${DateUtils.formatCurrency(record.remainingAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMsg = null
                    },
                    label = { Text("Repayment Amount (Rs.) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Date
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                }

                // Time
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = DateUtils.format12HourTime(timeText),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time") },
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showTimePicker = true })
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (e.g. Cash, Direct)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                errorMsg?.let { err ->
                    Text(err, color = FinanceExpenseRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) {
                        errorMsg = "Please enter an amount greater than 0."
                        return@Button
                    }
                    onSave(amt, dateText, timeText, noteText.trim())
                }
            ) {
                Text("Save Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dateText = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        timeText = String.format(Locale.US, "%02d:%02d:00", timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

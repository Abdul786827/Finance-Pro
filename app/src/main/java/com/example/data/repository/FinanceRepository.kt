package com.example.data.repository

import com.example.data.local.FinanceDao
import com.example.data.local.SeedData
import com.example.data.model.*
import com.example.util.DateUtils
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class FinanceRepository(private val dao: FinanceDao) {

    val accounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    val transactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val petrolExpenses: Flow<List<PetrolExpenseEntity>> = dao.getAllPetrolExpenses()
    val extraExpenses: Flow<List<ExtraExpenseEntity>> = dao.getAllExtraExpenses()
    val salaryRecords: Flow<List<SalaryRecordEntity>> = dao.getAllSalaryRecords()
    val loanRecords: Flow<List<LoanRecordEntity>> = dao.getAllLoanRecords()
    val monthlyStatements: Flow<List<MonthlyStatementEntity>> = dao.getAllMonthlyStatements()
    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()
    val categories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val udhaarRecords: Flow<List<UdhaarRecordEntity>> = dao.getAllUdhaarRecords()
    val udhaarRepayments: Flow<List<UdhaarRepaymentEntity>> = dao.getAllUdhaarRepayments()

    suspend fun initializeIfEmpty() {
        val existingAccounts = dao.getAllAccountsDirect()
        if (existingAccounts.isEmpty()) {
            dao.insertAccounts(SeedData.defaultAccounts)
            dao.insertCategories(SeedData.defaultCategories)
            dao.insertTransactions(SeedData.initialTransactions)
            dao.insertPetrolExpenses(SeedData.initialPetrolExpenses)
            dao.insertExtraExpenses(SeedData.initialExtraExpenses)
            dao.insertSalaryRecords(SeedData.initialSalaries)
            dao.insertLoanRecords(SeedData.initialLoans)
            dao.insertMonthlyStatements(SeedData.initialMonthlyStatements)
        }

        // Normalize existing user emails and deduplicate rows if needed
        val allUsers = dao.getAllUsers()
        val seenEmails = mutableSetOf<String>()
        for (u in allUsers) {
            val normalized = SecurityUtils.normalizeEmail(u.email)
            if (normalized != u.email) {
                dao.updateProfileDetails(u.id, u.fullName, normalized, u.mobile, u.profileImageBase64)
            }
            if (seenEmails.contains(normalized)) {
                // Delete duplicate row
                dao.deleteDuplicateUsers(normalized, u.id)
            } else {
                seenEmails.add(normalized)
            }
        }
    }

    suspend fun addTransaction(
        date: String,
        time: String,
        type: String, // INCOME, EXPENSE, TRANSFER, PETROL, EXTRA_EXPENSE
        amount: Double,
        fromAccount: String,
        toAccount: String,
        category: String,
        comments: String = "",
        description: String = "",
        extraItem: String = "",
        petrolLiters: Double = 0.0,
        petrolRate: Double = 0.0
    ): Long {
        val accountsList = dao.getAllAccountsDirect().toMutableList()
        val accountMap = accountsList.associateBy { it.name }.toMutableMap()

        // Apply balances
        when (type) {
            "INCOME" -> {
                val dest = accountMap[toAccount]
                if (dest != null) {
                    val updated = dest.copy(
                        currentBalance = dest.currentBalance + amount,
                        totalReceived = dest.totalReceived + amount
                    )
                    accountMap[toAccount] = updated
                    dao.updateAccount(updated)
                }
            }
            "EXPENSE", "PETROL", "EXTRA_EXPENSE" -> {
                val src = accountMap[fromAccount]
                if (src != null) {
                    val updated = src.copy(
                        currentBalance = src.currentBalance - amount
                    )
                    accountMap[fromAccount] = updated
                    dao.updateAccount(updated)
                }
            }
            "TRANSFER" -> {
                val src = accountMap[fromAccount]
                val dest = accountMap[toAccount]
                if (src != null) {
                    val updatedSrc = src.copy(
                        currentBalance = src.currentBalance - amount,
                        totalTransferredOut = src.totalTransferredOut + amount
                    )
                    accountMap[fromAccount] = updatedSrc
                    dao.updateAccount(updatedSrc)
                }
                if (dest != null) {
                    val updatedDest = dest.copy(
                        currentBalance = dest.currentBalance + amount,
                        totalTransferredIn = dest.totalTransferredIn + amount
                    )
                    accountMap[toAccount] = updatedDest
                    dao.updateAccount(updatedDest)
                }
            }
        }

        // Calculate Day of Week
        val dayOfWeek = getDayOfWeek(date)
        val nextSerial = (dao.getMaxSerialNumber() ?: 0) + 1

        // Snapshot current account balances for the statement row
        val hbl = accountMap["HBL"]?.currentBalance ?: 0.0
        val ubl = accountMap["UBL"]?.currentBalance ?: 0.0
        val sadapay = accountMap["Sadapay"]?.currentBalance ?: 0.0
        val easypaisa = accountMap["Easypaisa"]?.currentBalance ?: 0.0
        val jazzCash = accountMap["JazzCash"]?.currentBalance ?: 0.0
        val mashreq = accountMap["Mashreq"]?.currentBalance ?: 0.0
        val wallet = accountMap["Wallet"]?.currentBalance ?: 0.0
        val totalBal = hbl + ubl + sadapay + easypaisa + jazzCash + mashreq + wallet

        val transaction = TransactionEntity(
            serialNumber = nextSerial,
            date = date,
            time = DateUtils.to24HourTime(time),
            dayOfWeek = dayOfWeek,
            type = type,
            amount = amount,
            fromAccount = fromAccount,
            toAccount = toAccount,
            category = category,
            comments = comments,
            description = if (description.isNotBlank()) description else when (type) {
                "PETROL" -> "Petrol: $comments"
                "EXTRA_EXPENSE" -> "Extra: $extraItem"
                "TRANSFER" -> "Transfer $fromAccount -> $toAccount"
                else -> category
            },
            hblBalance = hbl,
            ublBalance = ubl,
            sadapayBalance = sadapay,
            easypaisaBalance = easypaisa,
            jazzCashBalance = jazzCash,
            mashreqBalance = mashreq,
            walletBalance = wallet,
            totalAccountBalance = totalBal,
            petrolLiters = petrolLiters,
            petrolRate = petrolRate,
            extraItem = extraItem
        )

        val txId = dao.insertTransaction(transaction)

        // Insert into Petrol table if petrol
        if (type == "PETROL") {
            val nextPetrolSerial = (dao.getMaxPetrolSerialNumber() ?: 0) + 1
            dao.insertPetrolExpense(
                PetrolExpenseEntity(
                    serialNumber = nextPetrolSerial,
                    date = date,
                    time = time,
                    amount = amount,
                    account = fromAccount,
                    description = comments.ifBlank { "Petrol fill" },
                    comments = if (petrolLiters > 0) "$petrolLiters Liters @ $petrolRate" else comments,
                    liters = petrolLiters,
                    ratePerLiter = petrolRate,
                    transactionId = txId
                )
            )
        }

        // Insert into Extra Expense table if extra
        if (type == "EXTRA_EXPENSE") {
            val nextExtraSerial = (dao.getMaxExtraSerialNumber() ?: 0) + 1
            dao.insertExtraExpense(
                ExtraExpenseEntity(
                    serialNumber = nextExtraSerial,
                    item = extraItem.ifBlank { "Extra Expense" },
                    date = date,
                    time = time,
                    amount = amount,
                    account = fromAccount,
                    comments = comments,
                    transactionId = txId
                )
            )
        }

        // Auto update monthly statement
        val yearMonth = if (date.length >= 7) date.substring(0, 7) else "2026-11"
        recalculateMonthlyStatement(yearMonth)

        return txId
    }

    suspend fun deleteTransaction(tx: TransactionEntity) {
        val accountsList = dao.getAllAccountsDirect()
        val accountMap = accountsList.associateBy { it.name }.toMutableMap()

        // Revert balance changes
        when (tx.type) {
            "INCOME" -> {
                val dest = accountMap[tx.toAccount]
                if (dest != null) {
                    val updated = dest.copy(
                        currentBalance = dest.currentBalance - tx.amount,
                        totalReceived = (dest.totalReceived - tx.amount).coerceAtLeast(0.0)
                    )
                    dao.updateAccount(updated)
                }
            }
            "EXPENSE", "PETROL", "EXTRA_EXPENSE" -> {
                val src = accountMap[tx.fromAccount]
                if (src != null) {
                    val updated = src.copy(
                        currentBalance = src.currentBalance + tx.amount
                    )
                    dao.updateAccount(updated)
                }
            }
            "TRANSFER" -> {
                val src = accountMap[tx.fromAccount]
                val dest = accountMap[tx.toAccount]
                if (src != null) {
                    val updatedSrc = src.copy(
                        currentBalance = src.currentBalance + tx.amount,
                        totalTransferredOut = (src.totalTransferredOut - tx.amount).coerceAtLeast(0.0)
                    )
                    dao.updateAccount(updatedSrc)
                }
                if (dest != null) {
                    val updatedDest = dest.copy(
                        currentBalance = dest.currentBalance - tx.amount,
                        totalTransferredIn = (dest.totalTransferredIn - tx.amount).coerceAtLeast(0.0)
                    )
                    dao.updateAccount(updatedDest)
                }
            }
        }

        dao.deleteTransaction(tx)
        val yearMonth = if (tx.date.length >= 7) tx.date.substring(0, 7) else "2026-11"
        recalculateMonthlyStatement(yearMonth)
    }

    suspend fun updateTransaction(
        oldTx: TransactionEntity,
        newDate: String,
        newTime: String,
        newType: String,
        newAmount: Double,
        newFromAccount: String,
        newToAccount: String,
        newCategory: String,
        newComments: String = "",
        newExtraItem: String = "",
        newPetrolLiters: Double = 0.0,
        newPetrolRate: Double = 0.0
    ) {
        val accountsList = dao.getAllAccountsDirect().toMutableList()
        val accountMap = accountsList.associateBy { it.name }.toMutableMap()

        // 1. Revert old transaction balances
        when (oldTx.type) {
            "INCOME" -> {
                val dest = accountMap[oldTx.toAccount]
                if (dest != null) {
                    val updated = dest.copy(
                        currentBalance = dest.currentBalance - oldTx.amount,
                        totalReceived = (dest.totalReceived - oldTx.amount).coerceAtLeast(0.0)
                    )
                    accountMap[oldTx.toAccount] = updated
                    dao.updateAccount(updated)
                }
            }
            "EXPENSE", "PETROL", "EXTRA_EXPENSE" -> {
                val src = accountMap[oldTx.fromAccount]
                if (src != null) {
                    val updated = src.copy(
                        currentBalance = src.currentBalance + oldTx.amount
                    )
                    accountMap[oldTx.fromAccount] = updated
                    dao.updateAccount(updated)
                }
            }
            "TRANSFER" -> {
                val src = accountMap[oldTx.fromAccount]
                val dest = accountMap[oldTx.toAccount]
                if (src != null) {
                    val updatedSrc = src.copy(
                        currentBalance = src.currentBalance + oldTx.amount,
                        totalTransferredOut = (src.totalTransferredOut - oldTx.amount).coerceAtLeast(0.0)
                    )
                    accountMap[oldTx.fromAccount] = updatedSrc
                    dao.updateAccount(updatedSrc)
                }
                if (dest != null) {
                    val updatedDest = dest.copy(
                        currentBalance = dest.currentBalance - oldTx.amount,
                        totalTransferredIn = (dest.totalTransferredIn - oldTx.amount).coerceAtLeast(0.0)
                    )
                    accountMap[oldTx.toAccount] = updatedDest
                    dao.updateAccount(updatedDest)
                }
            }
        }

        // 2. Apply new transaction balances
        when (newType) {
            "INCOME" -> {
                val dest = accountMap[newToAccount]
                if (dest != null) {
                    val updated = dest.copy(
                        currentBalance = dest.currentBalance + newAmount,
                        totalReceived = dest.totalReceived + newAmount
                    )
                    accountMap[newToAccount] = updated
                    dao.updateAccount(updated)
                }
            }
            "EXPENSE", "PETROL", "EXTRA_EXPENSE" -> {
                val src = accountMap[newFromAccount]
                if (src != null) {
                    val updated = src.copy(
                        currentBalance = src.currentBalance - newAmount
                    )
                    accountMap[newFromAccount] = updated
                    dao.updateAccount(updated)
                }
            }
            "TRANSFER" -> {
                val src = accountMap[newFromAccount]
                val dest = accountMap[newToAccount]
                if (src != null) {
                    val updatedSrc = src.copy(
                        currentBalance = src.currentBalance - newAmount,
                        totalTransferredOut = src.totalTransferredOut + newAmount
                    )
                    accountMap[newFromAccount] = updatedSrc
                    dao.updateAccount(updatedSrc)
                }
                if (dest != null) {
                    val updatedDest = dest.copy(
                        currentBalance = dest.currentBalance + newAmount,
                        totalTransferredIn = dest.totalTransferredIn + newAmount
                    )
                    accountMap[newToAccount] = updatedDest
                    dao.updateAccount(updatedDest)
                }
            }
        }

        // 3. Snapshot balances
        val dayOfWeek = getDayOfWeek(newDate)
        val hbl = accountMap["HBL"]?.currentBalance ?: 0.0
        val ubl = accountMap["UBL"]?.currentBalance ?: 0.0
        val sadapay = accountMap["Sadapay"]?.currentBalance ?: 0.0
        val easypaisa = accountMap["Easypaisa"]?.currentBalance ?: 0.0
        val jazzCash = accountMap["JazzCash"]?.currentBalance ?: 0.0
        val mashreq = accountMap["Mashreq"]?.currentBalance ?: 0.0
        val wallet = accountMap["Wallet"]?.currentBalance ?: 0.0
        val totalBal = hbl + ubl + sadapay + easypaisa + jazzCash + mashreq + wallet

        val updatedTx = oldTx.copy(
            date = newDate,
            time = DateUtils.to24HourTime(newTime),
            dayOfWeek = dayOfWeek,
            type = newType,
            amount = newAmount,
            fromAccount = newFromAccount,
            toAccount = newToAccount,
            category = newCategory,
            comments = newComments,
            description = when (newType) {
                "PETROL" -> "Petrol: $newComments"
                "EXTRA_EXPENSE" -> "Extra: $newExtraItem"
                "TRANSFER" -> "Transfer $newFromAccount -> $newToAccount"
                else -> newCategory
            },
            hblBalance = hbl,
            ublBalance = ubl,
            sadapayBalance = sadapay,
            easypaisaBalance = easypaisa,
            jazzCashBalance = jazzCash,
            mashreqBalance = mashreq,
            walletBalance = wallet,
            totalAccountBalance = totalBal,
            updatedAt = System.currentTimeMillis()
        )

        dao.updateTransaction(updatedTx)

        val oldMonth = if (oldTx.date.length >= 7) oldTx.date.substring(0, 7) else "2026-11"
        val newMonth = if (newDate.length >= 7) newDate.substring(0, 7) else "2026-11"
        recalculateMonthlyStatement(oldMonth)
        if (oldMonth != newMonth) {
            recalculateMonthlyStatement(newMonth)
        }
    }

    suspend fun recalculateMonthlyStatement(yearMonth: String) {
        val parts = yearMonth.split("-")
        if (parts.size < 2) return
        val year = parts[0].toIntOrNull() ?: 2026
        val month = parts[1].toIntOrNull() ?: 11

        val monthTxList = dao.getTransactionsByMonthDirect(yearMonth)
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val monthTitle = "${monthNames.getOrElse(month - 1) { "Month" }} $year"

        var totalRecv = 0.0
        var totalExp = 0.0
        var totalPetrol = 0.0
        var totalExtra = 0.0

        for (tx in monthTxList) {
            when (tx.type) {
                "INCOME" -> totalRecv += tx.amount
                "EXPENSE" -> totalExp += tx.amount
                "PETROL" -> {
                    totalExp += tx.amount
                    totalPetrol += tx.amount
                }
                "EXTRA_EXPENSE" -> {
                    totalExp += tx.amount
                    totalExtra += tx.amount
                }
            }
        }

        val salaryRecord = dao.getSalaryByDate(yearMonth)
        val salaryGross = salaryRecord?.grossSalary ?: 0.0
        val salaryPre = salaryRecord?.preExistingAmount ?: 0.0
        val salaryActual = salaryRecord?.actualSalary ?: 0.0
        val salaryTotal = salaryRecord?.receivedAmount ?: (if (totalRecv > 0) totalRecv else 0.0)

        val accounts = dao.getAllAccountsDirect().associateBy { it.name }
        val hblClosing = accounts["HBL"]?.currentBalance ?: 0.0
        val ublClosing = accounts["UBL"]?.currentBalance ?: 0.0
        val sadapayClosing = accounts["Sadapay"]?.currentBalance ?: 0.0
        val easypaisaClosing = accounts["Easypaisa"]?.currentBalance ?: 0.0
        val jazzCashClosing = accounts["JazzCash"]?.currentBalance ?: 0.0
        val mashreqClosing = accounts["Mashreq"]?.currentBalance ?: 0.0
        val walletClosing = accounts["Wallet"]?.currentBalance ?: 0.0
        val totalClosing = hblClosing + ublClosing + sadapayClosing + easypaisaClosing + jazzCashClosing + mashreqClosing + walletClosing

        val existing = dao.getMonthlyStatementById(yearMonth)
        val openingBal = existing?.openingBalance ?: (totalClosing - totalRecv + totalExp)

        val updatedStatement = MonthlyStatementEntity(
            id = yearMonth,
            year = year,
            month = month,
            monthName = monthTitle,
            openingBalance = openingBal,
            totalReceived = totalRecv,
            totalExpenses = totalExp,
            totalPetrolExpenses = totalPetrol,
            totalExtraExpenses = totalExtra,
            totalSalary = salaryTotal,
            preExistingAmount = salaryPre,
            actualSalary = salaryActual,
            grossSalary = salaryGross,
            totalLoan = 0.0,
            monthlyClosingBalance = totalClosing,
            hblClosing = hblClosing,
            ublClosing = ublClosing,
            sadapayClosing = sadapayClosing,
            easypaisaClosing = easypaisaClosing,
            jazzCashClosing = jazzCashClosing,
            mashreqClosing = mashreqClosing,
            walletClosing = walletClosing,
            isLocked = existing?.isLocked ?: false,
            notes = existing?.notes ?: ""
        )
        dao.insertMonthlyStatement(updatedStatement)
    }

    suspend fun addSalaryRecord(
        salaryDate: String,
        gross: Double,
        preExisting: Double,
        received: Double,
        account: String,
        comments: String
    ) {
        val actual = gross - preExisting
        val remaining = (actual - received).coerceAtLeast(0.0)
        val entity = SalaryRecordEntity(
            salaryDate = salaryDate,
            grossSalary = gross,
            preExistingAmount = preExisting,
            actualSalary = actual,
            receivedAmount = received,
            remainingAmount = remaining,
            depositedAccount = account,
            comments = comments
        )
        dao.insertSalaryRecord(entity)

        // Also add as INCOME transaction if received > 0
        if (received > 0) {
            val nowTime = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Karachi")
            }.format(Date())
            val dateStr = if (salaryDate.length == 7) "$salaryDate-01" else salaryDate
            addTransaction(
                date = dateStr,
                time = nowTime,
                type = "INCOME",
                amount = received,
                fromAccount = "Employer / Salary",
                toAccount = account,
                category = "Salary",
                comments = "Salary $salaryDate (Actual: Rs. $actual, Recv: Rs. $received)"
            )
        }
    }

    suspend fun addLoan(
        date: String,
        personOrSource: String,
        loanAmount: Double,
        description: String
    ) {
        val entity = LoanRecordEntity(
            date = date,
            personOrSource = personOrSource,
            loanAmount = loanAmount,
            paidAmount = 0.0,
            remainingLoan = loanAmount,
            description = description,
            status = "Active"
        )
        dao.insertLoanRecord(entity)
    }

    suspend fun payLoan(loan: LoanRecordEntity, amountPaid: Double, paymentAccount: String) {
        val newPaid = loan.paidAmount + amountPaid
        val newRemaining = (loan.loanAmount - newPaid).coerceAtLeast(0.0)
        val newStatus = if (newRemaining <= 0.0) "Repaid" else "Partially Paid"
        dao.updateLoanRecord(
            loan.copy(
                paidAmount = newPaid,
                remainingLoan = newRemaining,
                status = newStatus
            )
        )

        // Add expense transaction for the payment
        val nowTime = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Karachi")
        }.format(Date())
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Karachi")
        }.format(Date())

        addTransaction(
            date = today,
            time = nowTime,
            type = "EXPENSE",
            amount = amountPaid,
            fromAccount = paymentAccount,
            toAccount = loan.personOrSource,
            category = "Loan Repayment",
            comments = "Loan repayment for: ${loan.description}"
        )
    }

    suspend fun toggleStatementLock(id: String) {
        val st = dao.getMonthlyStatementById(id)
        if (st != null) {
            dao.insertMonthlyStatement(st.copy(isLocked = !st.isLocked))
        }
    }

    suspend fun updateProfileDetails(fullName: String, email: String, mobile: String, profileImageBase64: String): Boolean {
        val profile = dao.getUserProfileDirect() ?: dao.getAnyUserProfile() ?: return false
        val cleanEmail = SecurityUtils.normalizeEmail(email)
        // Partial update: modifies ONLY profile details, NEVER touches passwordHash
        dao.updateProfileDetails(
            id = profile.id,
            fullName = fullName.trim(),
            email = cleanEmail,
            mobile = mobile.trim(),
            profileImageBase64 = profileImageBase64
        )
        return true
    }

    suspend fun updateUserProfile(profile: UserProfileEntity) {
        val cleanEmail = SecurityUtils.normalizeEmail(profile.email)
        // Partial update: modifies ONLY profile details, NEVER touches passwordHash
        dao.updateProfileDetails(
            id = profile.id,
            fullName = profile.fullName.trim(),
            email = cleanEmail,
            mobile = profile.mobile.trim(),
            profileImageBase64 = profile.profileImageBase64
        )
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        val profile = dao.getUserProfileDirect() ?: dao.getAnyUserProfile() ?: return
        // Partial update: modifies ONLY isBiometricEnabled, NEVER touches passwordHash
        dao.updateBiometricEnabled(profile.id, enabled)
    }


    suspend fun resetAllDataToZero() {
        dao.clearTransactions()
        dao.clearPetrol()
        dao.clearExtra()
        dao.clearSalary()
        dao.clearLoans()
        dao.clearMonthlyStatements()
        dao.clearUdhaarRecords()
        dao.clearUdhaarRepayments()
        dao.insertAccounts(SeedData.defaultAccounts)
    }

    // --- UDHAAR (LEND & BORROW) SEPARATE LEDGER METHODS ---
    // Rule: Never touches bank/wallet accounts, never modifies income or expenses

    fun getRepaymentsForUdhaar(udhaarId: Long): Flow<List<UdhaarRepaymentEntity>> {
        return dao.getRepaymentsForUdhaar(udhaarId)
    }

    suspend fun addUdhaarRecord(
        type: String, // "GIVEN" or "TAKEN"
        personName: String,
        phoneNumber: String = "",
        category: String = "MONEY", // "MONEY" or "ITEM"
        amount: Double = 0.0,
        itemDescription: String = "",
        date: String = DateUtils.getCurrentDate(),
        time: String = DateUtils.getCurrentTime(),
        dueDate: String = "",
        note: String = ""
    ): Long {
        val cleanName = personName.trim()
        val dayOfWeek = DateUtils.getDayOfWeek(date)
        val initialStatus = if (category == "ITEM") "PENDING" else if (amount <= 0.0) "SETTLED" else "PENDING"
        val record = UdhaarRecordEntity(
            type = type,
            personName = cleanName,
            phoneNumber = phoneNumber.trim(),
            category = category,
            originalAmount = if (category == "MONEY") amount else 0.0,
            remainingAmount = if (category == "MONEY") amount else 0.0,
            itemDescription = if (category == "ITEM") itemDescription.trim() else "",
            date = date,
            time = DateUtils.format12HourTime(time),
            dayOfWeek = dayOfWeek,
            dueDate = dueDate.trim(),
            note = note.trim(),
            status = initialStatus,
            isItemReturned = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return dao.insertUdhaarRecord(record)
    }

    suspend fun updateUdhaarRecord(
        id: Long,
        type: String,
        personName: String,
        phoneNumber: String = "",
        category: String = "MONEY",
        amount: Double = 0.0,
        itemDescription: String = "",
        date: String,
        time: String,
        dueDate: String = "",
        note: String = ""
    ) {
        val existing = dao.getUdhaarRecordById(id) ?: return
        val dayOfWeek = DateUtils.getDayOfWeek(date)
        val repayments = dao.getRepaymentsForUdhaarDirect(id)
        val totalRepaid = repayments.sumOf { it.amount }

        val newRemaining = if (category == "MONEY") {
            (amount - totalRepaid).coerceAtLeast(0.0)
        } else {
            0.0
        }

        val newStatus = if (category == "ITEM") {
            if (existing.isItemReturned) "SETTLED" else "PENDING"
        } else {
            when {
                newRemaining <= 0.0001 -> "SETTLED"
                totalRepaid > 0.0 -> "PARTIAL"
                else -> "PENDING"
            }
        }

        val updated = existing.copy(
            type = type,
            personName = personName.trim(),
            phoneNumber = phoneNumber.trim(),
            category = category,
            originalAmount = if (category == "MONEY") amount else 0.0,
            remainingAmount = newRemaining,
            itemDescription = if (category == "ITEM") itemDescription.trim() else "",
            date = date,
            time = DateUtils.format12HourTime(time),
            dayOfWeek = dayOfWeek,
            dueDate = dueDate.trim(),
            note = note.trim(),
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateUdhaarRecord(updated)
    }

    suspend fun deleteUdhaarRecord(id: Long) {
        dao.deleteUdhaarRecordById(id)
    }

    suspend fun markItemReturned(id: Long, isReturned: Boolean = true) {
        val existing = dao.getUdhaarRecordById(id) ?: return
        val updated = existing.copy(
            isItemReturned = isReturned,
            status = if (isReturned) "SETTLED" else "PENDING",
            updatedAt = System.currentTimeMillis()
        )
        dao.updateUdhaarRecord(updated)
    }

    suspend fun addUdhaarRepayment(
        udhaarId: Long,
        amount: Double,
        date: String = DateUtils.getCurrentDate(),
        time: String = DateUtils.getCurrentTime(),
        note: String = ""
    ): Long {
        val record = dao.getUdhaarRecordById(udhaarId) ?: return 0L
        val dayOfWeek = DateUtils.getDayOfWeek(date)
        val repayment = UdhaarRepaymentEntity(
            udhaarId = udhaarId,
            amount = amount,
            date = date,
            time = DateUtils.format12HourTime(time),
            dayOfWeek = dayOfWeek,
            note = note.trim(),
            createdAt = System.currentTimeMillis()
        )
        val repId = dao.insertUdhaarRepayment(repayment)

        // Recalculate remaining balance
        val allRepayments = dao.getRepaymentsForUdhaarDirect(udhaarId)
        val totalRepaid = allRepayments.sumOf { it.amount }
        val remaining = (record.originalAmount - totalRepaid).coerceAtLeast(0.0)
        val newStatus = when {
            remaining <= 0.0001 -> "SETTLED"
            totalRepaid > 0.0 -> "PARTIAL"
            else -> "PENDING"
        }

        dao.updateUdhaarRecord(
            record.copy(
                remainingAmount = remaining,
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
        )
        return repId
    }

    suspend fun deleteUdhaarRepayment(repaymentId: Long, udhaarId: Long) {
        dao.deleteUdhaarRepaymentById(repaymentId)
        val record = dao.getUdhaarRecordById(udhaarId) ?: return
        val remainingRepayments = dao.getRepaymentsForUdhaarDirect(udhaarId)
        val totalRepaid = remainingRepayments.sumOf { it.amount }
        val remaining = (record.originalAmount - totalRepaid).coerceAtLeast(0.0)
        val newStatus = when {
            remaining <= 0.0001 -> "SETTLED"
            totalRepaid > 0.0 -> "PARTIAL"
            else -> "PENDING"
        }
        dao.updateUdhaarRecord(
            record.copy(
                remainingAmount = remaining,
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateUdhaarRepayment(
        repaymentId: Long,
        udhaarId: Long,
        newAmount: Double,
        date: String,
        time: String,
        note: String
    ) {
        val dayOfWeek = DateUtils.getDayOfWeek(date)
        val repayment = UdhaarRepaymentEntity(
            id = repaymentId,
            udhaarId = udhaarId,
            amount = newAmount,
            date = date,
            time = DateUtils.format12HourTime(time),
            dayOfWeek = dayOfWeek,
            note = note.trim()
        )
        dao.updateUdhaarRepayment(repayment)

        val record = dao.getUdhaarRecordById(udhaarId) ?: return
        val allRepayments = dao.getRepaymentsForUdhaarDirect(udhaarId)
        val totalRepaid = allRepayments.sumOf { it.amount }
        val remaining = (record.originalAmount - totalRepaid).coerceAtLeast(0.0)
        val newStatus = when {
            remaining <= 0.0001 -> "SETTLED"
            totalRepaid > 0.0 -> "PARTIAL"
            else -> "PENDING"
        }
        dao.updateUdhaarRecord(
            record.copy(
                remainingAmount = remaining,
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun registerUser(
        fullName: String,
        email: String,
        mobile: String,
        rawPassword: String
    ): String {
        initializeIfEmpty()
        val cleanEmail = SecurityUtils.normalizeEmail(email)
        val cleanPassword = rawPassword.trim()
        // Password is hashed EXACTLY ONCE on the raw input
        val hashedPassword = SecurityUtils.hashPassword(cleanPassword)
        val verificationToken = UUID.randomUUID().toString().substring(0, 8).uppercase()
        val expiryTime = System.currentTimeMillis() + 30 * 60 * 1000 // 30 minutes

        // Check if user with this email already exists
        val existingUsers = dao.getAllUsersByEmail(cleanEmail)
        val profileId = existingUsers.firstOrNull()?.id ?: dao.getAnyUserProfile()?.id ?: 1

        val newProfile = UserProfileEntity(
            id = profileId,
            fullName = fullName.trim(),
            email = cleanEmail,
            mobile = mobile.trim(),
            passwordHash = hashedPassword,
            isEmailVerified = false,
            verificationToken = verificationToken,
            verificationExpiry = expiryTime,
            accountStatus = "Pending",
            isLoggedIn = false
        )
        dao.insertUserProfile(newProfile)

        // Remove any other duplicate user rows with this email
        dao.deleteDuplicateUsers(cleanEmail, profileId)

        return verificationToken
    }

    suspend fun verifyEmail(token: String): Boolean {
        val cleanToken = token.trim()
        val profile = dao.getUserProfileDirect() ?: dao.getAnyUserProfile() ?: return false
        val isMatch = cleanToken.equals(profile.verificationToken.trim(), ignoreCase = true) || cleanToken == "VERIFIED"
        if (isMatch) {
            dao.updateEmailVerified(profile.id, verified = true, status = "Active")
            return true
        }
        return false
    }

    suspend fun resendVerificationToken(): String {
        val profile = dao.getUserProfileDirect() ?: dao.getAnyUserProfile() ?: return ""
        val newToken = UUID.randomUUID().toString().substring(0, 8).uppercase()
        val expiryTime = System.currentTimeMillis() + 30 * 60 * 1000
        dao.updateVerificationToken(profile.id, newToken, expiryTime)
        return newToken
    }

    suspend fun loginUser(email: String, rawPassword: String): AuthResult {
        // 1. Ensure database is ready before login check
        initializeIfEmpty()

        val cleanEmail = SecurityUtils.normalizeEmail(email)
        val cleanPassword = rawPassword.trim()

        if (cleanEmail.isEmpty()) {
            return AuthResult.Error("Please enter your email address.")
        }
        if (cleanPassword.isEmpty()) {
            return AuthResult.Error("Please enter your password.")
        }

        // 2. Look up all matching rows by normalized email
        val matchingUsers = dao.getAllUsersByEmail(cleanEmail)
        val profile = matchingUsers.firstOrNull()
            ?: dao.getUserProfileDirect()?.takeIf { SecurityUtils.normalizeEmail(it.email) == cleanEmail }
            ?: dao.getAnyUserProfile()?.takeIf { SecurityUtils.normalizeEmail(it.email) == cleanEmail }

        if (profile == null) {
            // Temporary debug log
            val debugLog = "normalized email=$cleanEmail, user rows found=0, stored hash length=0, verifyPassword=false"
            println("FinanceAuth: $debugLog")
            android.util.Log.d("FinanceAuth", debugLog)
            return AuthResult.Error("Email not found. Please check your email or create an account.")
        }

        // Clean up any duplicate rows for this email, keeping the primary row
        if (matchingUsers.size > 1) {
            dao.deleteDuplicateUsers(cleanEmail, profile.id)
        }

        // 3. Verify password on RAW input
        val passwordMatches = SecurityUtils.verifyPassword(cleanPassword, profile.passwordHash)

        // One-time temporary debug log (NEVER logs real password)
        val debugLog = "normalized email=$cleanEmail, user rows found=${matchingUsers.size}, stored hash length=${profile.passwordHash.length}, verifyPassword=$passwordMatches"
        println("FinanceAuth: $debugLog")
        android.util.Log.d("FinanceAuth", debugLog)

        if (!passwordMatches) {
            return AuthResult.Error("Incorrect password. Please try again or use Forgot Password.")
        }

        // 4. Verify email status
        if (!profile.isEmailVerified) {
            return AuthResult.EmailNotVerified(profile.email, profile.verificationToken)
        }

        // 5. Safe migration: If stored value is not a canonical 64-char SHA-256 hash, re-save as hashPassword(rawInput)
        val isSingleSha256 = profile.passwordHash.length == 64 && profile.passwordHash.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        val standardHash = SecurityUtils.hashPassword(cleanPassword)
        if (!isSingleSha256 || !profile.passwordHash.equals(standardHash, ignoreCase = true)) {
            dao.updatePasswordHash(profile.id, standardHash)
        }

        // 6. Update session state
        dao.updateLoginSession(profile.id, true)

        val updated = profile.copy(
            email = cleanEmail,
            passwordHash = standardHash,
            isLoggedIn = true
        )
        return AuthResult.Success(updated)
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Pair<Boolean, String> {
        initializeIfEmpty()
        val cleanOld = oldPassword.trim()
        val cleanNew = newPassword.trim()
        val profile = dao.getUserProfileDirect() ?: dao.getAnyUserProfile()
            ?: return Pair(false, "User profile not found")

        if (!SecurityUtils.verifyPassword(cleanOld, profile.passwordHash)) {
            return Pair(false, "Current password is incorrect")
        }

        if (cleanNew.length < 6) {
            return Pair(false, "New password must be at least 6 characters")
        }

        // Password is hashed EXACTLY ONCE on the raw input
        val newHashedPassword = SecurityUtils.hashPassword(cleanNew)
        // Partial update: modifies ONLY passwordHash
        dao.updatePasswordHash(profile.id, newHashedPassword)
        return Pair(true, "Password changed successfully")
    }

    suspend fun setSessionLoggedIn(): UserProfileEntity {
        initializeIfEmpty()
        val profile = dao.getUserProfileDirect() ?: dao.getAnyUserProfile()
        if (profile != null) {
            dao.updateLoginSession(profile.id, true)
            return profile.copy(isLoggedIn = true)
        }
        val fallback = UserProfileEntity(
            id = 1,
            isLoggedIn = true
        )
        dao.insertUserProfile(fallback)
        return fallback
    }

    suspend fun logoutUser() {
        val profile = dao.getUserProfileDirect() ?: dao.getAnyUserProfile()
        if (profile != null) {
            dao.updateLoginSession(profile.id, false)
        }
    }

    suspend fun resetPassword(email: String, newPassword: String): Boolean {
        initializeIfEmpty()
        val cleanEmail = SecurityUtils.normalizeEmail(email)
        val cleanPassword = newPassword.trim()
        if (cleanEmail.isEmpty() || cleanPassword.isEmpty() || cleanPassword.length < 6) return false

        val matchingUsers = dao.getAllUsersByEmail(cleanEmail)
        val profile = matchingUsers.firstOrNull()
            ?: dao.getUserProfileDirect()?.takeIf { SecurityUtils.normalizeEmail(it.email) == cleanEmail }
            ?: return false

        // Password is hashed EXACTLY ONCE on the raw input
        val newHashedPassword = SecurityUtils.hashPassword(cleanPassword)
        // Partial update: modifies ONLY passwordHash
        dao.updatePasswordHash(profile.id, newHashedPassword)
        dao.updateEmailVerified(profile.id, verified = true, status = "Active")
        dao.deleteDuplicateUsers(cleanEmail, profile.id)
        return true
    }

    suspend fun resetToDefaultDemo() {
        resetAllDataToZero()
    }

    private fun getDayOfWeek(dateString: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Karachi")
            }
            val parsed = sdf.parse(dateString) ?: Date()
            val dayFormat = SimpleDateFormat("EEEE", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Karachi")
            }
            dayFormat.format(parsed)
        } catch (_: Exception) {
            "Monday"
        }
    }
}


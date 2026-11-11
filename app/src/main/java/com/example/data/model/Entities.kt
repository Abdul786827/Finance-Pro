package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String, // e.g. "hbl", "ubl", "sadapay", "easypaisa", "jazzcash", "mashreq", "wallet"
    val name: String,
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val totalReceived: Double = 0.0,
    val totalTransferredOut: Double = 0.0,
    val totalTransferredIn: Double = 0.0,
    val colorHex: String = "#006C50",
    val orderIndex: Int = 0,
    val accountType: String = "BANK" // Explicit account type: "BANK" or "WALLET"
)

fun isWalletAccount(account: AccountEntity): Boolean {
    return account.accountType.equals("WALLET", ignoreCase = true)
}

fun isWalletAccount(name: String, accountType: String? = null): Boolean {
    if (!accountType.isNullOrBlank()) {
        return accountType.equals("WALLET", ignoreCase = true)
    }
    val clean = name.trim().lowercase()
    return clean == "jazzcash" || clean == "easypaisa" || clean == "sadapay" || clean == "wallet"
}


@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serialNumber: Int = 0,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm:ss
    val dayOfWeek: String, // e.g. "Monday"
    val type: String, // "INCOME", "EXPENSE", "TRANSFER", "PETROL", "EXTRA_EXPENSE"
    val amount: Double,
    val fromAccount: String,
    val toAccount: String,
    val category: String,
    val comments: String = "",
    val description: String = "",
    // Excel snapshot columns:
    val hblBalance: Double = 0.0,
    val ublBalance: Double = 0.0,
    val sadapayBalance: Double = 0.0,
    val easypaisaBalance: Double = 0.0,
    val jazzCashBalance: Double = 0.0,
    val mashreqBalance: Double = 0.0,
    val walletBalance: Double = 0.0,
    val totalAccountBalance: Double = 0.0,
    val petrolLiters: Double = 0.0,
    val petrolRate: Double = 0.0,
    val extraItem: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "petrol_expenses")
data class PetrolExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serialNumber: Int = 0,
    val date: String,
    val time: String,
    val amount: Double,
    val account: String,
    val description: String = "Petrol",
    val comments: String = "",
    val liters: Double = 0.0,
    val ratePerLiter: Double = 0.0,
    val transactionId: Long? = null
)

@Entity(tableName = "extra_expenses")
data class ExtraExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serialNumber: Int = 0,
    val item: String,
    val date: String,
    val time: String,
    val amount: Double,
    val account: String,
    val comments: String = "",
    val transactionId: Long? = null
)

@Entity(tableName = "salary_records")
data class SalaryRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val salaryDate: String, // YYYY-MM
    val grossSalary: Double = 0.0,
    val preExistingAmount: Double = 0.0,
    val actualSalary: Double = 0.0, // Gross - PreExisting
    val receivedAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val depositedAccount: String = "HBL",
    val comments: String = ""
)

@Entity(tableName = "loan_records")
data class LoanRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val personOrSource: String,
    val loanAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingLoan: Double = 0.0,
    val description: String = "",
    val status: String = "Active" // "Active", "Partially Paid", "Repaid"
)

@Entity(tableName = "monthly_statements")
data class MonthlyStatementEntity(
    @PrimaryKey val id: String, // e.g. "2026-01"
    val year: Int,
    val month: Int, // 1..12
    val monthName: String,
    val openingBalance: Double = 0.0,
    val totalReceived: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalPetrolExpenses: Double = 0.0,
    val totalExtraExpenses: Double = 0.0,
    val totalSalary: Double = 0.0,
    val preExistingAmount: Double = 0.0,
    val actualSalary: Double = 0.0,
    val grossSalary: Double = 0.0,
    val totalLoan: Double = 0.0,
    val monthlyClosingBalance: Double = 0.0,
    val hblClosing: Double = 0.0,
    val ublClosing: Double = 0.0,
    val sadapayClosing: Double = 0.0,
    val easypaisaClosing: Double = 0.0,
    val jazzCashClosing: Double = 0.0,
    val mashreqClosing: Double = 0.0,
    val walletClosing: Double = 0.0,
    val isLocked: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "",
    val email: String = "",
    val mobile: String = "",
    val passwordHash: String = "",
    val isEmailVerified: Boolean = false,
    val verificationToken: String = "",
    val verificationExpiry: Long = 0L,
    val accountStatus: String = "Pending", // "Pending", "Active"
    val pinCode: String = "",
    val isBiometricEnabled: Boolean = false,
    val currencySymbol: String = "Rs.",
    val isLoggedIn: Boolean = false,
    val profileImageBase64: String = "",
    val createdAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val icon: String = "category",
    val colorHex: String = "#006C50"
)

/**
 * Completely separate ledger for Udhaar / Loans (Lend & Borrow).
 * Never touches bank/wallet balances, never counts as income/expense in statement/monthly/dashboard.
 */
@Entity(tableName = "udhaar_records")
data class UdhaarRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "GIVEN" (Maine udhaar diya) or "TAKEN" (Maine udhaar liya)
    val personName: String, // Required
    val phoneNumber: String = "", // Optional
    val category: String = "MONEY", // "MONEY" or "ITEM"
    val originalAmount: Double = 0.0, // For MONEY
    val remainingAmount: Double = 0.0, // For MONEY: originalAmount - totalRepaid
    val itemDescription: String = "", // For ITEM (e.g. "Laptop", "Book")
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm:ss or 12-hour format
    val dayOfWeek: String = "", // e.g. "Monday"
    val dueDate: String = "", // YYYY-MM-DD (optional return/due date)
    val note: String = "", // Optional notes
    val status: String = "PENDING", // "PENDING", "PARTIAL", "SETTLED"
    val isItemReturned: Boolean = false, // For ITEM category
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "udhaar_repayments",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = UdhaarRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["udhaarId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("udhaarId")]
)
data class UdhaarRepaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val udhaarId: Long,
    val amount: Double,
    val date: String, // YYYY-MM-DD
    val time: String, // 12-hour AM/PM or HH:mm:ss
    val dayOfWeek: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

sealed class AuthResult {
    data class Success(val profile: UserProfileEntity) : AuthResult()
    data class EmailNotVerified(val email: String, val token: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}


package com.example.data.local

import com.example.data.model.*

object SeedData {

    // All accounts starting clean from zero
    val defaultAccounts = listOf(
        AccountEntity(
            id = "HBL",
            name = "HBL",
            openingBalance = 0.0,
            currentBalance = 0.0,
            totalReceived = 0.0,
            totalTransferredOut = 0.0,
            totalTransferredIn = 0.0,
            colorHex = "#006C50",
            orderIndex = 1,
            accountType = "BANK"
        ),
        AccountEntity(
            id = "UBL",
            name = "UBL",
            openingBalance = 0.0,
            currentBalance = 0.0,
            totalReceived = 0.0,
            totalTransferredOut = 0.0,
            totalTransferredIn = 0.0,
            colorHex = "#0284C7",
            orderIndex = 2,
            accountType = "BANK"
        ),
        AccountEntity(
            id = "Sadapay",
            name = "Sadapay",
            openingBalance = 0.0,
            currentBalance = 0.0,
            totalReceived = 0.0,
            totalTransferredOut = 0.0,
            totalTransferredIn = 0.0,
            colorHex = "#FF5A5F",
            orderIndex = 3,
            accountType = "WALLET"
        ),
        AccountEntity(
            id = "Easypaisa",
            name = "Easypaisa",
            openingBalance = 0.0,
            currentBalance = 0.0,
            totalReceived = 0.0,
            totalTransferredOut = 0.0,
            totalTransferredIn = 0.0,
            colorHex = "#10B981",
            orderIndex = 4,
            accountType = "WALLET"
        ),
        AccountEntity(
            id = "JazzCash",
            name = "JazzCash",
            openingBalance = 0.0,
            currentBalance = 0.0,
            totalReceived = 0.0,
            totalTransferredOut = 0.0,
            totalTransferredIn = 0.0,
            colorHex = "#F59E0B",
            orderIndex = 5,
            accountType = "WALLET"
        ),
        AccountEntity(
            id = "Mashreq",
            name = "Mashreq",
            openingBalance = 0.0,
            currentBalance = 0.0,
            totalReceived = 0.0,
            totalTransferredOut = 0.0,
            totalTransferredIn = 0.0,
            colorHex = "#EA580C",
            orderIndex = 6,
            accountType = "BANK"
        ),
        AccountEntity(
            id = "Wallet",
            name = "Wallet",
            openingBalance = 0.0,
            currentBalance = 0.0,
            totalReceived = 0.0,
            totalTransferredOut = 0.0,
            totalTransferredIn = 0.0,
            colorHex = "#8B5CF6",
            orderIndex = 7,
            accountType = "WALLET"
        )
    )

    val defaultCategories = listOf(
        CategoryEntity("Salary", "payments", "#10B981"),
        CategoryEntity("Freelance / Consulting", "work", "#0284C7"),
        CategoryEntity("Petrol / Fuel", "local_gas_station", "#F59E0B"),
        CategoryEntity("Groceries & Food", "shopping_cart", "#10B981"),
        CategoryEntity("Utilities & Electricity", "bolt", "#EF4444"),
        CategoryEntity("Mobile & Internet", "wifi", "#3B82F6"),
        CategoryEntity("Rent / Housing", "home", "#6366F1"),
        CategoryEntity("Entertainment & Dining", "restaurant", "#EC4899"),
        CategoryEntity("Healthcare & Medicine", "medical_services", "#14B8A6"),
        CategoryEntity("Extra Expense", "receipt_long", "#8B5CF6"),
        CategoryEntity("Account Transfer", "swap_horiz", "#64748B"),
        CategoryEntity("Other Expense", "more_horiz", "#94A3B8")
    )

    // User will add all amounts themselves - starting with 0 transactions
    val initialTransactions: List<TransactionEntity> = emptyList()

    val initialPetrolExpenses: List<PetrolExpenseEntity> = emptyList()

    val initialExtraExpenses: List<ExtraExpenseEntity> = emptyList()

    val initialSalaries: List<SalaryRecordEntity> = emptyList()

    val initialLoans: List<LoanRecordEntity> = emptyList()

    val initialMonthlyStatements: List<MonthlyStatementEntity> = emptyList()
}

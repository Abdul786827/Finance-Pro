package com.example.ui.navigation

enum class AppScreen(val title: String, val iconName: String) {
    DASHBOARD("Dashboard", "dashboard"),
    DAILY_STATEMENT("Daily Statement", "table_chart"),
    ADD_TRANSACTION("New Transaction", "add_circle"),
    ACCOUNTS("Accounts & Transfers", "account_balance"),
    PETROL_EXTRA("Petrol & Extra", "local_gas_station"),
    STATEMENTS("Monthly & Annual", "calendar_month"),
    SALARY_LOANS("Salary & Loans", "payments"),
    UDHAAR("Udhaar (Lend & Borrow)", "handshake"),
    REPORTS("Reports & Export", "picture_as_pdf"),
    SETTINGS("Profile & Settings", "settings")
}

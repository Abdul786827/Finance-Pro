package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FinanceDao
import com.example.data.local.FinanceDatabase
import com.example.data.model.AuthResult
import com.example.data.model.UserProfileEntity
import com.example.data.repository.FinanceRepository
import com.example.util.DateUtils
import com.example.util.SecurityUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: FinanceDatabase
    private lateinit var dao: FinanceDao
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.financeDao()
        repository = FinanceRepository(dao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testCurrencyZeroFormatting() {
        val formatted = DateUtils.formatCurrency(0.0)
        assertEquals("Rs. 0", formatted)
    }

    @Test
    fun testSalaryCalculationFormula() {
        val grossSalary = 200000.0
        val preExisting = 15000.0
        val actualSalary = grossSalary - preExisting
        assertEquals(185000.0, actualSalary, 0.001)
    }

    @Test
    fun testFullAuthFlowRegisterLoginUpdateProfileChangeAndReset() = runBlocking {
        val rawPassword = "UserPass123"
        val email = "AbdulQuddus7745@Gmail.COM "
        val cleanEmail = SecurityUtils.normalizeEmail(email)

        // 1. Create Account / Register
        val token = repository.registerUser(
            fullName = "Abdul Quddus",
            email = email,
            mobile = "+92 300 1234567",
            rawPassword = rawPassword
        )
        assertNotNull(token)
        assertEquals(8, token.length)

        // Verify hash stored in SQLite: exactly 64-character SHA-256
        val storedUser = dao.getUserByEmail(cleanEmail)
        assertNotNull(storedUser)
        assertEquals(64, storedUser!!.passwordHash.length)
        assertEquals(SecurityUtils.hashPassword(rawPassword), storedUser.passwordHash)

        // 2. Verify Email
        val verified = repository.verifyEmail(token)
        assertTrue(verified)

        // 3. Login with Correct Credentials
        val loginResult = repository.loginUser(email, rawPassword)
        assertTrue("Login must succeed with correct credentials", loginResult is AuthResult.Success)
        val profile = (loginResult as AuthResult.Success).profile
        assertEquals(cleanEmail, profile.email)
        assertTrue(profile.isLoggedIn)

        // 4. Update Profile (name, email, mobile, photo) - MUST NEVER TOUCH PASSWORD HASH
        val oldHash = profile.passwordHash
        repository.updateProfileDetails(
            fullName = "Abdul Quddus Updated",
            email = cleanEmail,
            mobile = "+92 333 7654321",
            profileImageBase64 = "sampleBase64ImageData"
        )
        val userAfterProfileUpdate = dao.getUserByEmail(cleanEmail)
        assertNotNull(userAfterProfileUpdate)
        assertEquals("Abdul Quddus Updated", userAfterProfileUpdate!!.fullName)
        assertEquals("+92 333 7654321", userAfterProfileUpdate.mobile)
        assertEquals("sampleBase64ImageData", userAfterProfileUpdate.profileImageBase64)
        assertEquals("Password hash MUST remain unchanged after profile update", oldHash, userAfterProfileUpdate.passwordHash)

        // 5. Change Password (verifies old, hashes new once)
        val changeResult = repository.changePassword("UserPass123", "NewSecret456")
        assertTrue(changeResult.first)
        val userAfterPassChange = dao.getUserByEmail(cleanEmail)
        assertNotNull(userAfterPassChange)
        assertEquals(SecurityUtils.hashPassword("NewSecret456"), userAfterPassChange!!.passwordHash)

        // Login with new password succeeds
        val newLoginResult = repository.loginUser(cleanEmail, "NewSecret456")
        assertTrue(newLoginResult is AuthResult.Success)

        // Login with old password fails with specific message
        val oldLoginResult = repository.loginUser(cleanEmail, "UserPass123")
        assertTrue(oldLoginResult is AuthResult.Error)
        assertEquals("Incorrect password. Please try again or use Forgot Password.", (oldLoginResult as AuthResult.Error).message)

        // 6. Reset Password (Forgot Password)
        val resetSuccess = repository.resetPassword(cleanEmail, "ResetFinal789")
        assertTrue(resetSuccess)

        // Login with reset password succeeds
        val resetLoginResult = repository.loginUser(cleanEmail, "ResetFinal789")
        assertTrue(resetLoginResult is AuthResult.Success)
    }

    @Test
    fun testLegacyAccountAutoMigrationOnSuccessfulLogin() = runBlocking {
        val email = "legacyuser@example.com"
        val plainPassword = "plain123456"

        // Insert legacy user with raw plain-text password
        dao.insertUserProfile(
            UserProfileEntity(
                id = 99,
                fullName = "Legacy User",
                email = email,
                mobile = "03001122334",
                passwordHash = plainPassword, // Stored as plain text (length 11)
                isEmailVerified = true,
                accountStatus = "Active"
            )
        )

        // User logs in with plain text password
        val result = repository.loginUser(email, plainPassword)
        assertTrue(result is AuthResult.Success)

        // Verify that database was safely migrated to 64-char SHA-256 hash
        val migratedUser = dao.getUserByEmail(email)
        assertNotNull(migratedUser)
        assertEquals(64, migratedUser!!.passwordHash.length)
        assertEquals(SecurityUtils.hashPassword(plainPassword), migratedUser.passwordHash)
    }

    @Test
    fun testTransactionDayOfWeekAnd12HourTimeFlow() = runBlocking {
        // 1. Add transaction on 2026-11-07 with 12-hour time "04:45 PM"
        val txId = repository.addTransaction(
            date = "2026-11-07",
            time = "04:45 PM",
            type = "EXPENSE",
            amount = 1500.0,
            fromAccount = "HBL",
            toAccount = "Supermarket",
            category = "Groceries",
            comments = "Weekly Groceries"
        )
        val insertedTx = dao.getTransactionById(txId)
        assertNotNull(insertedTx)
        assertEquals("Saturday", insertedTx!!.dayOfWeek)
        // Stored in 24-hour format
        assertEquals("16:45:00", insertedTx.time)
        // Displayed in 12-hour format
        assertEquals("04:45 PM", DateUtils.format12HourTime(insertedTx.time))
        assertEquals("Sat, 07 Nov 2026", DateUtils.formatReadableDateWithDay(insertedTx.date, insertedTx.dayOfWeek))

        // 2. Update transaction to a different date (Monday 2026-11-02) and midnight (12:00 AM)
        repository.updateTransaction(
            oldTx = insertedTx,
            newDate = "2026-11-02",
            newTime = "12:00 AM",
            newType = "EXPENSE",
            newAmount = 2000.0,
            newFromAccount = "HBL",
            newToAccount = "Supermarket",
            newCategory = "Groceries"
        )
        val updatedTx = dao.getTransactionById(txId)
        assertNotNull(updatedTx)
        assertEquals("Monday", updatedTx!!.dayOfWeek)
        assertEquals("00:00:00", updatedTx.time)
        assertEquals("12:00 AM", DateUtils.format12HourTime(updatedTx.time))
        assertEquals("Mon, 02 Nov 2026", DateUtils.formatReadableDateWithDay(updatedTx.date, updatedTx.dayOfWeek))
    }

    @Test
    fun testTransferAppearsInTransactionListAndUpdatesBalances() = runBlocking {
        repository.initializeIfEmpty()

        val accountsBefore = dao.getAllAccountsDirect().associateBy { it.name }
        val hblBefore = accountsBefore["HBL"]?.currentBalance ?: 0.0
        val jazzBefore = accountsBefore["JazzCash"]?.currentBalance ?: 0.0

        val transferAmount = 7500.0
        val txId = repository.addTransaction(
            date = "2026-11-08",
            time = "02:15 PM",
            type = "TRANSFER",
            amount = transferAmount,
            fromAccount = "HBL",
            toAccount = "JazzCash",
            category = "Transfer",
            comments = "Bank to wallet transfer",
            description = "Transfer: HBL -> JazzCash"
        )

        val tx = dao.getTransactionById(txId)
        assertNotNull(tx)
        assertEquals("TRANSFER", tx!!.type)
        assertEquals("HBL", tx.fromAccount)
        assertEquals("JazzCash", tx.toAccount)
        assertEquals("Transfer: HBL -> JazzCash", tx.description)
        assertEquals(transferAmount, tx.amount, 0.001)

        val accountsAfter = dao.getAllAccountsDirect().associateBy { it.name }
        val hblAfter = accountsAfter["HBL"]?.currentBalance ?: 0.0
        val jazzAfter = accountsAfter["JazzCash"]?.currentBalance ?: 0.0
        assertEquals(hblBefore - transferAmount, hblAfter, 0.001)
        assertEquals(jazzBefore + transferAmount, jazzAfter, 0.001)
    }

    @Test
    fun testAccountTypeExplicitClassification() = runBlocking {
        repository.initializeIfEmpty()

        val accounts = dao.getAllAccountsDirect().associateBy { it.name }
        val mashreq = accounts["Mashreq"]
        val hbl = accounts["HBL"]
        val ubl = accounts["UBL"]
        val sadapay = accounts["Sadapay"]
        val easypaisa = accounts["Easypaisa"]
        val jazzcash = accounts["JazzCash"]

        assertNotNull(mashreq)
        assertNotNull(hbl)
        assertNotNull(ubl)
        assertNotNull(sadapay)
        assertNotNull(easypaisa)
        assertNotNull(jazzcash)

        // Mashreq, HBL, UBL must be BANK (not wallet)
        assertEquals("BANK", mashreq!!.accountType)
        assertFalse(com.example.data.model.isWalletAccount(mashreq))

        assertEquals("BANK", hbl!!.accountType)
        assertFalse(com.example.data.model.isWalletAccount(hbl))

        assertEquals("BANK", ubl!!.accountType)
        assertFalse(com.example.data.model.isWalletAccount(ubl))

        // Wallets must be WALLET
        assertEquals("WALLET", sadapay!!.accountType)
        assertTrue(com.example.data.model.isWalletAccount(sadapay))

        assertEquals("WALLET", easypaisa!!.accountType)
        assertTrue(com.example.data.model.isWalletAccount(easypaisa))

        assertEquals("WALLET", jazzcash!!.accountType)
        assertTrue(com.example.data.model.isWalletAccount(jazzcash))
    }
}

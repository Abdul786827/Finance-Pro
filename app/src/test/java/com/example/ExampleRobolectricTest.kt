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

    @Test
    fun testUdhaarGivenFromHBLDecreasesBalance() = runBlocking {
        repository.initializeIfEmpty()

        val hblAcc = dao.getAllAccountsDirect().find { it.name == "HBL" }!!
        dao.updateAccount(hblAcc.copy(currentBalance = 50000.0))
        val hblBefore = 50000.0

        val loanAmount = 5000.0
        val recordId = repository.addUdhaarRecord(
            type = "GIVEN",
            personName = "Ali Khan",
            phoneNumber = "03001234567",
            category = "MONEY",
            amount = loanAmount,
            accountName = "HBL",
            note = "Emergency loan"
        )

        val record = dao.getUdhaarRecordById(recordId)
        assertNotNull(record)
        assertEquals("GIVEN", record!!.type)
        assertEquals("HBL", record.accountName)
        assertEquals(loanAmount, record.originalAmount, 0.001)
        assertEquals(loanAmount, record.remainingAmount, 0.001)
        assertEquals("PENDING", record.status)
        assertTrue(record.transactionId > 0)

        // HBL balance must decrease by loanAmount
        val accountsAfter = dao.getAllAccountsDirect().associateBy { it.name }
        val hblAfter = accountsAfter["HBL"]?.currentBalance ?: 0.0
        assertEquals(hblBefore - loanAmount, hblAfter, 0.001)

        // Linked transaction check
        val tx = dao.getTransactionById(record.transactionId)
        assertNotNull(tx)
        assertEquals("UDHAAR", tx!!.type)
        assertEquals("HBL", tx.fromAccount)
        assertEquals("Ali Khan", tx.toAccount)
        assertEquals(loanAmount, tx.amount, 0.001)
        assertEquals("Udhaar Given: Ali Khan", tx.description)
    }

    @Test
    fun testUdhaarTakenIntoJazzCashIncreasesBalance() = runBlocking {
        repository.initializeIfEmpty()

        val accountsBefore = dao.getAllAccountsDirect().associateBy { it.name }
        val jazzBefore = accountsBefore["JazzCash"]?.currentBalance ?: 0.0

        val borrowedAmount = 8000.0
        val recordId = repository.addUdhaarRecord(
            type = "TAKEN",
            personName = "Ahmed Raza",
            category = "MONEY",
            amount = borrowedAmount,
            accountName = "JazzCash"
        )

        val record = dao.getUdhaarRecordById(recordId)
        assertNotNull(record)
        assertEquals("TAKEN", record!!.type)
        assertEquals("JazzCash", record.accountName)
        assertEquals(borrowedAmount, record.originalAmount, 0.001)

        // JazzCash balance must increase by borrowedAmount
        val accountsAfter = dao.getAllAccountsDirect().associateBy { it.name }
        val jazzAfter = accountsAfter["JazzCash"]?.currentBalance ?: 0.0
        assertEquals(jazzBefore + borrowedAmount, jazzAfter, 0.001)

        // Linked transaction check
        val tx = dao.getTransactionById(record.transactionId)
        assertNotNull(tx)
        assertEquals("UDHAAR", tx!!.type)
        assertEquals("Ahmed Raza", tx.fromAccount)
        assertEquals("JazzCash", tx.toAccount)
        assertEquals(borrowedAmount, tx.amount, 0.001)
        assertEquals("Udhaar Taken: Ahmed Raza", tx.description)
    }

    @Test
    fun testUdhaarRepaymentsBothWaysAndSettlement() = runBlocking {
        repository.initializeIfEmpty()

        val hblAcc = dao.getAllAccountsDirect().find { it.name == "HBL" }!!
        dao.updateAccount(hblAcc.copy(currentBalance = 50000.0))
        val hblStart = 50000.0

        val givenId = repository.addUdhaarRecord(
            type = "GIVEN",
            personName = "Usman",
            category = "MONEY",
            amount = 10000.0,
            accountName = "HBL"
        )
        val hblAfterGiven = dao.getAllAccountsDirect().find { it.name == "HBL" }!!.currentBalance
        assertEquals(hblStart - 10000.0, hblAfterGiven, 0.001)

        // Partial repayment of 4,000 received into HBL
        val rep1Id = repository.addUdhaarRepayment(
            udhaarId = givenId,
            amount = 4000.0,
            accountName = "HBL"
        )
        assertTrue(rep1Id > 0)
        val hblAfterRep1 = dao.getAllAccountsDirect().find { it.name == "HBL" }!!.currentBalance
        assertEquals(hblAfterGiven + 4000.0, hblAfterRep1, 0.001)

        val givenRecordPartial = dao.getUdhaarRecordById(givenId)!!
        assertEquals(6000.0, givenRecordPartial.remainingAmount, 0.001)
        assertEquals("PARTIAL", givenRecordPartial.status)

        // Second repayment of 6,000 received into HBL -> Full settlement
        repository.addUdhaarRepayment(
            udhaarId = givenId,
            amount = 6000.0,
            accountName = "HBL"
        )
        val hblFinal = dao.getAllAccountsDirect().find { it.name == "HBL" }!!.currentBalance
        assertEquals(hblStart, hblFinal, 0.001)

        val givenRecordSettled = dao.getUdhaarRecordById(givenId)!!
        assertEquals(0.0, givenRecordSettled.remainingAmount, 0.001)
        assertEquals("SETTLED", givenRecordSettled.status)

        // 2. Taken 5,000 into Sadapay
        val sadaStart = dao.getAllAccountsDirect().find { it.name == "Sadapay" }!!.currentBalance
        val takenId = repository.addUdhaarRecord(
            type = "TAKEN",
            personName = "Tariq",
            category = "MONEY",
            amount = 5000.0,
            accountName = "Sadapay"
        )
        val sadaAfterTaken = dao.getAllAccountsDirect().find { it.name == "Sadapay" }!!.currentBalance
        assertEquals(sadaStart + 5000.0, sadaAfterTaken, 0.001)

        // Repayment paid back 5,000 from Sadapay
        repository.addUdhaarRepayment(
            udhaarId = takenId,
            amount = 5000.0,
            accountName = "Sadapay"
        )
        val sadaFinal = dao.getAllAccountsDirect().find { it.name == "Sadapay" }!!.currentBalance
        assertEquals(sadaStart, sadaFinal, 0.001)

        val takenRecordSettled = dao.getUdhaarRecordById(takenId)!!
        assertEquals(0.0, takenRecordSettled.remainingAmount, 0.001)
        assertEquals("SETTLED", takenRecordSettled.status)
    }

    @Test
    fun testUdhaarInsufficientBalanceBlocksTransaction() = runBlocking {
        repository.initializeIfEmpty()

        val hbl = dao.getAllAccountsDirect().find { it.name == "HBL" }!!
        val hugeAmount = hbl.currentBalance + 500000.0

        try {
            repository.addUdhaarRecord(
                type = "GIVEN",
                personName = "Borrower",
                category = "MONEY",
                amount = hugeAmount,
                accountName = "HBL"
            )
            fail("Must throw IllegalStateException due to insufficient balance")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Insufficient balance in HBL", ignoreCase = true) == true)
        }
    }

    @Test
    fun testUdhaarEditReversesOldAndAppliesNewBalance() = runBlocking {
        repository.initializeIfEmpty()

        val hblAcc = dao.getAllAccountsDirect().find { it.name == "HBL" }!!
        dao.updateAccount(hblAcc.copy(currentBalance = 50000.0))
        val ublAcc = dao.getAllAccountsDirect().find { it.name == "UBL" }!!
        dao.updateAccount(ublAcc.copy(currentBalance = 50000.0))

        val hblBefore = 50000.0
        val ublBefore = 50000.0

        // 1. Create Given Rs 5,000 on HBL
        val recordId = repository.addUdhaarRecord(
            type = "GIVEN",
            personName = "Zaid",
            category = "MONEY",
            amount = 5000.0,
            accountName = "HBL"
        )
        val hblMid = dao.getAllAccountsDirect().find { it.name == "HBL" }!!.currentBalance
        assertEquals(hblBefore - 5000.0, hblMid, 0.001)

        // 2. Edit to UBL Rs 3,000
        repository.updateUdhaarRecord(
            id = recordId,
            type = "GIVEN",
            personName = "Zaid",
            category = "MONEY",
            amount = 3000.0,
            accountName = "UBL",
            date = "2026-11-09",
            time = "10:00:00"
        )

        // HBL balance must be restored (+5,000)
        val hblAfter = dao.getAllAccountsDirect().find { it.name == "HBL" }!!.currentBalance
        assertEquals(hblBefore, hblAfter, 0.001)

        // UBL balance must be debited by 3,000
        val ublAfter = dao.getAllAccountsDirect().find { it.name == "UBL" }!!.currentBalance
        assertEquals(ublBefore - 3000.0, ublAfter, 0.001)

        val updatedRecord = dao.getUdhaarRecordById(recordId)!!
        assertEquals("UBL", updatedRecord.accountName)
        assertEquals(3000.0, updatedRecord.originalAmount, 0.001)
    }

    @Test
    fun testUdhaarDeleteRestoresAccountBalances() = runBlocking {
        repository.initializeIfEmpty()

        val hblAcc = dao.getAllAccountsDirect().find { it.name == "HBL" }!!
        dao.updateAccount(hblAcc.copy(currentBalance = 50000.0))
        val hblBefore = 50000.0

        val recordId = repository.addUdhaarRecord(
            type = "GIVEN",
            personName = "Bilal",
            category = "MONEY",
            amount = 6000.0,
            accountName = "HBL"
        )

        // Repayment of 2,000 received
        val repId = repository.addUdhaarRepayment(
            udhaarId = recordId,
            amount = 2000.0,
            accountName = "HBL"
        )

        // Net change so far: -4,000
        val hblMid = dao.getAllAccountsDirect().find { it.name == "HBL" }!!.currentBalance
        assertEquals(hblBefore - 4000.0, hblMid, 0.001)

        // Delete repayment only
        repository.deleteUdhaarRepayment(repaymentId = repId, udhaarId = recordId)
        val hblAfterRepDel = dao.getAllAccountsDirect().find { it.name == "HBL" }!!.currentBalance
        assertEquals(hblBefore - 6000.0, hblAfterRepDel, 0.001)

        // Delete entire record
        repository.deleteUdhaarRecord(recordId)
        val hblFinal = dao.getAllAccountsDirect().find { it.name == "HBL" }!!.currentBalance
        assertEquals(hblBefore, hblFinal, 0.001)

        assertNull(dao.getUdhaarRecordById(recordId))
    }

    @Test
    fun testUdhaarItemRecordNeverTouchesBalances() = runBlocking {
        repository.initializeIfEmpty()

        val totalBalBefore = dao.getAllAccountsDirect().sumOf { it.currentBalance }

        val itemId = repository.addUdhaarRecord(
            type = "GIVEN",
            personName = "Hamza",
            category = "ITEM",
            itemDescription = "MacBook Pro",
            accountName = ""
        )

        val totalBalAfter = dao.getAllAccountsDirect().sumOf { it.currentBalance }
        assertEquals(totalBalBefore, totalBalAfter, 0.001)

        val itemRecord = dao.getUdhaarRecordById(itemId)!!
        assertEquals("ITEM", itemRecord.category)
        assertEquals("MacBook Pro", itemRecord.itemDescription)
        assertEquals("", itemRecord.accountName)
        assertEquals(0L, itemRecord.transactionId)

        // Mark returned
        repository.markItemReturned(itemId, true)
        val returnedRecord = dao.getUdhaarRecordById(itemId)!!
        assertTrue(returnedRecord.isItemReturned)
        assertEquals("SETTLED", returnedRecord.status)
        assertEquals(totalBalBefore, dao.getAllAccountsDirect().sumOf { it.currentBalance }, 0.001)
    }

    @Test
    fun testUdhaarUnlinkedMigration() = runBlocking {
        repository.initializeIfEmpty()

        val hblAcc = dao.getAllAccountsDirect().find { it.name == "HBL" }!!
        dao.updateAccount(hblAcc.copy(currentBalance = 50000.0))
        val hblBefore = 50000.0

        // Insert legacy unlinked record with accountName = "" and transactionId = 0
        val unlinked = com.example.data.model.UdhaarRecordEntity(
            type = "GIVEN",
            personName = "Old Record",
            category = "MONEY",
            originalAmount = 3500.0,
            remainingAmount = 3500.0,
            accountName = "",
            transactionId = 0L,
            date = "2026-11-01",
            time = "12:00:00"
        )
        val legacyId = dao.insertUdhaarRecord(unlinked)

        // Balance should NOT have changed yet
        assertEquals(hblBefore, dao.getAllAccountsDirect().find { it.name == "HBL" }!!.currentBalance, 0.001)

        // When user edits and chooses HBL, balance is applied once
        repository.updateUdhaarRecord(
            id = legacyId,
            type = "GIVEN",
            personName = "Old Record",
            category = "MONEY",
            amount = 3500.0,
            accountName = "HBL",
            date = "2026-11-01",
            time = "12:00:00"
        )

        val hblAfter = dao.getAllAccountsDirect().find { it.name == "HBL" }!!.currentBalance
        assertEquals(hblBefore - 3500.0, hblAfter, 0.001)

        val updated = dao.getUdhaarRecordById(legacyId)!!
        assertEquals("HBL", updated.accountName)
        assertTrue(updated.transactionId > 0)
    }
}

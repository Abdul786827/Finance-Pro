package com.example

import com.example.data.model.TransactionEntity
import com.example.data.model.isWalletAccount
import com.example.util.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPasswordHashingAndVerification() {
    val rawPassword = "  mySecret123  "
    val cleanPassword = rawPassword.trim()
    val hash = SecurityUtils.hashPassword(rawPassword)

    assertNotNull(hash)
    assertEquals(64, hash.length) // SHA-256 is 64 hex characters

    // Correct password matches
    assertTrue(SecurityUtils.verifyPassword("mySecret123", hash))
    assertTrue(SecurityUtils.verifyPassword("  mySecret123  ", hash))

    // Wrong password fails
    assertFalse(SecurityUtils.verifyPassword("wrongPassword", hash))
    assertFalse(SecurityUtils.verifyPassword("", hash))
  }

  @Test
  fun testLegacyPasswordVerification() {
    // 1. Plain text legacy password (e.g. "123456")
    assertTrue(SecurityUtils.verifyPassword("123456", "123456"))
    assertTrue(SecurityUtils.verifyPassword(" 123456 ", "123456"))

    // 2. MD5 fallback
    val md5 = SecurityUtils.md5Hex("123456")
    assertTrue(SecurityUtils.verifyPassword("123456", md5))

    // 3. Double-hash SHA-256 fallback
    val singleHash = SecurityUtils.hashPassword("123456")
    val doubleHash = SecurityUtils.hashPassword(singleHash)
    assertTrue(SecurityUtils.verifyPassword("123456", doubleHash))
  }

  @Test
  fun testEmailNormalization() {
    val email1 = "  AbdulQuddus7745@Gmail.COM "
    val normalized = SecurityUtils.normalizeEmail(email1)
    assertEquals("abdulquddus7745@gmail.com", normalized)

    val email2 = "USER@EXAMPLE.COM"
    assertEquals("user@example.com", SecurityUtils.normalizeEmail(email2))
  }

  @Test
  fun testWalletVsBankClassification() {
    // Wallets: Sadapay, Easypaisa, JazzCash, Wallet
    assertTrue(isWalletAccount("Sadapay"))
    assertTrue(isWalletAccount("sadapay"))
    assertTrue(isWalletAccount("Easypaisa"))
    assertTrue(isWalletAccount("JazzCash"))
    assertTrue(isWalletAccount("Wallet"))

    // Banks: HBL, UBL, Mashreq
    assertFalse(isWalletAccount("HBL"))
    assertFalse(isWalletAccount("UBL"))
    assertFalse(isWalletAccount("Mashreq"))
  }

  @Test
  fun testTransactionEntityFormDataMapping() {
    val tx = TransactionEntity(
      id = 1L,
      serialNumber = 101,
      date = "2026-11-06",
      time = "14:30:00",
      dayOfWeek = "Friday",
      type = "EXPENSE",
      amount = 4500.0,
      fromAccount = "HBL",
      toAccount = "Metro Cash & Carry",
      category = "Groceries",
      description = "Monthly family groceries",
      comments = "Invoice #8841",
      hblBalance = 45500.0,
      totalAccountBalance = 150000.0,
      petrolLiters = 0.0,
      petrolRate = 0.0,
      extraItem = ""
    )

    assertEquals(101, tx.serialNumber)
    assertEquals("2026-11-06", tx.date)
    assertEquals("Groceries", tx.category)
    assertEquals(4500.0, tx.amount, 0.001)
    assertEquals("HBL", tx.fromAccount)
    assertEquals("Metro Cash & Carry", tx.toAccount)
    assertEquals("Monthly family groceries", tx.description)
  }

  @Test
  fun testTransferTransactionCreation() {
    val transferTx = TransactionEntity(
      serialNumber = 102,
      date = "2026-11-06",
      time = "15:00:00",
      dayOfWeek = "Friday",
      type = "TRANSFER",
      amount = 12000.0,
      fromAccount = "HBL",
      toAccount = "Sadapay",
      category = "Account Transfer",
      description = "Transfer HBL -> Sadapay",
      comments = "Wallet load",
      hblBalance = 33500.0,
      sadapayBalance = 22000.0,
      totalAccountBalance = 150000.0
    )

    assertEquals("TRANSFER", transferTx.type)
    assertEquals("HBL", transferTx.fromAccount)
    assertEquals("Sadapay", transferTx.toAccount)
    assertNotEquals(transferTx.fromAccount, transferTx.toAccount)
    assertEquals(12000.0, transferTx.amount, 0.001)
  }

  @Test
  fun testDayOfWeekAutoCalculation() {
    assertEquals("Sunday", com.example.util.DateUtils.getDayOfWeek("2026-11-01"))
    assertEquals("Monday", com.example.util.DateUtils.getDayOfWeek("2026-11-02"))
    assertEquals("Tuesday", com.example.util.DateUtils.getDayOfWeek("2026-11-03"))
    assertEquals("Wednesday", com.example.util.DateUtils.getDayOfWeek("2026-11-04"))
    assertEquals("Thursday", com.example.util.DateUtils.getDayOfWeek("2026-11-05"))
    assertEquals("Friday", com.example.util.DateUtils.getDayOfWeek("2026-11-06"))
    assertEquals("Saturday", com.example.util.DateUtils.getDayOfWeek("2026-11-07"))
  }

  @Test
  fun test12HourTimeFormat() {
    // Midnight must be 12:00 AM
    assertEquals("12:00 AM", com.example.util.DateUtils.format12HourTime("00:00:00"))
    assertEquals("12:00 AM", com.example.util.DateUtils.format12HourTime("00:00"))

    // Noon must be 12:00 PM
    assertEquals("12:00 PM", com.example.util.DateUtils.format12HourTime("12:00:00"))
    assertEquals("12:00 PM", com.example.util.DateUtils.format12HourTime("12:00"))

    // Afternoon / Evening times
    assertEquals("04:45 PM", com.example.util.DateUtils.format12HourTime("16:45:00"))
    assertEquals("04:45 PM", com.example.util.DateUtils.format12HourTime("16:45"))
    assertEquals("11:59 PM", com.example.util.DateUtils.format12HourTime("23:59:59"))

    // Morning times
    assertEquals("08:30 AM", com.example.util.DateUtils.format12HourTime("08:30:00"))
    assertEquals("08:30 AM", com.example.util.DateUtils.format12HourTime("08:30"))

    // Preserves / normalizes already 12-hour values
    assertEquals("04:45 PM", com.example.util.DateUtils.format12HourTime("04:45 PM"))
    assertEquals("12:00 AM", com.example.util.DateUtils.format12HourTime("12:00 AM"))
  }

  @Test
  fun testTo24HourTimeConversion() {
    // Midnight 12:00 AM -> 00:00:00
    assertEquals("00:00:00", com.example.util.DateUtils.to24HourTime("12:00 AM"))

    // Noon 12:00 PM -> 12:00:00
    assertEquals("12:00:00", com.example.util.DateUtils.to24HourTime("12:00 PM"))

    // 04:45 PM -> 16:45:00
    assertEquals("16:45:00", com.example.util.DateUtils.to24HourTime("04:45 PM"))

    // 08:30 AM -> 08:30:00
    assertEquals("08:30:00", com.example.util.DateUtils.to24HourTime("08:30 AM"))

    // Already 24-hour preserves
    assertEquals("16:45:00", com.example.util.DateUtils.to24HourTime("16:45:00"))
  }

  @Test
  fun testFormatReadableDateWithDay() {
    val formatted = com.example.util.DateUtils.formatReadableDateWithDay("2026-11-07")
    assertEquals("Sat, 07 Nov 2026", formatted)

    val mondayFormatted = com.example.util.DateUtils.formatReadableDateWithDay("2026-11-02")
    assertEquals("Mon, 02 Nov 2026", mondayFormatted)
  }

  @Test
  fun testUdhaarRepaymentRemainingAndStatusCalculation() {
    val original = 50000.0
    var repaid = 15000.0
    var remaining = (original - repaid).coerceAtLeast(0.0)
    var status = when {
      remaining <= 0.0001 -> "SETTLED"
      repaid > 0.0 -> "PARTIAL"
      else -> "PENDING"
    }
    assertEquals(35000.0, remaining, 0.001)
    assertEquals("PARTIAL", status)

    // Full repayment
    repaid += 35000.0
    remaining = (original - repaid).coerceAtLeast(0.0)
    status = when {
      remaining <= 0.0001 -> "SETTLED"
      repaid > 0.0 -> "PARTIAL"
      else -> "PENDING"
    }
    assertEquals(0.0, remaining, 0.001)
    assertEquals("SETTLED", status)
  }

  @Test
  fun testUdhaarItemTracking() {
    val itemRecord = com.example.data.model.UdhaarRecordEntity(
      type = "GIVEN",
      personName = "Ali Raza",
      category = "ITEM",
      itemDescription = "MacBook Pro M2",
      date = "2026-11-10",
      time = "10:00 AM",
      dueDate = "2026-11-20",
      status = "PENDING",
      isItemReturned = false
    )
    assertEquals("ITEM", itemRecord.category)
    assertEquals("MacBook Pro M2", itemRecord.itemDescription)
    assertFalse(itemRecord.isItemReturned)
    assertEquals(0.0, itemRecord.originalAmount, 0.001)
    assertEquals(0.0, itemRecord.remainingAmount, 0.001)

    // Mark as returned
    val returnedRecord = itemRecord.copy(isItemReturned = true, status = "SETTLED")
    assertTrue(returnedRecord.isItemReturned)
    assertEquals("SETTLED", returnedRecord.status)
  }
}

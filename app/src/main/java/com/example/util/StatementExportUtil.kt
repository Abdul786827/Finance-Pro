package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.MonthlyStatementEntity
import com.example.data.model.TransactionEntity
import java.io.File
import java.io.FileOutputStream

object StatementExportUtil {

    fun generateDailyStatementCsv(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        // 17 Columns from Statement for app.xlsx
        sb.append("Sr#,Day,Date,Time,Expense,Received,Balance,From,To,Comments,HBL,UBL,Sadapay,Easypaisa,JazzCash,Mashreq,Wallet,Total Balance\n")

        for (tx in transactions) {
            val expense = if (tx.type in listOf("EXPENSE", "PETROL", "EXTRA_EXPENSE")) tx.amount else 0.0
            val received = if (tx.type == "INCOME") tx.amount else 0.0
            val cleanComments = tx.comments.replace(",", " ")
            val cleanFrom = tx.fromAccount.replace(",", " ")
            val cleanTo = tx.toAccount.replace(",", " ")

            val day = tx.dayOfWeek.ifBlank { DateUtils.getDayOfWeek(tx.date) }
            val time12 = DateUtils.format12HourTime(tx.time)

            sb.append("${tx.serialNumber},")
            sb.append("$day,")
            sb.append("${tx.date},")
            sb.append("$time12,")
            sb.append("$expense,")
            sb.append("$received,")
            sb.append("${tx.totalAccountBalance},")
            sb.append("$cleanFrom,")
            sb.append("$cleanTo,")
            sb.append("$cleanComments,")
            sb.append("${tx.hblBalance},")
            sb.append("${tx.ublBalance},")
            sb.append("${tx.sadapayBalance},")
            sb.append("${tx.easypaisaBalance},")
            sb.append("${tx.jazzCashBalance},")
            sb.append("${tx.mashreqBalance},")
            sb.append("${tx.walletBalance},")
            sb.append("${tx.totalAccountBalance}\n")
        }
        return sb.toString()
    }

    fun generateAnnualSummaryCsv(
        year: Int,
        statements: List<MonthlyStatementEntity>
    ): String {
        val sb = StringBuilder()
        // Sheet 2: Annual Summary Columns
        sb.append("Month,Received,Expenses,Petrol,Extra Expenses,Salary,Pre-existing,Actual Salary,Gross Salary,Loan,Closing Balance\n")

        for (st in statements) {
            sb.append("${st.monthName},")
            sb.append("${st.totalReceived},")
            sb.append("${st.totalExpenses},")
            sb.append("${st.totalPetrolExpenses},")
            sb.append("${st.totalExtraExpenses},")
            sb.append("${st.totalSalary},")
            sb.append("${st.preExistingAmount},")
            sb.append("${st.actualSalary},")
            sb.append("${st.grossSalary},")
            sb.append("${st.totalLoan},")
            sb.append("${st.monthlyClosingBalance}\n")
        }
        return sb.toString()
    }

    fun exportAndShareCsv(context: Context, filename: String, csvContent: String) {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val file = File(reportsDir, filename)
        FileOutputStream(file).use { it.write(csvContent.toByteArray()) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Financial Statement: $filename")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Statement CSV")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun exportAndShareDailyPdf(
        context: Context,
        title: String,
        transactions: List<TransactionEntity>
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(1000, 1400, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(0, 108, 80)
            textSize = 28f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(0, 108, 80)
            textSize = 12f
            isFakeBoldText = true
        }
        val cellPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        val incomePaint = Paint().apply {
            color = Color.rgb(16, 185, 129)
            textSize = 11f
            isFakeBoldText = true
        }
        val expensePaint = Paint().apply {
            color = Color.rgb(239, 68, 68)
            textSize = 11f
            isFakeBoldText = true
        }

        var y = 50f
        canvas.drawText("Finance Pro - Personal Statement Management", 40f, y, titlePaint)
        y += 26f
        canvas.drawText("Report: $title | Timezone: Asia/Karachi (PKT)", 40f, y, subtitlePaint)
        y += 24f

        // Table Header
        canvas.drawLine(40f, y, 960f, y, linePaint)
        y += 18f
        canvas.drawText("Sr#", 45f, y, headerPaint)
        canvas.drawText("Date & Time", 80f, y, headerPaint)
        canvas.drawText("Day", 240f, y, headerPaint)
        canvas.drawText("Type", 295f, y, headerPaint)
        canvas.drawText("From / To", 375f, y, headerPaint)
        canvas.drawText("Amount", 540f, y, headerPaint)
        canvas.drawText("Comments", 640f, y, headerPaint)
        canvas.drawText("Total Balance", 830f, y, headerPaint)
        y += 8f
        canvas.drawLine(40f, y, 960f, y, linePaint)
        y += 20f

        val transferPaint = Paint().apply {
            color = Color.rgb(2, 132, 199)
            textSize = 11f
            isFakeBoldText = true
        }

        var totalRecv = 0.0
        var totalExp = 0.0
        var totalTransfer = 0.0

        for (tx in transactions.take(45)) { // First 45 items fit cleanly on page 1
            if (tx.type == "INCOME") totalRecv += tx.amount
            if (tx.type in listOf("EXPENSE", "PETROL", "EXTRA_EXPENSE")) totalExp += tx.amount
            if (tx.type == "TRANSFER") totalTransfer += tx.amount

            val day = tx.dayOfWeek.ifBlank { DateUtils.getDayOfWeek(tx.date) }
            val time12 = DateUtils.format12HourTime(tx.time)
            val dateWithDay = DateUtils.formatReadableDateWithDay(tx.date, day)

            canvas.drawText("${tx.serialNumber}", 45f, y, cellPaint)
            canvas.drawText("$dateWithDay  $time12", 80f, y, cellPaint)
            canvas.drawText(day.take(3), 240f, y, cellPaint)
            canvas.drawText(tx.type, 295f, y, cellPaint)
            val fromTo = "${tx.fromAccount.take(10)} -> ${tx.toAccount.take(10)}"
            canvas.drawText(fromTo, 375f, y, cellPaint)

            val amtText = DateUtils.formatCurrency(tx.amount)
            if (tx.type == "INCOME") {
                canvas.drawText("+$amtText", 540f, y, incomePaint)
            } else if (tx.type == "TRANSFER") {
                canvas.drawText(amtText, 540f, y, transferPaint)
            } else {
                canvas.drawText("-$amtText", 540f, y, expensePaint)
            }

            val displayComments = if (tx.type == "TRANSFER" && tx.comments.isBlank()) {
                "Transfer: ${tx.fromAccount} -> ${tx.toAccount}"
            } else {
                tx.comments
            }
            canvas.drawText(displayComments.take(24), 640f, y, cellPaint)
            canvas.drawText(DateUtils.formatCurrency(tx.totalAccountBalance), 830f, y, cellPaint)

            y += 8f
            canvas.drawLine(40f, y, 960f, y, linePaint)
            y += 18f
        }

        // Summary footer
        y += 15f
        val summaryBoxPaint = Paint().apply {
            color = Color.rgb(240, 245, 242)
        }
        canvas.drawRect(40f, y, 960f, y + 60f, summaryBoxPaint)
        y += 25f
        canvas.drawText("Total Received: ${DateUtils.formatCurrency(totalRecv)}", 50f, y, incomePaint)
        canvas.drawText("Total Expenses: ${DateUtils.formatCurrency(totalExp)}", 280f, y, expensePaint)
        canvas.drawText("Transfers: ${DateUtils.formatCurrency(totalTransfer)}", 510f, y, transferPaint)
        val net = totalRecv - totalExp
        canvas.drawText("Net Flow: ${DateUtils.formatCurrency(net)}", 740f, y, headerPaint)

        pdfDocument.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val file = File(reportsDir, "Daily_Statement_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Financial Statement PDF")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share PDF Statement")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

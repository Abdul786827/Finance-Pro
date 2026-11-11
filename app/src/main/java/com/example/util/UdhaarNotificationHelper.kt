package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.data.model.UdhaarRecordEntity
import java.text.SimpleDateFormat
import java.util.*

object UdhaarNotificationHelper {

    private const val CHANNEL_ID = "udhaar_reminders_channel"
    private const val CHANNEL_NAME = "Udhaar & Due Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for pending udhaar loans and item returns"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Checks all pending udhaar records and triggers reminders for records due today or tomorrow.
     * Only triggers if notification permission is granted on Android 13+.
     */
    fun checkAndNotifyDueRecords(context: Context, records: List<UdhaarRecordEntity>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        createNotificationChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())

        val calendarTomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowStr = sdf.format(calendarTomorrow.time)

        for (record in records) {
            if (record.status == "SETTLED" || record.isItemReturned) continue
            if (record.dueDate.isBlank()) continue

            val isDueToday = record.dueDate == todayStr
            val isDueTomorrow = record.dueDate == tomorrowStr

            if (isDueToday || isDueTomorrow) {
                val prefix = if (isDueToday) "Due Today" else "Due Tomorrow"
                val typeDesc = if (record.type == "GIVEN") "To Receive from" else "To Pay to"
                val amountOrItem = if (record.category == "MONEY") {
                    "Rs. ${record.remainingAmount}"
                } else {
                    record.itemDescription.ifBlank { "Item" }
                }

                val title = "Udhaar Reminder: $prefix"
                val body = "$typeDesc ${record.personName} - $amountOrItem (${record.dueDate})"

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

                manager.notify((record.id + if (isDueToday) 10000 else 20000).toInt(), notification)
            }
        }
    }
}

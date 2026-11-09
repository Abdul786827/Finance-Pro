package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val TIMEZONE_KARACHI = TimeZone.getTimeZone("Asia/Karachi")

    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TIMEZONE_KARACHI
        }
        return sdf.format(Date())
    }

    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
            timeZone = TIMEZONE_KARACHI
        }
        return sdf.format(Date())
    }

    fun getCurrentTime12Hour(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US).apply {
            timeZone = TIMEZONE_KARACHI
        }
        return sdf.format(Date())
    }

    /**
     * Converts any 24-hour ("HH:mm:ss", "HH:mm") or 12-hour string into 12-hour format with AM/PM (e.g. "04:45 PM").
     * Midnight (00:00) maps to "12:00 AM", and noon (12:00) maps to "12:00 PM".
     */
    fun format12HourTime(timeStr: String?): String {
        if (timeStr.isNullOrBlank()) return ""
        val trimmed = timeStr.trim()
        val upper = trimmed.uppercase(Locale.US)

        // Already contains AM or PM
        if (upper.contains("AM") || upper.contains("PM")) {
            val formats12 = listOf(
                SimpleDateFormat("hh:mm:ss a", Locale.US),
                SimpleDateFormat("hh:mm a", Locale.US),
                SimpleDateFormat("h:mm:ss a", Locale.US),
                SimpleDateFormat("h:mm a", Locale.US)
            )
            for (fmt in formats12) {
                try {
                    val parsed = fmt.parse(upper)
                    if (parsed != null) {
                        return SimpleDateFormat("hh:mm a", Locale.US).format(parsed)
                    }
                } catch (_: Exception) {}
            }
            return trimmed
        }

        // 24-hour formats
        val formats24 = listOf(
            SimpleDateFormat("HH:mm:ss", Locale.US),
            SimpleDateFormat("HH:mm", Locale.US),
            SimpleDateFormat("H:mm", Locale.US)
        )
        for (fmt in formats24) {
            try {
                val parsed = fmt.parse(trimmed)
                if (parsed != null) {
                    return SimpleDateFormat("hh:mm a", Locale.US).format(parsed)
                }
            } catch (_: Exception) {}
        }

        return trimmed
    }

    /**
     * Converts a 12-hour ("04:45 PM") or 24-hour time string into a canonical 24-hour "HH:mm:ss" string for DB persistence.
     * Midnight ("12:00 AM") becomes "00:00:00", Noon ("12:00 PM") becomes "12:00:00".
     */
    fun to24HourTime(timeStr: String?): String {
        if (timeStr.isNullOrBlank()) return getCurrentTime()
        val trimmed = timeStr.trim()
        val upper = trimmed.uppercase(Locale.US)

        if (upper.contains("AM") || upper.contains("PM")) {
            val formats12 = listOf(
                SimpleDateFormat("hh:mm:ss a", Locale.US),
                SimpleDateFormat("hh:mm a", Locale.US),
                SimpleDateFormat("h:mm:ss a", Locale.US),
                SimpleDateFormat("h:mm a", Locale.US)
            )
            for (fmt in formats12) {
                try {
                    val parsed = fmt.parse(upper)
                    if (parsed != null) {
                        return SimpleDateFormat("HH:mm:ss", Locale.US).format(parsed)
                    }
                } catch (_: Exception) {}
            }
        }

        val formats24 = listOf(
            SimpleDateFormat("HH:mm:ss", Locale.US),
            SimpleDateFormat("HH:mm", Locale.US),
            SimpleDateFormat("H:mm", Locale.US)
        )
        for (fmt in formats24) {
            try {
                val parsed = fmt.parse(trimmed)
                if (parsed != null) {
                    return SimpleDateFormat("HH:mm:ss", Locale.US).format(parsed)
                }
            } catch (_: Exception) {}
        }

        return trimmed
    }

    fun formatDisplayDateTime(dateStr: String, timeStr: String): String {
        val datePart = formatReadableDateWithDay(dateStr)
        val timePart = format12HourTime(timeStr)
        return if (timePart.isNotBlank()) "$datePart | $timePart" else datePart
    }

    fun formatReadableDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TIMEZONE_KARACHI
            }
            val parsed = inputFormat.parse(dateStr) ?: return dateStr
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.US).apply {
                timeZone = TIMEZONE_KARACHI
            }
            outputFormat.format(parsed)
        } catch (_: Exception) {
            dateStr
        }
    }

    /**
     * Formats date with day name, e.g. "Sat, 07 Nov 2026".
     * If dayOfWeek is provided, can use its short form; otherwise parses dateStr.
     */
    fun formatReadableDateWithDay(dateStr: String, dayOfWeek: String? = null): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TIMEZONE_KARACHI
            }
            val parsed = inputFormat.parse(dateStr)
            if (parsed != null) {
                val outputFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).apply {
                    timeZone = TIMEZONE_KARACHI
                }
                outputFormat.format(parsed)
            } else {
                val day = if (!dayOfWeek.isNullOrBlank()) dayOfWeek.take(3) else getDayOfWeek(dateStr).take(3)
                "$day, $dateStr"
            }
        } catch (_: Exception) {
            val day = if (!dayOfWeek.isNullOrBlank()) dayOfWeek.take(3) else "Mon"
            "$day, $dateStr"
        }
    }

    fun getDayOfWeek(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TIMEZONE_KARACHI
            }
            val parsed = sdf.parse(dateStr) ?: return "Monday"
            val dayFormat = SimpleDateFormat("EEEE", Locale.US).apply {
                timeZone = TIMEZONE_KARACHI
            }
            dayFormat.format(parsed)
        } catch (_: Exception) {
            "Monday"
        }
    }

    fun formatCurrency(amount: Double, symbol: String = "Rs. "): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        return "$symbol${formatter.format(amount)}"
    }
}

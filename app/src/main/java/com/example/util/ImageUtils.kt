package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

/**
 * Utility for compressing, storing, and decoding user profile images.
 * Keeps Base64 database payloads small and fast (< 100 KB) by downscaling and compressing to JPEG.
 */
object ImageUtils {

    fun uriToBase64(context: Context, uri: Uri, maxDimension: Int = 400): String? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds only to determine sample size
            var input: InputStream? = contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var inSampleSize = 1
            val maxSide = max(origWidth, origHeight)
            while (maxSide / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            // 2. Decode sampled bitmap
            input = contentResolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val sampledBitmap = BitmapFactory.decodeStream(input, null, decodeOptions)
            input?.close()

            if (sampledBitmap == null) return null

            // 3. Scale precisely if still larger than maxDimension
            val finalBitmap = if (sampledBitmap.width > maxDimension || sampledBitmap.height > maxDimension) {
                val ratio = maxDimension.toFloat() / max(sampledBitmap.width, sampledBitmap.height)
                val targetW = (sampledBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (sampledBitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true)
            } else {
                sampledBitmap
            }

            // 4. Compress to JPEG and Base64 encode
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun base64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}

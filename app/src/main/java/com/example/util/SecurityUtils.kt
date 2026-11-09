package com.example.util

import java.security.MessageDigest
import java.util.Locale

/**
 * Security and credential utilities for Finance Pro.
 * Provides consistent password hashing, multi-format legacy verification,
 * and email normalization.
 */
object SecurityUtils {

    /**
     * Produces a standard lowercase SHA-256 hex digest of the trimmed password.
     */
    fun hashPassword(password: String): String {
        val trimmed = password.trim()
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(trimmed.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Produces a standard lowercase MD5 hex digest of the trimmed string.
     */
    fun md5Hex(input: String): String {
        val trimmed = input.trim()
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(trimmed.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Produces a standard lowercase SHA-1 hex digest of the trimmed string.
     */
    fun sha1Hex(input: String): String {
        val trimmed = input.trim()
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(trimmed.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies raw user input against stored password or hash.
     * Backwards-compatible across:
     * 1. Exact plain-text match (legacy migration support, e.g. "123456").
     * 2. Standard SHA-256 hex digest.
     * 3. Double SHA-256 hex digest (if previously double-hashed).
     * 4. MD5 hex digest fallback.
     * 5. SHA-1 hex digest fallback.
     */
    fun verifyPassword(rawInput: String, storedPasswordOrHash: String): Boolean {
        val trimmedInput = rawInput.trim()
        val trimmedStored = storedPasswordOrHash.trim()
        if (trimmedInput.isEmpty() || trimmedStored.isEmpty()) {
            return false
        }
        // 1. Direct plain match
        if (trimmedInput == trimmedStored) {
            return true
        }
        // 2. Standard SHA-256 hex digest
        val sha256 = hashPassword(trimmedInput)
        if (sha256.equals(trimmedStored, ignoreCase = true)) {
            return true
        }
        // 3. Double SHA-256 hex digest
        val doubleSha256 = hashPassword(sha256)
        if (doubleSha256.equals(trimmedStored, ignoreCase = true)) {
            return true
        }
        // 4. MD5 fallback
        val md5 = md5Hex(trimmedInput)
        if (md5.equals(trimmedStored, ignoreCase = true)) {
            return true
        }
        // 5. SHA-1 fallback
        val sha1 = sha1Hex(trimmedInput)
        if (sha1.equals(trimmedStored, ignoreCase = true)) {
            return true
        }
        return false
    }

    /**
     * Normalizes an email address by trimming spaces and converting to lowercase.
     */
    fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.US)
    }
}

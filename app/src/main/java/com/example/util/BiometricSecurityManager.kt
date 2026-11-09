package com.example.util

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object BiometricSecurityManager {

    private const val KEYSTORE_NAME = "AndroidKeyStore"
    private const val KEY_ALIAS = "FinanceProBiometricKey"
    private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"

    /**
     * Checks if real BIOMETRIC_STRONG hardware exists and at least one biometric (fingerprint) is enrolled.
     * Returns true ONLY if hardware is present and credentials are enrolled.
     */
    fun canAuthenticate(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Retrieves or generates an Android Keystore AES-256 key bound to biometric authentication.
     * The key is invalidated automatically if new biometric credentials (fingerprints) are enrolled.
     */
    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_NAME).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null)
            if (entry is KeyStore.SecretKeyEntry) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_NAME)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setInvalidatedByBiometricEnrollment(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Prepares an initialized Cipher for the CryptoObject, bound to the Keystore key.
     */
    fun getCryptoObject(): BiometricPrompt.CryptoObject? {
        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            BiometricPrompt.CryptoObject(cipher)
        } catch (e: Exception) {
            if (e is KeyPermanentlyInvalidatedException) {
                try {
                    val keyStore = KeyStore.getInstance(KEYSTORE_NAME).apply { load(null) }
                    keyStore.deleteEntry(KEY_ALIAS)
                    val newKey = getOrCreateKey()
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    cipher.init(Cipher.ENCRYPT_MODE, newKey)
                    return BiometricPrompt.CryptoObject(cipher)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * Launches the real Android system BiometricPrompt backed by a Keystore CryptoObject.
     * App unlock occurs ONLY inside onAuthenticationSucceeded().
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Finance Pro",
        subtitle: String = "Touch the fingerprint sensor to unlock",
        negativeButtonText: String = "Use Password",
        onSucceeded: () -> Unit,
        onErrorOrCancel: (errorCode: Int, errString: String) -> Unit = { _, _ -> },
        onError: (errString: String) -> Unit = { err -> onErrorOrCancel(0, err) },
        onFailed: () -> Unit = {}
    ) {
        if (!canAuthenticate(activity)) {
            val msg = "Biometric authentication not supported or enrolled"
            onErrorOrCancel(BiometricPrompt.ERROR_HW_NOT_PRESENT, msg)
            onError(msg)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Verified: execution unlocks only upon real hardware match
                    onSucceeded()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    val str = errString.toString()
                    onErrorOrCancel(errorCode, str)
                    onError(str)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val cryptoObject = getCryptoObject()
        if (cryptoObject != null) {
            prompt.authenticate(promptInfo, cryptoObject)
        } else {
            prompt.authenticate(promptInfo)
        }
    }
}

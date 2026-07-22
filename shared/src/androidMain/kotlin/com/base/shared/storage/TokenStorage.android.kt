package com.base.shared.storage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.base.shared.models.TokenWithExpiry
import com.base.shared.utils.Logger
import kotlinx.datetime.Instant
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val PREFS_NAME = "token_prefs"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_EXPIRES_AT = "expires_at"
private const val KEY_ALIAS = "BaseAppTokenKey"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_IV_LENGTH = 12
private const val GCM_TAG_LENGTH = 16

actual class TokenStorage actual constructor(
    platformContext: Any?
) {
    private val context = platformContext as Context
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    init {
        generateKeyIfNeeded()
    }

    private fun generateKeyIfNeeded() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        val encryptedWithIv = iv + encryptedData
        return Base64.encodeToString(encryptedWithIv, Base64.DEFAULT)
    }

    private fun decrypt(encryptedText: String): String? {
        return try {
            val encryptedWithIv = Base64.decode(encryptedText, Base64.DEFAULT)
            val iv = encryptedWithIv.sliceArray(0..GCM_IV_LENGTH - 1)
            val encryptedData = encryptedWithIv.sliceArray(GCM_IV_LENGTH until encryptedWithIv.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val decryptedData = cipher.doFinal(encryptedData)
            String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    actual suspend fun save(token: TokenWithExpiry) {
        Logger.authInfo("STORAGE_SAVE_START", "Starting token save to Android encrypted storage")
        Logger.logToken("ACCESS", token.accessToken, "STORAGE_SAVE_INPUT")
        
        try {
            val encryptedToken = encrypt(token.accessToken)
            Logger.authDebug("STORAGE_SAVE_ENCRYPTED", "Token encrypted successfully")
            
            sharedPreferences.edit()
                .putString(KEY_ACCESS_TOKEN, encryptedToken)
                .putLong(KEY_EXPIRES_AT, token.expiresAt.epochSeconds)
                .apply()
            
            Logger.authInfo("STORAGE_SAVE_SUCCESS", "Token saved to Android storage with expiry: ${token.expiresAt}")
        } catch (e: Exception) {
            Logger.authError("STORAGE_SAVE_FAILED", "Failed to save token to Android storage", e)
            throw e
        }
    }

    actual suspend fun load(): TokenWithExpiry? {
        Logger.authInfo("STORAGE_LOAD_START", "Loading token from Android encrypted storage")
        
        try {
            val encryptedToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
            val expiresAt = sharedPreferences.getLong(KEY_EXPIRES_AT, -1)
            
            if (encryptedToken != null && expiresAt != -1L) {
                Logger.authDebug("STORAGE_LOAD_FOUND", "Encrypted token found, attempting decryption")
                val decryptedToken = decrypt(encryptedToken)
                
                if (decryptedToken != null) {
                    val token = TokenWithExpiry(
                        accessToken = decryptedToken,
                        expiresAt = Instant.fromEpochSeconds(expiresAt)
                    )
                    Logger.logToken("ACCESS", token.accessToken, "STORAGE_LOAD_SUCCESS")
                    Logger.authInfo("STORAGE_LOAD_COMPLETE", "Token loaded successfully, expires: ${token.expiresAt}")
                    return token
                } else {
                    Logger.authError("STORAGE_LOAD_DECRYPT_FAILED", "Failed to decrypt stored token")
                    return null
                }
            } else {
                Logger.authInfo("STORAGE_LOAD_NOT_FOUND", "No token found in Android storage")
                return null
            }
        } catch (e: Exception) {
            Logger.authError("STORAGE_LOAD_FAILED", "Failed to load token from Android storage", e)
            return null
        }
    }

    actual suspend fun clear() {
        Logger.authInfo("STORAGE_CLEAR_START", "Clearing token from Android encrypted storage")
        
        try {
            sharedPreferences.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_EXPIRES_AT)
                .apply()
            
            Logger.authInfo("STORAGE_CLEAR_SUCCESS", "Token cleared from Android storage")
        } catch (e: Exception) {
            Logger.authError("STORAGE_CLEAR_FAILED", "Failed to clear token from Android storage", e)
            throw e
        }
    }
}

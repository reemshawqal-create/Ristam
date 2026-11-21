package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.CipherVault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext_returnsRistam() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ristam", appName)
    }

    @Test
    fun backupEncryptionAndDecryption_roundTripAndWrongPassphraseFails() {
        val sampleJson = """{"app":"Ristam","posts":[{"id":1,"caption":"تجربة"}]}"""
        val encrypted = CipherVault.encryptBackupPayload(sampleJson, "StrongPass#2026")
        assertTrue(encrypted.startsWith("RISTAM_ENC_BAK_V1|"))

        val decryptedOk = CipherVault.decryptBackupPayload(encrypted, "StrongPass#2026")
        assertTrue(decryptedOk.isSuccess)
        assertEquals(sampleJson, decryptedOk.getOrNull())

        val decryptedWrong = CipherVault.decryptBackupPayload(encrypted, "WrongPass")
        assertTrue(decryptedWrong.isFailure)
    }

    @Test
    fun masterPasswordHashing_verifiesCorrectAndRejectsIncorrect() {
        val salt = CipherVault.generateSaltBase64()
        val verifier = CipherVault.hashPasswordVerifier("mySecretVault", salt)

        assertTrue(CipherVault.verifyPassword("mySecretVault", salt, verifier))
        assertFalse(CipherVault.verifyPassword("wrongVault", salt, verifier))
    }
}

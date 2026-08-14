package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PatientSettings
import com.example.data.preferences.PatientSettingsManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PatientSettingsManagerTest {

    private lateinit var context: Context
    private lateinit var manager: PatientSettingsManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = PatientSettingsManager(context)
        // Clear preferences before each test
        context.getSharedPreferences("richfarmaci_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `test disclaimer initial state and update`() {
        assertFalse("Disclaimer should not be accepted by default", manager.isDisclaimerAccepted())
        
        manager.setDisclaimerAccepted(true)
        assertTrue("Disclaimer should be accepted after setting it to true", manager.isDisclaimerAccepted())
        
        manager.setDisclaimerAccepted(false)
        assertFalse("Disclaimer should be false after setting it to false", manager.isDisclaimerAccepted())
    }

    @Test
    fun `test donation count logic`() {
        assertEquals(0, manager.getDonationCount())
        
        manager.incrementDonationCount()
        assertEquals(1, manager.getDonationCount())
        
        manager.incrementDonationCount()
        assertEquals(2, manager.getDonationCount())
    }

    @Test
    fun `test patient settings persistence`() {
        val settings = PatientSettings(
            pazienteNome = "Mario Rossi",
            pazienteCf = "mrrossi80a01h501z",
            medicoNome = "Dr. Bianchi",
            medicoTelefono = "333 1234567",
            medicoEmail = "bianchi@example.com",
            secondoIndirizzo = "Via Roma 1",
            messaggioTesta = "Test Head",
            messaggioCoda = "Test Tail",
            tipoInvio = 1,
            notificheAttive = true,
            descrizioneNotifica = "Prendi farmaco"
        )
        
        manager.saveSettings(settings)
        
        val retrieved = manager.getSettings()
        assertEquals("Mario Rossi", retrieved.pazienteNome)
        assertEquals("MRROSSI80A01H501Z", retrieved.pazienteCf) // Should be uppercase
        assertEquals("3331234567", retrieved.medicoTelefono) // Should be cleaned
        assertEquals("bianchi@example.com", retrieved.medicoEmail)
        assertEquals(1, retrieved.tipoInvio)
        assertTrue(retrieved.notificheAttive)
    }

    @Test
    fun `test isConfigured logic`() {
        assertFalse(manager.isConfigured())
        
        val incompleteSettings = PatientSettings(
            pazienteNome = "Mario",
            pazienteCf = "CF",
            medicoNome = "", // Missing medico name
            medicoTelefono = "123"
        )
        manager.saveSettings(incompleteSettings)
        assertFalse(manager.isConfigured())
        
        val completeSettings = PatientSettings(
            pazienteNome = "Mario",
            pazienteCf = "CF",
            medicoNome = "Dr. Rossi",
            medicoTelefono = "123",
            tipoInvio = 0 // WhatsApp needs phone
        )
        manager.saveSettings(completeSettings)
        assertTrue(manager.isConfigured())
    }
}

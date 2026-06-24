package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.PatientSettings

class PatientSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("richfarmaci_prefs", Context.MODE_PRIVATE)

    fun getSettings(): PatientSettings {
        return PatientSettings(
            pazienteNome = prefs.getString("paziente_nome", "") ?: "",
            pazienteCf = prefs.getString("paziente_cf", "")?.uppercase() ?: "",
            medicoNome = prefs.getString("medico_nome", "") ?: "",
            medicoTelefono = prefs.getString("medico_telefono", "") ?: "",
            medicoEmail = prefs.getString("medico_email", "") ?: "",
            secondoIndirizzo = prefs.getString("secondo_indirizzo", "") ?: "",
            messaggioTesta = prefs.getString(
                "messaggio_testa",
                "Gentile Dottore, Le chiedo cortesemente la prescrizione dei seguenti medicinali intestati a me:"
            ) ?: "Gentile Dottore, Le chiedo cortesemente la prescrizione dei seguenti medicinali intestati a me:",
            messaggioCoda = prefs.getString(
                "messaggio_coda",
                "La ringrazio per la disponibilità. Cordiali saluti."
            ) ?: "La ringrazio per la disponibilità. Cordiali saluti.",
            tipoInvio = prefs.getInt("tipo_invio_int", 0) // Default to 0 (WhatsApp)
        )
    }

    fun saveSettings(settings: PatientSettings) {
        prefs.edit().apply {
            putString("paziente_nome", settings.pazienteNome.trim())
            // Always convert patient C.F. to total uppercase as per spec
            putString("paziente_cf", settings.pazienteCf.trim().uppercase())
            putString("medico_nome", settings.medicoNome.trim())
            // Clean phone numbers from spaces or weird chars
            val cleanPhone = settings.medicoTelefono.replace("\\s".toRegex(), "").replace("[^+0-9]".toRegex(), "")
            putString("medico_telefono", cleanPhone)
            putString("medico_email", settings.medicoEmail.trim())
            putString("secondo_indirizzo", settings.secondoIndirizzo.trim())
            putString("messaggio_testa", settings.messaggioTesta.trim())
            putString("messaggio_coda", settings.messaggioCoda.trim())
            putInt("tipo_invio_int", settings.tipoInvio)
            apply()
        }
    }

    fun isConfigured(): Boolean {
        val settings = getSettings()
        val hasContact = if (settings.tipoInvio == 2) {
            settings.medicoEmail.isNotBlank()
        } else {
            settings.medicoTelefono.isNotBlank()
        }
        
        return settings.pazienteNome.isNotBlank() && 
               settings.pazienteCf.isNotBlank() && 
               settings.medicoNome.isNotBlank() &&
               hasContact
    }
}

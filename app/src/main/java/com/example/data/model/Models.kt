package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val scatole: Int = 1,
    val note: String = "",
    val inPausa: Boolean = false
)

data class SentMedication(
    val nome: String,
    val scatole: Int,
    val note: String = ""
)

@Entity(tableName = "sent_requests")
data class SentRequest(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val data: String, // Readable Date string, e.g. "15 Giugno 2026 - 15:30"
    val pazienteNome: String,
    val medicoNome: String,
    val farmaciSerialized: String, // custom string serialization
    val testoCompleto: String
) {
    // Convenience helper to get the structured medication list
    fun getSentMedications(): List<SentMedication> {
        return deserializeMedicines(farmaciSerialized)
    }

    companion object {
        fun serializeMedicines(list: List<SentMedication>): String {
            return list.joinToString("|||") { 
                "${escape(it.nome)}###${it.scatole}###${escape(it.note)}" 
            }
        }

        fun deserializeMedicines(str: String): List<SentMedication> {
            if (str.isBlank()) return emptyList()
            return str.split("|||").mapNotNull { part ->
                val subparts = part.split("###")
                if (subparts.size >= 2) {
                    val nome = unescape(subparts[0])
                    val scatole = subparts[1].toIntOrNull() ?: 1
                    val note = if (subparts.size > 2) unescape(subparts[2]) else ""
                    SentMedication(nome, scatole, note)
                } else {
                    null
                }
            }
        }

        private fun escape(s: String): String = s.replace("|", "\\bar").replace("#", "\\hash")
        private fun unescape(s: String): String = s.replace("\\bar", "|").replace("\\hash", "#")
    }
}

// tipoInvio: 0 = WhatsApp, 1 = SMS, 2 = Email
data class PatientSettings(
    val pazienteNome: String = "",
    val pazienteCf: String = "",
    val medicoNome: String = "",
    val medicoTelefono: String = "",
    val medicoEmail: String = "",
    val secondoIndirizzo: String = "",
    val messaggioTesta: String = "Gentile Dottore, Le chiedo cortesemente la prescrizione dei seguenti medicinali intestati a me:",
    val messaggioCoda: String = "La ringrazio per la disponibilità. Cordiali saluti.",
    val tipoInvio: Int = 0
)

package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val pazienteNome: String = "",
    val pazienteCf: String = "",
    val medicoNome: String = "",
    val medicoTelefono: String = "",
    val medicoEmail: String = "",
    val secondoIndirizzo: String = "",
    val messaggioTesta: String = "Gentile Dottore, Le chiedo cortesemente la prescrizione dei seguenti medicinali intestati a me:",
    val messaggioCoda: String = "La ringrazio per la disponibilità. Cordiali saluti.",
    val tipoInvio: Int = 0,
    val notificheAttive: Boolean = false,
    val descrizioneNotifica: String = "È ora di prendere il farmaco"
)

@Entity(
    tableName = "medications",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("profileId")]
)
data class Medication(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val nome: String,
    val scatole: Int = 1,
    val note: String = "",
    val inPausa: Boolean = false,
    val notificaAttiva: Boolean = false,
    val orarioNotifica: String = "08:00",
    val frequenzaValore: Int = 1, // 0 significa nessuna ripetizione, default 1
    val frequenzaTipo: String = "GIORNI" // "ORE" o "GIORNI"
)

data class SentMedication(
    val nome: String,
    val scatole: Int,
    val note: String = ""
)

@Entity(
    tableName = "sent_requests",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("profileId")]
)
data class SentRequest(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val profileId: String,
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
    val tipoInvio: Int = 0,
    val notificheAttive: Boolean = false,
    val descrizioneNotifica: String = "È ora di prendere il farmaco"
)

package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.data.db.AppDatabase
import com.example.data.preferences.PatientSettingsManager
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all alarms on boot
            rescheduleAllAlarms(context)
            return
        }

        val medicationId = intent.getStringExtra("medication_id") ?: return
        val medicationName = intent.getStringExtra("medication_name") ?: "Farmaco"
        val description = intent.getStringExtra("description") ?: "È ora di prendere il farmaco"

        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val medications = db.medicationDao().getAllMedicationsSnapshot()
            val medication = medications.find { it.id == medicationId }
            
            if (medication != null) {
                val profile = db.profileDao().getProfileById(medication.profileId)
                // MOSTRA NOTIFICA SOLO SE IL PROFILO HA LE NOTIFICHE ATTIVE GLOBALMENTE
                if (profile?.notificheAttive == true) {
                    NotificationHelper.showNotification(context, medicationName, description)
                }

                // Reschedule the next occurrence ONLY if both medication and profile have notifications active
                if (medication.notificaAttiva && !medication.inPausa && profile?.notificheAttive == true) {
                    val nextDescription = profile.descrizioneNotifica
                    NotificationHelper.scheduleNotification(context, medication, nextDescription)
                }
            }
        }
    }


    private fun rescheduleNextAlarm(context: Context, medicationId: String) {
        // Funzione svuotata perché la logica è stata spostata in onReceive per efficienza di accesso al DB
    }

    private fun rescheduleAllAlarms(context: Context) {
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val medications = db.medicationDao().getAllMedicationsSnapshot()
            medications.filter { it.notificaAttiva && !it.inPausa }.forEach { med ->
                val profile = db.profileDao().getProfileById(med.profileId)
                // Reschedula solo se il profilo ha le notifiche attive
                if (profile?.notificheAttive == true) {
                    val description = profile.descrizioneNotifica
                    NotificationHelper.scheduleNotification(context, med, description)
                }
            }
        }
    }
}

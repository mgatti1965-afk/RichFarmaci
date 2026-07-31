package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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

        showNotification(context, medicationName, description)

        // Reschedule the next occurrence for this specific medication
        rescheduleNextAlarm(context, medicationId)
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medication_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Promemoria Farmaci",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun rescheduleNextAlarm(context: Context, medicationId: String) {
        val db = AppDatabase.getDatabase(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            val medications = db.medicationDao().getAllMedicationsSnapshot()
            val medication = medications.find { it.id == medicationId }
            
            if (medication != null && medication.notificaAttiva && !medication.inPausa) {
                // Recuperiamo il profilo specifico di questo farmaco per avere la descrizione notifica corretta
                val profile = db.profileDao().getProfileById(medication.profileId)
                val description = profile?.descrizioneNotifica ?: "È ora di prendere il farmaco"
                NotificationHelper.scheduleNotification(context, medication, description)
            }
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val medications = db.medicationDao().getAllMedicationsSnapshot()
            medications.filter { it.notificaAttiva && !it.inPausa }.forEach { med ->
                val profile = db.profileDao().getProfileById(med.profileId)
                val description = profile?.descrizioneNotifica ?: "È ora di prendere il farmaco"
                NotificationHelper.scheduleNotification(context, med, description)
            }
        }
    }
}

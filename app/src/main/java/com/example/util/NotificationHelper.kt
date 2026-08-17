package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.Medication
import com.example.receiver.NotificationReceiver
import java.util.*

object NotificationHelper {
    private const val TAG = "NotificationHelper"

    fun scheduleNotification(context: Context, medication: Medication, description: String) {
        if (!medication.notificaAttiva || medication.inPausa) {
            cancelNotification(context, medication)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("medication_id", medication.id)
            putExtra("medication_name", medication.nome)
            putExtra("description", description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medication.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val currentTime = System.currentTimeMillis()
        val orari = medication.orarioNotifica.split(",").filter { it.isNotBlank() }
        if (orari.isEmpty()) return

        var nextScheduleTime = Long.MAX_VALUE

        for (orario in orari) {
            val calendar = Calendar.getInstance().apply {
                val parts = orario.split(":")
                if (parts.size == 2) {
                    set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 8)
                    set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }

            var scheduleTime = calendar.timeInMillis

            // Se l'orario base è passato, cerchiamo la prossima ripetizione oggi o domani/futuro
            if (scheduleTime <= currentTime) {
                if (medication.frequenzaValore > 0) {
                    if (medication.frequenzaTipo == "ORE") {
                        // Calcoliamo quante ripetizioni servono per arrivare al futuro (in ore)
                        while (scheduleTime <= currentTime) {
                            calendar.add(Calendar.HOUR_OF_DAY, medication.frequenzaValore)
                            scheduleTime = calendar.timeInMillis
                        }
                    } else {
                        // Calcoliamo quante ripetizioni servono per arrivare al futuro (in giorni)
                        while (scheduleTime <= currentTime) {
                            calendar.add(Calendar.DAY_OF_YEAR, medication.frequenzaValore)
                            scheduleTime = calendar.timeInMillis
                        }
                    }
                } else {
                    // Nessuna ripetizione: se l'orario è passato, non facciamo nulla.
                    // L'orario rimarrà nel passato e non verrà considerato per la schedulazione.
                }
            }
            
            if (scheduleTime > currentTime && scheduleTime < nextScheduleTime) {
                nextScheduleTime = scheduleTime
            }
        }

        if (nextScheduleTime == Long.MAX_VALUE) return
        val scheduleTime = nextScheduleTime

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduleTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        scheduleTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduleTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled notification for ${medication.nome} at ${Date(scheduleTime)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm", e)
        }
    }

    fun cancelNotification(context: Context, medication: Medication) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medication.id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled notification for ${medication.nome}")
        }
    }

    fun updateAllNotifications(context: Context, medications: List<Medication>, notificationsEnabled: Boolean, description: String) {
        if (!notificationsEnabled) {
            medications.forEach { cancelNotification(context, it) }
        } else {
            medications.forEach { scheduleNotification(context, it, description) }
        }
    }
}

package com.example.util

import android.content.Context
import android.os.Environment
import com.example.data.db.AppDatabase
import com.example.data.model.BackupData
import com.example.data.model.SentRequest
import com.example.data.preferences.PatientSettingsManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BackupManager {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        
    private val adapter = moshi.adapter(BackupData::class.java)

    suspend fun exportToDownload(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val settingsManager = PatientSettingsManager(context)

            // 1. Leggiamo tutti i dati dal Database
            val profiles = db.profileDao().getAllProfilesSnapshot()
            val medications = db.medicationDao().getAllMedicationsSnapshot()
            
            val requestsList = mutableListOf<SentRequest>()
            val cursor = db.openHelper.readableDatabase.query("SELECT id, profileId, data, pazienteNome, medicoNome, farmaciSerialized, testoCompleto FROM sent_requests")
            while (cursor.moveToNext()) {
                requestsList.add(
                    SentRequest(
                        id = cursor.getString(0),
                        profileId = cursor.getString(1),
                        data = cursor.getString(2),
                        pazienteNome = cursor.getString(3),
                        medicoNome = cursor.getString(4),
                        farmaciSerialized = cursor.getString(5),
                        testoCompleto = cursor.getString(6)
                    )
                )
            }
            cursor.close()

            // 2. Costruiamo l'oggetto di backup
            val backupData = BackupData(
                version = 1,
                profiles = profiles,
                medications = medications,
                sentRequests = requestsList,
                donationCount = settingsManager.getDonationCount(),
                onboardingShown = settingsManager.isOnboardingShown(),
                disclaimerAccepted = settingsManager.isDisclaimerAccepted()
            )

            // 3. Convertiamo in JSON
            val jsonString = adapter.indent("  ").toJson(backupData)

            // 4. Salviamo nella cartella Download pubblica
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            val backupFile = File(downloadDir, "RichFarmaci_Backup.json")
            backupFile.writeText(jsonString)

            Result.success(backupFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromDownload(context: Context): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupFile = File(downloadDir, "RichFarmaci_Backup.json")
            
            if (!backupFile.exists()) {
                return@withContext Result.failure(Exception("File 'RichFarmaci_Backup.json' non trovato nella cartella Download."))
            }

            val jsonString = backupFile.readText()
            val backupData = adapter.fromJson(jsonString) ?: return@withContext Result.failure(Exception("File di backup corrotto o non valido."))

            val db = AppDatabase.getDatabase(context)

            // Puliamo il database attuale per evitare conflitti
            db.clearAllTables()

            // Ripristiniamo i profili
            for (profile in backupData.profiles) {
                db.profileDao().insertProfile(profile)
            }

            // Ripristiniamo i farmaci
            db.medicationDao().insertAll(backupData.medications)

            // Ripristiniamo la cronologia richieste
            for (req in backupData.sentRequests) {
                db.sentRequestDao().insert(req)
            }

            // Ripristiniamo le preferenze globali
            val prefs = context.getSharedPreferences("richfarmaci_prefs", Context.MODE_PRIVATE)
            val success = prefs.edit().apply {
                putInt("donation_count", backupData.donationCount)
                putBoolean("onboarding_shown", backupData.onboardingShown)
                putBoolean("disclaimer_accepted", backupData.disclaimerAccepted)
                
                // Impostiamo l'ultimo profilo attivo come attivo se disponibile
                if (backupData.profiles.isNotEmpty()) {
                    putString("active_profile_id", backupData.profiles.first().id)
                }
            }.commit() // Usiamo commit per essere sicuri che i dati siano scritti prima di ricaricare il ViewModel

            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.annotation.VisibleForTesting
import com.example.data.model.Medication
import com.example.data.model.SentRequest
import com.example.data.model.Profile

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Medication::class, SentRequest::class, Profile::class], version = 5, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun sentRequestDao(): SentRequestDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @VisibleForTesting
        fun setTestInstance(database: AppDatabase) {
            INSTANCE = database
        }

        @JvmField
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migrazione da versione 3 a 4: rinomina ripetiOgniOre in frequenzaValore e aggiunge frequenzaTipo
                db.execSQL("ALTER TABLE medications RENAME TO medications_old")
                db.execSQL("CREATE TABLE medications (id TEXT NOT NULL, nome TEXT NOT NULL, scatole INTEGER NOT NULL, note TEXT NOT NULL, inPausa INTEGER NOT NULL, notificaAttiva INTEGER NOT NULL, orarioNotifica TEXT NOT NULL, frequenzaValore INTEGER NOT NULL, frequenzaTipo TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("INSERT INTO medications (id, nome, scatole, note, inPausa, notificaAttiva, orarioNotifica, frequenzaValore, frequenzaTipo) SELECT id, nome, scatole, note, inPausa, notificaAttiva, orarioNotifica, ripetiOgniOre, 'ORE' FROM medications_old")
                db.execSQL("DROP TABLE medications_old")
            }
        }

        @JvmField
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Creazione tabella profiles
                db.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`id` TEXT NOT NULL, `pazienteNome` TEXT NOT NULL, `pazienteCf` TEXT NOT NULL, `medicoNome` TEXT NOT NULL, `medicoTelefono` TEXT NOT NULL, `medicoEmail` TEXT NOT NULL, `secondoIndirizzo` TEXT NOT NULL, `messaggioTesta` TEXT NOT NULL, `messaggioCoda` TEXT NOT NULL, `tipoInvio` INTEGER NOT NULL, `notificheAttive` INTEGER NOT NULL, `descrizioneNotifica` TEXT NOT NULL, PRIMARY KEY(`id`))")
                
                // 2. Tentativo di recupero dati da sent_requests per popolare il profilo
                var oldPaziente = ""
                var oldMedico = ""
                try {
                    val cursor = db.query("SELECT * FROM sent_requests LIMIT 1")
                    if (cursor.moveToFirst()) {
                        val pIdx = cursor.getColumnIndex("pazienteNome")
                        val mIdx = cursor.getColumnIndex("medicoNome")
                        if (pIdx >= 0) oldPaziente = (cursor.getString(pIdx) ?: "").replace("'", "''")
                        if (mIdx >= 0) oldMedico = (cursor.getString(mIdx) ?: "").replace("'", "''")
                    }
                    cursor.close()
                } catch (e: Exception) {
                    // Tabella vuota o campi non trovati
                }

                // 3. Inserimento profilo predefinito con dati recuperati
                val defaultProfileId = "default_profile"
                db.execSQL("INSERT OR IGNORE INTO profiles (id, pazienteNome, pazienteCf, medicoNome, medicoTelefono, medicoEmail, secondoIndirizzo, messaggioTesta, messaggioCoda, tipoInvio, notificheAttive, descrizioneNotifica) VALUES ('$defaultProfileId', '$oldPaziente', '', '$oldMedico', '', '', '', 'Gentile Dottore, Le chiedo cortesemente la prescrizione dei seguenti medicinali intestati a me:', 'La ringrazio per la disponibilità. Cordiali saluti.', 0, 0, 'È ora di prendere il farmaco')")

                // 4. Aggiornamento tabella medications per aggiungere profileId e FK
                db.execSQL("ALTER TABLE medications RENAME TO medications_old")
                db.execSQL("CREATE TABLE medications (id TEXT NOT NULL, profileId TEXT NOT NULL, nome TEXT NOT NULL, scatole INTEGER NOT NULL, note TEXT NOT NULL, inPausa INTEGER NOT NULL, notificaAttiva INTEGER NOT NULL, orarioNotifica TEXT NOT NULL, frequenzaValore INTEGER NOT NULL, frequenzaTipo TEXT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(profileId) REFERENCES profiles(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_medications_profileId ON medications (profileId)")
                db.execSQL("INSERT INTO medications (id, profileId, nome, scatole, note, inPausa, notificaAttiva, orarioNotifica, frequenzaValore, frequenzaTipo) SELECT id, '$defaultProfileId', nome, scatole, note, inPausa, notificaAttiva, orarioNotifica, frequenzaValore, frequenzaTipo FROM medications_old")
                db.execSQL("DROP TABLE medications_old")

                // 5. Aggiornamento tabella sent_requests per aggiungere profileId e FK
                db.execSQL("ALTER TABLE sent_requests RENAME TO sent_requests_old")
                db.execSQL("CREATE TABLE sent_requests (id TEXT NOT NULL, profileId TEXT NOT NULL, data TEXT NOT NULL, pazienteNome TEXT NOT NULL, medicoNome TEXT NOT NULL, farmaciSerialized TEXT NOT NULL, testoCompleto TEXT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(profileId) REFERENCES profiles(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sent_requests_profileId ON sent_requests (profileId)")
                db.execSQL("INSERT INTO sent_requests (id, profileId, data, pazienteNome, medicoNome, farmaciSerialized, testoCompleto) SELECT id, '$defaultProfileId', data, pazienteNome, medicoNome, farmaciSerialized, testoCompleto FROM sent_requests_old")
                db.execSQL("DROP TABLE sent_requests_old")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "richfarmaci_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

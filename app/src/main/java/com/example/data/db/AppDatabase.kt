package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Medication
import com.example.data.model.SentRequest

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Medication::class, SentRequest::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun sentRequestDao(): SentRequestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create new table with the new schema
                db.execSQL("""
                    CREATE TABLE medications_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        nome TEXT NOT NULL,
                        scatole INTEGER NOT NULL DEFAULT 1,
                        note TEXT NOT NULL DEFAULT '',
                        inPausa INTEGER NOT NULL DEFAULT 0,
                        notificaAttiva INTEGER NOT NULL DEFAULT 0,
                        orarioNotifica TEXT NOT NULL DEFAULT '08:00',
                        frequenzaValore INTEGER NOT NULL DEFAULT 0,
                        frequenzaTipo TEXT NOT NULL DEFAULT 'ORE'
                    )
                """.trimIndent())

                // 2. Copy data from old table to new table
                db.execSQL("""
                    INSERT INTO medications_new (id, nome, scatole, note, inPausa, notificaAttiva, orarioNotifica, frequenzaValore, frequenzaTipo)
                    SELECT id, nome, scatole, note, inPausa, notificaAttiva, orarioNotifica, ripetiOgniOre, 'ORE' FROM medications
                """.trimIndent())

                // 3. Drop old table
                db.execSQL("DROP TABLE medications")

                // 4. Rename new table to old table name
                db.execSQL("ALTER TABLE medications_new RENAME TO medications")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "richfarmaci_database"
                )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

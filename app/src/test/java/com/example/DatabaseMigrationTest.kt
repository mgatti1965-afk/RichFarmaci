package com.example

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        // 1. Create database in version 3
        var db = helper.createDatabase(TEST_DB, 3)

        // 2. Insert data using SQL (since the Medication model is now at version 4)
        db.execSQL(
            """
            INSERT INTO medications (id, nome, scatole, note, inPausa, notificaAttiva, orarioNotifica, ripetiOgniOre)
            VALUES ('test-id', 'Aspirina', 2, 'Dopo i pasti', 0, 1, '08:00', 12)
            """.trimIndent()
        )
        db.close()

        // 3. Migrate to version 4
        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, AppDatabase.MIGRATION_3_4)

        // 4. Verify data integrity in the new schema
        val cursor = db.query("SELECT * FROM medications WHERE id = 'test-id'")
        cursor.moveToFirst()

        val nomeIdx = cursor.getColumnIndex("nome")
        val freqValIdx = cursor.getColumnIndex("frequenzaValore")
        val freqTipoIdx = cursor.getColumnIndex("frequenzaTipo")

        assertEquals("Aspirina", cursor.getString(nomeIdx))
        assertEquals(12, cursor.getInt(freqValIdx))
        assertEquals("ORE", cursor.getString(freqTipoIdx))

        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        // 1. Create database in version 4
        var db = helper.createDatabase(TEST_DB, 4)

        // 2. Insert data into version 4
        db.execSQL(
            """
            INSERT INTO medications (id, nome, scatole, note, inPausa, notificaAttiva, orarioNotifica, frequenzaValore, frequenzaTipo)
            VALUES ('med-1', 'Paracetamolo', 1, '', 0, 0, '10:00', 1, 'GIORNI')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO sent_requests (id, data, pazienteNome, medicoNome, farmaciSerialized, testoCompleto)
            VALUES ('req-1', '2023-10-27', 'Mario Rossi', 'Dr. Bianchi', '[]', 'Testo')
            """.trimIndent()
        )
        db.close()

        // 3. Migrate to version 5
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, AppDatabase.MIGRATION_4_5)

        // 4. Verify Profile recovery
        val profileCursor = db.query("SELECT * FROM profiles WHERE id = 'default_profile'")
        profileCursor.moveToFirst()
        assertEquals("Mario Rossi", profileCursor.getString(profileCursor.getColumnIndex("pazienteNome")))
        assertEquals("Dr. Bianchi", profileCursor.getString(profileCursor.getColumnIndex("medicoNome")))
        profileCursor.close()

        // 5. Verify Medication links
        val medCursor = db.query("SELECT profileId FROM medications WHERE id = 'med-1'")
        medCursor.moveToFirst()
        assertEquals("default_profile", medCursor.getString(0))
        medCursor.close()

        // 6. Verify Request links
        val reqCursor = db.query("SELECT profileId FROM sent_requests WHERE id = 'req-1'")
        reqCursor.moveToFirst()
        assertEquals("default_profile", reqCursor.getString(0))
        reqCursor.close()

        db.close()
    }
}

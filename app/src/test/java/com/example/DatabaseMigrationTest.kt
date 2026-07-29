package com.example

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
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
}

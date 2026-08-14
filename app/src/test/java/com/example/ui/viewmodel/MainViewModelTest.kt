package com.example.ui.viewmodel

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.PatientSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var database: AppDatabase
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setup() {
        // Create a fresh in-memory database for each test
        database = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase.setTestInstance(database)
        viewModel = MainViewModel(application)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `savePatientSettings with special CF should prepopulate medications if empty`() = runTest {
        // 1. Add profile
        viewModel.addProfile("Test Profile")
        advanceUntilIdle()
        
        val profileId = viewModel.activeProfileId.value ?: ""

        // 2. Save settings with special CF
        val settings = PatientSettings(
            pazienteNome = "Special User",
            pazienteCf = "CLLRNN40M59L957V",
            medicoNome = "Dr. Special",
            medicoTelefono = "123456"
        )
        
        viewModel.savePatientSettings(settings)
        advanceUntilIdle()

        // 3. Verify medications are pre-populated
        val medications = database.medicationDao().getMedicationsSnapshotByProfile(profileId)
        assertTrue("Medications should be pre-populated for special CF", medications.isNotEmpty())
    }

    @Test
    fun `savePatientSettings with normal CF should NOT prepopulate medications`() = runTest {
        // 1. Setup a profile
        viewModel.addProfile("Normal User")
        advanceUntilIdle()
        val activeProfile = viewModel.activeProfile.filterNotNull().first()
        val profileId = activeProfile.id

        // 2. Save settings with normal CF
        val normalCf = "RSSMRA80A01H501U"
        val settings = PatientSettings(
            pazienteNome = "Mario Rossi",
            pazienteCf = normalCf,
            medicoNome = "Dr. Bianchi",
            medicoTelefono = "123456"
        )
        
        viewModel.savePatientSettings(settings)
        advanceUntilIdle()

        // 3. Verify medications are NOT pre-populated
        val medications = database.medicationDao().getMedicationsSnapshotByProfile(profileId)
        assertTrue("Medications should NOT be pre-populated for normal CF", medications.isEmpty())
    }
}

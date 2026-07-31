package com.example.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.asLiveData
import com.example.data.db.AppDatabase
import com.example.data.model.PatientSettings
import com.example.data.model.Profile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        // Use in-memory database for testing
        database = AppDatabase.getDatabase(application)
        viewModel = MainViewModel(application)
    }

    @Test
    fun `savePatientSettings with special CF should prepopulate medications if empty`() = runTest {
        // 1. Setup a profile
        val profileName = "Test Profile"
        viewModel.addProfile(profileName)
        
        // Wait for profile to be active
        val activeProfile = viewModel.activeProfile.filterNotNull().first()
        val profileId = activeProfile.id

        // 2. Save settings with special CF
        val specialCf = "CLLRNN40M59L957V"
        val settings = PatientSettings(
            pazienteNome = "Special User",
            pazienteCf = specialCf,
            medicoNome = "Dr. Special"
        )
        
        viewModel.savePatientSettings(settings)
        advanceUntilIdle()

        // 3. Verify medications are pre-populated
        val medications = database.medicationDao().getMedicationsSnapshotByProfile(profileId)
        assertTrue("Medications should be pre-populated for special CF", medications.isNotEmpty())
        assertTrue(medications.any { it.nome == "Zanedip 10 mg" })
    }

    @Test
    fun `savePatientSettings with normal CF should NOT prepopulate medications`() = runTest {
        // 1. Setup a profile
        viewModel.addProfile("Normal User")
        val activeProfile = viewModel.activeProfile.filterNotNull().first()
        val profileId = activeProfile.id

        // 2. Save settings with normal CF
        val normalCf = "RSSMRA80A01H501U"
        val settings = PatientSettings(
            pazienteNome = "Mario Rossi",
            pazienteCf = normalCf,
            medicoNome = "Dr. Bianchi"
        )
        
        viewModel.savePatientSettings(settings)
        advanceUntilIdle()

        // 3. Verify medications are NOT pre-populated
        val medications = database.medicationDao().getMedicationsSnapshotByProfile(profileId)
        assertTrue("Medications should NOT be pre-populated for normal CF", medications.isEmpty())
    }
}

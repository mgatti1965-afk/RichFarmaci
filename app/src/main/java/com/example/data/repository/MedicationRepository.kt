package com.example.data.repository

import com.example.data.db.MedicationDao
import com.example.data.model.Medication
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MedicationRepository(private val medicationDao: MedicationDao) {
    
    fun getMedicationsByProfile(profileId: String): Flow<List<Medication>> = 
        medicationDao.getMedicationsByProfile(profileId)

    suspend fun getMedicationsSnapshotByProfile(profileId: String): List<Medication> =
        medicationDao.getMedicationsSnapshotByProfile(profileId)

    suspend fun insert(medication: Medication) = medicationDao.insert(medication)

    suspend fun update(medication: Medication) = medicationDao.update(medication)

    suspend fun delete(medication: Medication) = medicationDao.delete(medication)

    suspend fun deleteById(id: String) = medicationDao.deleteById(id)

    suspend fun clearAll() = medicationDao.clearAll()

    fun getDefaultMedicationsList(profileId: String) = listOf(
        Medication(profileId = profileId, nome = "Zanedip 10 mg", scatole = 2, note = "compresse"),
        Medication(profileId = profileId, nome = "Lasix 25 mg", scatole = 4, note = "compresse"),
        Medication(profileId = profileId, nome = "Pantoprazolo 20 mg", scatole = 1, note = "protettore stomaco"),
        Medication(profileId = profileId, nome = "Cardioaspirina 100 mg", scatole = 1, note = "dopo pranzo"),
        Medication(profileId = profileId, nome = "Zyloric 100 mg", scatole = 1, note = "compresse"),
        Medication(profileId = profileId, nome = "Tareg 160 mg", scatole = 1, note = "compresse della pressione"),
        Medication(profileId = profileId, nome = "Dibase 50.000 u.i./2,5ml", scatole = 1, note = "gocce orali")
    )

    suspend fun forcePrepopulateWithDefaults(profileId: String) {
        // We don't want to clear ALL medications from ALL profiles, just prepopulate for this one
        medicationDao.insertAll(getDefaultMedicationsList(profileId))
    }
}

package com.example.data.repository

import com.example.data.db.MedicationDao
import com.example.data.model.Medication
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MedicationRepository(private val medicationDao: MedicationDao) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()

    suspend fun insert(medication: Medication) = medicationDao.insert(medication)

    suspend fun update(medication: Medication) = medicationDao.update(medication)

    suspend fun delete(medication: Medication) = medicationDao.delete(medication)

    suspend fun deleteById(id: String) = medicationDao.deleteById(id)

    suspend fun clearAll() = medicationDao.clearAll()

    val defaultMedicationsList = listOf(
        Medication(id = UUID.randomUUID().toString(), nome = "Zanedip 10 mg", scatole = 2, note = "compresse"),
        Medication(id = UUID.randomUUID().toString(), nome = "Lasix 25 mg", scatole = 4, note = "compresse"),
        Medication(id = UUID.randomUUID().toString(), nome = "Pantoprazolo 20 mg", scatole = 1, note = "protettore stomaco"),
        Medication(id = UUID.randomUUID().toString(), nome = "Cardioaspirina 100 mg", scatole = 1, note = "dopo pranzo"),
        Medication(id = UUID.randomUUID().toString(), nome = "Zyloric 100 mg", scatole = 1, note = "compresse"),
        Medication(id = UUID.randomUUID().toString(), nome = "Tareg 160 mg", scatole = 1, note = "compresse della pressione"),
        Medication(id = UUID.randomUUID().toString(), nome = "Dibase 50.000 u.i./2,5ml", scatole = 1, note = "gocce orali")
    )

    suspend fun checkAndPrepopulateIfEmpty() {
        val snapshot = medicationDao.getAllMedicationsSnapshot()
        if (snapshot.isEmpty()) {
            medicationDao.insertAll(defaultMedicationsList)
        }
    }

    suspend fun forcePrepopulateWithDefaults() {
        medicationDao.clearAll()
        medicationDao.insertAll(defaultMedicationsList)
    }
}

package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE profileId = :profileId ORDER BY nome ASC")
    fun getMedicationsByProfile(profileId: String): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE profileId = :profileId ORDER BY nome ASC")
    suspend fun getMedicationsSnapshotByProfile(profileId: String): List<Medication>

    @Query("SELECT * FROM medications")
    suspend fun getAllMedicationsSnapshot(): List<Medication>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: Medication)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medications: List<Medication>)

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM medications")
    suspend fun clearAll()
}

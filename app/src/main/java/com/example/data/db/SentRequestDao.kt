package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SentRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface SentRequestDao {
    @Query("SELECT * FROM sent_requests WHERE profileId = :profileId ORDER BY data DESC")
    fun getSentRequestsByProfile(profileId: String): Flow<List<SentRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sentRequest: SentRequest)

    @Delete
    suspend fun delete(sentRequest: SentRequest)

    @Query("DELETE FROM sent_requests WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sent_requests")
    suspend fun clearAll()
}

package com.example.data.repository

import com.example.data.db.SentRequestDao
import com.example.data.model.SentRequest
import kotlinx.coroutines.flow.Flow

class SentRequestRepository(private val sentRequestDao: SentRequestDao) {
    fun getSentRequestsByProfile(profileId: String): Flow<List<SentRequest>> = 
        sentRequestDao.getSentRequestsByProfile(profileId)

    suspend fun insert(sentRequest: SentRequest) {
        sentRequestDao.insert(sentRequest)
    }

    suspend fun delete(sentRequest: SentRequest) {
        sentRequestDao.delete(sentRequest)
    }

    suspend fun deleteById(id: String) {
        sentRequestDao.deleteById(id)
    }

    suspend fun clearAll() {
        sentRequestDao.clearAll()
    }
}

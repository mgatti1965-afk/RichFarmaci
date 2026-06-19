package com.example.data.repository

import com.example.data.db.SentRequestDao
import com.example.data.model.SentRequest
import kotlinx.coroutines.flow.Flow

class SentRequestRepository(private val sentRequestDao: SentRequestDao) {
    val allSentRequests: Flow<List<SentRequest>> = sentRequestDao.getAllSentRequests()

    suspend fun insert(sentRequest: SentRequest) {
        sentRequestDao.insert(sentRequest)
    }

    suspend fun delete(sentRequest: SentRequest) {
        sentRequestDao.delete(sentRequest)
    }

    suspend fun deleteById(id: String) {
        sentRequestDao.deleteById(id)
    }
}

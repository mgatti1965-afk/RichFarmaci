package com.example.data.repository

import com.example.data.db.ProfileDao
import com.example.data.model.Profile
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val profileDao: ProfileDao) {
    val allProfiles: Flow<List<Profile>> = profileDao.getAllProfiles()

    suspend fun getProfileById(id: String): Profile? = profileDao.getProfileById(id)

    suspend fun insertProfile(profile: Profile) = profileDao.insertProfile(profile)

    suspend fun updateProfile(profile: Profile) = profileDao.updateProfile(profile)

    suspend fun deleteProfile(profile: Profile) = profileDao.deleteProfile(profile)
}

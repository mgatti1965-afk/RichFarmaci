package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = 1,
    val profiles: List<Profile> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val sentRequests: List<SentRequest> = emptyList(),
    val donationCount: Int = 0,
    val onboardingShown: Boolean = true,
    val disclaimerAccepted: Boolean = true
)

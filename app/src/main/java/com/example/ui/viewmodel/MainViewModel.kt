package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.model.Medication
import com.example.data.model.PatientSettings
import com.example.data.model.Profile
import com.example.data.model.SentMedication
import com.example.data.model.SentRequest
import com.example.data.preferences.PatientSettingsManager
import com.example.data.repository.MedicationRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.SentRequestRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val medicationRepository = MedicationRepository(db.medicationDao())
    private val sentRequestRepository = SentRequestRepository(db.sentRequestDao())
    private val profileRepository = ProfileRepository(db.profileDao())
    private val settingsManager = PatientSettingsManager(application)

    // Profiles List
    val profiles: StateFlow<List<Profile>> = profileRepository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active Profile State
    private val _activeProfileId = MutableStateFlow(settingsManager.getActiveProfileId())
    val activeProfileId: StateFlow<String?> = _activeProfileId.asStateFlow()

    private val _activeProfile = MutableStateFlow<Profile?>(null)
    val activeProfile: StateFlow<Profile?> = _activeProfile.asStateFlow()

    // Screen navigation state
    private val _currentTab = MutableStateFlow(0) // 0 = Request, 1 = History
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    // Settings State
    private val _settings = MutableStateFlow(settingsManager.getSettings())
    val settings: StateFlow<PatientSettings> = _settings.asStateFlow()

    // Configuration Validity
    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    // Medications Flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val medications: StateFlow<List<Medication>> = _activeProfileId
        .flatMapLatest { id ->
            if (id != null) medicationRepository.getMedicationsByProfile(id)
            else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // History Flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val sentRequests: StateFlow<List<SentRequest>> = _activeProfileId
        .flatMapLatest { id ->
            if (id != null) sentRequestRepository.getSentRequestsByProfile(id)
            else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User selection states for the active request form
    private val _selectedMedicationIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMedicationIds: StateFlow<Set<String>> = _selectedMedicationIds.asStateFlow()

    // Map of medication ID -> requested boxes (different from standard quantity)
    private val _selectedQuantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val selectedQuantities: StateFlow<Map<String, Int>> = _selectedQuantities.asStateFlow()

    // Map of medication ID -> customized transient notes (different from standard medication notes)
    private val _selectedNotes = MutableStateFlow<Map<String, String>>(emptyMap())
    val selectedNotes: StateFlow<Map<String, String>> = _selectedNotes.asStateFlow()

    init {
        viewModelScope.launch {
            migrateFromSharedPreferencesIfNeeded()
            
            // Select first profile on startup
            val currentProfiles = db.profileDao().getAllProfilesSnapshot()
            if (currentProfiles.isNotEmpty()) {
                selectProfile(currentProfiles.first().id)
            }
            
            updateConfigStatus()
        }
    }

    private suspend fun migrateFromSharedPreferencesIfNeeded() {
        val currentProfiles = db.profileDao().getAllProfilesSnapshot()
        if (currentProfiles.isEmpty() && settingsManager.isConfigured()) {
            val legacySettings = settingsManager.getSettings()
            val newProfile = Profile(
                pazienteNome = legacySettings.pazienteNome,
                pazienteCf = legacySettings.pazienteCf,
                medicoNome = legacySettings.medicoNome,
                medicoTelefono = legacySettings.medicoTelefono,
                medicoEmail = legacySettings.medicoEmail,
                secondoIndirizzo = legacySettings.secondoIndirizzo,
                messaggioTesta = legacySettings.messaggioTesta,
                messaggioCoda = legacySettings.messaggioCoda,
                tipoInvio = legacySettings.tipoInvio,
                notificheAttive = legacySettings.notificheAttive,
                descrizioneNotifica = legacySettings.descrizioneNotifica
            )
            profileRepository.insertProfile(newProfile)
            
            // Migrate medications if any (using default if CF matches)
            val cfUppercase = newProfile.pazienteCf.trim().uppercase()
            if (cfUppercase == "CLLRNN40M59L957V") {
                medicationRepository.forcePrepopulateWithDefaults(newProfile.id)
            }
        }
    }

    fun selectProfile(id: String) {
        viewModelScope.launch {
            _activeProfileId.value = id
            settingsManager.setActiveProfileId(id)
            refreshActiveProfile()
            clearFormSelection()
            updateConfigStatus()
        }
    }

    private suspend fun refreshActiveProfile() {
        val id = _activeProfileId.value
        if (id != null) {
            _activeProfile.value = profileRepository.getProfileById(id)
        } else {
            _activeProfile.value = null
        }
    }

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    fun setShowSettings(show: Boolean) {
        _showSettings.value = show
    }

    private fun updateConfigStatus() {
        _isConfigured.value = _activeProfile.value != null
        _settings.value = _activeProfile.value?.let { p ->
            PatientSettings(
                pazienteNome = p.pazienteNome,
                pazienteCf = p.pazienteCf,
                medicoNome = p.medicoNome,
                medicoTelefono = p.medicoTelefono,
                medicoEmail = p.medicoEmail,
                secondoIndirizzo = p.secondoIndirizzo,
                messaggioTesta = p.messaggioTesta,
                messaggioCoda = p.messaggioCoda,
                tipoInvio = p.tipoInvio,
                notificheAttive = p.notificheAttive,
                descrizioneNotifica = p.descrizioneNotifica
            )
        } ?: PatientSettings()
    }

    // Update selection from UI (handles both selection toggle and quantity)
    fun updateSelection(medication: Medication, quantity: Int) {
        val currentSelected = _selectedMedicationIds.value
        if (quantity <= 0) {
            if (currentSelected.contains(medication.id)) {
                toggleMedicationSelection(medication)
            }
        } else {
            if (!currentSelected.contains(medication.id)) {
                toggleMedicationSelection(medication)
            }
            setQuantityForMedication(medication.id, quantity)
        }
    }

    fun generateRequestIntent(context: Context): Intent? {
        val message = buildFormattedMessage()
        val currentSettings = _settings.value
        
        return when (currentSettings.tipoInvio) {
            0 -> { // WhatsApp
                val intent = Intent(Intent.ACTION_VIEW)
                val phone = currentSettings.medicoTelefono.replace("+", "").replace(" ", "")
                val url = "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}"
                intent.data = Uri.parse(url)
                intent
            }
            1 -> { // SMS
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("smsto:${currentSettings.medicoTelefono}")
                intent.putExtra("sms_body", message)
                intent
            }
            2 -> { // Email
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:${currentSettings.medicoEmail}")
                intent.putExtra(Intent.EXTRA_SUBJECT, "Richiesta Farmaci: ${currentSettings.pazienteNome}")
                intent.putExtra(Intent.EXTRA_TEXT, message)
                intent
            }
            else -> null
        }
    }

    // Toggle medication selection for the rapid order
    fun toggleMedicationSelection(medication: Medication) {
        val currentSelected = _selectedMedicationIds.value.toMutableSet()
        if (currentSelected.contains(medication.id)) {
            currentSelected.remove(medication.id)
            // Cleanup local temp states
            val currentQuants = _selectedQuantities.value.toMutableMap()
            currentQuants.remove(medication.id)
            _selectedQuantities.value = currentQuants

            val currentNotes = _selectedNotes.value.toMutableMap()
            currentNotes.remove(medication.id)
            _selectedNotes.value = currentNotes
        } else {
            currentSelected.add(medication.id)
            // Pre-fill with item standard values
            setQuantityForMedication(medication.id, medication.scatole)
            setNoteForMedication(medication.id, "")
        }
        _selectedMedicationIds.value = currentSelected
    }

    fun setQuantityForMedication(medicationId: String, qty: Int) {
        val currentQuants = _selectedQuantities.value.toMutableMap()
        currentQuants[medicationId] = qty.coerceAtLeast(1)
        _selectedQuantities.value = currentQuants
    }

    fun setNoteForMedication(medicationId: String, note: String) {
        val currentNotes = _selectedNotes.value.toMutableMap()
        currentNotes[medicationId] = note
        _selectedNotes.value = currentNotes
    }

    // Save profile configurations
    fun savePatientSettings(newSettings: PatientSettings) {
        viewModelScope.launch {
            val currentProfile = _activeProfile.value
            if (currentProfile != null) {
                val updatedProfile = currentProfile.copy(
                    pazienteNome = newSettings.pazienteNome,
                    pazienteCf = newSettings.pazienteCf,
                    medicoNome = newSettings.medicoNome,
                    medicoTelefono = newSettings.medicoTelefono,
                    medicoEmail = newSettings.medicoEmail,
                    secondoIndirizzo = newSettings.secondoIndirizzo,
                    messaggioTesta = newSettings.messaggioTesta,
                    messaggioCoda = newSettings.messaggioCoda,
                    tipoInvio = newSettings.tipoInvio,
                    notificheAttive = newSettings.notificheAttive,
                    descrizioneNotifica = newSettings.descrizioneNotifica
                )
                profileRepository.updateProfile(updatedProfile)
                _activeProfile.value = updatedProfile
                updateConfigStatus()

                // Update notifications scheduling
                val currentMedications = medications.value
                com.example.util.NotificationHelper.updateAllNotifications(
                    getApplication(),
                    currentMedications,
                    newSettings.notificheAttive,
                    newSettings.descrizioneNotifica
                )

                // Check Special CF criteria
                val cfUppercase = newSettings.pazienteCf.trim().uppercase()
                if (cfUppercase == "CLLRNN40M59L957V") {
                    val dbMedicationsSnapshot = medicationRepository.getMedicationsSnapshotByProfile(updatedProfile.id)
                    if (dbMedicationsSnapshot.isEmpty()) {
                        medicationRepository.forcePrepopulateWithDefaults(updatedProfile.id)
                    }
                }
            } else {
                // Create new profile if none active? 
                // For now, let's assume we use addProfile for creation
            }
        }
    }

    fun addProfile(name: String) {
        viewModelScope.launch {
            val newProfile = Profile(pazienteNome = name)
            profileRepository.insertProfile(newProfile)
            selectProfile(newProfile.id)
        }
    }

    fun deleteCurrentProfile() {
        viewModelScope.launch {
            _activeProfile.value?.let { profile ->
                profileRepository.deleteProfile(profile)
                _activeProfileId.value = null
                settingsManager.setActiveProfileId(null)
                
                val remainingProfiles = db.profileDao().getAllProfilesSnapshot()
                if (remainingProfiles.isNotEmpty()) {
                    selectProfile(remainingProfiles.first().id)
                } else {
                    _activeProfile.value = null
                    updateConfigStatus()
                }
            }
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profile)
            if (_activeProfileId.value == profile.id) {
                _activeProfileId.value = null
                settingsManager.setActiveProfileId(null)
                val remainingProfiles = db.profileDao().getAllProfilesSnapshot()
                if (remainingProfiles.isNotEmpty()) {
                    selectProfile(remainingProfiles.first().id)
                } else {
                    _activeProfile.value = null
                    updateConfigStatus()
                }
            }
        }
    }

    // Medication Manager Operations (Crud in configure panel)
    fun addMedication(nome: String, scatole: Int, note: String, notificaAttiva: Boolean = false, orarioNotifica: String = "08:00", frequenzaValore: Int = 0, frequenzaTipo: String = "ORE") {
        viewModelScope.launch {
            val profileId = _activeProfileId.value ?: return@launch
            if (nome.isNotBlank()) {
                val newMed = Medication(
                    id = UUID.randomUUID().toString(),
                    profileId = profileId,
                    nome = nome.trim(),
                    scatole = scatole.coerceAtLeast(0),
                    note = note.trim(),
                    inPausa = false,
                    notificaAttiva = notificaAttiva,
                    orarioNotifica = orarioNotifica,
                    frequenzaValore = frequenzaValore,
                    frequenzaTipo = frequenzaTipo
                )
                medicationRepository.insert(newMed)
                
                // Schedule notification if active
                if (_settings.value.notificheAttive) {
                    com.example.util.NotificationHelper.scheduleNotification(
                        getApplication(),
                        newMed,
                        _settings.value.descrizioneNotifica
                    )
                }
            }
        }
    }

    fun toggleMedicationStandby(medication: Medication) {
        viewModelScope.launch {
            val updated = medication.copy(inPausa = !medication.inPausa)
            medicationRepository.update(updated)

            // Remove if selected for order and goes in standby
            if (updated.inPausa && _selectedMedicationIds.value.contains(updated.id)) {
                toggleMedicationSelection(updated)
            }

            // Update notification
            if (_settings.value.notificheAttive) {
                if (updated.inPausa) {
                    com.example.util.NotificationHelper.cancelNotification(getApplication(), updated)
                } else {
                    com.example.util.NotificationHelper.scheduleNotification(
                        getApplication(),
                        updated,
                        _settings.value.descrizioneNotifica
                    )
                }
            }
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.update(medication)
            
            // Update notification
            if (_settings.value.notificheAttive) {
                com.example.util.NotificationHelper.scheduleNotification(
                    getApplication(),
                    medication,
                    _settings.value.descrizioneNotifica
                )
            }
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.delete(medication)
            if (_selectedMedicationIds.value.contains(medication.id)) {
                toggleMedicationSelection(medication)
            }
            
            // Cancel notification
            com.example.util.NotificationHelper.cancelNotification(getApplication(), medication)
        }
    }

    // Generate formatted text message for request
    fun buildFormattedMessage(): String {
        val currentProfile = _settings.value
        val sections = mutableListOf<String>()

        // 1. Frase di testa
        if (currentProfile.messaggioTesta.isNotBlank()) {
            sections.add(currentProfile.messaggioTesta.trim())
        }

        // 2. Lista farmaci con doppio a capo tra uno e l'altro
        val listMedications = medications.value
        val selectedIds = _selectedMedicationIds.value
        val medsList = listMedications.filter { selectedIds.contains(it.id) }.map { med ->
            val qty = _selectedQuantities.value[med.id] ?: med.scatole
            val tempNote = _selectedNotes.value[med.id] ?: ""
            // Standard static medicine notes
            val baseNote = med.note

            val noteStr = when {
                tempNote.isNotBlank() && baseNote.isNotBlank() -> " ($baseNote - $tempNote)"
                tempNote.isNotBlank() -> " ($tempNote)"
                baseNote.isNotBlank() -> " ($baseNote)"
                else -> ""
            }

            val scatolaWord = if (qty == 1) "scatola" else "scatole"
            "- ${med.nome}: $qty $scatolaWord$noteStr"
        }

        if (medsList.isNotEmpty()) {
            sections.add(medsList.joinToString("\n\n"))
        }

        // 3. Dati Paziente e recapito
        val footerParts = mutableListOf<String>()
        footerParts.add("Nome Paziente: ${currentProfile.pazienteNome}")
        footerParts.add("Codice Fiscale: ${currentProfile.pazienteCf.uppercase()}")

        if (currentProfile.secondoIndirizzo.isNotBlank()) {
            footerParts.add("Note di Recapito: ${currentProfile.secondoIndirizzo}")
        }

        if (currentProfile.tipoInvio == 2 && currentProfile.medicoEmail.isNotBlank()) {
            footerParts.add("Inviato via Email a: ${currentProfile.medicoEmail}")
        }

        sections.add(footerParts.joinToString("\n"))

        // 4. Frase di coda
        if (currentProfile.messaggioCoda.isNotBlank()) {
            sections.add(currentProfile.messaggioCoda.trim())
        }

        // Unisce tutto con doppio a capo tra le sezioni principali
        return sections.joinToString("\n\n")
    }

    // Logs the sent request into database history
    fun recordSentRequest(messageText: String) {
        viewModelScope.launch {
            val profileId = _activeProfileId.value ?: return@launch
            val currentProfile = _settings.value
            val listMedications = medications.value
            val selectedIds = _selectedMedicationIds.value

            val sentMeds = listMedications.filter { selectedIds.contains(it.id) }.map { med ->
                val qty = _selectedQuantities.value[med.id] ?: med.scatole
                val tempNote = _selectedNotes.value[med.id] ?: ""
                val baseNote = med.note
                val noteCombined = when {
                    tempNote.isNotBlank() && baseNote.isNotBlank() -> "$baseNote - $tempNote"
                    tempNote.isNotBlank() -> tempNote
                    else -> baseNote
                }
                SentMedication(
                    nome = med.nome,
                    scatole = qty,
                    note = noteCombined
                )
            }

            val format = SimpleDateFormat("dd MMM yyyy, 'Ore' HH:mm", Locale.ITALIAN)
            val formattedDate = format.format(Date())

            val req = SentRequest(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                data = formattedDate,
                pazienteNome = currentProfile.pazienteNome,
                medicoNome = currentProfile.medicoNome,
                farmaciSerialized = SentRequest.serializeMedicines(sentMeds),
                testoCompleto = messageText
            )

            sentRequestRepository.insert(req)

            // Reset current form selection states after preparing
            clearFormSelection()
        }
    }

    fun repeatSentRequest(request: SentRequest) {
        // Logging historical sent items does not require full form selection
        // Successive clicks trigger intent directly with the pre-baked message
    }

    fun clearFormSelection() {
        _selectedMedicationIds.value = emptySet()
        _selectedQuantities.value = emptyMap()
        _selectedNotes.value = emptyMap()
    }

    fun deleteHistoryItem(request: SentRequest) {
        viewModelScope.launch {
            sentRequestRepository.delete(request)
        }
    }
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

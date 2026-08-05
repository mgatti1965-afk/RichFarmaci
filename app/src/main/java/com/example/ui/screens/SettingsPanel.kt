package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Medication
import com.example.data.model.PatientSettings
import com.example.data.model.Profile
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.capitalizeWords

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanelContent(
    settings: PatientSettings,
    medications: List<Medication>,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var showHelp by remember { mutableStateOf(false) }
    var showWarningBanner by remember { mutableStateOf(false) }

    LaunchedEffect(showWarningBanner) {
        if (showWarningBanner) {
            kotlinx.coroutines.delay(3500)
            showWarningBanner = false
        }
    }

    if (showHelp) {
        HelpDialog(type = "configurazione", onDismiss = { showHelp = false })
    }

    var pNome by remember(settings) { mutableStateOf(settings.pazienteNome) }
    var pCf by remember(settings) { mutableStateOf(settings.pazienteCf) }
    var mNome by remember(settings) { mutableStateOf(settings.medicoNome) }
    var mTel by remember(settings) { mutableStateOf(settings.medicoTelefono) }
    var mEmail by remember(settings) { mutableStateOf(settings.medicoEmail) }
    var secInd by remember(settings) { mutableStateOf(settings.secondoIndirizzo) }
    var msgTesta by remember(settings) { mutableStateOf(settings.messaggioTesta) }
    var msgCoda by remember(settings) { mutableStateOf(settings.messaggioCoda) }
    var valType by remember(settings) { mutableIntStateOf(settings.tipoInvio) }
    var nAttive by remember(settings) { mutableStateOf(settings.notificheAttive) }
    var nDesc by remember(settings) { mutableStateOf(settings.descrizioneNotifica) }

    var exitAttempted by remember(settings) { mutableStateOf(false) }
    val profileId by viewModel.activeProfileId.collectAsState()
    val isNewProfile = profileId == null

    if (pCf == "!!!" || pCf == "!!! ") {
        AlertDialog(
            onDismissRequest = { pCf = "" },
            title = { Text("⚠️ RESET TOTALE", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("Sei sicuro di voler eliminare TUTTI i dati? Questa operazione cancellerà ogni profilo e ogni farmaco salvato e non è reversibile.") },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.resetEverything()
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("ELIMINA TUTTO", color = White) }
            },
            dismissButton = {
                TextButton(onClick = { pCf = "" }) { Text("ANNULLA") }
            }
        )
    }

    val hasChanges = remember(pNome, pCf, mNome, mTel, mEmail, secInd, msgTesta, msgCoda, valType, nAttive, nDesc) {
        pNome != settings.pazienteNome ||
        pCf != settings.pazienteCf ||
        mNome != settings.medicoNome ||
        mTel != settings.medicoTelefono ||
        mEmail != settings.medicoEmail ||
        secInd != settings.secondoIndirizzo ||
        msgTesta != settings.messaggioTesta ||
        msgCoda != settings.messaggioCoda ||
        valType != settings.tipoInvio ||
        nAttive != settings.notificheAttive ||
        nDesc != settings.descrizioneNotifica
    }

    BackHandler(enabled = hasChanges) {
        if (exitAttempted) {
            onClose()
        } else {
            showWarningBanner = true
            exitAttempted = true
        }
    }

    var showMedicationDialog by remember { mutableStateOf(false) }
    var medicationToEdit by remember { mutableStateOf<Medication?>(null) }

    if (showMedicationDialog) {
        MedicationEditorDialog(
            profileId = profileId ?: "",
            medication = medicationToEdit,
            notificationsEnabled = nAttive,
            onDismiss = { showMedicationDialog = false },
            onSave = { updatedMed ->
                if (medicationToEdit == null) {
                    viewModel.addMedication(
                        updatedMed.nome,
                        updatedMed.scatole,
                        updatedMed.note,
                        updatedMed.notificaAttiva,
                        updatedMed.orarioNotifica,
                        updatedMed.frequenzaValore,
                        updatedMed.frequenzaTipo
                    )
                    Toast.makeText(context, "Farmaco aggiunto!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateMedication(updatedMed)
                    Toast.makeText(context, "Farmaco aggiornato!", Toast.LENGTH_SHORT).show()
                }
                showMedicationDialog = false
            }
        )
    }

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { uri ->
            if (uri != null) {
                var cName = ""
                var cPhone = ""
                var cEmail = ""
                val resolver = context.contentResolver
                try {
                    resolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameCol = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                            if (nameCol >= 0) cName = cursor.getString(nameCol) ?: ""

                            val idCol = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                            if (idCol >= 0) {
                                val contactId = cursor.getString(idCol)
                                val hasPhoneCol = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                                val hasPhone = if (hasPhoneCol >= 0) cursor.getInt(hasPhoneCol) else 0

                                if (hasPhone > 0) {
                                    resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", arrayOf(contactId), null)?.use { pCursor ->
                                        if (pCursor.moveToFirst()) {
                                            val numCol = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                            if (numCol >= 0) cPhone = pCursor.getString(numCol) ?: ""
                                        }
                                    }
                                }
                                resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", arrayOf(contactId), null)?.use { eCursor ->
                                    if (eCursor.moveToFirst()) {
                                        val emailCol = eCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                                        if (emailCol >= 0) cEmail = eCursor.getString(emailCol) ?: ""
                                    }
                                }
                            }
                        }
                    }
                    if (cName.isNotBlank()) mNome = cName.capitalizeWords()
                    if (cPhone.isNotBlank()) mTel = cPhone.replace("\\s".toRegex(), "").replace("[^+0-9]".toRegex(), "")
                    if (cEmail.isNotBlank()) mEmail = cEmail
                } catch (e: Exception) {
                    Toast.makeText(context, "Errore nell'accesso ai contatti.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) pickContactLauncher.launch(null)
            else Toast.makeText(context, "Permesso necessario.", Toast.LENGTH_LONG).show()
        }
    )

    Column(modifier = Modifier.fillMaxSize().background(GrayBackground)) {
        Column(modifier = Modifier.fillMaxWidth().background(White).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "⚙️ Configurazione", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    if (isNewProfile) {
                        Text(text = "CREAZIONE NUOVO PROFILO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showHelp = true }, modifier = Modifier.background(GrayBackground, CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Aiuto", tint = Slate900)
                    }
                    IconButton(
                        onClick = { 
                            if (hasChanges && !exitAttempted) {
                                showWarningBanner = true
                                exitAttempted = true
                            } else {
                                onClose()
                            }
                        }, 
                        modifier = Modifier.background(GrayBackground, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Slate900)
                    }
                }
            }
        }
        HorizontalDivider(color = GrayBorder)

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            item {
                ProfileContextSwitcher(
                    profiles = profiles,
                    activeProfile = activeProfile,
                    onProfileSelected = { viewModel.selectProfile(it) },
                    onAddProfile = { showAddProfileDialog = true },
                    onDeleteProfile = { viewModel.deleteProfile(it) }
                )
                HorizontalDivider(color = GrayBorder, modifier = Modifier.padding(top = 8.dp))
            }

            item { Text(text = "1. Anagrafica Paziente e Medico", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Slate900) }
            item {
                OutlinedTextField(
                    value = pNome,
                    onValueChange = { pNome = it.capitalizeWords() },
                    label = { Text("Nome e Cognome Paziente (*)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = GrayBorder,
                        focusedLabelColor = GreenPrimary,
                        unfocusedLabelColor = Slate600,
                        unfocusedContainerColor = White,
                        focusedContainerColor = White,
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = pCf,
                    onValueChange = { pCf = it.uppercase() },
                    label = { Text("Codice Fiscale (*)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = GrayBorder,
                        focusedLabelColor = GreenPrimary,
                        unfocusedLabelColor = Slate600,
                        unfocusedContainerColor = White,
                        focusedContainerColor = White,
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = GreenLightBg), border = BorderStroke(1.2.dp, GreenPrimary), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Importa Medico da Rubrica", fontWeight = FontWeight.Bold, color = Slate900)
                            Text("Seleziona dal telefono il contatto del medico", fontSize = 13.sp, color = Slate600)
                        }
                        Button(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) pickContactLauncher.launch(null)
                            else permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) { Text("SCEGLI", color = White) }
                    }
                }
            }
            item { OutlinedTextField(value = mNome, onValueChange = { mNome = it.capitalizeWords() }, label = { Text("Nome Medico (*)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = GrayBorder, focusedLabelColor = GreenPrimary, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White, focusedTextColor = Slate900, unfocusedTextColor = Slate900), shape = RoundedCornerShape(12.dp)) }
            item { OutlinedTextField(value = mTel, onValueChange = { mTel = it }, label = { Text("Cellulare Medico (o Email*)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = GrayBorder, focusedLabelColor = GreenPrimary, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White, focusedTextColor = Slate900, unfocusedTextColor = Slate900), shape = RoundedCornerShape(12.dp)) }
            item { OutlinedTextField(value = mEmail, onValueChange = { mEmail = it }, label = { Text("Email Medico (o Cellulare*)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = GrayBorder, focusedLabelColor = GreenPrimary, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White, focusedTextColor = Slate900, unfocusedTextColor = Slate900), shape = RoundedCornerShape(12.dp)) }
            item { OutlinedTextField(value = secInd, onValueChange = { secInd = it }, label = { Text("Note recapito (Opzionale)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = GrayBorder, focusedLabelColor = GreenPrimary, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White, focusedTextColor = Slate900, unfocusedTextColor = Slate900), shape = RoundedCornerShape(12.dp)) }
            
            item {
                HorizontalDivider(color = GrayBorder)
                Text(text = "2. Opzioni Messaggio", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Slate900, modifier = Modifier.padding(top = 10.dp))
            }
            item { OutlinedTextField(value = msgTesta, onValueChange = { msgTesta = it }, label = { Text("Frase di Testa") }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = GrayBorder, focusedLabelColor = GreenPrimary, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White, focusedTextColor = Slate900, unfocusedTextColor = Slate900), shape = RoundedCornerShape(12.dp)) }
            item { OutlinedTextField(value = msgCoda, onValueChange = { msgCoda = it }, label = { Text("Frase di Coda") }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, unfocusedBorderColor = GrayBorder, focusedLabelColor = GreenPrimary, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White, focusedTextColor = Slate900, unfocusedTextColor = Slate900), shape = RoundedCornerShape(12.dp)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tipo Invio Predefinito:", fontWeight = FontWeight.Bold, color = Slate900)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("WhatsApp", "SMS", "Email").forEachIndexed { index, label ->
                            val selected = valType == index
                            Surface(
                                modifier = Modifier.weight(1f).height(48.dp).clickable { valType = index },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) (if (index == 0) GreenPrimary else if (index == 1) Slate900 else Color(0xFF6366F1)) else White,
                                border = if (!selected) BorderStroke(1.dp, GrayBorder) else null
                            ) { Box(contentAlignment = Alignment.Center) { Text(label, color = if (selected) White else Slate600, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) } }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Attiva Notifiche", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        Switch(
                            checked = nAttive,
                            onCheckedChange = { nAttive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = White,
                                checkedTrackColor = GreenPrimary,
                                uncheckedThumbColor = White,
                                uncheckedTrackColor = GrayBorder
                            )
                        )
                    }
                    if (nAttive) {
                        OutlinedTextField(
                            value = nDesc,
                            onValueChange = { nDesc = it.capitalizeWords() },
                            label = { Text("Descrizione Notifica") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = GrayBorder,
                                focusedLabelColor = GreenPrimary,
                                unfocusedLabelColor = Slate600,
                                unfocusedContainerColor = White,
                                focusedContainerColor = White,
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
            item {
                val formsFilled = pNome.isNotBlank() && pCf.isNotBlank() && mNome.isNotBlank() && ((valType == 2 && mEmail.isNotBlank()) || (valType != 2 && mTel.isNotBlank()))
                
                Button(
                    onClick = {
                        if (formsFilled) {
                            viewModel.savePatientSettings(PatientSettings(pazienteNome = pNome, pazienteCf = pCf, medicoNome = mNome, medicoTelefono = mTel, medicoEmail = mEmail, secondoIndirizzo = secInd, messaggioTesta = msgTesta, messaggioCoda = msgCoda, tipoInvio = valType, notificheAttive = nAttive, descrizioneNotifica = nDesc))
                            Toast.makeText(context, "Profilo salvato correttamente!", Toast.LENGTH_SHORT).show()
                            exitAttempted = false // Reset exit flag on success
                            onClose()
                        } else {
                            Toast.makeText(context, "Attenzione: Compila tutti i campi obbligatori segnati con (*)", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (formsFilled) Color(0xFFD32F2F) else Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = hasChanges || isNewProfile
                ) {
                    Text(
                        if (formsFilled) "SALVA CONFIGURAZIONE" else "COMPILA CAMPI OBBLIGATORI (*)",
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
            }

            if (!isNewProfile) {
                item {
                    HorizontalDivider(color = GrayBorder)
                    Text(text = "3. Gestore Rubrica Farmaci", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Slate900, modifier = Modifier.padding(top = 10.dp))
                }
                item {
                    Button(
                        onClick = {
                            medicationToEdit = null
                            showMedicationDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NUOVO FARMACO/NOTIFICA", fontWeight = FontWeight.Bold, color = White)
                    }
                }
                item { Text("I Tuoi Farmaci Salvati:", fontWeight = FontWeight.Bold, color = Slate900, modifier = Modifier.padding(top = 8.dp)) }
                if (medications.isEmpty()) {
                    item { Text("La rubrica è vuota.", color = Slate600, modifier = Modifier.padding(16.dp)) }
                } else {
                    items(medications, key = { it.id }) { med ->
                        ConfigurationMedicationRow(
                            med = med,
                            notificationsEnabled = settings.notificheAttive,
                            onEdit = {
                                medicationToEdit = med
                                showMedicationDialog = true
                            },
                            onToggleStandby = { viewModel.toggleMedicationStandby(med) },
                            onDelete = { viewModel.deleteMedication(med) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showAddProfileDialog) {
        AlertDialog(
            onDismissRequest = { showAddProfileDialog = false },
            title = { Text("Nuovo Profilo") },
            text = {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it.capitalizeWords() },
                    label = { Text("Nome Paziente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = GrayBorder,
                        focusedLabelColor = GreenPrimary,
                        unfocusedLabelColor = Slate600,
                        unfocusedContainerColor = White,
                        focusedContainerColor = White,
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newProfileName.isNotBlank()) {
                        viewModel.addProfile(newProfileName)
                        newProfileName = ""
                        showAddProfileDialog = false
                    }
                }) {
                    Text("AGGIUNGI", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProfileDialog = false }) {
                    Text("ANNULLA")
                }
            }
        )
    }

    AnimatedVisibility(
        visible = showWarningBanner,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(bottom = 90.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = Color.Red,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = "Modifiche non salvate. Premi ancora per uscire.",
                    color = White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

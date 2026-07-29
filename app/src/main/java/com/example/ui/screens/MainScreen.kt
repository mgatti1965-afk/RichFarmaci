package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.model.Medication
import com.example.data.model.PatientSettings
import com.example.ui.viewmodel.MainViewModel

// Simple Slate-based colors for UI consistency
val GreenPrimary = Color(0xFF16A34A)
val GreenLightBg = Color(0xFFDCFCE7)
val Slate900 = Color(0xFF0F172A)
val Slate600 = Color(0xFF475569)
val GrayBackground = Color(0xFFF1F5F9)
val GrayDarker = Color(0xFFE2E8F0)
val GrayBorder = Color(0xFFCBD5E1)
val White = Color.White

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val selectedIds by viewModel.selectedMedicationIds.collectAsState()
    val selectedQuantities by viewModel.selectedQuantities.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val sentRequests by viewModel.sentRequests.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var helpType by remember { mutableStateOf<String?>(null) }
    var viewingRequestText by remember { mutableStateOf<String?>(null) }

    if (helpType != null) {
        HelpDialog(type = helpType!!, onDismiss = { helpType = null })
    }

    if (viewingRequestText != null) {
        AlertDialog(
            onDismissRequest = { viewingRequestText = null },
            title = { Text("Dettaglio Messaggio", fontWeight = FontWeight.Bold, color = Slate900) },
            text = { 
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    Text(viewingRequestText!!, fontSize = 14.sp, color = Slate600)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingRequestText = null }) {
                    Text("Chiudi", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = GrayBackground
        )
    }

    Scaffold { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = GrayBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PharmacyCross(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "RichFarmaci",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { helpType = if (currentTab == 0) "richiesta" else "cronologia" },
                            modifier = Modifier.background(GrayBackground, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Aiuto", tint = Slate600)
                        }
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.background(GrayBackground, CircleShape)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Impostazioni", tint = Slate600)
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = currentTab,
                    containerColor = White,
                    contentColor = GreenPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                            color = GreenPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = currentTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = { Text("RICHIESTA", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                        selectedContentColor = GreenPrimary,
                        unselectedContentColor = Slate600
                    )
                    Tab(
                        selected = currentTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = { Text("CRONOLOGIA", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        selectedContentColor = GreenPrimary,
                        unselectedContentColor = Slate600
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (currentTab == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Patient Info Summary
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = GreenLightBg),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${settings.pazienteNome.ifBlank { "Configura profilo" }} • Medico: ${settings.medicoNome.ifBlank { "..." }}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate900
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Cosa ti serve oggi?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                            ) {
                                items(medications.filter { !it.inPausa }) { med ->
                                    val isSelected = selectedIds.contains(med.id)
                                    val quantity = selectedQuantities[med.id] ?: 0
                                    MedicationItem(
                                        med = med,
                                        isSelected = isSelected,
                                        requestedQuantity = quantity,
                                        notificationsEnabled = settings.notificheAttive,
                                        onQuantityChange = { viewModel.updateSelection(med, it) }
                                    )
                                }
                            }
                        }

                        // Bottom Send Action
                        val selectedCount = selectedIds.size
                        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                            AnimatedVisibility(
                                visible = selectedCount > 0,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val message = viewModel.buildFormattedMessage()
                                        val intent = viewModel.generateRequestIntent(context)
                                        if (intent != null) {
                                            context.startActivity(intent)
                                            viewModel.recordSentRequest(message)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "INVIA AL MEDICO ($selectedCount)",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                }
                            }
                        }
                    } else {
                        // History Screen
                        if (sentRequests.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = GrayBorder)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Nessuna richiesta inviata.", color = Slate600)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                items(sentRequests) { request ->
                                    HistoryItem(
                                        request = request,
                                        onView = { viewingRequestText = request.testoCompleto },
                                        onDelete = { viewModel.deleteHistoryItem(request) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Settings Overlay
            if (showSettings) {
                Dialog(onDismissRequest = { /* BackHandler handles this inside content */ }) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = GrayBackground,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        SettingsPanelContent(
                            settings = settings,
                            medications = medications,
                            viewModel = viewModel,
                            onClose = { showSettings = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationItem(
    med: Medication,
    isSelected: Boolean,
    requestedQuantity: Int,
    notificationsEnabled: Boolean,
    onQuantityChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onQuantityChange(if (isSelected) 0 else med.scatole) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GreenLightBg else White
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) GreenPrimary else GrayBorder
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = med.nome,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                if (med.note.isNotBlank()) {
                    Text(
                        text = med.note,
                        fontSize = 13.sp,
                        color = Slate600
                    )
                }
                if (notificationsEnabled && med.notificaAttiva) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = GreenPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val unit = if (med.frequenzaTipo == "ORE") "h" else "gg"
                        val repeatText = if (med.frequenzaValore > 0) " (ogni ${med.frequenzaValore}$unit)" else ""
                        Text(
                            text = "Notifica: ${med.orarioNotifica}$repeatText",
                            fontSize = 12.sp,
                            color = GreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    IconButton(onClick = { if (requestedQuantity > 1) onQuantityChange(requestedQuantity - 1) }) {
                        Text("−", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    }
                    Text(
                        text = requestedQuantity.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { onQuantityChange(requestedQuantity + 1) }) {
                        Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(GrayBackground, CircleShape)
                            .border(2.dp, Slate600, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    request: com.example.data.model.SentRequest,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GrayBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.data,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Inviata a: ${request.medicoNome}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate900
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = request.getSentMedications().joinToString(", ") { "${it.nome} (${it.scatole})" },
                fontSize = 14.sp,
                color = Slate600,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onView,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GrayBackground),
                border = BorderStroke(1.dp, GrayBorder)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Slate600, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("VISUALIZZA TESTO INVIATO", color = Slate600, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanelContent(
    settings: PatientSettings,
    medications: List<Medication>,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }

    if (showHelp) {
        HelpDialog(type = "configurazione", onDismiss = { showHelp = false })
    }

    var pNome by remember { mutableStateOf(settings.pazienteNome) }
    var pCf by remember { mutableStateOf(settings.pazienteCf) }
    var mNome by remember { mutableStateOf(settings.medicoNome) }
    var mTel by remember { mutableStateOf(settings.medicoTelefono) }
    var mEmail by remember { mutableStateOf(settings.medicoEmail) }
    var secInd by remember { mutableStateOf(settings.secondoIndirizzo) }
    var msgTesta by remember { mutableStateOf(settings.messaggioTesta) }
    var msgCoda by remember { mutableStateOf(settings.messaggioCoda) }
    var valType by remember { mutableIntStateOf(settings.tipoInvio) }
    var nAttive by remember { mutableStateOf(settings.notificheAttive) }
    var nDesc by remember { mutableStateOf(settings.descrizioneNotifica) }

    var exitAttempted by remember { mutableStateOf(false) }

    val hasChanges = remember(settings, pNome, pCf, mNome, mTel, mEmail, secInd, msgTesta, msgCoda, valType, nAttive, nDesc) {
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
            Toast.makeText(context, "Modifiche non salvate. Premi ancora per uscire.", Toast.LENGTH_LONG).show()
            exitAttempted = true
        }
    }

    var showMedicationDialog by remember { mutableStateOf(false) }
    var medicationToEdit by remember { mutableStateOf<Medication?>(null) }

    if (showMedicationDialog) {
        MedicationEditorDialog(
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
                    if (cName.isNotBlank()) mNome = cName
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
                Text(text = "⚙️ Configurazione", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showHelp = true }, modifier = Modifier.background(GrayBackground, CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Aiuto", tint = Slate900)
                    }
                    IconButton(
                        onClick = { 
                            if (hasChanges && !exitAttempted) {
                                Toast.makeText(context, "Modifiche non salvate. Premi ancora per uscire.", Toast.LENGTH_LONG).show()
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
            item { Text(text = "1. Anagrafica Paziente e Medico", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Slate900) }
            item {
                OutlinedTextField(value = pNome, onValueChange = { pNome = it }, label = { Text("Nome e Cognome Paziente") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate900, unfocusedBorderColor = GrayBorder, focusedLabelColor = Slate900, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White), shape = RoundedCornerShape(12.dp))
            }
            item {
                OutlinedTextField(value = pCf, onValueChange = { pCf = it.uppercase() }, label = { Text("Codice Fiscale") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate900, unfocusedBorderColor = GrayBorder, focusedLabelColor = Slate900, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), shape = RoundedCornerShape(12.dp))
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = GreenLightBg), border = BorderStroke(1.2.dp, Slate900), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Importa Medico da Rubrica", fontWeight = FontWeight.Bold, color = Slate900)
                            Text("Seleziona dal telefono il contatto del medico", fontSize = 13.sp)
                        }
                        Button(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) pickContactLauncher.launch(null)
                            else permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) { Text("Scegli") }
                    }
                }
            }
            item { OutlinedTextField(value = mNome, onValueChange = { mNome = it }, label = { Text("Nome Medico") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate900, unfocusedBorderColor = GrayBorder, focusedLabelColor = Slate900, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White), shape = RoundedCornerShape(12.dp)) }
            item { OutlinedTextField(value = mTel, onValueChange = { mTel = it }, label = { Text("Cellulare Medico") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate900, unfocusedBorderColor = GrayBorder, focusedLabelColor = Slate900, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White), shape = RoundedCornerShape(12.dp)) }
            item { OutlinedTextField(value = mEmail, onValueChange = { mEmail = it }, label = { Text("Email Medico") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate900, unfocusedBorderColor = GrayBorder, focusedLabelColor = Slate900, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White), shape = RoundedCornerShape(12.dp)) }
            item { OutlinedTextField(value = secInd, onValueChange = { secInd = it }, label = { Text("Note recapito (Opzionale)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate900, unfocusedBorderColor = GrayBorder, focusedLabelColor = Slate900, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White), shape = RoundedCornerShape(12.dp)) }
            
            item {
                HorizontalDivider(color = GrayBorder)
                Text(text = "2. Opzioni Messaggio", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Slate900, modifier = Modifier.padding(top = 10.dp))
            }
            item { OutlinedTextField(value = msgTesta, onValueChange = { msgTesta = it }, label = { Text("Frase di Testa") }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate900, unfocusedBorderColor = GrayBorder, focusedLabelColor = Slate900, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White), shape = RoundedCornerShape(12.dp)) }
            item { OutlinedTextField(value = msgCoda, onValueChange = { msgCoda = it }, label = { Text("Frase di Coda") }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate900, unfocusedBorderColor = GrayBorder, focusedLabelColor = Slate900, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White), shape = RoundedCornerShape(12.dp)) }
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
                            onValueChange = { nDesc = it },
                            label = { Text("Descrizione Notifica") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate900, unfocusedBorderColor = GrayBorder, focusedLabelColor = Slate900, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
            item {
                val formsFilled = pNome.isNotBlank() && pCf.isNotBlank() && mNome.isNotBlank() && ((valType == 2 && mEmail.isNotBlank()) || (valType != 2 && mTel.isNotBlank()))
                if (hasChanges) {
                    Button(onClick = {
                        if (formsFilled) {
                            viewModel.savePatientSettings(PatientSettings(pazienteNome = pNome, pazienteCf = pCf, medicoNome = mNome, medicoTelefono = mTel, medicoEmail = mEmail, secondoIndirizzo = secInd, messaggioTesta = msgTesta, messaggioCoda = msgCoda, tipoInvio = valType, notificheAttive = nAttive, descrizioneNotifica = nDesc))
                            Toast.makeText(context, "Profilo salvato!", Toast.LENGTH_SHORT).show()
                            onClose()
                        } else Toast.makeText(context, "Riempi i campi obbligatori (Nome, CF, Medico e Recapito).", Toast.LENGTH_LONG).show()
                    }, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(16.dp)) {
                        Text("SALVA CONFIGURAZIONE", fontWeight = FontWeight.Bold, color = White)
                    }
                }
            }

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
                    Text("AGGIUNGI NUOVO FARMACO", fontWeight = FontWeight.Bold, color = White)
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
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ConfigurationMedicationRow(
    med: Medication,
    notificationsEnabled: Boolean,
    onEdit: () -> Unit,
    onToggleStandby: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (med.inPausa) Color(0xFFF1F5F9) else White),
        border = BorderStroke(1.2.dp, if (med.inPausa) GrayBorder else Slate900),
        modifier = Modifier.fillMaxWidth().clickable { onEdit() }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = med.nome, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (med.inPausa) Slate600 else Slate900, modifier = Modifier.alpha(if (med.inPausa) 0.8f else 1f))
                if (med.note.isNotBlank()) Text(text = med.note, fontSize = 13.sp, color = Slate600, modifier = Modifier.alpha(if (med.inPausa) 0.8f else 1f))
                Text(text = "N. scatole: ${med.scatole}", fontSize = 13.sp, color = Slate600, modifier = Modifier.alpha(if (med.inPausa) 0.8f else 1f))
                if (notificationsEnabled && med.notificaAttiva) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = GreenPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val unit = if (med.frequenzaTipo == "ORE") "h" else "gg"
                        val repeatText = if (med.frequenzaValore > 0) " (ogni ${med.frequenzaValore}$unit)" else ""
                        Text(text = "Notifica: ${med.orarioNotifica}$repeatText", fontSize = 12.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onToggleStandby, colors = ButtonDefaults.buttonColors(containerColor = if (med.inPausa) GreenPrimary else Color(0xFFCBD5E1)), shape = RoundedCornerShape(8.dp)) {
                    Text(text = if (med.inPausa) "Riattiva" else "Pausa", fontSize = 13.sp, color = if (med.inPausa) White else Slate900)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(44.dp).background(Color(0xFFFEE2E2), CircleShape)) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color.Red)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationEditorDialog(
    medication: Medication? = null,
    onDismiss: () -> Unit,
    onSave: (Medication) -> Unit,
    notificationsEnabled: Boolean
) {
    val context = LocalContext.current
    var nome by remember { mutableStateOf(medication?.nome ?: "") }
    var scatole by remember { mutableIntStateOf(medication?.scatole ?: 1) }
    var note by remember { mutableStateOf(medication?.note ?: "") }
    var notificaAttiva by remember { mutableStateOf(medication?.notificaAttiva ?: false) }
    var orarioNotifica by remember { mutableStateOf(medication?.orarioNotifica ?: "08:00") }
    var frequenzaValore by remember { mutableIntStateOf(medication?.frequenzaValore ?: 0) }
    var frequenzaTipo by remember { mutableStateOf(medication?.frequenzaTipo ?: "ORE") }

    var exitAttempted by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val hasChanges = remember(medication, nome, scatole, note, notificaAttiva, orarioNotifica, frequenzaValore, frequenzaTipo) {
        if (medication == null) {
            nome.isNotBlank() || scatole != 1 || note.isNotBlank() || notificaAttiva || orarioNotifica != "08:00" || frequenzaValore != 0
        } else {
            nome != medication.nome ||
                    scatole != medication.scatole ||
                    note != medication.note ||
                    notificaAttiva != medication.notificaAttiva ||
                    orarioNotifica != medication.orarioNotifica ||
                    frequenzaValore != medication.frequenzaValore ||
                    frequenzaTipo != medication.frequenzaTipo
        }
    }

    val handleDismiss = {
        if (hasChanges && !exitAttempted) {
            Toast.makeText(context, "Modifiche non salvate. Premi ancora la 'X' per uscire.", Toast.LENGTH_LONG).show()
            exitAttempted = true
        } else {
            onDismiss()
        }
    }

    if (showTimePicker) {
        val parts = orarioNotifica.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val tpState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    orarioNotifica = String.format("%02d:%02d", tpState.hour, tpState.minute)
                    showTimePicker = false
                }) {
                    Text("OK", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Annulla", color = Slate600)
                }
            }
        ) {
            TimePicker(state = tpState)
        }
    }

    Dialog(onDismissRequest = handleDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = GrayBackground),
            border = BorderStroke(1.dp, GrayBorder)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (medication == null) "Aggiungi Farmaco" else "Modifica Farmaco",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    IconButton(
                        onClick = handleDismiss,
                        modifier = Modifier.background(GrayDarker, CircleShape).size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Slate900, modifier = Modifier.size(20.dp))
                    }
                }

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome Farmaco") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Slate900,
                        unfocusedBorderColor = GrayBorder,
                        focusedLabelColor = Slate900,
                        unfocusedLabelColor = Slate600,
                        unfocusedContainerColor = White,
                        focusedContainerColor = White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("N. scatole:", fontWeight = FontWeight.Bold, color = Slate600)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { if (scatole > 1) scatole-- },
                            modifier = Modifier.size(36.dp).background(Slate900, RoundedCornerShape(8.dp))
                        ) {
                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                        }
                        Text(
                            text = scatole.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = { scatole++ },
                            modifier = Modifier.size(36.dp).background(GreenPrimary, RoundedCornerShape(8.dp))
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (es: dopo i pasti)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Slate900,
                        unfocusedBorderColor = GrayBorder,
                        focusedLabelColor = Slate900,
                        unfocusedLabelColor = Slate600,
                        unfocusedContainerColor = White,
                        focusedContainerColor = White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (notificationsEnabled) {
                    HorizontalDivider(color = GrayBorder.copy(alpha = 0.5f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = notificaAttiva,
                            onCheckedChange = { notificaAttiva = it },
                            colors = CheckboxDefaults.colors(checkedColor = GreenPrimary)
                        )
                        Text("Attiva Notifica", fontWeight = FontWeight.Bold, color = Slate900)
                    }

                    if (notificaAttiva) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Orario:", fontSize = 14.sp, color = Slate600)
                                Surface(
                                    onClick = { showTimePicker = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = White,
                                    border = BorderStroke(1.dp, GrayBorder)
                                ) {
                                    Text(
                                        orarioNotifica,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Ripetizione:", fontSize = 14.sp, color = Slate600)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(White, RoundedCornerShape(8.dp))
                                            .border(1.dp, GrayBorder, RoundedCornerShape(8.dp))
                                    ) {
                                        IconButton(
                                            onClick = { if (frequenzaValore > 0) frequenzaValore-- },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text("−", fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = if (frequenzaValore == 0) "0" else frequenzaValore.toString(),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(28.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        IconButton(
                                            onClick = { frequenzaValore++ },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text("+", fontWeight = FontWeight.Bold, color = GreenPrimary)
                                        }
                                    }
                                    
                                    if (frequenzaValore == 0) {
                                        Text("Nessuna ripetizione", fontSize = 14.sp, color = Slate600, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .background(White, RoundedCornerShape(8.dp))
                                                .border(1.dp, GrayBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            listOf("ORE", "GIORNI").forEach { tipo ->
                                                val selected = frequenzaTipo == tipo
                                                Surface(
                                                    modifier = Modifier
                                                        .clickable { frequenzaTipo = tipo }
                                                        .width(50.dp),
                                                    color = if (selected) GreenPrimary else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = if (tipo == "ORE") "Ore" else "GG",
                                                        modifier = Modifier.padding(vertical = 8.dp),
                                                        color = if (selected) White else Slate600,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (nome.isNotBlank()) {
                            onSave(
                                (medication ?: Medication(nome = nome)).copy(
                                    nome = nome.trim(),
                                    scatole = scatole,
                                    note = note.trim(),
                                    notificaAttiva = notificaAttiva,
                                    orarioNotifica = orarioNotifica,
                                    frequenzaValore = frequenzaValore,
                                    frequenzaTipo = frequenzaTipo
                                )
                            )
                        } else {
                            Toast.makeText(context, "Il nome è obbligatorio", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SALVA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun HelpDialog(type: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = GreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = when(type) { "richiesta" -> "Guida Richiesta"; "cronologia" -> "Guida Cronologia"; else -> "Guida Configurazione" }, fontWeight = FontWeight.Bold, color = Slate900)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when(type) {
                    "richiesta" -> {
                        HelpItem("Seleziona i farmaci cliccando sul loro nome.")
                        HelpItem("Regola il numero di scatole con i tasti + e -.")
                        HelpItem("Premi 'INVIA AL MEDICO' per inviare la richiesta.")
                        HelpItem("Riceverai notifiche negli orari impostati.")
                    }
                    "cronologia" -> {
                        HelpItem("Qui trovi lo storico delle richieste inviate.")
                        HelpItem("Usa 'VISUALIZZA' per leggere il testo completo.")
                        HelpItem("Puoi eliminare vecchie richieste con l'icona cestino.")
                    }
                    "configurazione" -> {
                        HelpItem("Inserisci il tuo Codice Fiscale per permettere al medico di emettere la ricetta elettronica.")
                        HelpItem("Usa il tasto 'Scegli' per importare i dati del medico direttamente dalla tua rubrica telefonica.")
                        HelpItem("Aggiungi i farmaci che usi abitualmente con il loro N. scatole.")
                        HelpItem("Puoi impostare notifiche ricorrenti (es. ogni 8 ore) per non dimenticare le assunzioni.")
                        HelpItem("Il sistema ri-programma automaticamente la notifica successiva dopo ogni conferma.")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Ho capito", color = GreenPrimary, fontWeight = FontWeight.Bold) } },
        shape = RoundedCornerShape(16.dp),
        containerColor = GrayBackground
    )
}

@Composable
fun HelpItem(text: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Text("• ", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 18.sp)
        Text(text = text, fontSize = 15.sp, color = Slate600)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = GrayBackground
    )
}

@Composable
fun PharmacyCross(modifier: Modifier = Modifier, color: Color = GreenPrimary) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.32f).background(color, RoundedCornerShape(percent = 25)))
        Box(modifier = Modifier.fillMaxWidth(0.32f).fillMaxHeight().background(color, RoundedCornerShape(percent = 25)))
    }
}

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

    var showExitConfirmation by remember { mutableStateOf(false) }

    val hasChanges = pNome != settings.pazienteNome ||
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

    BackHandler(enabled = hasChanges) {
        showExitConfirmation = true
    }

    var dNome by remember { mutableStateOf("") }
    var dScatole by remember { mutableIntStateOf(1) }
    var dNote by remember { mutableStateOf("") }
    var dNotifica by remember { mutableStateOf(false) }
    var dOrario by remember { mutableStateOf("08:00") }
    var dRipeti by remember { mutableIntStateOf(0) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editMedicationId by remember { mutableStateOf("") }
    var editMedicationName by remember { mutableStateOf("") }
    var editMedicationBoxes by remember { mutableIntStateOf(1) }
    var editMedicationNotes by remember { mutableStateOf("") }
    var editMedicationNotifica by remember { mutableStateOf(false) }
    var editMedicationOrario by remember { mutableStateOf("08:00") }
    var editMedicationRipeti by remember { mutableIntStateOf(0) }

    var showTimePicker by remember { mutableStateOf(false) }
    var isEditingTimeForAdd by remember { mutableStateOf(true) }

    if (showTimePicker) {
        val initialTime = if (isEditingTimeForAdd) dOrario else editMedicationOrario
        val parts = initialTime.split(":")
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
                    val formattedTime = String.format("%02d:%02d", tpState.hour, tpState.minute)
                    if (isEditingTimeForAdd) dOrario = formattedTime else editMedicationOrario = formattedTime
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

    if (showEditDialog) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrayBackground),
                border = BorderStroke(1.2.dp, Slate900)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Modifica Farmaco", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    
                    OutlinedTextField(
                        value = editMedicationName,
                        onValueChange = { editMedicationName = it },
                        label = { Text("Nome Farmaco", fontSize = 15.sp) },
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
                        Text("Scatole standard:", fontWeight = FontWeight.Bold, color = Slate600)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { if (editMedicationBoxes > 1) editMedicationBoxes-- },
                                modifier = Modifier.size(40.dp).background(Slate900, RoundedCornerShape(8.dp))
                            ) {
                                Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = White)
                            }
                            Text(text = editMedicationBoxes.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                            IconButton(
                                onClick = { editMedicationBoxes++ },
                                modifier = Modifier.size(40.dp).background(GreenPrimary, RoundedCornerShape(8.dp))
                            ) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = White)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editMedicationNotes,
                        onValueChange = { editMedicationNotes = it },
                        label = { Text("Note aggiuntive", fontSize = 15.sp) },
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

                    // Notifica Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = if (editMedicationNotifica) GreenPrimary else Slate600, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Attiva Notifica", color = Slate900, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = editMedicationNotifica,
                            onCheckedChange = { editMedicationNotifica = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = White,
                                checkedTrackColor = GreenPrimary,
                                uncheckedThumbColor = White,
                                uncheckedTrackColor = GrayBorder
                            )
                        )
                    }

                    if (editMedicationNotifica) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Orario:", fontWeight = FontWeight.Bold, color = Slate600)
                            Surface(
                                onClick = { isEditingTimeForAdd = false; showTimePicker = true },
                                shape = RoundedCornerShape(8.dp),
                                color = White,
                                border = BorderStroke(1.dp, GrayBorder)
                            ) {
                                Text(editMedicationOrario, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, color = Slate900)
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ripeti ogni (ore):", fontWeight = FontWeight.Bold, color = Slate600)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(GrayDarker, RoundedCornerShape(8.dp)).padding(2.dp)) {
                                IconButton(onClick = { if (editMedicationRipeti > 0) editMedicationRipeti = if (editMedicationRipeti == 1) 0 else editMedicationRipeti - 1 }, modifier = Modifier.size(36.dp)) {
                                    Text("−", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(50.dp)) {
                                    Text(if (editMedicationRipeti == 0) "Mai" else "${editMedicationRipeti}h", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                }
                                IconButton(onClick = { if (editMedicationRipeti < 12) editMedicationRipeti++ }, modifier = Modifier.size(36.dp)) {
                                    Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = GreenPrimary)
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showEditDialog = false }) { Text("Annulla") }
                        Button(
                            onClick = {
                                viewModel.updateMedication(Medication(id = editMedicationId, nome = editMedicationName, scatole = editMedicationBoxes, note = editMedicationNotes, notificaAttiva = editMedicationNotifica, orarioNotifica = editMedicationOrario, ripetiOgniOre = editMedicationRipeti))
                                showEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) { Text("Salva", color = White) }
                    }
                }
            }
        }
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Modifiche non salvate", fontWeight = FontWeight.Bold, color = Slate900) },
            text = { Text("Ci sono delle modifiche non salvate. Vuoi uscire comunque senza salvare?") },
            confirmButton = {
                TextButton(onClick = onClose) { Text("Esci senza salvare", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) { Text("Rimani qui") }
            },
            containerColor = GrayBackground
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
                    IconButton(onClick = { if (hasChanges) showExitConfirmation = true else onClose() }, modifier = Modifier.background(GrayBackground, CircleShape)) {
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
                        } else Toast.makeText(context, "Riempi i campi obbligatori.", Toast.LENGTH_LONG).show()
                    }, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(16.dp), enabled = formsFilled) {
                        Text("SALVA CONFIGURAZIONE", fontWeight = FontWeight.Bold, color = White)
                    }
                }
            }

            item {
                HorizontalDivider(color = GrayBorder)
                Text(text = "3. Gestore Rubrica Farmaci", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Slate900, modifier = Modifier.padding(top = 10.dp))
            }
            item {
                Card(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.2.dp, Slate900), colors = CardDefaults.cardColors(containerColor = GrayDarker), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Registra Nuovo Farmaco:", fontWeight = FontWeight.Bold, color = Slate900)
                        OutlinedTextField(value = dNome, onValueChange = { dNome = it }, label = { Text("Nome Farmaco") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate900, unfocusedBorderColor = GrayBorder, focusedLabelColor = Slate900, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White), shape = RoundedCornerShape(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Scatole standard:", fontWeight = FontWeight.Bold, color = Slate600)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { if (dScatole > 1) dScatole-- }, modifier = Modifier.size(44.dp).background(Slate900, RoundedCornerShape(8.dp))) { Text("−", fontSize = 24.sp, color = White) }
                                Text(dScatole.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                                IconButton(onClick = { dScatole++ }, modifier = Modifier.size(44.dp).background(GreenPrimary, RoundedCornerShape(8.dp))) { Text("+", fontSize = 24.sp, color = White) }
                            }
                        }
                        OutlinedTextField(value = dNote, onValueChange = { dNote = it }, label = { Text("Note (Opzionale)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Slate900, unfocusedBorderColor = GrayBorder, focusedLabelColor = Slate900, unfocusedLabelColor = Slate600, unfocusedContainerColor = White, focusedContainerColor = White), shape = RoundedCornerShape(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = dNotifica, onCheckedChange = { dNotifica = it }, colors = CheckboxDefaults.colors(checkedColor = GreenPrimary))
                                Text("Promemoria", fontWeight = FontWeight.Bold, color = Slate900)
                            }
                            if (dNotifica) {
                                Surface(
                                    onClick = { isEditingTimeForAdd = true; showTimePicker = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = White,
                                    border = BorderStroke(1.dp, GrayBorder)
                                ) {
                                    Text(dOrario, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, color = Slate900)
                                }
                            }
                        }
                        
                        if (dNotifica) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ripeti ogni (ore):", fontSize = 14.sp, color = Slate600)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(White, RoundedCornerShape(8.dp)).border(1.dp, GrayBorder, RoundedCornerShape(8.dp)).padding(2.dp)) {
                                    IconButton(onClick = { if (dRipeti > 0) dRipeti = if (dRipeti == 1) 0 else dRipeti - 1 }, modifier = Modifier.size(32.dp)) {
                                        Text("−", fontWeight = FontWeight.Bold)
                                    }
                                    Text(if (dRipeti == 0) "Mai" else "${dRipeti}h", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                                    IconButton(onClick = { if (dRipeti < 12) dRipeti++ }, modifier = Modifier.size(32.dp)) {
                                        Text("+", fontWeight = FontWeight.Bold, color = GreenPrimary)
                                    }
                                }
                            }
                        }

                        Button(onClick = {
                            if (dNome.isNotBlank()) {
                                viewModel.addMedication(dNome, dScatole, dNote, dNotifica, dOrario, dRipeti)
                                dNome = ""; dScatole = 1; dNote = ""; dNotifica = false; dOrario = "08:00"; dRipeti = 0
                                Toast.makeText(context, "Aggiunto!", Toast.LENGTH_SHORT).show()
                            } else Toast.makeText(context, "Nome obbligatorio!", Toast.LENGTH_SHORT).show()
                        }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("AGGIUNGI ALLA RUBRICA", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { Text("I Tuoi Farmaci Salvati:", fontWeight = FontWeight.Bold, color = Slate900, modifier = Modifier.padding(top = 8.dp)) }
            if (medications.isEmpty()) {
                item { Text("La rubrica è vuota.", color = Slate600, modifier = Modifier.padding(16.dp)) }
            } else {
                items(medications, key = { it.id }) { med ->
                    ConfigurationMedicationRow(
                        med = med,
                        onEdit = {
                            editMedicationId = med.id; editMedicationName = med.nome
                            editMedicationBoxes = med.scatole; editMedicationNotes = med.note
                            editMedicationNotifica = med.notificaAttiva; editMedicationOrario = med.orarioNotifica
                            editMedicationRipeti = med.ripetiOgniOre
                            showEditDialog = true
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
                Text(text = "Quantità standard: ${med.scatole}", fontSize = 13.sp, color = Slate600, modifier = Modifier.alpha(if (med.inPausa) 0.8f else 1f))
                if (med.notificaAttiva) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = GreenPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val repeatText = if (med.ripetiOgniOre > 0) " (ogni ${med.ripetiOgniOre}h)" else ""
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
                        HelpItem("Riceverai notifiche di promemoria negli orari impostati.")
                    }
                    "cronologia" -> {
                        HelpItem("Qui trovi lo storico delle richieste inviate.")
                        HelpItem("Usa 'VISUALIZZA' per leggere il testo completo.")
                        HelpItem("Puoi eliminare vecchie richieste con l'icona cestino.")
                    }
                    "configurazione" -> {
                        HelpItem("Inserisci il tuo Codice Fiscale per permettere al medico di emettere la ricetta elettronica.")
                        HelpItem("Usa il tasto 'Scegli' per importare i dati del medico direttamente dalla tua rubrica telefonica.")
                        HelpItem("Aggiungi i farmaci che usi abitualmente con le loro quantità standard.")
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

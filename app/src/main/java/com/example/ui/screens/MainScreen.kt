package com.example.ui.screens

import android.Manifest
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
val GreenLightBg = Color(0xFFF0FDF4)
val Slate900 = Color(0xFF0F172A)
val Slate600 = Color(0xFF475569)
val GrayBackground = Color(0xFFF8FAFC)
val GrayBorder = Color(0xFFE2E8F0)
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
            title = { Text("Dettaglio Messaggio", fontWeight = FontWeight.Bold) },
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
            containerColor = White
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
                            Icon(Icons.Default.HelpOutline, contentDescription = "Aiuto", tint = Slate600)
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
                        icon = { Icon(Icons.Default.EditNote, contentDescription = null) }
                    )
                    Tab(
                        selected = currentTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = { Text("CRONOLOGIA", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.History, contentDescription = null) }
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
                                color = Slate600
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
                                modifier = Modifier
                                    .padding(16.dp)
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
                Dialog(onDismissRequest = { /* BackHandler handles this */ }) {
                    Surface(modifier = Modifier.fillMaxSize(), color = White) {
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
                            .border(1.dp, GrayBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Empty circle
                    }
                }
            }
        }
    }
}

@Composable
fun LayoutGrid(
    columns: Int,
    rows: Int,
    mainSpacing: androidx.compose.ui.unit.Dp,
    crossSpacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content) { measurables, constraints ->
        val mainSpacingPx = mainSpacing.roundToPx()
        val crossSpacingPx = crossSpacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints) }
        
        var totalWidth = 0
        var totalHeight = 0
        val rowHeights = IntArray(rows) { 0 }
        
        val rows = placeables.chunked(columns)
        rows.forEachIndexed { rowIndex, row ->
            var rowWidth = 0
            row.forEach { placeable ->
                rowWidth += placeable.width + mainSpacingPx
                rowHeights[rowIndex] = maxOf(rowHeights[rowIndex], placeable.height)
            }
            totalWidth = maxOf(totalWidth, rowWidth - mainSpacingPx)
            totalHeight += rowHeights[rowIndex] + crossSpacingPx
        }
        totalHeight -= crossSpacingPx

        layout(
            width = maxOf(constraints.minWidth, totalWidth),
            height = maxOf(constraints.minHeight, totalHeight)
        ) {
            var currentY = 0
            rows.forEachIndexed { rowIndex, row ->
                var currentX = 0
                val rowHeight = rowHeights[rowIndex]
                row.forEach { placeable ->
                    placeable.placeRelative(currentX, currentY + (rowHeight - placeable.height) / 2)
                    currentX += placeable.width + mainSpacingPx
                }
                currentY += rowHeight + crossSpacingPx
            }
        }
    }
}


// -----------------------------------------------------------------------------------------------
// SETTINGS SCREEN OVERLAY (CONFIG PANEL + INVENTORY MANAGEMENT)
// -----------------------------------------------------------------------------------------------
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

    // Local profile fields
    var pNome by remember { mutableStateOf(settings.pazienteNome) }
    var pCf by remember { mutableStateOf(settings.pazienteCf) }
    var mNome by remember { mutableStateOf(settings.medicoNome) }
    var mTel by remember { mutableStateOf(settings.medicoTelefono) }
    var mEmail by remember { mutableStateOf(settings.medicoEmail) }
    var secInd by remember { mutableStateOf(settings.secondoIndirizzo) }
    var msgTesta by remember { mutableStateOf(settings.messaggioTesta) }
    var msgCoda by remember { mutableStateOf(settings.messaggioCoda) }
    var valType by remember { mutableStateOf(settings.tipoInvio) } // 0=WA, 1=SMS, 2=Email

    var showExitConfirmation by remember { mutableStateOf(false) }

    val hasChanges = pNome != settings.pazienteNome ||
            pCf != settings.pazienteCf ||
            mNome != settings.medicoNome ||
            mTel != settings.medicoTelefono ||
            mEmail != settings.medicoEmail ||
            secInd != settings.secondoIndirizzo ||
            msgTesta != settings.messaggioTesta ||
            msgCoda != settings.messaggioCoda ||
            valType != settings.tipoInvio

    BackHandler(enabled = hasChanges) {
        showExitConfirmation = true
    }

    // Local drug inventory fields
    var dNome by remember { mutableStateOf("") }
    var dScatole by remember { mutableStateOf(1) }
    var dNote by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf(false) }
    var editMedicationId by remember { mutableStateOf("") }
    var editMedicationName by remember { mutableStateOf("") }
    var editMedicationBoxes by remember { mutableStateOf(1) }
    var editMedicationNotes by remember { mutableStateOf("") }

    if (showEditDialog) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Modifica Farmaco", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    
                    OutlinedTextField(
                        value = editMedicationName,
                        onValueChange = { editMedicationName = it },
                        label = { Text("Nome Farmaco") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Scatole standard:",
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { if (editMedicationBoxes > 1) editMedicationBoxes-- }) {
                                Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Slate900)
                            }
                            Text(
                                text = editMedicationBoxes.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            IconButton(onClick = { editMedicationBoxes++ }) {
                                Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editMedicationNotes,
                        onValueChange = { editMedicationNotes = it },
                        label = { Text("Note aggiuntive") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Annulla")
                        }
                        Button(
                            onClick = {
                                viewModel.updateMedication(
                                    Medication(
                                        id = editMedicationId,
                                        nome = editMedicationName,
                                        scatole = editMedicationBoxes,
                                        note = editMedicationNotes
                                    )
                                )
                                showEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Text("Salva", color = White)
                        }
                    }
                }
            }
        }
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Modifiche non salvate") },
            text = { Text("Ci sono delle modifiche non salvate. Vuoi uscire comunque senza salvare?") },
            confirmButton = {
                TextButton(onClick = onClose) {
                    Text("Esci senza salvare", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Rimani qui")
                }
            }
        )
    }

    // Launcher device system Contacts importer
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
                            if (nameCol >= 0) {
                                cName = cursor.getString(nameCol) ?: ""
                            }

                            val idCol = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                            if (idCol >= 0) {
                                val contactId = cursor.getString(idCol)
                                
                                // Phone Query
                                val hasPhoneCol = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                                val hasPhone = if (hasPhoneCol >= 0) cursor.getInt(hasPhoneCol) else 0

                                if (hasPhone > 0) {
                                    resolver.query(
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        null,
                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                        arrayOf(contactId),
                                        null
                                    )?.use { pCursor ->
                                        if (pCursor.moveToFirst()) {
                                            val numCol = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                            if (numCol >= 0) {
                                                cPhone = pCursor.getString(numCol) ?: ""
                                            }
                                        }
                                    }
                                }

                                // Email Query
                                resolver.query(
                                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                                    arrayOf(contactId),
                                    null
                                )?.use { eCursor ->
                                    if (eCursor.moveToFirst()) {
                                        val emailCol = eCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                                        if (emailCol >= 0) {
                                            cEmail = eCursor.getString(emailCol) ?: ""
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (cName.isNotBlank()) mNome = cName
                    if (cPhone.isNotBlank()) {
                        // Normalize phone from spaces
                        mTel = cPhone.replace("\\s".toRegex(), "").replace("[^+0-9]".toRegex(), "")
                    }
                    if (cEmail.isNotBlank()) mEmail = cEmail
                } catch (e: Exception) {
                    Toast.makeText(context, "Errore nell'accesso ai contatti.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Permission launcher for READ_CONTACTS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                pickContactLauncher.launch(null)
            } else {
                Toast.makeText(context, "Il permesso contatti è necessario per importare il medico.", Toast.LENGTH_LONG).show()
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overlay Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️ Configurazione",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showHelp = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(GrayBackground, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Aiuto",
                            tint = Slate900,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (hasChanges) {
                                showExitConfirmation = true
                            } else {
                                onClose()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(GrayBackground, CircleShape)
                            .testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = Slate900,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            Divider(color = GrayBorder, modifier = Modifier.padding(top = 8.dp))
        }

        // Section 1: Paziente & Medico
        item {
            Text(
                text = "1. Anagrafica Paziente e Medico",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Inserisci qui le tue informazioni. I caratteri grandi garantiscono un inserimento facilitato.",
                fontSize = 14.sp,
                color = Slate600
            )
        }

        item {
            OutlinedTextField(
                value = pNome,
                onValueChange = { pNome = it },
                label = { Text("Nome e Cognome Paziente (Obbligatorio)", fontSize = 16.sp) },
                placeholder = { Text("es: Mario Rossi", fontSize = 15.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("patient_name_input"),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = Slate900),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GrayBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = pCf,
                onValueChange = { pCf = it.uppercase() },
                label = { Text("Codice Fiscale Paziente (Obbligatorio)", fontSize = 16.sp) },
                placeholder = { Text("es: RSSMRA50A01F205Z", fontSize = 15.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("patient_cf_input"),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = Slate900),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GrayBorder
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            // Contacts picker tool for system Doctor
            Card(
                colors = CardDefaults.cardColors(containerColor = GreenLightBg),
                border = BorderStroke(1.2.dp, GreenPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Importa Medico da Rubrica (Consigliato)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Seleziona dal telefono il contatto del medico",
                            fontSize = 13.sp,
                            color = Slate600
                        )
                    }
                    Button(
                        onClick = {
                            when (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)) {
                                PackageManager.PERMISSION_GRANTED -> pickContactLauncher.launch(null)
                                else -> permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("choose_contact_picker_button")
                    ) {
                        Text("Scegli", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = mNome,
                onValueChange = { mNome = it },
                label = { Text("Nome Medico Curante (Obbligatorio)", fontSize = 16.sp) },
                placeholder = { Text("es: Dott. Giovanni Bianchi", fontSize = 15.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("doctor_name_input"),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = Slate900),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GrayBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = mTel,
                onValueChange = { mTel = it },
                label = { Text("Cellulare Medico", fontSize = 16.sp) },
                placeholder = { Text("es: 3391234567", fontSize = 15.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("doctor_phone_input"),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = Slate900),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GrayBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = mEmail,
                onValueChange = { mEmail = it },
                label = { Text("Email Medico", fontSize = 16.sp) },
                placeholder = { Text("es: dottore@studio.it", fontSize = 15.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("doctor_email_input"),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = Slate900),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GrayBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = secInd,
                onValueChange = { secInd = it },
                label = { Text("Note di recapito predefinite (Opzionale)", fontSize = 15.sp) },
                placeholder = { Text("es: Spedire via email a nome@paziente.it", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("second_address_input"),
                textStyle = LocalTextStyle.current.copy(fontSize = 17.sp, color = Slate900),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GrayBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Section 2: Modelli Testo
        item {
            Divider(color = GrayBorder, modifier = Modifier.padding(top = 6.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "2. Frasi Prefissate (Opzioni Messaggio)",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            OutlinedTextField(
                value = msgTesta,
                onValueChange = { msgTesta = it },
                label = { Text("Inizio Messaggio (Frase di Testa)", fontSize = 15.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .testTag("header_template_input"),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Slate900),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GrayBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = msgCoda,
                onValueChange = { msgCoda = it },
                label = { Text("Fine Messaggio (Frase di Coda)", fontSize = 15.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .testTag("footer_template_input"),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Slate900),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GrayBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tipo Invio Predefinito:",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("WhatsApp", "SMS", "Email").forEachIndexed { index, label ->
                        val selected = valType == index
                        val color = if (selected) {
                            when(index) {
                                0 -> GreenPrimary
                                1 -> Slate900
                                else -> Color(0xFF6366F1) // Indigo/Violet color instead of Red
                            }
                        } else GrayBorder

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable { valType = index },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) color else White,
                            border = if (!selected) BorderStroke(1.dp, GrayBorder) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = if (selected) White else Slate600,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Trigger Save
        item {
            val formsFilled = pNome.isNotBlank() && pCf.isNotBlank() && mNome.isNotBlank() && (
                    (valType == 2 && mEmail.isNotBlank()) || (valType != 2 && mTel.isNotBlank())
                    )

            if (hasChanges) {
                Button(
                    onClick = {
                        if (formsFilled) {
                            viewModel.savePatientSettings(
                                PatientSettings(
                                    pazienteNome = pNome,
                                    pazienteCf = pCf,
                                    medicoNome = mNome,
                                    medicoTelefono = mTel,
                                    medicoEmail = mEmail,
                                    secondoIndirizzo = secInd,
                                    messaggioTesta = msgTesta,
                                    messaggioCoda = msgCoda,
                                    tipoInvio = valType
                                )
                            )
                            Toast.makeText(context, "Profilo salvato!", Toast.LENGTH_SHORT).show()
                            onClose()
                        } else {
                            Toast.makeText(context, "Riempi tutti i campi obbligatori per il tipo di invio scelto", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("save_settings_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        disabledContainerColor = GrayBorder
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = formsFilled
                ) {
                    Text("SALVA CONFIGURAZIONE", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                }
            }
        }

        // Section 3: Medication inventory controller
        item {
            Divider(color = GrayBorder, modifier = Modifier.padding(top = 10.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "3. Gestore Rubrica Farmaci",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Aggiungi o elimina i farmaci che assumi periodicamente.",
                fontSize = 14.sp,
                color = Slate600
            )
        }

        // Add form
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, GrayBorder),
                colors = CardDefaults.cardColors(containerColor = GrayBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Registra Nuovo Farmaco:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dNome,
                        onValueChange = { dNome = it },
                        label = { Text("Nome Farmaco (es: Cardioaspirina 100 mg)", fontSize = 15.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_drug_name"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Slate900),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = GrayBorder,
                            focusedContainerColor = White,
                            unfocusedContainerColor = White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Scatole standard:",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate600,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { if (dScatole > 1) dScatole -= 1 },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Slate900, RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Box(
                                modifier = Modifier.width(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dScatole.toString(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            }

                            IconButton(
                                onClick = { dScatole += 1 },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(GreenPrimary, RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dNote,
                        onValueChange = { dNote = it },
                        label = { Text("Note aggiuntive (es: compresse, da 1000mg)", fontSize = 15.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_drug_notes"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Slate900),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = GrayBorder,
                            focusedContainerColor = White,
                            unfocusedContainerColor = White
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (dNome.isNotBlank()) {
                                viewModel.addMedication(dNome, dScatole, dNote)
                                dNome = ""
                                dScatole = 1
                                dNote = ""
                                Toast.makeText(context, "Farmaco aggiunto!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Il nome è obbligatorio!", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("register_new_drug_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AGGIUNGI ALLA RUBRICA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                }
            }
        }

        // Saved medicines list header
        item {
            Text(
                text = "I Tuoi Farmaci Salvati:",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Medication row items within LazyColumn
        if (medications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "La tua rubrica farmaci è vuota.",
                        fontSize = 15.sp,
                        color = Slate600,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
                    items(medications, key = { it.id }) { med ->
                        ConfigurationMedicationRow(
                            med = med,
                            onEdit = {
                                editMedicationId = med.id
                                editMedicationName = med.nome
                                editMedicationBoxes = med.scatole
                                editMedicationNotes = med.note
                                showEditDialog = true
                            },
                            onToggleStandby = { viewModel.toggleMedicationStandby(med) },
                            onDelete = { viewModel.deleteMedication(med) }
                        )
                    }
        }

        // bottom spacer to guarantee list bottom accessibility spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
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
        border = BorderStroke(1.2.dp, if (med.inPausa) GrayBorder else GreenPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("settings_med_row_${med.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = med.nome,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (med.inPausa) Slate600 else Slate900,
                        modifier = Modifier.alpha(if (med.inPausa) 0.6f else 1f)
                    )
                }
                if (med.note.isNotBlank()) {
                    Text(
                        text = med.note,
                        fontSize = 13.sp,
                        color = Slate600,
                        modifier = Modifier.alpha(if (med.inPausa) 0.6f else 1f)
                    )
                }
                Text(
                    text = "Quantità standard: ${med.scatole}",
                    fontSize = 13.sp,
                    color = Slate600,
                    modifier = Modifier.padding(top = 2.dp).alpha(if (med.inPausa) 0.6f else 1f)
                )
            }

            // Quick Operations Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Standby Button: Metti in pausa / Riattiva
                Button(
                    onClick = onToggleStandby,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (med.inPausa) GreenPrimary else Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("pause_med_button_${med.id}")
                ) {
                    Text(
                        text = if (med.inPausa) "Riattiva" else "Pausa",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (med.inPausa) White else Slate900
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFEE2E2), CircleShape)
                        .testTag("delete_med_button_${med.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Elimina",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// Visual layout helper extension decorators
fun Modifier.shadowUnderline(): Modifier = this.border(
    width = 1.dp,
    color = Color(0xFFE2E8F0)
)

@Composable
fun HelpDialog(type: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = GreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when(type) {
                        "richiesta" -> "Guida Richiesta"
                        "cronologia" -> "Guida Cronologia"
                        else -> "Guida Configurazione"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when(type) {
                    "richiesta" -> {
                        HelpItem("Seleziona i farmaci cliccando sul loro nome.")
                        HelpItem("Regola il numero di scatole con i tasti che appaiono dopo la selezione.")
                        HelpItem("Premi 'INVIA AL MEDICO' per inviare la richiesta tramite il canale scelto (WA/SMS/Email).")
                    }
                    "cronologia" -> {
                        HelpItem("Qui trovi l'elenco di tutte le richieste inviate in passato.")
                        HelpItem("Premi 'VISUALIZZA TESTO INVIATO' per leggere i dettagli della richiesta.")
                        HelpItem("Il cestino elimina la singola voce dallo storico.")
                    }
                    "configurazione" -> {
                        HelpItem("Anagrafica: inserisci nome e codice fiscale del paziente.")
                        HelpItem("Medico: importa il contatto dalla rubrica o inseriscilo manualmente.")
                        HelpItem("Tipo Invio: scegli WhatsApp per un invio rapido e gratuito.")
                        HelpItem("Rubrica: aggiungi qui i farmaci che prendi di solito per trovarli pronti all'uso.")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Ho capito", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = White
    )
}

@Composable
fun HelpItem(text: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Text("• ", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 18.sp)
        Text(text = text, fontSize = 15.sp, color = Slate600)
    }
}

@Composable
fun PharmacyCross(modifier: Modifier = Modifier, color: Color = GreenPrimary) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Horizontal bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.32f)
                .background(color, RoundedCornerShape(percent = 25))
        )
        // Vertical bar
        Box(
            modifier = Modifier
                .fillMaxWidth(0.32f)
                .fillMaxHeight()
                .background(color, RoundedCornerShape(percent = 25))
        )
    }
}

package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Profile
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onDisclaimerAccepted: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isExiting by remember { mutableStateOf(false) }

    // Intercetta l'uscita e mostra la schermata di saluto
    BackHandler(enabled = !isExiting) {
        isExiting = true
    }

    // Gestisce la chiusura automatica dopo 3 secondi
    if (isExiting) {
        LaunchedEffect(Unit) {
            delay(3000)
            var currentContext = context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is Activity) {
                    currentContext.finish()
                    break
                }
                currentContext = currentContext.baseContext
            }
        }
    }

    val settings by viewModel.settings.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val selectedIds by viewModel.selectedMedicationIds.collectAsState()
    val selectedQuantities by viewModel.selectedQuantities.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val sentRequests by viewModel.sentRequests.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val donationCount by viewModel.donationCount.collectAsState()
    val onboardingShown by viewModel.onboardingShown.collectAsState()
    val disclaimerAccepted by viewModel.disclaimerAccepted.collectAsState()
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var helpType by remember { mutableStateOf<String?>(null) }
    var viewingRequestText by remember { mutableStateOf<String?>(null) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showTestPasswordDialog by remember { mutableStateOf(false) }
    var testPassword by remember { mutableStateOf("") }
    var isPasswordWrong by remember { mutableStateOf(false) }
    var isTestMode by remember { mutableStateOf(false) }
    var showQuickMessageDialog by remember { mutableStateOf(false) }
    var quickMessageText by remember { mutableStateOf("") }
    
    val backupStatus by viewModel.backupStatus.collectAsState()
    val showAutoRestorePrompt by viewModel.showAutoRestorePrompt.collectAsState()
    var showBackupRestoreDialog by remember { mutableStateOf(false) }

    // Effetto per chiudere automaticamente il dialogo in caso di successo senza errori
    LaunchedEffect(backupStatus) {
        if (backupStatus == "RIPRISTINO_OK") {
            showBackupRestoreDialog = false
            viewModel.clearBackupStatus()
        }
    }

    if (!disclaimerAccepted) {
        DisclaimerDialog(onAccept = { 
            viewModel.setDisclaimerAccepted(true)
            onDisclaimerAccepted()
        })
        return // Blocca il rendering del resto dell'interfaccia
    }

    if (!onboardingShown) {
        OnboardingDialog(onDismiss = { viewModel.setOnboardingShown(true) })
        return // Blocca la sequenza finché l'onboarding non viene superato
    }

    if (showAutoRestorePrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAutoRestorePrompt() },
            title = { Text("Ripristino Dati Rilevato", fontWeight = FontWeight.Bold, color = Slate900) },
            text = {
                Text(
                    "È stato trovato un file di backup nella cartella Download. Vuoi ripristinare i tuoi dati (profili, farmaci e cronologia) ora? Questo ti permetterà di saltare la configurazione iniziale.",
                    fontSize = 14.sp,
                    color = Slate600
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmAutoRestore(context) }) {
                    Text("RIPRISTINA ORA", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAutoRestorePrompt() }) {
                    Text("CONFIGURA MANUALMENTE", color = Slate600)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = White
        )
        return // Blocca la sequenza finché non si compie la scelta del ripristino dati
    }

    if (showDonationDialog) {
        DonationDialog(
            donationCount = donationCount,
            appName = "RichFarmaci",
            onDismiss = { 
                showDonationDialog = false
                isTestMode = false 
            },
            onConfirm = {
                showDonationDialog = false
                if (!isTestMode) {
                    viewModel.incrementDonationCount()
                }
                val baseUrl = if (isTestMode) "https://www.sandbox.paypal.com/cgi-bin/webscr" else "https://www.paypal.com/cgi-bin/webscr"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$baseUrl?cmd=_xclick&business=marco.gatti65@alice.it&amount=5.00&currency_code=EUR&item_name=Offerta%20Caffe%20RichFarmaci&solution_type=Sole&landing_page=Billing"))
                context.startActivity(intent)
                isTestMode = false
            }
        )
    }

    if (showTestPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showTestPasswordDialog = false },
            title = { Text("Accesso Modalità Test", fontWeight = FontWeight.Bold, color = Slate900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Inserisci la password per attivare i pagamenti di test (Sandbox).", fontSize = 14.sp, color = Slate600)
                    OutlinedTextField(
                        value = testPassword,
                        onValueChange = { 
                            testPassword = it
                            isPasswordWrong = false
                        },
                        label = { Text("Password") },
                        isError = isPasswordWrong,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = GrayBorder,
                            errorBorderColor = Color.Red
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (isPasswordWrong) {
                        Text(
                            text = "Password errata.",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (testPassword == "Nidama01") {
                        isTestMode = true
                        showTestPasswordDialog = false
                        testPassword = ""
                        isPasswordWrong = false
                        showDonationDialog = true
                    } else {
                        isPasswordWrong = true
                    }
                }) {
                    Text("ENTRA", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showTestPasswordDialog = false 
                    testPassword = ""
                    isPasswordWrong = false
                }) {
                    Text("ANNULLA", color = Slate600)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = White
        )
    }

    if (showQuickMessageDialog) {
        AlertDialog(
            onDismissRequest = { showQuickMessageDialog = false },
            title = { Text("Messaggio Veloce", fontWeight = FontWeight.Bold, color = Slate900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Invia una comunicazione rapida al medico (es. febbre, appuntamento). I tuoi dati verranno aggiunti automaticamente.", fontSize = 14.sp, color = Slate600)
                    OutlinedTextField(
                        value = quickMessageText,
                        onValueChange = { quickMessageText = it },
                        placeholder = { Text("Scrivi qui il tuo messaggio...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = GrayBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
                    confirmButton = {
                Button(
                    onClick = {
                        if (quickMessageText.isNotBlank()) {
                            val intent = viewModel.generateQuickMessageIntent(quickMessageText)
                            if (intent != null) {
                                context.startActivity(intent)
                                viewModel.recordSentRequest("MESSAGGIO VELOCE: $quickMessageText")
                                quickMessageText = ""
                                showQuickMessageDialog = false
                            }
                        }
                    },
                    enabled = quickMessageText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("INVIA", color = White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickMessageDialog = false }) {
                    Text("ANNULLA", color = Slate600)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = White
        )
    }

    if (helpType != null) {
        HelpDialog(type = helpType!!, onDismiss = { helpType = null })
    }

    if (showBackupRestoreDialog || (backupStatus != null && backupStatus != "RIPRISTINO_OK")) {
        AlertDialog(
            onDismissRequest = { 
                showBackupRestoreDialog = false
                viewModel.clearBackupStatus()
            },
            title = { Text("Backup & Ripristino Dati", fontWeight = FontWeight.Bold, color = Slate900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Gestisci i profili, farmaci e cronologia salvandoli o ripristinandoli dalla cartella pubblica Download come file 'RichFarmaci_Backup.json'.",
                        fontSize = 14.sp,
                        color = Slate600
                    )
                    if (backupStatus != null && backupStatus != "RIPRISTINO_OK") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = GrayBackground),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = backupStatus!!,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (backupStatus!!.startsWith("Errore")) Color.Red else GreenPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = { viewModel.exportBackup(context) }) {
                        Text("ESPORTA", color = GreenPrimary, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { viewModel.importBackup(context) }) {
                        Text("RIPRISTINA", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showBackupRestoreDialog = false
                    viewModel.clearBackupStatus()
                }) {
                    Text("CHIUDI", color = Slate600)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = White
        )
        return // Blocca il rendering del resto dell'interfaccia durante operazioni di backup/ripristino
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
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GrayBackground)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { showDonationDialog = true },
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showTestPasswordDialog = true
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "☕", fontSize = 22.sp)
                        }
                        IconButton(
                            onClick = { helpType = if (currentTab == 0) "richiesta" else "cronologia" },
                            modifier = Modifier.background(GrayBackground, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Aiuto", tint = Slate600)
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GrayBackground)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { viewModel.setShowSettings(true) },
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showBackupRestoreDialog = true
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Impostazioni", tint = Slate600)
                        }
                    }
                }

                if (isExiting) {
                    // Schermata di Saluto (mantiene la testata sopra)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Grazie per aver usato l'app!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Se ti è stata utile, consigliala a parenti ed amici.",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                textAlign = TextAlign.Center,
                                lineHeight = 30.sp
                            )
                            Spacer(modifier = Modifier.height(48.dp))
                            PharmacyCross(modifier = Modifier.size(64.dp))
                        }
                    }
                } else {
                    // Contenuto normale dell'app (Nascosto durante l'uscita)
                    ProfileContextSwitcher(
                        profiles = profiles,
                        activeProfile = activeProfile,
                        onProfileSelected = { viewModel.selectProfile(it) }
                    )

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
                            text = { Text("Richiesta", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                            selectedContentColor = GreenPrimary,
                            unselectedContentColor = Slate600
                        )
                        Tab(
                            selected = currentTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            text = { Text("Cronologia", fontWeight = FontWeight.Bold) },
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
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 160.dp)
                                ) {
                                    items(medications.filter { !it.inPausa && it.scatole > 0 }) { med ->
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

                            // Bottom Actions
                            val selectedCount = selectedIds.size
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AnimatedVisibility(
                                    visible = selectedCount > 0,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
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
                                            text = "INVIA RICHIESTA AL MEDICO ($selectedCount)",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = White
                                        )
                                    }
                                }

                                // Tasto Messaggio Veloce (Visibile solo se non ci sono farmaci selezionati)
                                AnimatedVisibility(
                                    visible = selectedCount == 0,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Button(
                                        onClick = { showQuickMessageDialog = true },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GreenPrimary,
                                            contentColor = White
                                        )
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("MESSAGGIO VELOCE AL MEDICO", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                            onClose = { viewModel.setShowSettings(false) }
                        )
                    }
                }
            }
        }
    }

    if (showAddProfileDialog) {
        AlertDialog(
            onDismissRequest = { showAddProfileDialog = false },
            title = { Text("Nuovo Profilo", fontWeight = FontWeight.Bold, color = Slate900) },
            text = {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
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
                    Text("ANNULLA", color = Slate600)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = White
        )
    }
}


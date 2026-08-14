package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Profile
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
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
    var showQuickMessageDialog by remember { mutableStateOf(false) }
    var quickMessageText by remember { mutableStateOf("") }

    if (showQuickMessageDialog) {
        // ... (existing code for quick message)
    }

    if (!disclaimerAccepted) {
        DisclaimerDialog(onAccept = { viewModel.setDisclaimerAccepted(true) })
    } else if (!onboardingShown) {
        OnboardingDialog(onDismiss = { viewModel.setOnboardingShown(true) })
    }

    if (showDonationDialog) {
        val isBlocked = donationCount >= 2
        val hasWarning = donationCount == 1

        AlertDialog(
            onDismissRequest = { showDonationDialog = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isBlocked) "Grazie di cuore! ☕" else "Offri un caffè ☕",
                        fontWeight = FontWeight.Bold, 
                        fontSize = 20.sp, 
                        color = Slate900
                    )
                }
            },
            text = {
                Column {
                    if (hasWarning) {
                        Text(
                            "ATTENZIONE: Hai già sostenuto il progetto in precedenza.\n",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = if (isBlocked) 
                            "Hai già sostenuto il progetto il numero massimo di volte. Ti ringraziamo immensamente per il tuo supporto!"
                            else "Sostieni lo sviluppo di RichFarmaci con un contributo di 5€ per un buon caffè.\n\nVerrai reindirizzato su una pagina sicura gestita da PayPal dove potrai scegliere:\n• Se hai un account PayPal, usalo per procedere velocemente.\n• Se NON hai un account, potrai procedere comodamente con la tua carta di credito o prepagata cliccando su 'Paga con una carta'.",
                        fontSize = 16.sp,
                        color = Slate900
                    )
                }
            },
            confirmButton = {
                if (!isBlocked) {
                    Button(
                        onClick = {
                            showDonationDialog = false
                            viewModel.incrementDonationCount()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_xclick&business=marco.gatti65@alice.it&amount=5.00&currency_code=EUR&item_name=Offerta%20Caffe%20RichFarmaci&solution_type=Sole&landing_page=Billing"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SOSTIENI CON 5€", color = White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDonationDialog = false },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(if (isBlocked) "CHIUDI" else "ANNULLA", color = Slate600)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = White
        )
    }

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
                            onClick = { showDonationDialog = true },
                            modifier = Modifier.background(GrayBackground, CircleShape)
                        ) {
                            Text(text = "☕", fontSize = 20.sp)
                        }
                        IconButton(
                            onClick = { helpType = if (currentTab == 0) "richiesta" else "cronologia" },
                            modifier = Modifier.background(GrayBackground, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Aiuto", tint = Slate600)
                        }
                        IconButton(
                            onClick = { viewModel.setShowSettings(true) },
                            modifier = Modifier.background(GrayBackground, CircleShape)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Impostazioni", tint = Slate600)
                        }
                    }
                }

                // Profile Selector Context Switcher
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
                                        text = "INVIA AL MEDICO ($selectedCount)",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                }
                            }

                            // Tasto Messaggio Veloce (Sempre visibile in fondo)
                            OutlinedButton(
                                onClick = { showQuickMessageDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GreenPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = White.copy(alpha = 0.9f),
                                    contentColor = GreenPrimary
                                )
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("MESSAGGIO VELOCE AL MEDICO", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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


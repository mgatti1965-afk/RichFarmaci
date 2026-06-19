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
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Medication
import com.example.data.model.PatientSettings
import com.example.data.model.SentMedication
import com.example.data.model.SentRequest
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(activityApplication: Application) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(activityApplication)
    )

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()
    val isConfigured by viewModel.isConfigured.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val sentRequests by viewModel.sentRequests.collectAsStateWithLifecycle()

    val selectedIds by viewModel.selectedMedicationIds.collectAsStateWithLifecycle()
    val selectedQuants by viewModel.selectedQuantities.collectAsStateWithLifecycle()
    val selectedNotes by viewModel.selectedNotes.collectAsStateWithLifecycle()

    var showSendModal by remember { mutableStateOf(false) }
    var modalMessageText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PharmacyCross(
                            modifier = Modifier.size(26.dp),
                            color = GreenPrimary
                        )
                        Text(
                            text = "RichFarmaci",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Slate900
                        )
                    }
                },
                actions = {
                    SettingsButtonWithAlert(
                        isConfigured = isConfigured,
                        onClick = { viewModel.setShowSettings(true) }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White,
                    titleContentColor = Slate900
                ),
                modifier = Modifier.shadowUnderline()
            )
        },
        bottomBar = {
            if (!showSettings) {
                BottomNavigationBar(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
        },
        containerColor = GrayBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showSettings) {
                // Settings Screen Panel as full screen depth overlay
                SettingsPanelContent(
                    settings = settings,
                    medications = medications,
                    viewModel = viewModel,
                    onClose = { viewModel.setShowSettings(false) }
                )
            } else {
                when (currentTab) {
                    0 -> RequestTabContent(
                        isConfigured = isConfigured,
                        medications = medications,
                        selectedIds = selectedIds,
                        selectedQuants = selectedQuants,
                        selectedNotes = selectedNotes,
                        viewModel = viewModel,
                        onOpenSettings = { viewModel.setShowSettings(true) },
                        onPrepareRequest = {
                            modalMessageText = viewModel.buildFormattedMessage()
                            showSendModal = true
                        }
                    )
                    1 -> HistoryTabContent(
                        sentRequests = sentRequests,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Message Preview Modal Dialog
    if (showSendModal) {
        Dialog(
            onDismissRequest = { showSendModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, GreenPrimary, RoundedCornerShape(24.dp)),
                color = White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Anteprima Richiesta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Slate900
                        )
                        IconButton(onClick = { showSendModal = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = Slate600,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = GrayBorder
                    )

                    // Verbatim message body
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(GrayBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, GrayBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = modalMessageText,
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp,
                                    color = Slate900,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Button based on configuration
                    Button(
                        onClick = {
                            val tel = settings.medicoTelefono
                            if (settings.tipoInvio) {
                                // WhatsApp Direct Option
                                val escapedMsg = Uri.encode(modalMessageText)
                                val waUri = if (tel.isNotBlank()) {
                                    Uri.parse("https://api.whatsapp.com/send?phone=$tel&text=$escapedMsg")
                                } else {
                                    Uri.parse("https://api.whatsapp.com/send?text=$escapedMsg")
                                }
                                val intent = Intent(Intent.ACTION_VIEW, waUri)
                                try {
                                    context.startActivity(intent)
                                    viewModel.recordSentRequest(modalMessageText)
                                    showSendModal = false
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp non disponibile.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                // SMS Direct Option
                                val smsUri = Uri.parse("smsto:$tel")
                                val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                                    putExtra("sms_body", modalMessageText)
                                }
                                try {
                                    context.startActivity(intent)
                                    viewModel.recordSentRequest(modalMessageText)
                                    showSendModal = false
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Impossibile aprire SMS.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .testTag("send_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (settings.tipoInvio) GreenPrimary else Slate900
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (settings.tipoInvio) "Invia via WhatsApp" else "Invia via SMS",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsButtonWithAlert(isConfigured: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .testTag("settings_button_container")
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(GrayBackground, CircleShape)
                .testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configurazione",
                tint = Slate900,
                modifier = Modifier.size(32.dp)
            )
        }

        if (!isConfigured) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alphaAnim by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_alpha"
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .alpha(alphaAnim)
                    .background(OrangeAlert, CircleShape)
                    .border(2.dp, White, CircleShape)
                    .testTag("orange_pulse_dot")
            )
        }
    }
}

@Composable
fun BottomNavigationBar(currentTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = White,
        tonalElevation = 8.dp,
        modifier = Modifier.height(84.dp)
    ) {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            },
            label = {
                Text(
                    text = "1. Richiedi",
                    fontSize = 16.sp,
                    fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary,
                unselectedIconColor = Slate600,
                unselectedTextColor = Slate600,
                indicatorColor = GreenLightBg
            )
        )

        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            },
            label = {
                Text(
                    text = "2. Storico",
                    fontSize = 16.sp,
                    fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary,
                unselectedIconColor = Slate600,
                unselectedTextColor = Slate600,
                indicatorColor = GreenLightBg
            )
        )
    }
}

// -----------------------------------------------------------------------------------------------
// TAB 1: RICHIEDI (REQUEST FORM)
// -----------------------------------------------------------------------------------------------
@Composable
fun RequestTabContent(
    isConfigured: Boolean,
    medications: List<Medication>,
    selectedIds: Set<String>,
    selectedQuants: Map<String, Int>,
    selectedNotes: Map<String, String>,
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onPrepareRequest: () -> Unit
) {
    val activeMeds = medications.filter { !it.inPausa }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Visual banner repeating the pharmacy cross
        Card(
            colors = CardDefaults.cardColors(containerColor = White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, GrayBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFECFDF5), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    PharmacyCross(
                        modifier = Modifier.size(24.dp),
                        color = GreenPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Richiedi i Miei Farmaci",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Slate900
                    )
                    Text(
                        text = "Genera messaggi pronti per il tuo medico",
                        fontSize = 14.sp,
                        color = Slate600
                    )
                }
            }
        }
        if (!isConfigured) {
            // Highly apparent warning Banner inviting user to setup
            Card(
                colors = CardDefaults.cardColors(containerColor = SandAlertBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(2.dp, OrangeAlert, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = OrangeAlert,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Configurazione incompleta!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Devi prima configurare il tuo Nome, Codice Fiscale, il nome e il cellulare del Medico per preparare le richieste.",
                        fontSize = 16.sp,
                        color = Slate600,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onOpenSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAlert),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text("Configura Ora ⚙️", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                }
            }
        }

        // Informative guidance
        Text(
            text = "Seleziona i farmaci che ti servono oggi:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (activeMeds.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PharmacyCross(
                        modifier = Modifier.size(64.dp),
                        color = GreenPrimary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nessun farmaco attivo in rubrica.",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aggiungi medicinali premendo in alto su Configura ⚙️",
                        fontSize = 16.sp,
                        color = Slate600,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeMeds) { med ->
                    val isSelected = selectedIds.contains(med.id)
                    val customQty = selectedQuants[med.id] ?: med.scatole
                    val customNote = selectedNotes[med.id] ?: ""

                    ActiveMedicationSelectionCard(
                        med = med,
                        isSelected = isSelected,
                        qty = customQty,
                        tempNote = customNote,
                        onToggle = { viewModel.toggleMedicationSelection(med) },
                        onQtyChange = { viewModel.setQuantityForMedication(med.id, it) },
                        onNoteChange = { viewModel.setNoteForMedication(med.id, it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Send Request trigger button
        val canPrepare = isConfigured && selectedIds.isNotEmpty()
        Button(
            onClick = { if (canPrepare) onPrepareRequest() },
            enabled = canPrepare,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenPrimary,
                disabledContainerColor = GrayBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .testTag("prepare_request_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (canPrepare) White else Slate600
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (selectedIds.isEmpty()) "SELEZIONA FARMACI" else "PREPARA RICHIESTA (${selectedIds.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canPrepare) White else Slate600
                )
            }
        }
    }
}

@Composable
fun ActiveMedicationSelectionCard(
    med: Medication,
    isSelected: Boolean,
    qty: Int,
    tempNote: String,
    onToggle: () -> Unit,
    onQtyChange: (Int) -> Unit,
    onNoteChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GreenLightBg else White
        ),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.8.dp,
            color = if (isSelected) GreenPrimary else GrayBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("medication_card_${med.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Large visual state checkbox or pill
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isSelected) GreenPrimary else Color.Transparent,
                            CircleShape
                        )
                        .border(2.5.dp, if (isSelected) GreenPrimary else Slate600, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selezionato",
                            tint = White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = med.nome,
                        fontSize = 20.sp, // Elderly text sizing
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    if (med.note.isNotBlank()) {
                        Text(
                            text = med.note,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate600,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Text(
                        text = "Standard: ${med.scatole} ${if (med.scatole == 1) "confezione" else "confezioni"}",
                        fontSize = 14.sp,
                        color = Slate600,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Expanding controls on selection
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                        .clickable(enabled = false) {} // Prevent click propagating to card selection toggle
                ) {
                    Divider(color = GrayBorder, modifier = Modifier.padding(bottom = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Quante scatole vuoi?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Decrement
                            IconButton(
                                onClick = { if (qty > 1) onQtyChange(qty - 1) },
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Slate900, RoundedCornerShape(12.dp))
                                    .testTag("qty_minus_button_${med.id}")
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.offset(y = (-2).dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(54.dp)
                                    .height(54.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = qty.toString(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900,
                                    modifier = Modifier.testTag("qty_display_${med.id}")
                                )
                            }

                            // Increment
                            IconButton(
                                onClick = { onQtyChange(qty + 1) },
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(GreenPrimary, RoundedCornerShape(12.dp))
                                    .testTag("qty_plus_button_${med.id}")
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.offset(y = (-1).dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // TextField transient/temporary notes
                    OutlinedTextField(
                        value = tempNote,
                        onValueChange = onNoteChange,
                        label = { Text("Note urgenti o temporanee (es: compresse mattina)", fontSize = 15.sp) },
                        placeholder = { Text("es: solo mezza scatola, urgenze", fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("temp_note_input_${med.id}"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Slate900),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = GrayBorder
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------------
// TAB 2: STORICO / CRONOLOGIA (HISTORY)
// -----------------------------------------------------------------------------------------------
@Composable
fun HistoryTabContent(
    sentRequests: List<SentRequest>,
    viewModel: MainViewModel
) {
    val context = LocalContext.current

    if (sentRequests.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Slate600
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "La cronologia è vuota.",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Qui vedrai l'elenco delle ricette richieste in passato.",
                    fontSize = 16.sp,
                    color = Slate600,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Cronologia Richieste Inviate:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sentRequests, key = { it.id }) { req ->
                    HistoryItemRow(
                        request = req,
                        settings = viewModel.settings.value,
                        onDelete = { viewModel.deleteHistoryItem(req) },
                        onRepeatRequest = {
                            val msgEncoded = Uri.encode(req.testoCompleto)
                            val tel = viewModel.settings.value.medicoTelefono
                            val tipoInvio = viewModel.settings.value.tipoInvio

                            if (tipoInvio) {
                                val waUri = if (tel.isNotBlank()) {
                                    Uri.parse("https://api.whatsapp.com/send?phone=$tel&text=$msgEncoded")
                                } else {
                                    Uri.parse("https://api.whatsapp.com/send?text=$msgEncoded")
                                }
                                val intent = Intent(Intent.ACTION_VIEW, waUri)
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp non disponibile.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                val smsUri = Uri.parse("smsto:$tel")
                                val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                                    putExtra("sms_body", req.testoCompleto)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Impossibile aprire SMS.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun HistoryItemRow(
    request: SentRequest,
    settings: com.example.data.model.PatientSettings,
    onDelete: () -> Unit,
    onRepeatRequest: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = BorderStroke(1.2.dp, GrayBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("history_item_${request.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Summary row
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // DateTime label
                Box(
                    modifier = Modifier
                        .background(Slate900, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = request.data,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFEE2E2), CircleShape)
                        .testTag("delete_history_item_${request.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Elimina",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Inviata a: Dott. ${request.medicoNome}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Wrapped medications list of chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val list = request.getSentMedications()
                val displayCount = 2
                val itemsToDisplay = list.take(displayCount)

                FlowRow(
                    mainAxisSpacing = 6.dp,
                    crossAxisSpacing = 6.dp
                ) {
                    itemsToDisplay.forEach { med ->
                        Box(
                            modifier = Modifier
                                .background(GreenLightBg, RoundedCornerShape(10.dp))
                                .border(1.dp, GreenPrimary, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "💊 ${med.nome} x${med.scatole}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                        }
                    }

                    if (list.size > displayCount) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+${list.size - displayCount}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                    }
                }
            }

            // Expanded Collapsible Area
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                ) {
                    Divider(color = GrayBorder, modifier = Modifier.padding(bottom = 10.dp))

                    Text(
                        text = "Testo della richiesta:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Slate600,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Verbatim box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrayBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, GrayBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = request.testoCompleto,
                            fontSize = 15.sp,
                            color = Slate900,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Resend based on config
                        Button(
                            onClick = onRepeatRequest,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (settings.tipoInvio) GreenPrimary else Slate900
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(54.dp)
                                .testTag("retry_request_${request.id}")
                        ) {
                            Text(
                                text = if (settings.tipoInvio) "Invia di nuovo WA" else "Invia di nuovo SMS",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }

                        // Copy To Clipboard
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("RichFarmaci_Message", request.testoCompleto)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Testo copiato!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("copy_text_${request.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copia", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                        }
                    }
                }
            }
        }
    }
}

// Simple FlowRow helper implementation for wrapping chips
@Composable
fun FlowRow(
    mainAxisSpacing: androidx.compose.ui.unit.Dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content = content) { measurables, constraints ->
        val mainSpacingPx = mainAxisSpacing.roundToPx()
        val crossSpacingPx = crossAxisSpacing.roundToPx()

        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()

        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)

            if (currentRowWidth + placeable.width > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                rowWidths.add(currentRowWidth - mainSpacingPx)
                rowHeights.add(currentRowHeight)

                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }

            currentRow.add(placeable)
            currentRowWidth += placeable.width + mainSpacingPx
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowWidths.add(currentRowWidth - mainSpacingPx)
            rowHeights.add(currentRowHeight)
        }

        val totalHeight = rowHeights.sum() + (rowHeights.size - 1).coerceAtLeast(0) * crossSpacingPx
        val totalWidth = rowWidths.maxOrNull() ?: 0

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanelContent(
    settings: PatientSettings,
    medications: List<Medication>,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    // Local profile fields
    var pNome by remember { mutableStateOf(settings.pazienteNome) }
    var pCf by remember { mutableStateOf(settings.pazienteCf) }
    var mNome by remember { mutableStateOf(settings.medicoNome) }
    var mTel by remember { mutableStateOf(settings.medicoTelefono) }
    var secInd by remember { mutableStateOf(settings.secondoIndirizzo) }
    var msgTesta by remember { mutableStateOf(settings.messaggioTesta) }
    var msgCoda by remember { mutableStateOf(settings.messaggioCoda) }
    var valType by remember { mutableStateOf(settings.tipoInvio) } // true = WA, false = SMS

    // Local drug inventory fields
    var dNome by remember { mutableStateOf("") }
    var dScatole by remember { mutableStateOf(1) }
    var dNote by remember { mutableStateOf("") }

    // Launcher device system Contacts importer
    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { uri ->
            if (uri != null) {
                var cName = ""
                var cPhone = ""
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
                                val hasPhoneCol = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                                val hasPhone = if (hasPhoneCol >= 0) cursor.getInt(hasPhoneCol) else 0

                                if (hasPhone > 0) {
                                    val phoneCursor = resolver.query(
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        null,
                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                        arrayOf(contactId),
                                        null
                                    )
                                    phoneCursor?.use { pCursor ->
                                        if (pCursor.moveToFirst()) {
                                            val numCol = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                            if (numCol >= 0) {
                                                cPhone = pCursor.getString(numCol) ?: ""
                                            }
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

                IconButton(
                    onClick = onClose,
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
                label = { Text("Cellulare Medico (Obbligatorio)", fontSize = 16.sp) },
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
                    .height(96.dp)
                    .testTag("header_template_input"),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Slate900),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GrayBorder
                ),
                maxLines = 3,
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
                    .height(96.dp)
                    .testTag("footer_template_input"),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Slate900),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GrayBorder
                ),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tipo Invio Predefinito:",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("SMS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Switch(
                        checked = valType,
                        onCheckedChange = { valType = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GreenPrimary,
                            checkedTrackColor = GreenLightBg,
                            uncheckedThumbColor = Slate600,
                            uncheckedTrackColor = GrayBorder
                        ),
                        modifier = Modifier.testTag("delivery_type_switch")
                    )
                    Text("WhatsApp", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
                }
            }
        }

        // Action Trigger Save
        item {
            val formsFilled = pNome.isNotBlank() && pCf.isNotBlank() && mNome.isNotBlank() && mTel.isNotBlank()
            Button(
                onClick = {
                    if (formsFilled) {
                        viewModel.savePatientSettings(
                            PatientSettings(
                                pazienteNome = pNome,
                                pazienteCf = pCf,
                                medicoNome = mNome,
                                medicoTelefono = mTel,
                                secondoIndirizzo = secInd,
                                messaggioTesta = msgTesta,
                                messaggioCoda = msgCoda,
                                tipoInvio = valType
                            )
                        )
                        Toast.makeText(context, "Profilo salvato!", Toast.LENGTH_SHORT).show()
                        onClose()
                    } else {
                        Toast.makeText(context, "Riempi tutti i campi obbligatori (*)", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("save_settings_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    disabledContainerColor = GrayBorder
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = formsFilled
            ) {
                Text("SALVA CONFIGURAZIONE", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
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
                            color = Slate600
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
    onToggleStandby: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (med.inPausa) Color(0xFFF1F5F9) else White),
        border = BorderStroke(1.2.dp, if (med.inPausa) GrayBorder else GreenPrimary),
        modifier = Modifier
            .fillMaxWidth()
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
                    if (med.inPausa) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PAUSA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate600
                            )
                        }
                    }
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


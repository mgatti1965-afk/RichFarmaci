package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontStyle
import java.text.SimpleDateFormat
import java.util.*
import com.example.data.model.Medication
import com.example.ui.theme.*

@Composable
fun NotificationPreview(
    orarioNotifica: String,
    frequenzaValore: Int,
    frequenzaTipo: String
) {
    val occurrences = remember(orarioNotifica, frequenzaValore, frequenzaTipo) {
        val list = mutableListOf<Long>()
        val orariList = orarioNotifica.split(",").filter { it.isNotBlank() }
        if (orariList.isEmpty()) return@remember emptyList<Date>()

        val now = System.currentTimeMillis()

        orariList.forEach { orario ->
            val calendar = Calendar.getInstance()
            val parts = orario.split(":")
            calendar.set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: 8)
            calendar.set(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            var nextTime = calendar.timeInMillis
            
            if (nextTime <= now) {
                if (frequenzaValore > 0) {
                    if (frequenzaTipo == "ORE") {
                        while (nextTime <= now) {
                            calendar.add(Calendar.HOUR_OF_DAY, frequenzaValore)
                            nextTime = calendar.timeInMillis
                        }
                    } else {
                        while (nextTime <= now) {
                            calendar.add(Calendar.DAY_OF_YEAR, frequenzaValore)
                            nextTime = calendar.timeInMillis
                        }
                    }
                }
                // Se frequenzaValore == 0 e l'orario è passato, rimarrà nel passato
            }
            
            if (nextTime > now) {
                list.add(nextTime)

                // Aggiungiamo ripetizioni solo se frequenzaValore > 0
                if (frequenzaValore > 0) {
                    repeat(4) {
                        if (frequenzaTipo == "ORE") {
                            calendar.add(Calendar.HOUR_OF_DAY, frequenzaValore)
                        } else {
                            calendar.add(Calendar.DAY_OF_YEAR, frequenzaValore)
                        }
                        list.add(calendar.timeInMillis)
                    }
                }
            }
        }
        
        list.distinct().sorted().take(5).map { Date(it) }
    }

    if (occurrences.isEmpty()) return

    val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALY)
    val dateFormat = SimpleDateFormat("dd MMM", Locale.ITALY)
    val dayFormat = SimpleDateFormat("EEE", Locale.ITALY)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(GrayDarker.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Prossime Notifiche:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Slate600,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            occurrences.forEach { date ->
                Column(
                    modifier = Modifier
                        .width(80.dp)
                        .background(White, RoundedCornerShape(8.dp))
                        .border(1.dp, GrayBorder, RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayFormat.format(date).replace(".", "").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = dateFormat.format(date).replace(".", ""),
                        fontSize = 12.sp,
                        color = Slate900,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = timeFormat.format(date),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        if (frequenzaTipo == "ORE" && frequenzaValore > 0 && 24 % frequenzaValore != 0) {
            Text(
                text = "Nota: l'orario cambierà ogni giorno (deriva)",
                fontSize = 11.sp,
                color = Color.Red.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationEditorDialog(
    profileId: String,
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
    var frequenzaValore by remember { mutableIntStateOf(medication?.frequenzaValore ?: 1) }
    var frequenzaTipo by remember { mutableStateOf(medication?.frequenzaTipo ?: "GIORNI") }

    val isMultipleTimes = remember(orarioNotifica) { orarioNotifica.split(",").filter { it.isNotBlank() }.size > 1 }

    LaunchedEffect(isMultipleTimes) {
        if (isMultipleTimes && frequenzaTipo == "ORE") {
            frequenzaTipo = "GIORNI"
        }
    }

    var exitAttempted by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerTargetIndex by remember { mutableIntStateOf(-1) }
    var showWarningBanner by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showFullManual by remember { mutableStateOf(false) }

    if (showFullManual) {
        ManualDialog(onDismiss = { showFullManual = false })
    }

    LaunchedEffect(showWarningBanner) {
        if (showWarningBanner) {
            kotlinx.coroutines.delay(3500)
            showWarningBanner = false
        }
    }

    val hasChanges = remember(medication, nome, scatole, note, notificaAttiva, orarioNotifica, frequenzaValore, frequenzaTipo) {
        if (medication == null) {
            nome.isNotBlank() || scatole != 1 || note.isNotBlank() || notificaAttiva || orarioNotifica != "08:00" || frequenzaValore != 1 || frequenzaTipo != "GIORNI"
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
            showWarningBanner = true
            exitAttempted = true
        } else {
            onDismiss()
        }
    }

    if (showTimePicker) {
        val orariList = orarioNotifica.split(",").filter { it.isNotBlank() }.toMutableList()
        val targetTime = if (timePickerTargetIndex >= 0 && timePickerTargetIndex < orariList.size) {
            orariList[timePickerTargetIndex]
        } else "08:00"

        val parts = targetTime.split(":")
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
                    val newTime = String.format("%02d:%02d", tpState.hour, tpState.minute)
                    if (timePickerTargetIndex >= 0 && timePickerTargetIndex < orariList.size) {
                        orariList[timePickerTargetIndex] = newTime
                    } else {
                        orariList.add(newTime)
                    }
                    orarioNotifica = orariList.sorted().joinToString(",")
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

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = GreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guida Notifiche", fontWeight = FontWeight.Bold, color = Slate900)
                }
            },
            text = {
                val helpScroll = rememberScrollState()
                Column(
                    modifier = Modifier.verticalScroll(helpScroll).heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HelpItem("ORARI MULTIPLI: Puoi aggiungere più orari di assunzione. Usa il tasto '+' per aggiungerne uno nuovo e la 'X' per rimuoverlo.")
                    HelpItem("RIPETIZIONE 0: Con valore a zero, la notifica scatterà SOLO per gli orari rimanenti di OGGI. Da domani il promemoria si fermerà.")
                    HelpItem("FREQUENZA: Se inserisci più orari, la ripetizione viene fissata in 'Giorni' per garantire precisione.")
                    HelpItem("ANTEPRIMA: Il box colorato in fondo mostra le prossime 5 notifiche. Verificalo sempre per confermare la tua scelta.")
                    HelpItem("PAUSA: Sospendi un farmaco per bloccare sia gli ordini che i promemoria senza perdere i dati.")
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showFullManual = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Manuale", color = White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showHelpDialog = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ho capito", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Dialog(onDismissRequest = handleDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
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
                        text = if (medication == null) "Nuovo Farmaco" else "Modifica Farmaco",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier.background(GrayDarker, CircleShape).size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Aiuto",
                                tint = Slate900,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = handleDismiss,
                            modifier = Modifier.background(GrayDarker, CircleShape).size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Slate900, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome Farmaco") },
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
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
                            onClick = { if (scatole > 0) scatole-- },
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
                    onValueChange = { note = it.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } },
                    label = { Text("Note (es: dopo i pasti)") },
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
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Orari:", fontSize = 14.sp, color = Slate600)
                                val orariList = orarioNotifica.split(",").filter { it.isNotBlank() }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    orariList.forEachIndexed { index, time ->
                                        Surface(
                                            onClick = { 
                                                timePickerTargetIndex = index
                                                showTimePicker = true 
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = White,
                                            border = BorderStroke(1.dp, GrayBorder)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    time,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Rimuovi",
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            val newList = orariList.toMutableList()
                                                            newList.removeAt(index)
                                                            orarioNotifica = newList.joinToString(",")
                                                        },
                                                    tint = Color.Red
                                                )
                                            }
                                        }
                                    }
                                    
                                    IconButton(
                                        onClick = { 
                                            timePickerTargetIndex = -1
                                            showTimePicker = true 
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(GreenPrimary, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Aggiungi", tint = White, modifier = Modifier.size(20.dp))
                                    }
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
                                            Text("−", fontWeight = FontWeight.Bold, color = Slate900)
                                        }
                                        Text(
                                            text = if (frequenzaValore == 0) "0" else frequenzaValore.toString(),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(28.dp),
                                            textAlign = TextAlign.Center,
                                            color = Slate900
                                        )
                                        IconButton(
                                            onClick = { frequenzaValore++ },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text("+", fontWeight = FontWeight.Bold, color = GreenPrimary)
                                        }
                                    }
                                    
                                    if (frequenzaValore == 0) {
                                        Text("Solo oggi (nessuna ripetizione)", fontSize = 14.sp, color = Slate600, fontStyle = FontStyle.Italic)
                                    } else {
                                        Text(
                                            text = if (frequenzaTipo == "ORE") "ore" else "gg",
                                            fontSize = 14.sp,
                                            color = Slate900,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(
                                            modifier = Modifier
                                                .background(White, RoundedCornerShape(8.dp))
                                                .border(1.dp, GrayBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            listOf("ORE", "GIORNI").forEach { tipo ->
                                                val selected = frequenzaTipo == tipo
                                                val enabled = !isMultipleTimes || tipo == "GIORNI"
                                                
                                                Surface(
                                                    modifier = Modifier
                                                        .clickable(enabled = enabled) { frequenzaTipo = tipo }
                                                        .width(50.dp),
                                                    color = if (selected) GreenPrimary else if (!enabled) GrayBorder.copy(alpha = 0.3f) else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = if (tipo == "ORE") "Ore" else "GG",
                                                        modifier = Modifier.padding(vertical = 8.dp),
                                                        color = if (selected) White else if (!enabled) GrayBorder else Slate600,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (isMultipleTimes) {
                                    Text("Con più orari la ripetizione è impostata in giorni.", fontSize = 11.sp, color = Slate600, fontStyle = FontStyle.Italic)
                                }
                            }
                            
                            NotificationPreview(
                                orarioNotifica = orarioNotifica,
                                frequenzaValore = frequenzaValore,
                                frequenzaTipo = frequenzaTipo
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (nome.isNotBlank()) {
                            onSave(
                                (medication ?: Medication(profileId = profileId, nome = nome)).copy(
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

        AnimatedVisibility(
            visible = showWarningBanner,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 20.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = Color.Red,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = "Modifiche non salvate. Premi ancora la 'X' per uscire.",
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
        containerColor = White
    )
}

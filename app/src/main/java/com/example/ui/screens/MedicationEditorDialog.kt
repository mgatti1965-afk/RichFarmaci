package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.data.model.Medication
import com.example.ui.theme.*

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

    var exitAttempted by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

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
                        text = if (medication == null) "Nuovo Farmaco" else "Modifica Farmaco",
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
                    onValueChange = { nome = it.uppercase() },
                    label = { Text("Nome Farmaco") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Slate900,
                        unfocusedBorderColor = GrayBorder,
                        focusedLabelColor = Slate900,
                        unfocusedLabelColor = Slate600,
                        unfocusedContainerColor = BlueInputBg,
                        focusedContainerColor = BlueInputBg
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
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
                        focusedBorderColor = Slate900,
                        unfocusedBorderColor = GrayBorder,
                        focusedLabelColor = Slate900,
                        unfocusedLabelColor = Slate600,
                        unfocusedContainerColor = BlueInputBg,
                        focusedContainerColor = BlueInputBg
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

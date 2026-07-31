package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Profile
import com.example.ui.theme.*

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
                        HelpItem("Scegli il paziente tramite la barra azzurra in alto (se ne gestisci più di uno).")
                        HelpItem("Seleziona i farmaci cliccando sul loro nome.")
                        HelpItem("Regola il numero di scatole con i tasti + e -.")
                        HelpItem("Premi 'Invia al Medico' per trasmettere l'ordine.")
                    }
                    "cronologia" -> {
                        HelpItem("Seleziona il paziente tramite la barra azzurra in alto per vederne lo storico specifico.")
                        HelpItem("Usa 'Visualizza' per leggere il testo completo del messaggio inviato.")
                        HelpItem("Puoi eliminare le vecchie richieste usando l'icona del cestino.")
                    }
                    "configurazione" -> {
                        HelpItem("Usa la barra azzurra superiore per aggiungere nuovi pazienti o eliminare quelli esistenti.")
                        HelpItem("Inserisci il Codice Fiscale del paziente selezionato per la ricetta elettronica.")
                        HelpItem("Configura i dati del medico (puoi importarli dalla rubrica con il tasto 'Scegli').")
                        HelpItem("Aggiungi i farmaci abituali (se imposti 0 scatole, il farmaco rimarrà in lista solo per le notifiche).")
                        HelpItem("Attiva le notifiche per ricordarti di assumere i farmaci.")
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
fun ProfileContextSwitcher(
    profiles: List<Profile>,
    activeProfile: Profile?,
    onProfileSelected: (String) -> Unit,
    onAddProfile: (() -> Unit)? = null,
    onDeleteProfile: ((Profile) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = BlueInputBg,
            border = BorderStroke(1.dp, GrayBorder),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = activeProfile?.pazienteNome ?: "Seleziona Profilo",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Slate600
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(White)
        ) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(profile.pazienteNome, color = Slate900, fontWeight = if(profile.id == activeProfile?.id) FontWeight.Bold else FontWeight.Normal)
                            if (onDeleteProfile != null && profiles.size > 1) {
                                IconButton(
                                    onClick = {
                                        onDeleteProfile(profile)
                                        expanded = false
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    },
                    onClick = {
                        onProfileSelected(profile.id)
                        expanded = false
                    }
                )
            }
            if (onAddProfile != null) {
                HorizontalDivider(color = GrayBorder.copy(alpha = 0.5f))
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = GreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aggiungi nuovo paziente", color = GreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    onClick = {
                        onAddProfile()
                        expanded = false
                    }
                )
            }
        }
    }
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
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.32f).background(color, RoundedCornerShape(percent = 25)))
        Box(modifier = Modifier.fillMaxWidth(0.32f).fillMaxHeight().background(color, RoundedCornerShape(percent = 25)))
    }
}

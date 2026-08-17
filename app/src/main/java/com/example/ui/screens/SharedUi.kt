package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Profile
import com.example.ui.theme.*

@Composable
fun DisclaimerDialog(onAccept: () -> Unit) {
    var showFullManual by remember { mutableStateOf(false) }

    // Forza la chiusura dell'app se si preme il tasto indietro senza accettare
    val activity = (androidx.compose.ui.platform.LocalContext.current as? android.app.Activity)
    BackHandler {
        activity?.finish()
    }

    if (showFullManual) {
        ManualContentDialog(onDismiss = { showFullManual = false })
    }

    AlertDialog(
        onDismissRequest = { },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Limitazione di Responsabilità", fontWeight = FontWeight.Bold, color = Slate900)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "L’applicazione RichFarmaci ha scopo puramente gestionale e di supporto logistico. Non costituisce un dispositivo medico e non sostituisce in alcun modo il parere, la diagnosi o il consiglio del medico curante.",
                    fontSize = 14.sp,
                    color = Slate600
                )
                Text(
                    "Lo sviluppatore non si assume alcuna responsabilità per:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate900
                )
                HelpItem("Errori, ritardi o mancate consegne dei messaggi (WhatsApp, SMS, Email).")
                HelpItem("Malfunzionamento delle notifiche (dovute a risparmio energetico o sistema operativo).")
                HelpItem("Errori nei dati (nomi farmaci, dosaggi, CF) inseriti dall'utente.")
                Text(
                    "L'utente è tenuto a verificare sempre l'effettivo invio delle richieste. L'uso dell'app avviene sotto la piena e consapevole responsabilità dell'utente.",
                    fontSize = 14.sp,
                    color = Slate600,
                    fontWeight = FontWeight.Medium
                )

                TextButton(
                    onClick = { showFullManual = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("CONSULTA IL MANUALE COMPLETO", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ACCETTO E PROSEGUO", color = White, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = White
    )
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
                        HelpItem("Scegli il paziente tramite la barra azzurra in alto (se ne gestisci più di uno).")
                        HelpItem("Seleziona i farmaci cliccando sul loro nome e regola le scatole con + e -.")
                        HelpItem("Premi 'Invia al Medico' per trasmettere l'ordine dei farmaci selezionati.")
                        HelpItem("In aggiunta, puoi usare 'Messaggio veloce al Medico' per comunicazioni extra (es. febbre o appuntamenti).")
                        HelpItem("Se desideri sostenere il progetto, clicca sull'icona ☕ in alto a destra.")
                    }
                    "cronologia" -> {
                        HelpItem("Le richieste sono visualizzate in ordine cronologico, dalla più recente alla più vecchia.")
                        HelpItem("Seleziona il paziente tramite la barra azzurra in alto per vederne lo storico specifico.")
                        HelpItem("Usa 'Visualizza' per leggere il testo completo del messaggio inviato.")
                        HelpItem("Puoi eliminare le vecchie richieste usando l'icona del cestino.")
                    }
                    "configurazione" -> {
                        HelpItem("Usa la barra azzurra superiore per gestire i profili (aggiungi o elimina).")
                        HelpItem("1. ANAGRAFICA: Inserisci i dati di Paziente e Medico (usa 'SCEGLI' per la rubrica).")
                        HelpItem("2. OPZIONI: Scegli il canale (WhatsApp/SMS/Email) e personalizza i saluti.")
                        HelpItem("3. FARMACI: Aggiungi la tua terapia. Clicca sul tasto '?' dentro la scheda farmaco per i dettagli su notifiche e ripetizioni.")
                        HelpItem("SALVATAGGIO: Il tasto diventa ROSSO se ci sono modifiche. Cliccalo per confermare.")
                    }
                }
            }
        },
        confirmButton = {
            var showFullManual by remember { mutableStateOf(false) }
            if (showFullManual) {
                ManualContentDialog(onDismiss = { showFullManual = false })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showFullManual = true }) {
                    Text("MANUALE", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text("Ho capito", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = GrayBackground
    )
}

@Composable
fun OnboardingDialog(onDismiss: () -> Unit) {
    var showFullManual by remember { mutableStateOf(false) }

    if (showFullManual) {
        ManualContentDialog(onDismiss = { showFullManual = false })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                PharmacyCross(modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Benvenuto in RichFarmaci! 🏥",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Configura il tuo profilo in pochi passi:",
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900
                )
                HelpItem("Inserisci Nome, Codice Fiscale e dati del Medico.")
                HelpItem("Scegli il metodo di invio e salva la configurazione.")
                HelpItem("Aggiungi i tuoi Farmaci e attiva i promemoria.")
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = GrayBorder.copy(alpha = 0.5f))
                
                Text(
                    "LIMITAZIONE DI RESPONSABILITÀ:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Red.copy(alpha = 0.7f)
                )
                Text(
                    "L'app ha scopo logistico e non sostituisce il medico. Lo sviluppatore non risponde di mancati invii o errori. L'uso è a tuo rischio.",
                    fontSize = 12.sp,
                    color = Slate600,
                    lineHeight = 16.sp
                )

                TextButton(
                    onClick = { showFullManual = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("CONSULTA IL MANUALE COMPLETO", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ACCETTO E INIZIAMO", color = White, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = White
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
fun ManualContentDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                PharmacyCross(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Guida all'Uso RichFarmaci", fontWeight = FontWeight.ExtraBold, color = Slate900, fontSize = 22.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ManualSection(
                    title = "1. INSTALLAZIONE E SICUREZZA",
                    content = "Da WhatsApp: Clicca sul file e su 'Installa'. Da Email: Scarica l'allegato e clicca sulla notifica. Se appare 'App bloccata', clicca su 'Altre informazioni' e 'Installa comunque'. Autorizza sempre le notifiche al primo avvio.",
                    icon = { Icon(Icons.Default.Info, null, tint = GreenPrimary) }
                )

                ManualSection(
                    title = "2. MULTI-PROFILO",
                    content = "Gestisci più persone separatamente. Ogni profilo è un 'cassetto' isolato. Usa il tasto (+) per aggiungere un nuovo paziente. Cambia profilo cliccando sul nome in alto nella barra azzurra.",
                    icon = { Icon(Icons.Default.Group, null, tint = GreenPrimary) }
                )

                ManualSection(
                    title = "3. ANAGRAFICA E CONFIGURAZIONE",
                    content = "Inserisci dati Paziente e Medico (tasto 'SCEGLI' per rubrica). CONSIGLIO: Fai dei test con un numero di appoggio prima di inserire i dati reali del medico. Il tasto SALVA diventa ROSSO se ci sono modifiche.",
                    icon = { Icon(Icons.Default.Settings, null, tint = GreenPrimary) }
                )
                
                ManualSection(
                    title = "4. INVIO E CANALI",
                    content = "Scegli tra WhatsApp, SMS o Email (bordo verde). Personalizza frase di testa e coda. L'invio NON è immediato: potrai controllare il messaggio nel canale scelto prima di spedirlo.",
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, null, tint = GreenPrimary) }
                )

                ManualSection(
                    title = "5. FARMACI E NOTIFICHE",
                    content = "Aggiungi farmaci con quantità standard. Se le notifiche sono attive, imposta orari (anche multipli) e frequenza. Il tasto (?) nell'editor farmaco spiega come gestire orari e anteprime.",
                    icon = { Icon(Icons.Default.Add, null, tint = GreenPrimary) }
                )

                ManualSection(
                    title = "6. RICHIESTE E CRONOLOGIA",
                    content = "Seleziona i farmaci e premi 'INVIA AL MEDICO'. La Cronologia salva tutti gli invii effettuati in ordine cronologico.",
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, null, tint = GreenPrimary) }
                )
                
                ManualSection(
                    title = "7. BATTERIA E RESPONSABILITÀ",
                    content = "Imposta l'app su 'Senza restrizioni' nelle impostazioni batteria per notifiche affidabili. L'app è un supporto logistico, verifica sempre l'effettivo invio delle richieste.",
                    icon = { Icon(Icons.Default.Info, null, tint = Color.Red.copy(alpha = 0.7f)) },
                    isWarning = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Versione: 2.1.0\nSupporto tecnico: mgatt1965@gmail.com",
                    fontSize = 11.sp,
                    color = Slate600,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CHIUDI GUIDA", color = White, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = White
    )
}

@Composable
fun ManualSection(title: String, content: String, icon: @Composable () -> Unit, isWarning: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = if (isWarning) Color.Red.copy(alpha = 0.8f) else GreenPrimary, fontSize = 15.sp)
        }
        Text(content, fontSize = 14.sp, color = Slate600, lineHeight = 20.sp)
        HorizontalDivider(color = GrayBorder.copy(alpha = 0.3f), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun PharmacyCross(modifier: Modifier = Modifier, color: Color = GreenPrimary) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.32f).background(color, RoundedCornerShape(percent = 25)))
        Box(modifier = Modifier.fillMaxWidth(0.32f).fillMaxHeight().background(color, RoundedCornerShape(percent = 25)))
    }
}

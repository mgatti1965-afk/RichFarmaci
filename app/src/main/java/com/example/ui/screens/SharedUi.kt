package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        HelpItem("Seleziona i farmaci cliccando sul loro nome.")
                        HelpItem("Regola il numero di scatole con i tasti + e -.")
                        HelpItem("Premi 'Invia al Medico' per inviare la richiesta.")
                        HelpItem("Riceverai notifiche negli orari impostati.")
                    }
                    "cronologia" -> {
                        HelpItem("Qui trovi lo storico delle richieste inviate.")
                        HelpItem("Usa 'Visualizza' per leggere il testo completo.")
                        HelpItem("Puoi eliminare vecchie richieste con l'icona cestino.")
                    }
                    "configurazione" -> {
                        HelpItem("Inserisci il tuo Codice Fiscale per permettere al medico di emettere la ricetta elettronica.")
                        HelpItem("Usa il tasto 'Scegli' per importare i dati del medico direttamente dalla tua rubrica telefonica.")
                        HelpItem("Aggiungi i farmaci abituali. Nota: impostando 0 scatole, il farmaco non apparirà nell'elenco d'ordine. Questa opzione è ideale se vuoi usare l'app solo per le notifiche.")
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

@Composable
fun PharmacyCross(modifier: Modifier = Modifier, color: Color = GreenPrimary) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.32f).background(color, RoundedCornerShape(percent = 25)))
        Box(modifier = Modifier.fillMaxWidth(0.32f).fillMaxHeight().background(color, RoundedCornerShape(percent = 25)))
    }
}

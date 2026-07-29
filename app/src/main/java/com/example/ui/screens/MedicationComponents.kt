package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Medication
import com.example.data.model.SentRequest
import com.example.ui.theme.*

@Composable
fun MedicationItem(
    med: Medication,
    isSelected: Boolean,
    requestedQuantity: Int,
    notificationsEnabled: Boolean,
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
                if (notificationsEnabled && med.notificaAttiva) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = GreenPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val unit = if (med.frequenzaTipo == "ORE") "h" else "gg"
                        val repeatText = if (med.frequenzaValore > 0) " (ogni ${med.frequenzaValore}$unit)" else ""
                        Text(
                            text = "Notifica: ${med.orarioNotifica}$repeatText",
                            fontSize = 12.sp,
                            color = GreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
    request: SentRequest,
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
                Text("Visualizza testo inviato", color = Slate600, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ConfigurationMedicationRow(
    med: Medication,
    notificationsEnabled: Boolean,
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
                Text(text = "N. scatole: ${med.scatole}", fontSize = 13.sp, color = Slate600, modifier = Modifier.alpha(if (med.inPausa) 0.8f else 1f))
                if (notificationsEnabled && med.notificaAttiva) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = GreenPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val unit = if (med.frequenzaTipo == "ORE") "h" else "gg"
                        val repeatText = if (med.frequenzaValore > 0) " (ogni ${med.frequenzaValore}$unit)" else ""
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

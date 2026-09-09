package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Componente per la visualizzazione di testo Markdown avanzato.
 * Gestisce Titoli (#, ##, ###), Separatori (---) e Grassetto inline (**testo**).
 */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val lines = text.split("\n")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            when {
                // Livello 1: headlineMedium, Bold
                line.startsWith("# ") -> {
                    val content = line.substring(2)
                    Text(
                        text = content,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (content.contains("🔴")) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                // Livello 2: titleLarge, Bold
                line.startsWith("## ") -> {
                    val content = line.substring(3)
                    Text(
                        text = content,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (content.contains("🔴")) Color.Red else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                // Livello 3: titleMedium, Bold
                line.startsWith("### ") -> {
                    val content = line.substring(4)
                    Text(
                        text = content,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (content.contains("🔴")) Color.Red else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                // Separatore: HorizontalDivider
                line.startsWith("---") -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 2.dp)
                }
                // Testo normale con supporto Grassetto Inline
                else -> {
                    if (line.isNotBlank()) {
                        Text(
                            text = parseInlineMarkdown(line),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Converte il testo con sintassi **grassetto** in una AnnotatedString.
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        boldRegex.findAll(text).forEach { match ->
            // Aggiunge il testo prima del match
            append(text.substring(currentIndex, match.range.first))
            // Applica lo stile grassetto al contenuto tra gli asterischi
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            currentIndex = match.range.last + 1
        }
        // Aggiunge il testo rimanente
        if (currentIndex < text.length) append(text.substring(currentIndex))
    }
}

/**
 * Dialogo che integra il MarkdownText con scrolling verticale.
 */
@Composable
fun MarkdownAlertDialog(
    title: String,
    markdownContent: String,
    onDismiss: () -> Unit,
    confirmButtonText: String = "Ho capito"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp) // Limita l'altezza massima per favorire lo scroll
                    .verticalScroll(rememberScrollState())
            ) {
                MarkdownText(text = markdownContent)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmButtonText, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

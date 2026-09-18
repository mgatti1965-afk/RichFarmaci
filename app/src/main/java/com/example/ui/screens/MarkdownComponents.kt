package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Componente per la visualizzazione di testo Markdown avanzato con supporto per Indice cliccabile.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    // Mappa per salvare la posizione Y di ogni titolo per lo scrolling automatico
    val headerPositions = remember { mutableStateMapOf<String, Float>() }

    // Funzione per generare un ID unico (slug) dai titoli per il matching con l'indice
    fun normalizeAnchor(content: String): String {
        return content.lowercase()
            .replace("à", "a")
            .replace("è", "e")
            .replace("é", "e")
            .replace("ì", "i")
            .replace("ò", "o")
            .replace("ù", "u")
            .replace(Regex("[^a-z0-9\\s-]"), "") // Rimuove emoji e punteggiatura
            .trim()
            .replace(Regex("\\s+"), "-") // Sostituisce spazi con trattini
    }

    val lines = text.split("\n")
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            val annotated = parseInlineMarkdown(line)
            val hasLinks = annotated.getStringAnnotations("ANCHOR", 0, annotated.length).isNotEmpty() ||
                    annotated.getStringAnnotations("URL", 0, annotated.length).isNotEmpty()

            // Modifier per catturare la posizione dei titoli
            val onHeaderPositioned: (String) -> Modifier = { anchor ->
                Modifier.onGloballyPositioned { coords ->
                    headerPositions[anchor] = coords.positionInParent().y
                }
            }

            when {
                // --- TITOLI ---
                line.startsWith("# ") -> {
                    val content = line.substring(2)
                    Text(
                        text = content,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (content.contains("🔴")) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp).then(onHeaderPositioned(normalizeAnchor(content)))
                    )
                }
                line.startsWith("## ") -> {
                    val content = line.substring(3)
                    Text(
                        text = content,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (content.contains("🔴")) Color.Red else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).then(onHeaderPositioned(normalizeAnchor(content)))
                    )
                }
                line.startsWith("### ") -> {
                    val content = line.substring(4)
                    Text(
                        text = content,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (content.contains("🔴")) Color.Red else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp).then(onHeaderPositioned(normalizeAnchor(content)))
                    )
                }
                // --- SEPARATORE ---
                line.startsWith("---") -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
                // --- TESTO CON LINK / INDICE ---
                hasLinks -> {
                    val leadingSpaces = line.takeWhile { it == ' ' }.length
                    val isListItem = line.trim().startsWith("- ") || line.trim().startsWith("* ") || line.trim().matches(Regex("^\\d+\\.\\s.*"))
                    
                    ClickableText(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.padding(
                            start = if (isListItem) (16 + (leadingSpaces / 2) * 8).dp else 0.dp, 
                            top = 2.dp,
                            bottom = 2.dp
                        ),
                        onClick = { offset ->
                            // Se clicchi sulla riga, cerca l'ancora (link interno)
                            val anchor = annotated.getStringAnnotations("ANCHOR", offset, offset).firstOrNull()
                                ?: annotated.getStringAnnotations("ANCHOR", 0, annotated.length).firstOrNull()
                            
                            anchor?.let { annotation ->
                                val target = normalizeAnchor(annotation.item) // Normalizziamo anche il target
                                // Matching flessibile tra link dell'indice e titoli trovati
                                val match = headerPositions.keys.firstOrNull { 
                                    it == target || it.contains(target) || target.contains(it) 
                                }
                                match?.let { key ->
                                    headerPositions[key]?.let { y ->
                                        coroutineScope.launch { 
                                            // Scrolla fino alla posizione Y del titolo (con un piccolo margine)
                                            scrollState?.animateScrollTo(maxOf(0, y.toInt() - 20)) 
                                        }
                                    }
                                }
                            }
                            
                            // Gestione link esterni (URL)
                            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                        }
                    )
                }
                // --- TESTO NORMALE ---
                else -> {
                    if (line.isNotBlank()) {
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
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
 * Converte il testo Markdown in AnnotatedString, gestendo **grassetto** e [Link](#anchor).
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    val linkRegex = Regex("\\[(.*?)\\]\\((.*?)\\)")
    val boldRegex = Regex("\\*\\*(.*?)\\*\\*")

    return buildAnnotatedString {
        var currentIndex = 0
        val matches = (linkRegex.findAll(text) + boldRegex.findAll(text))
            .sortedBy { it.range.first }
            .toList()

        matches.forEach { match ->
            if (match.range.first >= currentIndex) {
                append(text.substring(currentIndex, match.range.first))

                val matchText = match.value
                if (matchText.startsWith("[")) {
                    val label = match.groupValues[1]
                    val link = match.groupValues[2]
                    if (link.startsWith("#")) {
                        pushStringAnnotation(tag = "ANCHOR", annotation = link.substring(1))
                    } else {
                        pushStringAnnotation(tag = "URL", annotation = link)
                    }
                    withStyle(style = SpanStyle(
                        color = GreenPrimary, 
                        fontWeight = FontWeight.Bold
                    )) {
                        append(label)
                    }
                    pop()
                } else {
                    val content = match.groupValues[1]
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(content)
                    }
                }
                currentIndex = match.range.last + 1
            }
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

/**
 * Dialogo generico per visualizzare testo Markdown.
 */
@Composable
fun MarkdownAlertDialog(
    title: String,
    markdownContent: String,
    onDismiss: () -> Unit,
    confirmButtonText: String = "Ho capito"
) {
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(scrollState)
            ) {
                MarkdownText(text = markdownContent, scrollState = scrollState)
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

/**
 * Dialogo specifico per il Manuale Utente.
 */
@Composable
fun ManualDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var manualText by remember { mutableStateOf("Caricamento in corso...") }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        manualText = try {
            context.assets.open("manuale.md").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            "Impossibile caricare il manuale utente."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PharmacyCross(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.manual_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .heightIn(max = 500.dp)
            ) {
                Box(modifier = Modifier.verticalScroll(scrollState)) {
                    MarkdownText(text = manualText, scrollState = scrollState)
                }

                // Freccia Torna Su (Stile GestBraccianti)
                AnimatedVisibility(
                    visible = scrollState.value > 150,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 8.dp, end = 8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(0)
                            }
                        },
                        containerColor = GreenPrimary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Torna in alto",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text(stringResource(R.string.btn_close).uppercase(), fontWeight = FontWeight.Bold, color = White)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = White
    )
}

@Composable
fun PharmacyCross(modifier: Modifier = Modifier, color: Color = GreenPrimary) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.32f).background(color, RoundedCornerShape(percent = 25)))
        Box(modifier = Modifier.fillMaxWidth(0.32f).fillMaxHeight().background(color, RoundedCornerShape(percent = 25)))
    }
}

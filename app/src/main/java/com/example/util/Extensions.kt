package com.example.util

fun String.capitalizeWords(): String =
    this.split(" ").joinToString(" ") { word ->
        if (word.isNotEmpty()) {
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else ""
    }

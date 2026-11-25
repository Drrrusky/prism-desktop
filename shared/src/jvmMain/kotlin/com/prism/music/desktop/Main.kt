package com.prism.music.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Prism"
    ) {
        MaterialTheme {
            App()
        }
    }
}

@Composable
private fun App() {
    Text("Prism Desktop")
}
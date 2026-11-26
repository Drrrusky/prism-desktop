package com.prism.music.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay

private enum class DesktopPage(val label: String) {
    Home("Accueil"),
    Search("Recherche"),
    Library("Bibliothèque"),
    Settings("Paramètres")
}

fun main() = application {
    Window(
        onCloseRequest = {
            DesktopPlayback.close()
            exitApplication()
        },
        title = "Prism"
    ) {
        MaterialTheme {
            PrismDesktopApp()
        }
    }
}

@Composable
private fun PrismDesktopApp() {
    var page by remember { mutableStateOf(DesktopPage.Home) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier.width(190.dp).fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("PRISM", style = MaterialTheme.typography.headlineMedium)
                    Text("Desktop", style = MaterialTheme.typography.labelLarge)
                    Divider()
                    DesktopPage.entries.forEach { destination ->
                        Button(onClick = { page = destination }, modifier = Modifier.fillMaxWidth()) {
                            Text(destination.label)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = DesktopPlayback::chooseAndAdd, modifier = Modifier.fillMaxWidth()) {
                        Text("Ajouter des fichiers")
                    }
                }
                Divider(modifier = Modifier.fillMaxSize().width(1.dp))
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize().padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(page.label, style = MaterialTheme.typography.headlineLarge)
                    when (page) {
                        DesktopPage.Home -> HomePage(onSearch = { page = DesktopPage.Search })
                        DesktopPage.Search -> SearchPage()
                        DesktopPage.Library -> LibraryPage()
                        DesktopPage.Settings -> SettingsPage()
                    }
                }
            }
            Divider()
            PlayerBar()
        }
    }
}

@Composable
private fun HomePage(onSearch: () -> Unit) {
    Text("Bienvenue dans Prism Desktop.")
    Text("Écoutez les fichiers audio de votre PC et gérez une file de lecture locale.")
    Button(onClick = onSearch) { Text("Rechercher de la musique") }
    Button(onClick = DesktopPlayback::chooseAndAdd) { Text("Ouvrir des fichiers audio") }
    if (DesktopPlayback.queue.isNotEmpty()) {
        Text("File de lecture : ${DesktopPlayback.queue.size} piste(s)")
        DesktopPlayback.currentTrack?.let { Text("En cours : ${it.name}") }
    }
    DesktopPlayback.errorMessage?.let { Text("Erreur : $it", color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun SearchPage() {
    var query by remember { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Titre ou nom de fichier") },
        singleLine = true
    )
    val matches = DesktopPlayback.queue.filter { it.name.contains(query, ignoreCase = true) }
    if (query.isBlank()) {
        Text("Recherche dans les fichiers déjà ajoutés à la file.")
    } else if (matches.isEmpty()) {
        Text("Aucun fichier correspondant dans la file.")
    } else {
        matches.forEach { file ->
            Button(onClick = { DesktopPlayback.playIndex(DesktopPlayback.queue.indexOf(file)) }) {
                Text(file.name)
            }
        }
    }
    Text("La recherche en ligne et le catalogue YouTube Music restent à intégrer.")
}

@Composable
private fun LibraryPage() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Fichiers de la file de lecture", modifier = Modifier.weight(1f))
        Button(onClick = DesktopPlayback::chooseAndAdd) { Text("Ajouter") }
    }
    if (DesktopPlayback.queue.isEmpty()) {
        Text("Aucun fichier ajouté. Utilisez « Ajouter » pour choisir vos fichiers audio.")
    } else {
        DesktopPlayback.queue.forEachIndexed { index, file ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { DesktopPlayback.playIndex(index) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text((if (index == DesktopPlayback.currentIndex) "▶ " else "") + file.name)
                }
                Button(onClick = { DesktopPlayback.remove(index) }) { Text("Retirer") }
            }
        }
    }
    DesktopPlayback.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun SettingsPage() {
    Text("Paramètres de Prism Desktop")
    Text("Le lecteur actuel lit les fichiers audio locaux pris en charge par Java Sound.")
    Text("La synchronisation, les comptes et les services en ligne ne sont pas encore disponibles sur Windows.")
}

@Composable
private fun PlayerBar() {
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(DesktopPlayback.isPlaying, DesktopPlayback.currentIndex) {
        while (true) {
            progress = DesktopPlayback.positionFraction()
            delay(250)
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Slider(
            value = progress,
            onValueChange = {
                progress = it
                DesktopPlayback.seekToFraction(it)
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(DesktopPlayback.currentTrack?.name ?: "Aucune piste")
                Text(
                    if (DesktopPlayback.isPlaying) "Lecture en cours" else "En pause",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = DesktopPlayback::previous, enabled = DesktopPlayback.queue.isNotEmpty()) {
                    Text("Précédent")
                }
                Button(
                    onClick = DesktopPlayback::playPause,
                    enabled = DesktopPlayback.queue.isNotEmpty() || !DesktopPlayback.isPlaying
                ) {
                    Text(if (DesktopPlayback.isPlaying) "Pause" else "Lecture")
                }
                Button(onClick = DesktopPlayback::next, enabled = DesktopPlayback.queue.isNotEmpty()) {
                    Text("Suivant")
                }
            }
        }
        DesktopPlayback.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

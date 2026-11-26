package com.prism.music.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay

private enum class DesktopPage(val label: String, val symbol: String) {
    Home("Accueil", "⌂"),
    Search("Recherche", "⌕"),
    Library("Bibliothèque", "♫"),
    Settings("Paramètres", "⚙")
}

fun main() = application {
    Window(
        onCloseRequest = {
            DesktopPlayback.close()
            exitApplication()
        },
        title = "Prism",
        resizable = true
    ) {
        MaterialTheme {
            PrismDesktopApp()
        }
    }
}

@Composable
private fun PrismDesktopApp() {
    var page by remember { mutableStateOf(DesktopPage.Home) }
    var searchQuery by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Sidebar(
                    selected = page,
                    onNavigate = { page = it },
                    onAdd = DesktopPlayback::chooseAndAdd
                )
                Divider(
                    modifier = Modifier.fillMaxHeight().width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(page.label, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Votre musique, votre espace.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (it.isNotBlank()) page = DesktopPage.Search
                            },
                            modifier = Modifier.width(280.dp),
                            singleLine = true,
                            label = { Text("Rechercher dans la bibliothèque") }
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    when (page) {
                        DesktopPage.Home -> HomePage(
                            onSearch = { page = DesktopPage.Search },
                            onLibrary = { page = DesktopPage.Library }
                        )
                        DesktopPage.Search -> SearchPage(searchQuery, onQueryChange = { searchQuery = it })
                        DesktopPage.Library -> LibraryPage()
                        DesktopPage.Settings -> SettingsPage()
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            PlayerBar()
        }
    }
}

@Composable
private fun Sidebar(
    selected: DesktopPage,
    onNavigate: (DesktopPage) -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier.width(220.dp).fillMaxHeight().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("PRISM", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("DESKTOP MUSIC", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        DesktopPage.entries.forEach { destination ->
            val label = "${destination.symbol}   ${destination.label}"
            if (selected == destination) {
                Button(onClick = { onNavigate(destination) }, modifier = Modifier.fillMaxWidth()) {
                    Text(label, modifier = Modifier.fillMaxWidth())
                }
            } else {
                TextButton(onClick = { onNavigate(destination) }, modifier = Modifier.fillMaxWidth()) {
                    Text(label, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Votre collection", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${DesktopPlayback.queue.size} piste(s) dans la file",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Text("＋ Ajouter des titres")
                }
            }
        }
    }
}

@Composable
private fun HomePage(onSearch: () -> Unit, onLibrary: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bienvenue dans Prism", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Retrouvez vos morceaux et lancez votre prochaine écoute.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = DesktopPlayback::chooseAndAdd) { Text("＋ Ajouter de la musique") }
                    Button(onClick = onLibrary) { Text("Ouvrir la bibliothèque") }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard("Titres", DesktopPlayback.queue.size.toString(), Modifier.weight(1f))
            SummaryCard("En lecture", DesktopPlayback.currentTrack?.name ?: "Aucune piste", Modifier.weight(2f))
            SummaryCard("État", if (DesktopPlayback.isPlaying) "Lecture" else "En pause", Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ajoutés récemment", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onLibrary) { Text("Voir la bibliothèque") }
        }
        if (DesktopPlayback.queue.isEmpty()) {
            EmptyState(
                title = "Votre bibliothèque est vide",
                description = "Ajoutez des fichiers audio depuis votre ordinateur pour commencer."
            )
        } else {
            DesktopPlayback.queue.take(6).forEach { file ->
                TrackRow(
                    title = file.nameWithoutExtension,
                    subtitle = file.parent ?: "Fichier local",
                    active = file == DesktopPlayback.currentTrack,
                    onPlay = { DesktopPlayback.playIndex(DesktopPlayback.queue.indexOf(file)) }
                )
            }
        }
        DesktopPlayback.errorMessage?.let { ErrorText(it) }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 2)
        }
    }
}

@Composable
private fun SearchPage(query: String, onQueryChange: (String) -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Titre, artiste ou nom de fichier") },
            singleLine = true
        )
        val matches = DesktopPlayback.queue.filter {
            it.name.contains(query, ignoreCase = true) || it.parent.orEmpty().contains(query, ignoreCase = true)
        }
        Text("${matches.size} résultat(s)", style = MaterialTheme.typography.titleMedium)
        if (query.isBlank()) {
            EmptyState("Rechercher votre musique", "La recherche parcourt les fichiers présents dans votre bibliothèque locale.")
        } else if (matches.isEmpty()) {
            EmptyState("Aucun résultat", "Essayez un autre titre ou ajoutez des fichiers à votre bibliothèque.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(matches) { _, file ->
                    TrackRow(
                        title = file.nameWithoutExtension,
                        subtitle = file.parent ?: "Fichier local",
                        active = file == DesktopPlayback.currentTrack,
                        onPlay = { DesktopPlayback.playIndex(DesktopPlayback.queue.indexOf(file)) }
                    )
                }
            }
        }
        Text(
            "La recherche en ligne et le catalogue musical connecté ne sont pas encore intégrés.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LibraryPage() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("${DesktopPlayback.queue.size} piste(s)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Button(onClick = DesktopPlayback::chooseAndAdd) { Text("＋ Ajouter") }
            Button(onClick = DesktopPlayback::importPlaylist) { Text("Importer M3U") }
            Button(onClick = DesktopPlayback::exportPlaylist, enabled = DesktopPlayback.queue.isNotEmpty()) { Text("Exporter") }
        }
        if (DesktopPlayback.queue.isEmpty()) {
            EmptyState("Aucun morceau pour le moment", "Ajoutez vos fichiers audio ou importez une playlist M3U.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(DesktopPlayback.queue.toList()) { index, file ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrackRow(
                            title = file.nameWithoutExtension,
                            subtitle = file.parent ?: "Fichier local",
                            active = index == DesktopPlayback.currentIndex,
                            onPlay = { DesktopPlayback.playIndex(index) },
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { DesktopPlayback.remove(index) }) { Text("Retirer") }
                    }
                }
            }
        }
        DesktopPlayback.errorMessage?.let { ErrorText(it) }
    }
}

@Composable
private fun TrackRow(
    title: String,
    subtitle: String,
    active: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.width(38.dp).height(38.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (active) "♫" else "♪", color = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Button(onClick = onPlay) { Text(if (active && DesktopPlayback.isPlaying) "En cours" else "Lire") }
        }
    }
}

@Composable
private fun EmptyState(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("♫", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsPage() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard("Lecture locale", "Lecture de fichiers audio stockés sur ce PC")
        SummaryCard("Formats", "WAV, MP3, AIFF et AU (selon les codecs disponibles)")
        SummaryCard("Playlists", "Import et export de playlists M3U / M3U8")
        SummaryCard("Données", "La file de lecture est enregistrée localement dans votre dossier utilisateur.")
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fonctionnalités à venir", style = MaterialTheme.typography.titleMedium)
                Text("Catalogue en ligne, comptes, synchronisation, pochettes et intégration des commandes multimédia Windows.")
            }
        }
    }
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
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 22.dp, vertical = 10.dp)
    ) {
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
            Column(Modifier.weight(1f)) {
                Text(DesktopPlayback.currentTrack?.nameWithoutExtension ?: "Prism — aucune piste", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    if (DesktopPlayback.isPlaying) "Lecture en cours" else "Prêt à écouter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = DesktopPlayback::previous, enabled = DesktopPlayback.queue.isNotEmpty()) { Text("⏮") }
                Button(
                    onClick = DesktopPlayback::playPause,
                    enabled = DesktopPlayback.queue.isNotEmpty() || !DesktopPlayback.isPlaying
                ) { Text(if (DesktopPlayback.isPlaying) "Ⅱ Pause" else "▶ Lecture") }
                Button(onClick = DesktopPlayback::next, enabled = DesktopPlayback.queue.isNotEmpty()) { Text("⏭") }
            }
        }
        DesktopPlayback.errorMessage?.let { ErrorText(it) }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

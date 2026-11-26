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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private enum class DesktopPage(val label: String) {
    Home("Accueil"),
    Search("Recherche"),
    Library("Bibliothèque"),
    Settings("Paramètres")
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Prism") {
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
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Column(
                    modifier = Modifier.width(190.dp).fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("PRISM", style = MaterialTheme.typography.headlineMedium)
                    Text("Desktop", style = MaterialTheme.typography.labelLarge)
                    Divider()
                    DesktopPage.entries.forEach { destination ->
                        Button(
                            onClick = { page = destination },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(destination.label)
                        }
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
    Text("La base de l’interface Windows est en place. Les fonctions musicales seront raccordées aux services desktop au fil du portage.")
    Button(onClick = onSearch) { Text("Rechercher de la musique") }
}

@Composable
private fun SearchPage() {
    var query by remember { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Titre, artiste ou album") },
        singleLine = true
    )
    Text(if (query.isBlank()) "Saisissez une recherche." else "Recherche prête : $query")
    Text("Les résultats YouTube Music et leur chargement seront ajoutés avec la couche réseau desktop.")
}

@Composable
private fun LibraryPage() {
    Text("Votre bibliothèque apparaîtra ici.")
    Text("L’accès à la base locale et l’import des playlists sont encore à connecter.")
}

@Composable
private fun SettingsPage() {
    Text("Paramètres de Prism Desktop")
    Text("Les préférences Windows et le stockage local seront implémentés dans les prochaines étapes.")
}

@Composable
private fun PlayerBar() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Aucune piste")
                Text("Lecteur Windows — moteur audio à connecter", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}, enabled = false) { Text("Précédent") }
                Button(onClick = {}, enabled = false) { Text("Lecture") }
                Button(onClick = {}, enabled = false) { Text("Suivant") }
            }
        }
    }
}

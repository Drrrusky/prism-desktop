package com.prism.music.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.prefs.Preferences
import java.nio.file.Files
import java.nio.file.Path
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Minimal desktop-local playback implementation. This is intentionally isolated
 * from Android Media3 and can later be replaced by a streaming-capable backend.
 */
object DesktopPlayback {
    val queue = mutableStateListOf<File>()
    var currentIndex by mutableStateOf(-1)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var clip: Clip? = null
    private val preferences = Preferences.userNodeForPackage(DesktopPlayback::class.java)
    private val queueFile = Path.of(System.getProperty("user.home"), ".prism", "desktop-queue.txt")
    private val supportedExtensions = setOf("wav", "mp3", "aiff", "aif", "au")
    private val favoritesFile = Path.of(System.getProperty("user.home"), ".prism", "favorites.txt")
    val favorites = mutableStateListOf<String>()
    var volume by mutableStateOf(preferences.getFloat("volume", 0.8f).coerceIn(0f, 1f))
        private set
    var repeatOne by mutableStateOf(preferences.getBoolean("repeatOne", false))
        private set
    var shuffleEnabled by mutableStateOf(preferences.getBoolean("shuffleEnabled", false))
        private set
    var repeatEnabled by mutableStateOf(preferences.getBoolean("repeatEnabled", false))
        private set

    init {
        val restored = try {
            if (Files.isRegularFile(queueFile)) {
                Files.readAllLines(queueFile, Charsets.UTF_8)
                    .filter { it.isNotBlank() }
                    .map(::File)
                    .filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        queue.addAll(restored)
        try {
            if (Files.isRegularFile(favoritesFile)) favorites.addAll(Files.readAllLines(favoritesFile, Charsets.UTF_8).filter { it.isNotBlank() })
        } catch (_: Exception) { }
        currentIndex = preferences.getInt("currentIndex", 0).coerceIn(-1, queue.lastIndex)
    }

    private fun persistQueue() {
        try {
            Files.createDirectories(queueFile.parent)
            Files.write(queueFile, queue.map { it.absolutePath }, Charsets.UTF_8)
            preferences.putInt("currentIndex", currentIndex)
        } catch (e: Exception) {
            errorMessage = e.message ?: "Impossible d’enregistrer la file de lecture."
        }
    }

    val currentTrack: File?
        get() = queue.getOrNull(currentIndex)

    fun isFavorite(file: File?): Boolean = file?.absolutePath in favorites

    fun toggleFavorite(file: File? = currentTrack) {
        val path = file?.absolutePath ?: return
        if (path in favorites) favorites.remove(path) else favorites.add(path)
        try {
            Files.createDirectories(favoritesFile.parent)
            Files.write(favoritesFile, favorites, Charsets.UTF_8)
            errorMessage = null
        } catch (e: Exception) { errorMessage = e.message ?: "Impossible d’enregistrer les favoris." }
    }

    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        preferences.putFloat("volume", volume)
        applyVolume()
    }

    fun toggleRepeatOne() {
        repeatOne = !repeatOne
        preferences.putBoolean("repeatOne", repeatOne)
    }
    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
        preferences.putBoolean("shuffleEnabled", shuffleEnabled)
    }
    fun toggleRepeat() {
        repeatEnabled = !repeatEnabled
        preferences.putBoolean("repeatEnabled", repeatEnabled)
    }

    private fun applyVolume() {
        val active = clip ?: return
        try {
            val control = active.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN) as javax.sound.sampled.FloatControl
            val gain = if (volume <= 0.001f) control.minimum else (20f * kotlin.math.log10(volume)).coerceIn(control.minimum, control.maximum)
            control.value = gain
        } catch (_: Exception) { }
    }

    fun chooseAndAdd() {
        val chooser = JFileChooser().apply {
            dialogTitle = "Ajouter des fichiers audio"
            isMultiSelectionEnabled = true
            fileFilter = FileNameExtensionFilter(
                "Fichiers audio (WAV, MP3, AIFF, AU)",
                "wav", "mp3", "aiff", "aif", "au"
            )
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            addFiles(chooser.selectedFiles.toList())
        }
    }

    fun chooseFolderAndAdd() {
        val chooser = JFileChooser().apply {
            dialogTitle = "Ajouter un dossier de musique"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            addFolder(chooser.selectedFile)
        }
    }

    fun addFolder(folder: File) {
        if (!folder.isDirectory) {
            errorMessage = "Sélectionnez un dossier valide."
            return
        }
        try {
            val discovered = Files.walk(folder.toPath()).use { paths ->
                paths.filter { Files.isRegularFile(it) }
                    .map(Path::toFile)
                    .filter { it.extension.lowercase() in supportedExtensions }
                    .toList()
            }
            addFiles(discovered)
            if (discovered.isEmpty()) errorMessage = "Aucun fichier audio pris en charge dans ce dossier."
        } catch (e: Exception) {
            errorMessage = e.message ?: "Impossible de parcourir ce dossier."
        }
    }

    fun addFiles(files: List<File>) {
        val accepted = files.filter { it.isFile && it.extension.lowercase() in supportedExtensions }.distinct()
        queue.addAll(accepted.filterNot { it in queue })
        errorMessage = null
        if (currentIndex < 0 && queue.isNotEmpty()) currentIndex = 0
        persistQueue()
    }

    fun importPlaylist() {
        val chooser = JFileChooser().apply {
            dialogTitle = "Importer une playlist M3U"
            fileFilter = FileNameExtensionFilter("Playlists M3U (*.m3u, *.m3u8)", "m3u", "m3u8")
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return
        try {
            val playlistFile = chooser.selectedFile.toPath()
            val base = playlistFile.toAbsolutePath().parent
            val imported = Files.readAllLines(playlistFile).asSequence()
                .map(String::trim)
                .map { it.removePrefix("\uFEFF") }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { entry ->
                    val path = Path.of(entry)
                    if (path.isAbsolute) path.toFile() else base.resolve(path).normalize().toFile()
                }
                .filter { it.isFile && it.extension.lowercase() in supportedExtensions }
                .toList()
            addFiles(imported)
            if (imported.isEmpty()) errorMessage = "La playlist ne contient aucun fichier audio local pris en charge."
        } catch (e: Exception) {
            errorMessage = e.message ?: "Impossible d’importer cette playlist."
        }
    }

    private val playlistDirectory = Path.of(System.getProperty("user.home"), ".prism", "playlists")

    fun savedPlaylists(): List<File> = try {
        if (Files.isDirectory(playlistDirectory)) {
            Files.list(playlistDirectory).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".m3u8", true) }
                    .map(Path::toFile).sorted(compareBy { it.name.lowercase() }).toList()
            }
        } else emptyList()
    } catch (_: Exception) { emptyList() }

    fun saveQueueAsPlaylist() {
        if (queue.isEmpty()) {
            errorMessage = "Ajoutez des titres avant d’enregistrer une playlist."
            return
        }
        val name = JOptionPane.showInputDialog(null, "Nom de la playlist", "Nouvelle playlist", JOptionPane.PLAIN_MESSAGE)
            ?.trim()?.takeIf { it.isNotEmpty() } ?: return
        try {
            Files.createDirectories(playlistDirectory)
            val safeName = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().trimEnd('.')
            if (safeName.isBlank()) { errorMessage = "Nom de playlist invalide."; return }
            Files.write(playlistDirectory.resolve("$safeName.m3u8"), buildList {
                add("#EXTM3U")
                queue.forEach { add(it.absolutePath) }
            }, Charsets.UTF_8)
            errorMessage = null
        } catch (e: Exception) { errorMessage = e.message ?: "Impossible d’enregistrer la playlist." }
    }

    fun deleteSavedPlaylist(playlist: File) {
        try {
            val normalized = playlist.toPath().toAbsolutePath().normalize()
            if (normalized.parent != playlistDirectory.toAbsolutePath().normalize()) {
                errorMessage = "Cette playlist ne se trouve pas dans le dossier Prism."
                return
            }
            Files.deleteIfExists(normalized)
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message ?: "Impossible de supprimer la playlist."
        }
    }

    fun loadSavedPlaylist(playlist: File) {
        try {
            val base = playlist.toPath().toAbsolutePath().parent
            val tracks = Files.readAllLines(playlist.toPath()).asSequence()
                .map(String::trim).map { it.removePrefix("\uFEFF") }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { entry -> val path = Path.of(entry); if (path.isAbsolute) path.toFile() else base.resolve(path).normalize().toFile() }
                .filter { it.isFile && it.extension.lowercase() in supportedExtensions }.toList()
            stopAndClose()
            queue.clear()
            queue.addAll(tracks)
            currentIndex = if (queue.isEmpty()) -1 else 0
            persistQueue()
            if (queue.isNotEmpty()) playCurrent()
            errorMessage = if (queue.isEmpty()) "Playlist vide ou fichiers introuvables." else null
        } catch (e: Exception) { errorMessage = e.message ?: "Impossible de charger la playlist." }
    }

    fun exportPlaylist() {
        if (queue.isEmpty()) {
            errorMessage = "La file de lecture est vide."
            return
        }
        val chooser = JFileChooser().apply {
            dialogTitle = "Exporter la file en playlist M3U"
            dialogType = JFileChooser.SAVE_DIALOG
            selectedFile = File("Prism-playlist.m3u8")
            fileFilter = FileNameExtensionFilter("Playlist M3U8 (*.m3u8)", "m3u8")
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return
        try {
            val selected = chooser.selectedFile.toPath()
            val destination = (if (selected.fileName.toString().endsWith(".m3u8", ignoreCase = true)) selected else Path.of("$selected.m3u8")).toAbsolutePath()
            val parent = destination.parent
            val lines = buildList {
                add("#EXTM3U")
                queue.forEach { file ->
                    val path = file.toPath().toAbsolutePath()
                    add(if (parent != null && parent.root == path.root) parent.relativize(path).toString() else path.toString())
                }
            }
            Files.write(destination, lines, Charsets.UTF_8)
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message ?: "Impossible d’exporter la playlist."
        }
    }

    fun clearQueue() {
        stopAndClose()
        queue.clear()
        currentIndex = -1
        persistQueue()
        errorMessage = null
    }

    fun playPause() {
        errorMessage = null
        val file = currentTrack ?: run {
            chooseAndAdd()
            return
        }
        try {
            val active = clip
            if (active != null && active.isOpen) {
                if (active.isRunning) {
                    active.stop()
                    isPlaying = false
                } else {
                    if (active.microsecondPosition >= active.microsecondLength) active.microsecondPosition = 0
                    active.start()
                    isPlaying = true
                }
            } else {
                openAndPlay(file)
            }
        } catch (e: Exception) {
            isPlaying = false
            errorMessage = e.message ?: "Impossible de lire ce fichier audio."
        }
    }

    fun playIndex(index: Int) {
        if (index !in queue.indices) return
        currentIndex = index
        persistQueue()
        playCurrent()
    }

    fun next() {
        if (queue.isEmpty()) return
        val nextIndex = if (shuffleEnabled && queue.size > 1) {
            (queue.indices.filter { it != currentIndex }).random()
        } else if (repeatEnabled) {
            (currentIndex + 1).mod(queue.size)
        } else {
            (currentIndex + 1).coerceAtMost(queue.lastIndex)
        }
        playIndex(nextIndex)
    }

    fun previous() {
        if (queue.isEmpty()) return
        val active = clip
        if (active != null && active.microsecondPosition > 3_000_000) {
            active.microsecondPosition = 0
            if (!active.isRunning) active.start()
            isPlaying = true
        } else {
            playIndex((currentIndex - 1).mod(queue.size))
        }
    }

    fun remove(index: Int) {
        if (index !in queue.indices) return
        val wasCurrent = index == currentIndex
        queue.removeAt(index)
        if (queue.isEmpty()) {
            stopAndClose()
            currentIndex = -1
        } else if (wasCurrent) {
            currentIndex = index.coerceAtMost(queue.lastIndex)
            playCurrent()
        } else if (index < currentIndex) {
            currentIndex--
        }
        persistQueue()
    }

    fun seekToFraction(fraction: Float) {
        val active = clip ?: return
        if (!active.isOpen || active.microsecondLength <= 0) return
        active.microsecondPosition = (active.microsecondLength * fraction.coerceIn(0f, 1f)).toLong()
    }

    fun positionFraction(): Float {
        val active = clip ?: return 0f
        if (!active.isOpen || active.microsecondLength <= 0) return 0f
        return (active.microsecondPosition.toFloat() / active.microsecondLength).coerceIn(0f, 1f)
    }

    fun close() {
        stopAndClose()
    }

    private fun playCurrent() {
        val file = currentTrack ?: return
        try {
            openAndPlay(file)
        } catch (e: Exception) {
            isPlaying = false
            errorMessage = e.message ?: "Format audio non pris en charge."
        }
    }

    private fun openAndPlay(file: File) {
        stopAndClose()
        val audioInput = AudioSystem.getAudioInputStream(file)
        val newClip = AudioSystem.getClip()
        try {
            newClip.open(audioInput)
        } finally {
            audioInput.close()
        }
        newClip.addLineListener { event ->
            if (event.type == javax.sound.sampled.LineEvent.Type.STOP &&
                newClip.microsecondPosition >= newClip.microsecondLength
            ) {
                javax.swing.SwingUtilities.invokeLater {
                    if (clip === newClip) {
                        if (repeatOne) {
                            newClip.microsecondPosition = 0
                            newClip.start()
                            isPlaying = true
                        } else {
                            isPlaying = false
                            if (shuffleEnabled && queue.size > 1) next()
                            else if (currentIndex < queue.lastIndex) playIndex(currentIndex + 1)
                            else if (repeatEnabled && queue.isNotEmpty()) playIndex(0)
                        }
                    }
                }
            }
        }
        clip = newClip
        applyVolume()
        newClip.start()
        isPlaying = true
        errorMessage = null
    }

    private fun stopAndClose() {
        clip?.let {
            try {
                it.stop()
                it.close()
            } catch (_: Exception) {
            }
        }
        clip = null
        isPlaying = false
    }

}

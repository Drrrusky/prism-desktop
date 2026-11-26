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

    fun addFiles(files: List<File>) {
        val accepted = files.filter { it.isFile && it.extension.lowercase() in supportedExtensions }
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
        playIndex((currentIndex + 1).mod(queue.size))
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
                        isPlaying = false
                        if (currentIndex in 0 until queue.lastIndex) playIndex(currentIndex + 1)
                    }
                }
            }
        }
        clip = newClip
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

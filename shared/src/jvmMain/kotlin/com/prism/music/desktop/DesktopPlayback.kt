package com.prism.music.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
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
                isPlaying = false
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

    private val supportedExtensions = setOf("wav", "mp3", "aiff", "aif", "au")
}

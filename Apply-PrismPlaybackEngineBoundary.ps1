$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$connection = Join-Path $root 'app\src\main\java\com\prism\music\playback\PlayerConnection.kt'
$engineFile = Join-Path $root 'app\src\main\java\com\prism\music\playback\Media3PlaybackEngine.kt'
$commonFile = Join-Path $root 'shared\src\commonMain\kotlin\com\prism\music\playback\PlaybackEngine.kt'

if (-not (Test-Path $connection -PathType Leaf)) { throw "PlayerConnection.kt introuvable. Lance ce script depuis la racine du dépôt Prism." }
if ((Test-Path $engineFile) -or (Test-Path $commonFile)) { throw 'Un des fichiers du patch existe déjà : arrêt sans écraser.' }

$source = [IO.File]::ReadAllText($connection)
$anchor = '    val player: ExoPlayer? get() = service?.player ?: testPlayer'
$count = ([regex]::Matches($source, [regex]::Escape($anchor))).Count
if ($count -ne 1) { throw "Ancre PlayerConnection attendue exactement une fois, trouvée : $count. Aucun fichier modifié." }

$common = @'
package com.prism.music.playback

/** Platform-neutral snapshot of the currently active playback engine. */
data class PlaybackSnapshot(
    val mediaId: String?,
    val positionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
)

/**
 * Small platform boundary for commands on the already-selected track.
 * Queue creation, resolving, caching and track selection remain owned by Prism's
 * existing playback pipeline; implementations must not bypass that pipeline.
 */
interface PlaybackEngine {
    val snapshot: PlaybackSnapshot

    /** Resume the currently selected item; does not select or resolve a new track. */
    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun setVolume(volume: Float)
}
'@

$android = @'
package com.prism.music.playback

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** Android implementation of the small platform-neutral playback command boundary. */
internal class Media3PlaybackEngine(
    private val player: ExoPlayer,
) : com.prism.music.playback.PlaybackEngine {

    override val snapshot: com.prism.music.playback.PlaybackSnapshot
        get() {
            val duration = player.duration
            return com.prism.music.playback.PlaybackSnapshot(
                mediaId = player.currentMediaItem?.mediaId,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = if (duration == C.TIME_UNSET || duration < 0L) 0L else duration,
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
            )
        }

    override fun play() = player.play()

    override fun pause() = player.pause()

    override fun seekTo(positionMs: Long) = player.seekTo(positionMs.coerceAtLeast(0L))

    override fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }
}
'@

$replacement = $anchor + [Environment]::NewLine + [Environment]::NewLine + '    /** Platform-neutral commands for the currently selected item; queue logic stays in this connection. */' + [Environment]::NewLine + '    val playbackEngine: com.prism.music.playback.PlaybackEngine?' + [Environment]::NewLine + '        get() = player?.let(::Media3PlaybackEngine)'
$updated = $source.Replace($anchor, $replacement)
$backup = "$connection.windows-port-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss').bak"
Copy-Item -LiteralPath $connection -Destination $backup

try {
    $commonDir = Split-Path -Parent $commonFile
    $androidDir = Split-Path -Parent $engineFile
    New-Item -ItemType Directory -Force -Path $commonDir, $androidDir | Out-Null
    [IO.File]::WriteAllText($commonFile, $common.TrimStart(), [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText($engineFile, $android.TrimStart(), [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText($connection, $updated, [Text.UTF8Encoding]::new($false))
} catch {
    Copy-Item -LiteralPath $backup -Destination $connection -Force
    Remove-Item -LiteralPath $commonFile, $engineFile -Force -ErrorAction SilentlyContinue
    throw
}

Write-Host 'Frontière PlaybackEngine ajoutée. Sauvegarde :' $backup
Write-Host 'Fichiers créés :' $commonFile, $engineFile
Write-Host 'Important : ce premier pas expose les commandes de base; il ne remplace pas encore toute la dépendance Media3 de PlayerConnection.'

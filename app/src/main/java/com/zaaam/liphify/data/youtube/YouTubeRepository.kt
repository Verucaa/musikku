package com.zaaam.liphify.data.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

data class YtTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationSec: Long,
)

sealed interface YtResult<out T> {
    data class Ok<T>(val value: T) : YtResult<T>
    data class Fail(val message: String) : YtResult<Nothing>
}

private const val FAIL_MSG = "Gagal ambil data dari YouTube, coba lagi"

/**
 * PRD-003/004/012: satu-satunya titik yang boleh memanggil NewPipeExtractor.
 * Semua call ditangkap sampai level Throwable (Exception DAN Error — parser
 * rapuh saat YouTube berubah, dan Error lolos dari catch Exception biasa).
 */
@Singleton
class YouTubeRepository @Inject constructor() {
    private fun service() = NewPipe.getService(0)

    suspend fun search(query: String): YtResult<List<YtTrack>> = withContext(Dispatchers.IO) {
        try {
            val svc = service()
            val extractor = svc.getSearchExtractor(query)
            extractor.fetchPage()
            coroutineContext.ensureActive()
            val items = extractor.initialPage.items.mapNotNull { item ->
                try {
                    // Hanya stream video (StreamInfoItem): playlist/channel/shelf
                    // tidak bisa di-resolve jadi audio dan hanya jadi sampah key.
                    if (item !is StreamInfoItem) return@mapNotNull null
                    val url = item.url ?: return@mapNotNull null
                    val videoId = Regex("v=([A-Za-z0-9_-]{11})").find(url)?.groupValues?.get(1)
                        ?: return@mapNotNull null
                    YtTrack(
                        videoId = videoId,
                        title = item.name?.ifBlank { "Unknown" } ?: "Unknown",
                        artist = item.uploaderName?.removeSuffix(" - Topic")?.ifBlank { "YouTube" } ?: "YouTube",
                        thumbnailUrl = try {
                            item.thumbnails.firstOrNull()?.url ?: ""
                        } catch (_: Throwable) {
                            ""
                        },
                        durationSec = try {
                            item.duration
                        } catch (_: Throwable) {
                            -1L
                        },
                    )
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    null
                }
            }.distinctBy { it.videoId }
            YtResult.Ok(items)
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            YtResult.Fail(FAIL_MSG)
        }
    }

    /** Resolve stream audio terbaik. URL YT itu time-limited: resolve lazy saat mau play. */
    suspend fun resolveAudioUrl(videoId: String): YtResult<String> = withContext(Dispatchers.IO) {
        try {
            val svc = service()
            val url = if (videoId.startsWith("http")) videoId else "https://www.youtube.com/watch?v=$videoId"
            val info = StreamInfo.getInfo(svc, url)
            coroutineContext.ensureActive()
            val audio = info.audioStreams.maxByOrNull {
                try {
                    it.bitrate
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    0
                }
            } ?: return@withContext YtResult.Fail("Stream audio tidak tersedia (mungkin age-restricted / private / region-block)")
            val streamUrl = try {
                audio.url
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                ""
            }
            if (streamUrl.isNullOrBlank()) {
                YtResult.Fail("Stream audio tidak tersedia (mungkin age-restricted / private / region-block)")
            } else {
                YtResult.Ok(streamUrl)
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            YtResult.Fail(FAIL_MSG)
        }
    }
}

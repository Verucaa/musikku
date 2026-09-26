package com.zaaam.liphify.data.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.stream.StreamInfo
import javax.inject.Inject
import javax.inject.Singleton

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

/**
 * PRD-003/004/012: satu-satunya titik yang boleh memanggil NewPipeExtractor.
 * Semua call di-wrap try-catch spesifik -> gagal = pesan, bukan crash.
 */
@Singleton
class YouTubeRepository @Inject constructor() {
    private fun service() = NewPipe.getService(0)

    suspend fun search(query: String): YtResult<List<YtTrack>> = withContext(Dispatchers.IO) {
        try {
            val svc = service()
            val extractor = svc.getSearchExtractor(query)
            extractor.fetchPage()
            val items = extractor.initialPage.items.mapNotNull { item ->
                try {
                    val url = item.url ?: return@mapNotNull null
                    val videoId = Regex("v=([A-Za-z0-9_-]{11})").find(url)?.groupValues?.get(1)
                        ?: url.substringAfterLast("/").substringAfter("v=").take(32)
                    YtTrack(
                        videoId = videoId.ifBlank { url },
                        title = item.name ?: "Unknown",
                        artist = "",
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url ?: "",
                        durationSec = -1,
                    )
                } catch (_: Exception) {
                    null
                }
            }
            YtResult.Ok(items)
        } catch (e: ParsingException) {
            YtResult.Fail("Gagal ambil data dari YouTube, coba lagi")
        } catch (e: ExtractionException) {
            YtResult.Fail("Gagal ambil data dari YouTube, coba lagi")
        } catch (e: Exception) {
            YtResult.Fail("Gagal ambil data dari YouTube, coba lagi")
        }
    }

    /** Resolve stream audio terbaik (>=128kbps bila tersedia). URL YT itu time-limited: resolve lazy saat mau play. */
    suspend fun resolveAudioUrl(videoId: String): YtResult<String> = withContext(Dispatchers.IO) {
        try {
            val svc = service()
            val url = if (videoId.startsWith("http")) videoId else "https://www.youtube.com/watch?v=$videoId"
            val info = StreamInfo.getInfo(svc, url)
            val audio = info.audioStreams.maxByOrNull {
                try {
                    it.bitrate
                } catch (_: Exception) {
                    0
                }
            } ?: return@withContext YtResult.Fail("Stream audio tidak tersedia (mungkin age-restricted / private / region-block)")
            val streamUrl = try {
                audio.url
            } catch (_: Exception) {
                ""
            }
            if (streamUrl.isNullOrBlank()) {
                YtResult.Fail("Stream audio tidak tersedia (mungkin age-restricted / private / region-block)")
            } else {
                YtResult.Ok(streamUrl)
            }
        } catch (e: ParsingException) {
            YtResult.Fail("Gagal ambil data dari YouTube, coba lagi")
        } catch (e: ExtractionException) {
            YtResult.Fail("Gagal ambil data dari YouTube, coba lagi")
        } catch (e: Exception) {
            YtResult.Fail("Gagal ambil data dari YouTube, coba lagi")
        }
    }
}

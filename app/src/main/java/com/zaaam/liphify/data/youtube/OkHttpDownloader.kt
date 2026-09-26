package com.zaaam.liphify.data.youtube

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response

/** Downloader NewPipeExtractor di atas OkHttp (stdlib). Dipakai sekali di LiPhifyApp. */
class OkHttpDownloader : Downloader() {
    private val client = OkHttpClient.Builder().followRedirects(true).build()

    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
        val builder = Request.Builder().url(request.url())
        for ((k, v) in request.headers()) {
            for (value in v) builder.addHeader(k, value)
        }
        val body = request.dataToSend()?.toRequestBody()
        when (request.httpMethod()) {
            "GET" -> builder.get()
            "POST" -> builder.post(body ?: ByteArray(0).toRequestBody())
            "HEAD" -> builder.head()
            else -> builder.method(request.httpMethod(), body)
        }
        client.newCall(builder.build()).execute().use { resp ->
            val headers = mutableMapOf<String, MutableList<String>>()
            for ((k, v) in resp.headers) {
                headers.getOrPut(k) { mutableListOf() }.add(v)
            }
            val respBody = resp.body?.string() ?: ""
            return Response(
                resp.code,
                resp.message,
                headers,
                respBody,
                resp.request.url.toString(),
            )
        }
    }
}

package com.tvlive.app.data.net

import okhttp3.Request

data class FetchResult(
    val content: String?,
    val etag: String?,
    val isModified: Boolean
)

object SourceFetcher {

    fun fetch(url: String, currentEtag: String? = null): FetchResult {
        val builder = Request.Builder().url(url)
        if (!currentEtag.isNullOrEmpty()) {
            builder.header("If-None-Match", currentEtag)
        }

        val response = HttpClient.client.newCall(builder.build()).execute()

        if (response.code() == 304) {
            response.close()
            return FetchResult(content = null, etag = currentEtag, isModified = false)
        }

        if (!response.isSuccessful()) {
            response.close()
            return FetchResult(content = null, etag = null, isModified = false)
        }

        val newEtag = response.header("ETag")
        val body = response.body()?.string()
        response.close()

        return FetchResult(
            content = body?.takeIf { it.isNotEmpty() },
            etag = newEtag,
            isModified = body != null
        )
    }
}

package com.liskovsoft.youtubeapi.app.nsigsolver.common

import com.liskovsoft.sharedutils.okhttp.OkHttpManager
import com.liskovsoft.youtubeapi.app.nsigsolver.provider.InfoExtractorError
import kotlinx.coroutines.delay
import okhttp3.Request

internal abstract class InfoExtractor {
    companion object {
        // A browser User-Agent is required; YouTube returns HTTP 404 for RSS
        // and other plain HTTP endpoints when the default OkHttp UA is used.
        private const val USER_AGENT_BROWSER =
            "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"
    }

    suspend fun downloadWebpage(url: String, tries: Int = 1, timeoutMs: Long = 1_000, errorMsg: String? = null): String {
        var tryCount = 0

        while (true) {
            try {
                val request = Request.Builder().url(url).header("User-Agent", USER_AGENT_BROWSER).build()
                val content = OkHttpManager.instance().client.newCall(request).execute().use {
                    if (!it.isSuccessful) throw InfoExtractorError(formatError(errorMsg, "Unexpected code $it"))
                    it.body()?.string()
                }
                return content ?: throw InfoExtractorError(formatError(errorMsg, "Empty content received for the $url"))
            } catch (e: Exception) {
                tryCount++
                if (tryCount >= tries)
                    throw InfoExtractorError(formatError(errorMsg, "Can't load the $url"), e)
                if (timeoutMs > 0)
                    delay(timeoutMs)
            }
        }
    }
}
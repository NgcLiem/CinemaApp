package com.ngcliem.movieexplorer.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the API key and language to every request, so the endpoints in [TmdbApi]
 * don't have to repeat them. The language is read on each request, so changing it
 * in the settings takes effect immediately. An endpoint may still pass its own
 * `language` parameter (e.g. to fetch an English fallback).
 */
class ApiKeyInterceptor(
    private val apiKey: String,
    private val language: () -> String = { "en-US" },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.url.newBuilder().addQueryParameter("api_key", apiKey)
        if (original.url.queryParameter("language") == null) {
            builder.addQueryParameter("language", language())
        }
        return chain.proceed(original.newBuilder().url(builder.build()).build())
    }
}

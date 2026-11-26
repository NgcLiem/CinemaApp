package com.ngcliem.movieexplorer.data.remote

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Some networks (several Vietnamese ISPs among them) block TMDB at the DNS level,
 * so the app shows no movies even though the internet works. For TMDB hosts we
 * resolve through DNS-over-HTTPS (Cloudflare, then Google) and fall back to the
 * system DNS; every other host uses the system DNS as usual.
 */
class TmdbDns(bootstrapClient: OkHttpClient) : Dns {

    private val resolvers: List<Dns> = listOf(
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(InetAddress.getByName("1.1.1.1"), InetAddress.getByName("1.0.0.1"))
            .build(),
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://dns.google/dns-query".toHttpUrl())
            .bootstrapDnsHosts(InetAddress.getByName("8.8.8.8"), InetAddress.getByName("8.8.4.4"))
            .build(),
        Dns.SYSTEM,
    )

    override fun lookup(hostname: String): List<InetAddress> {
        if (!isTmdbHost(hostname)) return Dns.SYSTEM.lookup(hostname)
        var lastError: Exception? = null
        for (resolver in resolvers) {
            try {
                val result = resolver.lookup(hostname)
                if (result.isNotEmpty()) return result
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw UnknownHostException("Cannot resolve $hostname").apply { lastError?.let(::initCause) }
    }

    private fun isTmdbHost(host: String) =
        host.endsWith("themoviedb.org") || host.endsWith("tmdb.org")
}

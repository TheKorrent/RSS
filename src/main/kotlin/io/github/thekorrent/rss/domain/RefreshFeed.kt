package io.github.thekorrent.rss.domain

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.google.common.eventbus.Subscribe
import io.github.thekorrent.rss.RssPlugin
import io.github.thekorrent.rss.RssPlugin.Companion.config
import io.github.thekorrent.rss.event.RefreshFeedScheduleEvent
import io.github.thekorrent.rss.model.Feed
import io.github.thekorrent.rss.model.Rss
import moe.shizuki.korrent.bittorrent.client.BitTorrentClient
import moe.shizuki.korrent.bittorrent.client.call.QBittorrentClient
import moe.shizuki.korrent.objectMapper
import moe.shizuki.korrent.plugin.annotation.KorrentEvent
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.security.MessageDigest

@KorrentEvent
class RefreshFeed {
    private val httpClient = OkHttpClient.Builder().apply {
        if (config.common.proxy.enabled) {
            proxy(Proxy(config.common.proxy.type, InetSocketAddress(config.common.proxy.host, config.common.proxy.port)))
        }
    }.build()
    
    @Subscribe
    fun refresh(event: RefreshFeedScheduleEvent) {
        val feeds = File(RssPlugin.pluginDataManager.pluginDataFolder, "feeds.json")
        val json = objectMapper.readValue(feeds.readText(), object : TypeReference<List<Feed>>() {})

        json.forEach { feed ->
            if (feed.enabled) {
                val rss = fetchRssFromFeed(feed)
                val filteredRss = filterString(filterDownloaded(feed.link, rss), feed.includes, feed.excludes, feed.regex)

                downloadRss(event.client, filteredRss, feed)
            }
        }
    }

    private fun sha256(input: String): String {
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).toHexString()
    }

    private fun downloadRss(client: BitTorrentClient, rss: Set<Rss>, feed: Feed) {
        val host = URI(feed.link).toURL().host
        val folder = File(File(RssPlugin.pluginDataManager.pluginDataFolder, "downloaded"), host).apply { mkdirs() }
        val file = File(folder, sha256(feed.link))

        val downloaded = if (file.exists()) {
            file.readText().split("\n").toMutableSet()
        } else {
            emptySet<String>().toMutableSet()
        }

        for (item in rss) {
            val call = httpClient.newCall(Request.Builder().apply {
                url(item.link)
            }.build())

            val torrent = call.execute().use { response ->
                if (!response.isSuccessful) {
                    return@use byteArrayOf()
                }

                return@use response.body?.bytes()
            }

            val torrentFile = File(File(RssPlugin.pluginDataManager.pluginCacheFolder, "torrent"), sha256(item.link))

            if (torrent != null) {
                torrentFile.parentFile.mkdirs()
                torrentFile.writeBytes(torrent)
            }

            if (client is QBittorrentClient) {
                val response = client.addTorrent(torrentFile, savepath = feed.path, category = feed.category, tags = feed.tags.joinToString(",")).execute()

                if (response.isSuccessful) {
                    downloaded.add(item.link)
                }
            }
        }

        val text = downloaded.filter { it.isNotEmpty() }.joinToString("\n")

        file.writeText(text)
    }

    private fun fetchRssFromFeed(feed: Feed): Set<Rss> {
        val call = httpClient.newCall(Request.Builder().apply {
            url(feed.link)
        }.build())

        val xml = call.execute().use { response ->
            if (!response.isSuccessful || response.body == null) {
                return@use null
            }

            return@use response.body!!.string()
        }

        if (xml == null) {
            return emptySet()
        }

        return parseXml(xml)
    }

    private fun filterString(rss: Set<Rss>, includes: List<String>, excludes: List<String>, regex: Boolean): Set<Rss> {
        if (regex) {
            val includeRegexes = includes.map { it.toRegex() }
            val excludeRegexes = excludes.map { it.toRegex() }

            return rss.filter { item ->
                val passesExcludes = excludeRegexes.none { exclude -> exclude.containsMatchIn(item.title) }

                val passesIncludes = if (includeRegexes.isEmpty()) {
                    true
                } else {
                    includeRegexes.any { include -> include.containsMatchIn(item.title) }
                }

                passesIncludes && passesExcludes
            }.toSet()
        }

        return rss.filter { item ->
            val passesExcludes = excludes.none { exclude -> item.title.contains(exclude) }

            val passesIncludes = if (includes.isEmpty()) {
                true
            } else {
                includes.any { include -> item.title.contains(include) }
            }

            passesIncludes && passesExcludes
        }.toSet()
    }

    private fun filterDownloaded(link: String, rss: Set<Rss>): Set<Rss> {
        val host = URI(link).toURL().host
        val folder = File(File(RssPlugin.pluginDataManager.pluginDataFolder, "downloaded"), host).apply { mkdirs() }
        val file = File(folder, sha256(link))

        val downloaded = if (file.exists()) {
            file.readText().split("\n").toSet()
        } else {
            mutableSetOf()
        }

        val willDownload = mutableSetOf<Rss>()

        for (item in rss) {
            if (downloaded.contains(item.link)) {
                continue
            }

            willDownload.add(item)
        }

        return willDownload.toSet()
    }

    private fun parseXml(xml: String): Set<Rss> {
        val mapper = XmlMapper()

        val tree = mapper.readTree(xml)
        val items = tree.at("/channel/item")

        val rss = mutableSetOf<Rss>()

        fun parseItem(item: JsonNode) {
            val title = item.path("title").asText()
            val enclosure = item.path("enclosure").path("url").asText()
            val link = item.path("link").asText()

            if (enclosure.startsWith("http")) {
                rss.add(Rss(title, enclosure))
            }

            if (link.startsWith("http")) {
                rss.add(Rss(title, link))
            }
        }

        if (items.isArray) {
            for (item in items) {
                parseItem(item)
            }
        } else {
            parseItem(items)
        }

        return rss
    }
}
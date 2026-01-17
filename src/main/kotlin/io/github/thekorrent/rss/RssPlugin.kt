package io.github.thekorrent.rss

import io.github.thekorrent.rss.model.Feed
import moe.shizuki.korrent.defaultNullPluginConfigManager
import moe.shizuki.korrent.defaultNullPluginDataManager
import moe.shizuki.korrent.objectMapper
import moe.shizuki.korrent.plugin.KorrentPlugin
import org.pf4j.PluginWrapper
import java.io.File

class RssPlugin(wrapper: PluginWrapper) : KorrentPlugin(wrapper) {
    companion object {
        val pluginDataManager get() = _pluginDataManager
        val config get() = _config

        private var _pluginConfigManager = defaultNullPluginConfigManager
        private var _pluginDataManager = defaultNullPluginDataManager
        private var _config = RssConfig()
    }

    init {
        _pluginConfigManager = this.pluginConfigManager
        _pluginDataManager = this.pluginDataManager
        _config = _pluginConfigManager.load()
    }

    override fun start() {
        val feeds = File(pluginDataManager.pluginDataFolder, "feeds.json")

        if (!feeds.exists()) {
            feeds.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listOf(Feed())))
        }

        super.start()
    }
}

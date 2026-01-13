package io.github.thekorrent.rss

import io.github.thekorrent.rss.model.Feed
import moe.shizuki.korrent.objectMapper
import moe.shizuki.korrent.plugin.KorrentPlugin
import moe.shizuki.korrent.plugin.config.PluginConfigManager
import moe.shizuki.korrent.plugin.data.PluginDataManager
import org.pf4j.PluginWrapper
import java.io.File

class RssPlugin(wrapper: PluginWrapper) : KorrentPlugin(wrapper) {
    companion object {
        val pluginConfigManager get() = _pluginConfigManager
        val pluginDataManager get() = _pluginDataManager

        private var _pluginConfigManager = PluginConfigManager("null")
        private var _pluginDataManager = PluginDataManager("null")
    }

    init {
        _pluginConfigManager = this.pluginConfigManager
        _pluginDataManager = this.pluginDataManager
    }

    override fun start() {
        val feeds = File(pluginDataManager.pluginDataFolder, "feeds.json")

        if (!feeds.exists()) {
            feeds.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listOf(Feed())))
        }

        super.start()
    }
}

package io.github.thekorrent.rss

import moe.shizuki.korrent.plugin.annotation.KorrentConfig
import moe.shizuki.korrent.plugin.config.PluginConfig

@KorrentConfig
class RssConfig(
    val rss: Rss = Rss()
): PluginConfig() {
    class Rss(
        val schedule: String = "0 */15 * * * *"
    )
}

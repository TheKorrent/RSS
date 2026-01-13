package io.github.thekorrent.rss.event

import moe.shizuki.korrent.bittorrent.event.ScheduleEvent
import moe.shizuki.korrent.plugin.annotation.KorrentScheduleEvent

@KorrentScheduleEvent("0 */15 * * * *", "rss.schedule")
class RefreshFeedScheduleEvent: ScheduleEvent()

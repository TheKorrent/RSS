package io.github.thekorrent.rss.model

data class Feed(
    val title: String = "",
    val link: String = "",
    val path: String = "",
    val category: String = "",
    val tags: List<String> = listOf(),
    val includes: List<String> = listOf(),
    val excludes: List<String> = listOf(),
    val regex: Boolean = false,
    val enabled: Boolean = false
)

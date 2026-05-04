package com.tvlive.app.data.model

data class ParseResult(
    val channels: List<ParsedChannel>,
    val parseTimeMs: Long
)

data class ParsedChannel(
    val name: String,
    val epgId: String?,
    val logoUrl: String?,
    val category: String,
    val sources: List<ParsedSource>
)

data class ParsedSource(
    val url: String,
    val streamType: String,
    val quality: String?,
    val provider: String?
)

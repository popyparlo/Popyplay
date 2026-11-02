package com.example.data.model

data class SyncedLine(
    val timeMs: Long,
    val text: String
)

object LyricsParser {
    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?]""")

    fun parseLrc(lrcContent: String?): List<SyncedLine> {
        if (lrcContent.isNullOrBlank()) return emptyList()

        val lines = mutableListOf<SyncedLine>()
        lrcContent.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach

            val match = TIMESTAMP_REGEX.find(trimmed)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val fraction = match.groupValues[3]
                val millis = when (fraction.length) {
                    1 -> (fraction.toLongOrNull() ?: 0L) * 100
                    2 -> (fraction.toLongOrNull() ?: 0L) * 10
                    3 -> fraction.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val timeMs = min * 60_000L + sec * 1000L + millis
                val text = trimmed.substring(match.range.last + 1).trim()
                if (text.isNotEmpty()) {
                    lines.add(SyncedLine(timeMs, text))
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}

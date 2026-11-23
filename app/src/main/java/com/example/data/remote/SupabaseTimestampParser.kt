package com.example.data.remote

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object SupabaseTimestampParser {
    private val fractionalRegex = Regex("\\.\\d+")
    private val tzOffsetColonRegex = Regex("([+-]\\d{2}):(\\d{2})$")

    fun parseIsoToMillis(isoString: String?): Long {
        val raw = isoString?.trim()?.takeIf { it.isNotBlank() } ?: return System.currentTimeMillis()
        val withoutFractional = fractionalRegex.replace(raw, "")
        val normalized = when {
            withoutFractional.endsWith("Z") -> withoutFractional.dropLast(1) + "+0000"
            tzOffsetColonRegex.containsMatchIn(withoutFractional) ->
                tzOffsetColonRegex.replace(withoutFractional, "$1$2")
            else -> "$withoutFractional+0000"
        }
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            fmt.parse(normalized)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}

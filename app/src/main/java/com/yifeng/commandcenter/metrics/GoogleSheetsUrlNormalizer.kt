package com.yifeng.commandcenter.metrics

import java.net.URI

sealed interface SheetUrlResult {
    data class Valid(val csvUrl: String) : SheetUrlResult
    data class Invalid(val reason: String) : SheetUrlResult
}

object GoogleSheetsUrlNormalizer {
    private val DOC_ID_REGEX = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)")
    private val PUBLISHED_ID_REGEX = Regex("/spreadsheets/d/e/([a-zA-Z0-9-_]+)")
    private val GID_REGEX = Regex("(?:[?&#]|^)gid=(\\d+)")

    fun normalizeToCsvUrl(rawInput: String): SheetUrlResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return SheetUrlResult.Invalid("Paste a Google Sheets link to connect.")
        }

        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else if (trimmed.startsWith("docs.google.com/", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            return SheetUrlResult.Invalid("Enter a valid https:// Google Sheets URL.")
        }

        val uri = runCatching { URI(withScheme) }.getOrNull()
            ?: return SheetUrlResult.Invalid("The URL format is invalid.")

        val host = uri.host?.lowercase()
            ?: return SheetUrlResult.Invalid("The URL is missing a valid host.")
        val path = uri.rawPath.orEmpty()
        val queryAndFragment = listOfNotNull(uri.rawQuery, uri.rawFragment).joinToString("&")
        val gid = GID_REGEX.find(queryAndFragment)?.groupValues?.getOrNull(1)

        if (host == "docs.google.com" && path.contains("/spreadsheets/")) {
            val publishedMatch = PUBLISHED_ID_REGEX.find(path)
            if (publishedMatch != null) {
                val pubId = publishedMatch.groupValues[1]
                val gidParam = gid?.let { "&gid=$it&single=true" }.orEmpty()
                return SheetUrlResult.Valid(
                    "https://docs.google.com/spreadsheets/d/e/$pubId/pub?output=csv$gidParam",
                )
            }

            val docMatch = DOC_ID_REGEX.find(path)
            if (docMatch != null) {
                val docId = docMatch.groupValues[1]
                if (path.contains("/gviz/tq") && queryAndFragment.contains("out:csv", ignoreCase = true)) {
                    return SheetUrlResult.Valid(withScheme)
                }
                val gidParam = gid?.let { "&gid=$it" }.orEmpty()
                return SheetUrlResult.Valid(
                    "https://docs.google.com/spreadsheets/d/$docId/export?format=csv$gidParam",
                )
            }

            return SheetUrlResult.Invalid("Could not find a spreadsheet ID in that Google Sheets link.")
        }

        if (uri.scheme.equals("https", ignoreCase = true) || uri.scheme.equals("http", ignoreCase = true)) {
            return SheetUrlResult.Valid(withScheme)
        }

        return SheetUrlResult.Invalid("Enter a valid https:// Google Sheets URL.")
    }
}

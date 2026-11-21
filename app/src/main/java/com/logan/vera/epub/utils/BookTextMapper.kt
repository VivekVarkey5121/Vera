package com.logan.vera.epub.utils

import androidx.compose.ui.text.AnnotatedString

class BookTextMapper {
    data class ImgEntry(
        val path: String,
        val yrel: Float
    ) {
        fun toXMLString(): String = "<img path=\"$path\" yrel=\"$yrel\" />"
    }

    companion object {
        fun parseImgTag(xml: String): ImgEntry? {
            val pathRegex = "path=\"([^\"]*)\"".toRegex()
            val yrelRegex = "yrel=\"([^\"]*)\"".toRegex()

            val path = pathRegex.find(xml)?.groupValues?.get(1) ?: return null
            val yrel = yrelRegex.find(xml)?.groupValues?.get(1)?.toFloatOrNull() ?: return null

            return ImgEntry(path, yrel)
        }
    }
}

fun String.toAnnotatedString(): androidx.compose.ui.text.AnnotatedString {
    val regex = Regex("<i>(.*?)</i>|<b>(.*?)</b>|([^<]+)", RegexOption.DOT_MATCHES_ALL)
    return androidx.compose.ui.text.buildAnnotatedString {
        regex.findAll(this@toAnnotatedString).forEach { match -> //note: the this@toAnnotatedString fetches the item this ws called on
            when {
                match.value.startsWith("<i>") -> {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                    append(match.groupValues[1].toAnnotatedString())
                    pop()
                }
                match.value.startsWith("<b>") -> {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                    append(match.groupValues[2].toAnnotatedString())
                    pop()
                }
                else -> {
                    append(match.value)
                }
            }
        }
    }
}

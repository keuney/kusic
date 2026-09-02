package com.keuney.music.data.source.providerA.mapper

import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal fun mapSearchResponse(response: JsonObject): List<Track> {
    check("error" !in response) { "Source returned an error" }
    val contents = response["contents"] as? JsonObject
        ?: error("Source search contents missing")
    val sections = contents.descendants("sectionListRenderer")
    check(sections.isNotEmpty()) { "Source search structure changed" }
    return sections.flatMap { it.descendants("videoRenderer") }
        .map(::mapTrack)
        .distinctBy(Track::id)
}

private fun mapTrack(item: JsonObject): Track {
    val id = item["videoId"].text()
    val title = item["title"].displayText()
    check(!id.isNullOrBlank() && title.isNotBlank()) { "Source track metadata missing" }
    val artist = (item["ownerText"] ?: item["longBylineText"] ?: item["shortBylineText"]).displayText()
    val duration = parseDuration(item["lengthText"].displayText())
    val thumbnails = item.at("thumbnail", "thumbnails") as? JsonArray
    val artwork = thumbnails?.lastOrNull().at("url").text()?.takeIf { it.startsWith("https://") }
    return Track(id, title, artist, artwork, duration, SourceType.Remote)
}

private fun parseDuration(value: String?): Long? {
    if (value == null || !value.matches(Regex("\\d{1,5}:\\d{2}(:\\d{2})?"))) return null
    val parts = value.split(':').map(String::toLong)
    if (parts.drop(1).any { it >= 60 }) return null
    return parts.fold(0L) { total, part -> total * 60 + part } * 1_000
}

private fun JsonElement?.at(vararg keys: String): JsonElement? =
    keys.fold(this) { element, key -> (element as? JsonObject)?.get(key) }

private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.contentOrNull

private fun JsonElement?.displayText(): String = at("simpleText").text()
    ?: (at("runs") as? JsonArray).orEmpty().joinToString("") { it.at("text").text().orEmpty() }.trim()

private fun JsonElement.descendants(key: String): List<JsonObject> = when (this) {
    is JsonObject -> listOfNotNull(this[key] as? JsonObject) + values.flatMap { it.descendants(key) }
    is JsonArray -> flatMap { it.descendants(key) }
    else -> emptyList()
}

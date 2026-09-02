package com.keuney.music.data.source.providerA

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import javax.inject.Inject

internal class ProviderAClient @Inject constructor(private val http: HttpClient) {
    suspend fun request(
        endpoint: String,
        fields: JsonObject,
        profile: ProviderAClientProfile = ProviderAConfig.search,
    ): JsonObject {
        require(endpoint.matches(Regex("[a-z]+"))) { "Invalid provider operation" }
        val payload = buildJsonObject {
            fields.forEach { (key, value) -> put(key, value) }
            put("context", Json.encodeToJsonElement(profile.context()))
        }
        return http.post("${ProviderAConfig.origin}/youtubei/v1/$endpoint") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Origin, ProviderAConfig.origin)
            header(HttpHeaders.UserAgent, profile.userAgent)
            header("X-Youtube-Client-Name", profile.clientId)
            header("X-Youtube-Client-Version", profile.clientVersion)
            setBody(Json.encodeToString(payload))
        }.body()
    }
}

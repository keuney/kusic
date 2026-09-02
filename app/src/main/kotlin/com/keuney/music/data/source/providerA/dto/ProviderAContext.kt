package com.keuney.music.data.source.providerA.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ProviderAContext(val client: ProviderAClientContext)

@Serializable
internal data class ProviderAClientContext(
    val clientName: String,
    val clientVersion: String,
    val hl: String,
    val gl: String,
)

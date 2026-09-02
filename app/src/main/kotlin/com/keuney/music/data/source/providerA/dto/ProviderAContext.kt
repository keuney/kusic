package com.keuney.music.data.source.providerA.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ProviderAContext(
    val client: ProviderAClientContext,
    val thirdParty: ProviderAThirdParty? = null,
)

/**
 * 선택 필드는 기본값 null이며 기본 Json 설정의 encodeDefaults=false 때문에 요청 본문에서 생략된다.
 * 클라이언트 종류마다 필요한 필드만 채운다.
 */
@Serializable
internal data class ProviderAClientContext(
    val clientName: String,
    val clientVersion: String,
    val hl: String,
    val gl: String,
    val androidSdkVersion: Int? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
)

@Serializable
internal data class ProviderAThirdParty(val embedUrl: String)

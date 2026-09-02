package com.keuney.music.data.source.providerA

import com.keuney.music.data.source.providerA.dto.ProviderAClientContext
import com.keuney.music.data.source.providerA.dto.ProviderAContext
import com.keuney.music.data.source.providerA.dto.ProviderAThirdParty

/**
 * 공급자 클라이언트 종류별 요청 설정. 공개 클라이언트의 관찰값이며 안정된 API 계약이 아니다.
 * [sendsSignatureTimestamp]가 true인 종류만 재생 요청에 플레이어 버전을 함께 보낸다.
 */
internal data class ProviderAClientProfile(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val osName: String? = null,
    val osVersion: String? = null,
    val androidSdkVersion: Int? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val embedUrl: String? = null,
    val sendsSignatureTimestamp: Boolean = true,
) {
    fun context(): ProviderAContext = ProviderAContext(
        client = ProviderAClientContext(
            clientName = clientName,
            clientVersion = clientVersion,
            hl = ProviderAConfig.hl,
            gl = ProviderAConfig.gl,
            androidSdkVersion = androidSdkVersion,
            deviceMake = deviceMake,
            deviceModel = deviceModel,
            osName = osName,
            osVersion = osVersion,
        ),
        thirdParty = embedUrl?.let(::ProviderAThirdParty),
    )
}

internal object ProviderAConfig {
    const val origin = "https://www.youtube.com"
    const val hl = "ko"
    const val gl = "KR"
    const val signatureTimestamp = 20684

    /** 검색 경로. KM-055에서 실제 계약 검사를 통과한 설정을 유지한다. */
    val search = ProviderAClientProfile(
        clientName = "WEB",
        clientVersion = "2.20260901.00.00",
        clientId = "1",
        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36",
    )

    private val androidVr = ProviderAClientProfile(
        clientName = "ANDROID_VR",
        clientVersion = "1.62.27",
        clientId = "28",
        userAgent = "com.google.android.apps.youtube.vr.oculus/1.62.27 " +
            "(Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
        osName = "Android",
        osVersion = "12L",
        androidSdkVersion = 32,
        deviceMake = "Oculus",
        deviceModel = "Quest 3",
        sendsSignatureTimestamp = false,
    )

    private val tvEmbedded = ProviderAClientProfile(
        clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
        clientVersion = "2.0",
        clientId = "85",
        userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.00) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Safari/605.1.15",
        embedUrl = "https://www.youtube.com/",
    )

    private val ios = ProviderAClientProfile(
        clientName = "IOS",
        clientVersion = "20.03.02",
        clientId = "5",
        userAgent = "com.google.ios.youtube/20.03.02 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X)",
        osName = "iPhone",
        osVersion = "18.1.0.22B83",
        deviceMake = "Apple",
        deviceModel = "iPhone16,2",
        sendsSignatureTimestamp = false,
    )

    private val android = ProviderAClientProfile(
        clientName = "ANDROID",
        clientVersion = "20.10.38",
        clientId = "3",
        userAgent = "com.google.android.youtube/20.10.38 (Linux; U; Android 14; ko_KR) gzip",
        osName = "Android",
        osVersion = "14",
        androidSdkVersion = 34,
        sendsSignatureTimestamp = false,
    )

    /**
     * 재생 요청에 사용할 후보 순서. 실제 계약 검사에서 직접 오디오 URL을 제공한 종류를 먼저 두고,
     * 나머지는 대체 경로로 남긴다. WEB은 직접 URL 없이 별도 전송 주소만 반환하므로 마지막이다.
     */
    val streamCandidates = listOf(ios, android, androidVr, tvEmbedded, search)

    val clientId: String get() = search.clientId
    val clientVersion: String get() = search.clientVersion
    val context: ProviderAContext get() = search.context()
}

package com.keuney.music.data.source.providerA

import com.keuney.music.data.source.providerA.dto.ProviderAClientContext
import com.keuney.music.data.source.providerA.dto.ProviderAContext

internal object ProviderAConfig {
    const val origin = "https://www.youtube.com"
    const val clientId = "1"
    const val clientVersion = "2.20260901.00.00"
    const val signatureTimestamp = 20684
    const val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

    val context = ProviderAContext(
        ProviderAClientContext("WEB", clientVersion, "ko", "KR"),
    )
}

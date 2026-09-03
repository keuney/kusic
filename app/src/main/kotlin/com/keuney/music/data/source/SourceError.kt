package com.keuney.music.data.source

import com.keuney.music.core.model.AppError
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.utils.io.errors.IOException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import kotlinx.serialization.SerializationException

/**
 * 공급자 구현이 실패 원인을 도메인에 전달하는 분류. 공급자 이름이나 응답 원문을 담지 않는다.
 */
internal enum class SourceFailure { Network, Parse, NotFound, Restricted, Unknown }

/** 분류를 가진 소스 예외. 구현체는 이 타입으로 실패를 알린다. */
internal interface SourceFailureAware {
    val failure: SourceFailure
}

/**
 * 인프라 예외와 공급자 실패를 사용자에게 보여줄 오류로 바꾼다(AGENTS.md 12).
 * 원문 예외와 메시지는 여기서 끊기며 UI로 넘어가지 않는다.
 */
internal fun Throwable.toAppError(): AppError = when (this) {
    is SourceFailureAware -> failure.toAppError()
    is HttpRequestTimeoutException -> AppError.Network
    is ClientRequestException -> httpStatusToFailure(response.status.value).toAppError()
    is ServerResponseException -> AppError.Network
    is ResponseException -> httpStatusToFailure(response.status.value).toAppError()
    is SerializationException -> AppError.SourceUnavailable
    is UnresolvedAddressException, is UnknownHostException, is IOException -> AppError.Network
    else -> AppError.Unknown
}

internal fun SourceFailure.toAppError(): AppError = when (this) {
    // 소스가 응답 구조나 전송 방식을 바꾼 경우다. 앱이 고칠 수 있는 문제가 아니다.
    SourceFailure.Parse -> AppError.SourceUnavailable
    SourceFailure.Network -> AppError.Network
    // 없는 트랙과 접근이 막힌 트랙 모두 "이 곡을 재생할 수 없다"로 보인다.
    // AppError.GeoRestricted는 지역 제한을 구조적으로 구분할 수 있을 때만 쓴다.
    SourceFailure.NotFound, SourceFailure.Restricted -> AppError.PlaybackUnavailable
    SourceFailure.Unknown -> AppError.Unknown
}

internal fun httpStatusToFailure(status: Int): SourceFailure = when {
    status == 401 || status == 403 -> SourceFailure.Restricted
    status == 404 || status == 410 -> SourceFailure.NotFound
    status == 408 || status == 429 || status >= 500 -> SourceFailure.Network
    else -> SourceFailure.Unknown
}

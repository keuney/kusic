package com.keuney.music.core.model

/** Safe error categories for app state; never carry infrastructure exceptions or URLs. */
sealed interface AppError {
    data object Network : AppError
    data object SourceUnavailable : AppError
    data object PlaybackUnavailable : AppError
    data object GeoRestricted : AppError
    data object Unknown : AppError
}

/**
 * Repository 경계에서 실패를 도메인 오류로 실어 나른다.
 * 원문 예외와 메시지는 담지 않으며, 화면은 [error]만 보고 문구를 고른다.
 */
class AppErrorException(val error: AppError) : Exception(error.toString())

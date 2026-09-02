package com.keuney.music.core.model

/** Safe error categories for app state; never carry infrastructure exceptions or URLs. */
sealed interface AppError {
    data object Network : AppError
    data object SourceUnavailable : AppError
    data object PlaybackUnavailable : AppError
    data object GeoRestricted : AppError
    data object Unknown : AppError
}

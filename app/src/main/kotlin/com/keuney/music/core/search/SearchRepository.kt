package com.keuney.music.core.search

import com.keuney.music.core.model.Track

/**
 * ViewModel과 MusicSource 사이의 경계(ARCHITECTURE 6).
 * 실패는 항상 [com.keuney.music.core.model.AppErrorException]으로 돌려주므로
 * 화면 계층은 공급자 예외나 인프라 타입을 알 필요가 없다.
 */
interface SearchRepository {
    suspend fun search(query: String): Result<List<Track>>
}

package com.keuney.music.core.search

import kotlinx.coroutines.flow.Flow

/**
 * 최근 검색어 저장소. 화면은 저장 수단을 알지 못한다.
 *
 * 목록은 최신이 먼저 오고 개수가 제한된다. 같은 검색어를 다시 검색하면 새 항목이 생기지 않고
 * 맨 앞으로 올라온다.
 */
interface SearchHistoryRepository {
    val queries: Flow<List<String>>

    /** 오류 없이 끝난 검색만 남긴다. 빈 검색어는 무시한다. */
    suspend fun record(query: String)

    suspend fun clear()
}

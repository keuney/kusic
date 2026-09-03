package com.keuney.music.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.keuney.music.core.search.SearchHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * 최근 검색어를 설정 DataStore에 둔다. 검색어 목록은 개수가 정해진 짧은 문자열 목록이며 조회나
 * 정렬이 필요하지 않아 Room 테이블을 쓸 이유가 없다.
 *
 * Preferences에는 순서를 지키는 목록 타입이 없으므로 JSON 배열 한 값으로 저장한다. 문자열 집합을
 * 쓰면 최신 순서를 잃는다.
 */
internal class SearchHistoryRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SearchHistoryRepository {
    override val queries: Flow<List<String>> = dataStore.data
        .map { it[QueriesKey].decode() }
        .distinctUntilChanged()

    override suspend fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { preferences ->
            val kept = preferences[QueriesKey].decode().filterNot { it == trimmed }
            preferences[QueriesKey] = Json.encodeToString(listOf(trimmed) + kept.take(MAX_QUERIES - 1))
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(QueriesKey) }
    }

    /** 저장된 값이 깨져 있으면 목록이 없는 것으로 본다. 검색어 목록 때문에 앱이 멈추면 안 된다. */
    private fun String?.decode(): List<String> = when (this) {
        null -> emptyList()
        else -> try {
            Json.decodeFromString<List<String>>(this)
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    private companion object {
        const val MAX_QUERIES = 10
        val QueriesKey = stringPreferencesKey("search_history")
    }
}

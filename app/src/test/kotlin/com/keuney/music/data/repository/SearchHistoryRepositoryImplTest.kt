package com.keuney.music.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.keuney.music.data.settings.createSettingsDataStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** KM-074: 성공한 검색 저장, 목록 지우기, 다시 열어도 유지. */
class SearchHistoryRepositoryImplTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun startsEmpty() = runBlocking {
        withStore { store ->
            assertEquals(emptyList<String>(), SearchHistoryRepositoryImpl(store).queries.first())
        }
    }

    @Test
    fun theNewestQueryComesFirst() = runBlocking {
        withStore { store ->
            val repository = SearchHistoryRepositoryImpl(store)

            repository.record("아이유")
            repository.record("뉴진스")

            assertEquals(listOf("뉴진스", "아이유"), repository.queries.first())
        }
    }

    @Test
    fun searchingTheSameQueryAgainMovesItUpInsteadOfAddingIt() = runBlocking {
        withStore { store ->
            val repository = SearchHistoryRepositoryImpl(store)

            repository.record("아이유")
            repository.record("뉴진스")
            repository.record("아이유")

            assertEquals(listOf("아이유", "뉴진스"), repository.queries.first())
        }
    }

    @Test
    fun aQueryIsTrimmedAndBlankIsIgnored() = runBlocking {
        withStore { store ->
            val repository = SearchHistoryRepositoryImpl(store)

            repository.record("  아이유  ")
            repository.record("   ")
            repository.record("")

            assertEquals(listOf("아이유"), repository.queries.first())
        }
    }

    @Test
    fun theOldestQueryIsDroppedPastTheLimit() = runBlocking {
        withStore { store ->
            val repository = SearchHistoryRepositoryImpl(store)

            for (index in 1..12) repository.record("검색어 $index")

            val queries = repository.queries.first()
            assertEquals(10, queries.size)
            assertEquals("검색어 12", queries.first())
            assertEquals("검색어 3", queries.last())
        }
    }

    @Test
    fun clearingRemovesEveryQuery() = runBlocking {
        withStore { store ->
            val repository = SearchHistoryRepositoryImpl(store)
            repository.record("아이유")

            repository.clear()

            assertEquals(emptyList<String>(), repository.queries.first())
        }
    }

    @Test
    fun queriesSurviveReopeningTheStore() = runBlocking {
        val file = File(temporaryFolder.root, "settings.preferences_pb")
        withStore(file) { store ->
            SearchHistoryRepositoryImpl(store).record("아이유")
        }
        // 앱 재시작에 해당한다. 저장소를 새로 열어도 목록이 남아야 한다.
        withStore(file) { store ->
            assertEquals(listOf("아이유"), SearchHistoryRepositoryImpl(store).queries.first())
        }
    }

    @Test
    fun clearingSurvivesReopeningTheStore() = runBlocking {
        val file = File(temporaryFolder.root, "settings.preferences_pb")
        withStore(file) { store ->
            val repository = SearchHistoryRepositoryImpl(store)
            repository.record("아이유")
            repository.clear()
        }
        withStore(file) { store ->
            assertEquals(emptyList<String>(), SearchHistoryRepositoryImpl(store).queries.first())
        }
    }

    @Test
    fun aBrokenStoredValueIsTreatedAsNoHistory() = runBlocking {
        withStore { store ->
            store.edit { it[stringPreferencesKey("search_history")] = "형식이 아닌 값" }

            val repository = SearchHistoryRepositoryImpl(store)
            assertEquals(emptyList<String>(), repository.queries.first())

            // 깨진 값 위에도 새 검색어를 남길 수 있어야 한다.
            repository.record("아이유")
            assertEquals(listOf("아이유"), repository.queries.first())
        }
    }

    private suspend fun withStore(
        file: File = File(temporaryFolder.root, "settings.preferences_pb"),
        block: suspend (DataStore<Preferences>) -> Unit,
    ) {
        val job = SupervisorJob()
        val store = createSettingsDataStore(file = file, scope = CoroutineScope(job + Dispatchers.IO))
        try {
            block(store)
        } finally {
            job.cancelAndJoin()
        }
    }
}

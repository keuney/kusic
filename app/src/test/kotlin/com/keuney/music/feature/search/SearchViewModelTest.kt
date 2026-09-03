package com.keuney.music.feature.search

import com.keuney.music.core.model.AppError
import com.keuney.music.core.model.AppErrorException
import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import com.keuney.music.core.search.SearchHistoryRepository
import com.keuney.music.core.search.SearchRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** KM-071: 다섯 상태와 StateFlow 노출을 확인한다. 재생 의존성이 없어 일반 단위 검사로 돈다. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsIdle() {
        assertEquals(SearchUiState.Idle, viewModel(Result.success(emptyList())).state.value)
    }

    @Test
    fun aSearchGoesThroughLoadingToSuccess() = runTest(dispatcher) {
        val tracks = listOf(track("a"), track("b"))
        val viewModel = viewModel(Result.success(tracks))

        viewModel.search("아이유")
        assertEquals("결과를 기다리는 동안 Loading이어야 한다", SearchUiState.Loading, viewModel.state.value)

        advanceUntilIdle()
        assertEquals(SearchUiState.Success(tracks), viewModel.state.value)
    }

    @Test
    fun noResultsAreEmptyRatherThanAnError() = runTest(dispatcher) {
        val viewModel = viewModel(Result.success(emptyList()))

        viewModel.search("없는 검색어")
        advanceUntilIdle()

        assertEquals(SearchUiState.Empty, viewModel.state.value)
    }

    @Test
    fun aFailureCarriesTheDomainErrorForTheScreen() = runTest(dispatcher) {
        val viewModel = viewModel(Result.failure(AppErrorException(AppError.Network)))

        viewModel.search("아이유")
        advanceUntilIdle()

        assertEquals(SearchUiState.Error(AppError.Network), viewModel.state.value)
    }

    @Test
    fun anUnexpectedFailureFallsBackToUnknown() = runTest(dispatcher) {
        val viewModel = viewModel(Result.failure(IllegalStateException("원문 노출 금지")))

        viewModel.search("아이유")
        advanceUntilIdle()

        assertEquals(SearchUiState.Error(AppError.Unknown), viewModel.state.value)
        assertTrue(viewModel.state.value.toString().contains("Unknown"))
    }

    @Test
    fun blankQueriesReturnToIdleWithoutCallingTheRepository() = runTest(dispatcher) {
        val repository = RecordingRepository(Result.success(listOf(track("a"))))
        val viewModel = SearchViewModel(repository, RecordingHistory())

        viewModel.search("   ")
        advanceUntilIdle()

        assertEquals(SearchUiState.Idle, viewModel.state.value)
        assertEquals(0, repository.queries.size)
    }

    @Test
    fun theQueryIsTrimmedBeforeItReachesTheRepository() = runTest(dispatcher) {
        val repository = RecordingRepository(Result.success(emptyList()))
        val viewModel = SearchViewModel(repository, RecordingHistory())

        viewModel.search("  아이유  ")
        advanceUntilIdle()

        assertEquals(listOf("아이유"), repository.queries)
    }

    @Test
    fun aLateResultFromAnEarlierSearchDoesNotOverwriteTheNewOne() = runTest(dispatcher) {
        val slow = SlowRepository()
        val viewModel = SearchViewModel(slow, RecordingHistory())

        viewModel.search("느린 검색어")
        advanceUntilIdle()
        viewModel.search("새 검색어")
        advanceUntilIdle()

        // 뒤늦게 첫 검색이 끝나도 화면은 두 번째 검색 결과를 유지해야 한다.
        slow.complete(0, listOf(track("old")))
        advanceUntilIdle()

        assertEquals(SearchUiState.Success(listOf(track("new"))), viewModel.state.value)
    }

    @Test
    fun clearingRemovesResultsAndReturnsToIdle() = runTest(dispatcher) {
        val viewModel = viewModel(Result.success(listOf(track("a"))))

        viewModel.search("아이유")
        advanceUntilIdle()
        viewModel.clear()

        assertEquals(SearchUiState.Idle, viewModel.state.value)
    }

    @Test
    fun aSuccessfulSearchIsKeptInTheHistory() = runTest(dispatcher) {
        val history = RecordingHistory()
        val viewModel = SearchViewModel(RecordingRepository(Result.success(listOf(track("a")))), history)

        viewModel.search("  아이유  ")
        advanceUntilIdle()

        // 저장하는 값은 화면 입력이 아니라 정리된 검색어다.
        assertEquals(listOf("아이유"), history.recorded)
    }

    @Test
    fun aSearchWithNoResultsIsStillKept() = runTest(dispatcher) {
        val history = RecordingHistory()
        val viewModel = SearchViewModel(RecordingRepository(Result.success(emptyList())), history)

        viewModel.search("없는 검색어")
        advanceUntilIdle()

        assertEquals(SearchUiState.Empty, viewModel.state.value)
        assertEquals(listOf("없는 검색어"), history.recorded)
    }

    @Test
    fun aFailedSearchIsNotKept() = runTest(dispatcher) {
        val history = RecordingHistory()
        val viewModel = SearchViewModel(
            RecordingRepository(Result.failure(AppErrorException(AppError.Network))),
            history,
        )

        viewModel.search("아이유")
        advanceUntilIdle()

        assertEquals(SearchUiState.Error(AppError.Network), viewModel.state.value)
        assertEquals(emptyList<String>(), history.recorded)
    }

    @Test
    fun aBlankQueryIsNotKept() = runTest(dispatcher) {
        val history = RecordingHistory()
        val viewModel = SearchViewModel(RecordingRepository(Result.success(emptyList())), history)

        viewModel.search("   ")
        advanceUntilIdle()

        assertEquals(emptyList<String>(), history.recorded)
    }

    @Test
    fun theStoredQueriesAreExposedToTheScreen() = runTest(dispatcher) {
        val history = RecordingHistory(listOf("뉴진스", "아이유"))
        val viewModel = SearchViewModel(RecordingRepository(Result.success(emptyList())), history)

        advanceUntilIdle()

        assertEquals(listOf("뉴진스", "아이유"), viewModel.recentQueries.value)
    }

    @Test
    fun clearingTheHistoryReachesTheRepository() = runTest(dispatcher) {
        val history = RecordingHistory(listOf("아이유"))
        val viewModel = SearchViewModel(RecordingRepository(Result.success(emptyList())), history)

        viewModel.clearRecentQueries()
        advanceUntilIdle()

        assertTrue(history.cleared)
        assertEquals(emptyList<String>(), viewModel.recentQueries.value)
    }

    private fun viewModel(result: Result<List<Track>>) =
        SearchViewModel(RecordingRepository(result), RecordingHistory())

    private fun track(id: String) = Track(id, "제목 $id", "아티스트", null, 180_000, SourceType.Remote)

    private class RecordingRepository(private val result: Result<List<Track>>) : SearchRepository {
        val queries = mutableListOf<String>()

        override suspend fun search(query: String): Result<List<Track>> {
            queries += query
            return result
        }
    }

    private class RecordingHistory(stored: List<String> = emptyList()) : SearchHistoryRepository {
        private val state = MutableStateFlow(stored)
        val recorded = mutableListOf<String>()
        var cleared = false
            private set

        override val queries: Flow<List<String>> = state

        override suspend fun record(query: String) {
            recorded += query
            state.value = listOf(query) + state.value.filterNot { it == query }
        }

        override suspend fun clear() {
            cleared = true
            state.value = emptyList()
        }
    }

    /** 첫 요청은 완료를 보류하고 이후 요청은 즉시 응답해 늦게 도착하는 결과를 재현한다. */
    private class SlowRepository : SearchRepository {
        private val pending = mutableListOf<CompletableDeferred<List<Track>>>()

        override suspend fun search(query: String): Result<List<Track>> {
            if (pending.isEmpty()) {
                val deferred = CompletableDeferred<List<Track>>()
                pending += deferred
                return Result.success(deferred.await())
            }
            return Result.success(listOf(Track("new", "제목 new", "아티스트", null, 180_000, SourceType.Remote)))
        }

        fun complete(index: Int, tracks: List<Track>) {
            pending[index].complete(tracks)
        }
    }
}

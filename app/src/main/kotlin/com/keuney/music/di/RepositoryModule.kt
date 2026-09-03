package com.keuney.music.di

import com.keuney.music.core.search.SearchHistoryRepository
import com.keuney.music.core.search.SearchRepository
import com.keuney.music.data.repository.SearchHistoryRepositoryImpl
import com.keuney.music.data.repository.SearchRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSearchRepository(repository: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(
        repository: SearchHistoryRepositoryImpl,
    ): SearchHistoryRepository
}

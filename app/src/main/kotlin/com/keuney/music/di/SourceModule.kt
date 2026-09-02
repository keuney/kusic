package com.keuney.music.di

import com.keuney.music.data.source.MusicSource
import com.keuney.music.data.source.providerA.ProviderAMusicSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SourceModule {
    @Binds
    @Singleton
    abstract fun bindMusicSource(source: ProviderAMusicSource): MusicSource
}

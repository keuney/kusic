package com.keuney.music.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path.Companion.toPath

internal fun createSettingsDataStore(
    file: File,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    storage = OkioStorage(
        fileSystem = FileSystem.SYSTEM,
        serializer = PreferencesSerializer,
        producePath = { file.absolutePath.toPath() },
    ),
    scope = scope,
)

/**
 * DataStore는 한 파일을 프로세스에서 하나만 열 수 있다. 주입 그래프가 다시 만들어져도
 * 같은 인스턴스를 쓰도록 프로세스 단위로 보관한다.
 */
internal object SettingsStoreHolder {
    @Volatile
    private var instance: DataStore<Preferences>? = null

    fun get(file: File): DataStore<Preferences> =
        instance ?: synchronized(this) {
            instance ?: createSettingsDataStore(file).also { instance = it }
        }
}

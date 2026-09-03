package com.keuney.music.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** KM-110: 새로 만든 데이터베이스가 열리고 다시 열리며, 기대한 표만 들어 있다. */
class KeuneyDatabaseTest {
    @Test
    fun opensAndReopensWithTheExpectedTables() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "keuney-smoke-${System.nanoTime()}.db"
        try {
            repeat(2) {
                val database = Room.databaseBuilder(context, KeuneyDatabase::class.java, name)
                    .addMigrations(*KEUNEY_MIGRATIONS)
                    .build()
                try {
                    val sqlite = database.openHelper.writableDatabase
                    assertEquals(2, sqlite.version)
                    assertEquals(EXPECTED_TABLES, database.userTables())
                    // 표는 비어 있다. 이 작업은 스키마까지이며 쓰기는 KM-111 이후다.
                    EXPECTED_TABLES.forEach { table ->
                        sqlite.query("SELECT count(*) FROM `$table`").use { cursor ->
                            cursor.moveToFirst()
                            assertEquals("$table 이 비어 있지 않다", 0, cursor.getInt(0))
                        }
                    }
                } finally {
                    database.close()
                }
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun noTableKeepsAPlaybackUrl() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "keuney-columns-${System.nanoTime()}.db"
        try {
            val database = Room.databaseBuilder(context, KeuneyDatabase::class.java, name)
                .addMigrations(*KEUNEY_MIGRATIONS)
                .build()
            try {
                val sqlite = database.openHelper.writableDatabase
                // AGENTS.md 8: 해석된 스트림 주소는 어디에도 저장하지 않는다. 앨범 이미지 주소는 예외다.
                database.userTables().forEach { table ->
                    sqlite.query("PRAGMA table_info(`$table`)").use { cursor ->
                        while (cursor.moveToNext()) {
                            val column = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                            assertTrue(
                                "$table.$column 이 재생 주소를 담을 수 있어 보인다",
                                column == "artwork_url" || !column.contains("url"),
                            )
                        }
                    }
                }
            } finally {
                database.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private fun KeuneyDatabase.userTables(): List<String> {
        val tables = mutableListOf<String>()
        openHelper.writableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
                "AND name NOT LIKE 'android_metadata' AND name NOT LIKE 'room_master_table' " +
                "ORDER BY name",
        ).use { cursor ->
            while (cursor.moveToNext()) tables += cursor.getString(0)
        }
        return tables
    }

    private companion object {
        val EXPECTED_TABLES = listOf(
            "favorites",
            "playback_history",
            "playlist_items",
            "playlists",
            "tracks",
        )
    }
}

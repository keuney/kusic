package com.keuney.music.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * KM-110: 버전 1 데이터베이스가 명시적 마이그레이션으로 버전 2가 된다.
 *
 * `runMigrationsAndValidate`가 결과 스키마를 내보낸 2.json과 견주므로, 마이그레이션 SQL이 Room이
 * 기대하는 정의와 한 글자라도 다르면 여기서 깨진다. 손으로 쓴 SQL을 믿을 수 있는 근거다.
 */
class KeuneyMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KeuneyDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun version1BecomesVersion2WithoutLosingTheDatabase() {
        // 버전 1에는 자리표시 표만 있었다. 실제로 그 상태를 만든다.
        helper.createDatabase(NAME, 1).use { old ->
            old.execSQL("INSERT INTO schema_baseline (id) VALUES (1)")
        }

        val migrated = helper.runMigrationsAndValidate(NAME, 2, true, MIGRATION_1_2)

        migrated.use { db ->
            assertEquals(2, db.version)
            val tables = mutableListOf<String>()
            db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
                    "AND name NOT LIKE 'android_metadata' AND name NOT LIKE 'room_master_table' " +
                    "ORDER BY name",
            ).use { cursor ->
                while (cursor.moveToNext()) tables += cursor.getString(0)
            }
            assertEquals(EXPECTED_TABLES, tables)
            assertTrue("자리표시 표가 남아 있다", "schema_baseline" !in tables)

            // 새 표는 쓸 수 있는 상태여야 한다. 외래 키 관계까지 확인한다.
            db.execSQL(
                "INSERT INTO tracks (id, title, artist, artwork_url, duration_ms, source) " +
                    "VALUES ('t1', '제목', '아티스트', NULL, 1000, 'Remote')",
            )
            db.execSQL("INSERT INTO favorites (track_id, added_at) VALUES ('t1', 1)")
            db.query("SELECT count(*) FROM favorites").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val NAME = "keuney-migration-test.db"
        val EXPECTED_TABLES = listOf(
            "favorites",
            "playback_history",
            "playlist_items",
            "playlists",
            "tracks",
        )
    }
}

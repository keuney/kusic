package com.keuney.music.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 버전 1은 자리표시 표 하나뿐이었고 버전 2에서 실제 라이브러리 표가 들어온다.
 *
 * 명시적 마이그레이션을 쓴다. 지우고 다시 만드는 방식은 이미 기기에 있는 데이터를 말없이
 * 없애는데, 이 앱은 사이드로드로 쓰이고 있어 그 데이터가 사용자의 유일한 사본이다.
 *
 * 아래 SQL은 손으로 쓴 것이 아니라 Room이 내보낸 스키마(app/schemas의 2.json)의 createSql을
 * 그대로 옮긴 것이다. 손으로 쓰면 Room이 기대하는 정의와 한 글자만 달라도 실행 중에 검증이
 * 깨진다. 표를 바꿀 때도 같은 방식으로 새 스키마 파일에서 옮긴다.
 * `KeuneyMigrationTest`가 내보낸 스키마와 실제 결과가 맞는지 확인한다.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 자리표시 표는 쓰인 적이 없다. 남겨 두면 스키마 검증이 어긋난다.
        db.execSQL("DROP TABLE IF EXISTS `schema_baseline`")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tracks` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`artist` TEXT NOT NULL, `artwork_url` TEXT, `duration_ms` INTEGER, " +
                "`source` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `favorites` (`track_id` TEXT NOT NULL, " +
                "`added_at` INTEGER NOT NULL, PRIMARY KEY(`track_id`), " +
                "FOREIGN KEY(`track_id`) REFERENCES `tracks`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `created_at` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlist_id` INTEGER NOT NULL, " +
                "`track_id` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`playlist_id`) REFERENCES `playlists`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`track_id`) REFERENCES `tracks`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playlist_items_playlist_id` " +
                "ON `playlist_items` (`playlist_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playlist_items_track_id` " +
                "ON `playlist_items` (`track_id`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playback_history` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `track_id` TEXT NOT NULL, " +
                "`played_at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`track_id`) REFERENCES `tracks`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playback_history_track_id` " +
                "ON `playback_history` (`track_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playback_history_played_at` " +
                "ON `playback_history` (`played_at`)",
        )
    }
}

/** 이 앱이 아는 모든 마이그레이션. 데이터베이스를 만드는 곳에서 한 번에 등록한다. */
internal val KEUNEY_MIGRATIONS = arrayOf(MIGRATION_1_2)

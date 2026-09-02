package com.keuney.music.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class KeuneyDatabaseTest {
    @Test
    fun opensAndReopensAnEmptyDatabase() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "keuney-smoke-${System.nanoTime()}.db"
        try {
            repeat(2) {
                val database = Room.databaseBuilder(context, KeuneyDatabase::class.java, name).build()
                try {
                    val sqlite = database.openHelper.writableDatabase
                    assertEquals(1, sqlite.version)
                    sqlite.query("SELECT count(*) FROM schema_baseline").use { cursor ->
                        cursor.moveToFirst()
                        assertEquals(0, cursor.getInt(0))
                    }
                } finally {
                    database.close()
                }
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}

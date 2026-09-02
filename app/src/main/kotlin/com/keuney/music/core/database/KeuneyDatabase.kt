package com.keuney.music.core.database

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Database(entities = [SchemaBaseline::class], version = 1, exportSchema = true)
abstract class KeuneyDatabase : RoomDatabase()

// Room requires at least one entity; library entities belong to KM-110.
@Entity(tableName = "schema_baseline")
internal data class SchemaBaseline(@PrimaryKey val id: Int)

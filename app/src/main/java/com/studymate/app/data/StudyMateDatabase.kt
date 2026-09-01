package com.studymate.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Single Room database backing StudyMate's offline vector store.
 *
 * DB name is fixed so it survives reinstalls of the same debug build. We deliberately
 * avoid `fallbackToDestructiveMigration` in release to protect user study data; add
 * explicit migrations if the schema evolves.
 */
@Database(
    entities = [DocumentEntity::class, ChunkEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(VectorConverters::class)
abstract class StudyMateDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun chunkDao(): ChunkDao

    companion object {
        @Volatile
        private var INSTANCE: StudyMateDatabase? = null

        fun get(context: Context): StudyMateDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyMateDatabase::class.java,
                    "studymate.db"
                )
                    // Schema export lands in app/schemas for versioned migrations.
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

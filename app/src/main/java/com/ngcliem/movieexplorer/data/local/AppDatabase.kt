package com.ngcliem.movieexplorer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Only the offline cache of the home sections stays on the device. Accounts, tickets,
 * reviews and favorites moved to Firebase in version 4.
 */
@Database(
    entities = [CachedMovieEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedMovieDao(): CachedMovieDao

    companion object {
        const val NAME = "movie_explorer.db"
    }
}

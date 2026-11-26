package com.ngcliem.movieexplorer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteMovieEntity::class,
        UserEntity::class,
        BookingEntity::class,
        UserReviewEntity::class,
        CachedMovieEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteMovieDao(): FavoriteMovieDao
    abstract fun userDao(): UserDao
    abstract fun bookingDao(): BookingDao
    abstract fun userReviewDao(): UserReviewDao
    abstract fun cachedMovieDao(): CachedMovieDao

    companion object {
        const val NAME = "movie_explorer.db"

        /** Version 2 adds accounts, bookings and reviews; existing favorites are kept. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `users` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`fullName` TEXT NOT NULL, `email` TEXT NOT NULL, `phone` TEXT NOT NULL, " +
                        "`passwordHash` TEXT NOT NULL, `salt` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bookings` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`userId` INTEGER NOT NULL, `code` TEXT NOT NULL, `movieId` INTEGER NOT NULL, " +
                        "`movieTitle` TEXT NOT NULL, `posterUrl` TEXT, `backdropUrl` TEXT, " +
                        "`cinema` TEXT NOT NULL, `showDateEpochDay` INTEGER NOT NULL, " +
                        "`showTime` TEXT NOT NULL, `format` TEXT NOT NULL, `seats` TEXT NOT NULL, " +
                        "`specialTags` TEXT NOT NULL, `totalPrice` INTEGER NOT NULL, " +
                        "`points` INTEGER NOT NULL, `bookedAt` INTEGER NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookings_userId` ON `bookings` (`userId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_reviews` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`movieId` INTEGER NOT NULL, `userId` INTEGER NOT NULL, " +
                        "`authorName` TEXT NOT NULL, `rating` INTEGER NOT NULL, " +
                        "`content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_user_reviews_movieId_userId` " +
                        "ON `user_reviews` (`movieId`, `userId`)",
                )
            }
        }

        /**
         * Version 3: favorites belong to a user (existing favorites are copied to every
         * existing account) and the home sections are cached for offline use.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorite_movies_new` (" +
                        "`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `overview` TEXT NOT NULL, " +
                        "`posterUrl` TEXT, `backdropUrl` TEXT, `releaseDate` TEXT NOT NULL, " +
                        "`voteAverage` REAL NOT NULL, `addedAt` INTEGER NOT NULL, " +
                        "`userId` INTEGER NOT NULL, PRIMARY KEY(`userId`, `id`))",
                )
                db.execSQL(
                    "INSERT INTO `favorite_movies_new` " +
                        "SELECT f.id, f.title, f.overview, f.posterUrl, f.backdropUrl, f.releaseDate, " +
                        "f.voteAverage, f.addedAt, u.id FROM `favorite_movies` f CROSS JOIN `users` u",
                )
                db.execSQL("DROP TABLE `favorite_movies`")
                db.execSQL("ALTER TABLE `favorite_movies_new` RENAME TO `favorite_movies`")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_movies` (" +
                        "`category` TEXT NOT NULL, `position` INTEGER NOT NULL, `id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `overview` TEXT NOT NULL, `posterUrl` TEXT, " +
                        "`backdropUrl` TEXT, `releaseDate` TEXT NOT NULL, `voteAverage` REAL NOT NULL, " +
                        "PRIMARY KEY(`category`, `position`))",
                )
            }
        }
    }
}

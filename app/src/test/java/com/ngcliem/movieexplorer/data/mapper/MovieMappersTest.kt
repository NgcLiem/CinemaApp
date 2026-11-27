package com.ngcliem.movieexplorer.data.mapper

import com.ngcliem.movieexplorer.data.remote.dto.GenreDto
import com.ngcliem.movieexplorer.data.remote.dto.MovieDetailDto
import com.ngcliem.movieexplorer.data.remote.dto.MovieDto
import com.ngcliem.movieexplorer.testutil.testMovie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieMappersTest {

    @Test
    fun `builds full poster url from TMDB path`() {
        val movie = MovieDto(id = 1, posterPath = "/abc.jpg").toDomain()

        assertEquals("https://image.tmdb.org/t/p/w500/abc.jpg", movie.posterUrl)
    }

    @Test
    fun `missing or blank image path gives null url`() {
        val movie = MovieDto(id = 1, posterPath = null, backdropPath = "  ").toDomain()

        assertNull(movie.posterUrl)
        assertNull(movie.backdropUrl)
    }

    @Test
    fun `missing release date becomes empty string`() {
        assertEquals("", MovieDto(id = 1, releaseDate = null).toDomain().releaseDate)
    }

    @Test
    fun `detail maps genres and cleans empty values`() {
        val detail = MovieDetailDto(
            id = 7,
            title = "Dune",
            tagline = "",
            runtime = 0,
            genres = listOf(GenreDto(1, "Sci-Fi"), GenreDto(2, "Adventure")),
        ).toDomain()

        assertEquals(listOf("Sci-Fi", "Adventure"), detail.genres)
        assertNull(detail.tagline)
        assertNull(detail.runtimeMinutes)
    }

    @Test
    fun `favorite round trip through Firestore data keeps movie data`() {
        val movie = testMovie(5)

        val restored = movie.toFavoriteData(addedAt = 123L).toFavoriteMovie()

        assertEquals(movie, restored)
    }
}

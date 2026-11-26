package com.ngcliem.movieexplorer.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
import com.ngcliem.movieexplorer.data.remote.dto.MovieDto
import com.ngcliem.movieexplorer.data.remote.dto.MovieListResponseDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MoviePagingSourceTest {

    private val config = PagingConfig(pageSize = 20)

    /** A fake API: each page contains one movie whose id equals the page number. */
    private fun fakeApi(totalPages: Int): suspend (Int) -> MovieListResponseDto = { page ->
        MovieListResponseDto(
            page = page,
            results = listOf(MovieDto(id = page, title = "Movie $page")),
            totalPages = totalPages,
        )
    }

    @Test
    fun `first page has no previous key and points to page 2`() = runTest {
        val pager = TestPager(config, MoviePagingSource(fakeApi(totalPages = 3)))

        val result = pager.refresh() as LoadResult.Page

        assertNull(result.prevKey)
        assertEquals(2, result.nextKey)
        assertEquals(1, result.data.single().id)
    }

    @Test
    fun `last page stops pagination`() = runTest {
        val pager = TestPager(config, MoviePagingSource(fakeApi(totalPages = 3)))

        pager.refresh()
        pager.append()
        val last = pager.append() as LoadResult.Page

        assertEquals(3, last.data.single().id)
        assertNull(last.nextKey)
    }

    @Test
    fun `stops at page 500 even if TMDB reports more pages`() = runTest {
        val pager = TestPager(config, MoviePagingSource(fakeApi(totalPages = 1000)))

        val result = pager.refresh(initialKey = 500) as LoadResult.Page

        assertNull(result.nextKey)
    }

    @Test
    fun `network failure returns an error result`() = runTest {
        val source = MoviePagingSource { throw IOException("offline") }
        val pager = TestPager(config, source)

        val result = pager.refresh()

        assertTrue(result is LoadResult.Error)
    }
}

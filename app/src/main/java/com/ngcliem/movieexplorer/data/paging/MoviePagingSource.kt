package com.ngcliem.movieexplorer.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ngcliem.movieexplorer.data.mapper.toDomain
import com.ngcliem.movieexplorer.data.remote.dto.MovieListResponseDto
import com.ngcliem.movieexplorer.domain.model.Movie
import kotlinx.coroutines.CancellationException

/**
 * Loads movies page by page for infinite scrolling.
 *
 * [loadPage] is the API call to make for a given page number, so the same class
 * is reused for "popular movies" and "search results".
 */
class MoviePagingSource(
    private val loadPage: suspend (page: Int) -> MovieListResponseDto,
) : PagingSource<Int, Movie>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: FIRST_PAGE
        return try {
            val response = loadPage(page)
            val movies = response.results.map { it.toDomain() }
            // TMDB never returns more than 500 pages, even if total_pages is higher.
            val lastPage = minOf(response.totalPages, MAX_PAGE)
            LoadResult.Page(
                data = movies,
                prevKey = if (page == FIRST_PAGE) null else page - 1,
                nextKey = if (page >= lastPage || movies.isEmpty()) null else page + 1,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    /** Decides which page to reload after a refresh, based on the current scroll position. */
    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        val anchor = state.anchorPosition ?: return null
        val closestPage = state.closestPageToPosition(anchor) ?: return null
        return closestPage.prevKey?.plus(1) ?: closestPage.nextKey?.minus(1)
    }

    companion object {
        const val FIRST_PAGE = 1
        const val MAX_PAGE = 500
    }
}

package com.ngcliem.movieexplorer.data.remote

import com.ngcliem.movieexplorer.data.remote.dto.MovieDetailDto
import com.ngcliem.movieexplorer.data.remote.dto.MovieListResponseDto
import com.ngcliem.movieexplorer.data.remote.dto.ReviewListResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit definition of the TMDB v3 endpoints used by the app. */
interface TmdbApi {

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int,
    ): MovieListResponseDto

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int,
    ): MovieListResponseDto

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: Int,
        /** Overrides the app language when not null (used for the English fallback). */
        @Query("language") language: String? = null,
    ): MovieDetailDto

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("page") page: Int,
    ): MovieListResponseDto

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("page") page: Int,
    ): MovieListResponseDto

    @GET("movie/{movie_id}/reviews")
    suspend fun getMovieReviews(
        @Path("movie_id") movieId: Int,
        @Query("page") page: Int = 1,
    ): ReviewListResponseDto
}

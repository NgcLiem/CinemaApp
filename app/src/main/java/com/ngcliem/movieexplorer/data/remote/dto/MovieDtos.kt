package com.ngcliem.movieexplorer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Data Transfer Objects: they mirror the JSON returned by the TMDB API.
// Default values protect the app when a field is missing in the response.

@Serializable
data class MovieListResponseDto(
    val page: Int = 1,
    val results: List<MovieDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
data class MovieDto(
    val id: Int,
    val title: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
)

@Serializable
data class MovieDetailDto(
    val id: Int,
    val title: String = "",
    val tagline: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val genres: List<GenreDto> = emptyList(),
)

@Serializable
data class GenreDto(
    val id: Int,
    val name: String,
)

@Serializable
data class ReviewListResponseDto(
    val page: Int = 1,
    val results: List<ReviewDto> = emptyList(),
)

@Serializable
data class ReviewDto(
    val id: String,
    val author: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("author_details") val authorDetails: AuthorDetailsDto? = null,
)

@Serializable
data class AuthorDetailsDto(
    val name: String? = null,
    val username: String? = null,
    val rating: Double? = null,
    @SerialName("avatar_path") val avatarPath: String? = null,
)

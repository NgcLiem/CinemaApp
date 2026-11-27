package com.ngcliem.movieexplorer.domain.model

data class User(
    /** Firebase uid. */
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    /** Epoch millis of the registration. */
    val createdAt: Long,
) {
    val initials: String
        get() = fullName.trim().split(" ").filter { it.isNotBlank() }
            .let { parts ->
                when {
                    parts.isEmpty() -> "?"
                    parts.size == 1 -> parts[0].take(1)
                    else -> parts.first().take(1) + parts.last().take(1)
                }
            }.uppercase()
}

package pl.apurtak.cinema.moviescatalog.model

import java.util.UUID

data class Movie(
    val id: UUID,
    val name: String,
    val durationMinutes: Int,
    val threeDimensionalGlassesNeeded: Boolean
)

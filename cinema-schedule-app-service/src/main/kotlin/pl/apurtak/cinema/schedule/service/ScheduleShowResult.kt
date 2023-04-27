package pl.apurtak.cinema.schedule.service

import pl.apurtak.cinema.moviescatalog.model.Movie
import java.time.LocalDate
import java.time.LocalTime

sealed interface ScheduleShowResult {
    data class Success(
        val roomId: String,
        val date: LocalDate,
        val startTime: LocalTime,
        val movie: Movie
    ) : ScheduleShowResult

    data class Error(
        val message: String
    ) : ScheduleShowResult
}
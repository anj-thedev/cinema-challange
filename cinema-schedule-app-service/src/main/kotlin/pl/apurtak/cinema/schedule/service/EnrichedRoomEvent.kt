package pl.apurtak.cinema.schedule.service

import pl.apurtak.cinema.moviescatalog.model.Movie
import java.time.LocalDate
import java.time.LocalTime

sealed interface EnrichedRoomEvent {

    @OptIn(ExperimentalStdlibApi::class)
    data class Show(
        val movie: Movie,
        val date: LocalDate,
        val startTime: LocalTime,
        val cleaningSlotTimeRange: OpenEndRange<LocalTime>
    ) : EnrichedRoomEvent

    data class Unavailability(
        val date: LocalDate,
        val startTime: LocalTime,
        val endTime: LocalTime
    ) : EnrichedRoomEvent
}

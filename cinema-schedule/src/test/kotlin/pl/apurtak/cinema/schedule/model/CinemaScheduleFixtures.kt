package pl.apurtak.cinema.schedule.model

import java.time.LocalDate
import java.time.LocalTime
import java.util.*

object CinemaScheduleFixtures {

    internal val defaultDate: LocalDate = LocalDate.of(2023, 4, 23)
    internal val defaultStartTime: LocalTime = LocalTime.of(12, 0)
    internal val sampleMovieId1 = UUID.randomUUID()
    internal val sampleMovieId2 = UUID.randomUUID()
    internal fun sampleShow(
        movieId: UUID = sampleMovieId1,
        date: LocalDate = defaultDate,
        startTime: LocalTime = defaultStartTime,
        durationMinutes: Int = 90,
        cleaningSlotDurationMinutes: Int = 15
    ) = RoomEvent.Show(
        movieId, date, startTime, durationMinutes, cleaningSlotDurationMinutes
    )
}

internal fun givenCinemaScheduleEvents(cinemaScheduleEvents: List<CinemaScheduleEvent>): CinemaSchedule =
    cinemaScheduleEvents.fold(CinemaSchedule.empty()) { schedule, event -> schedule.applyEvent(event) }
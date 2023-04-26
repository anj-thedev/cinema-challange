package pl.apurtak.cinema.schedule

import java.time.LocalDate
import java.time.LocalTime

sealed interface CinemaScheduleCommand {
    data class ScheduleRoomEvent(
        val roomId: String, val event: RoomEvent
    ) : CinemaScheduleCommand

    data class CancelRoomEvent(
        val roomId: String, val date: LocalDate, val startTime: LocalTime
    ) : CinemaScheduleCommand
}
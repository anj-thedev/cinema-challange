package pl.apurtak.cinema.schedule

import java.time.LocalDate
import java.time.LocalTime

sealed interface CinemaScheduleCommand {
    data class ScheduleRoomEventCommand(
        val roomId: String, val event: RoomEvent
    ) : CinemaScheduleCommand

    data class CancelRoomEventCommand(
        val roomId: String, val date: LocalDate, val startTime: LocalTime
    ) : CinemaScheduleCommand
}
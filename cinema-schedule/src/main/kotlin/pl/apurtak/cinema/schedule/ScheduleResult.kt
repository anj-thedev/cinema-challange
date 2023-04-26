package pl.apurtak.cinema.schedule

import java.time.LocalTime

sealed interface ScheduleResult {
    data class Success(
        val events: List<CinemaScheduleEvent>
    ) : ScheduleResult

    sealed interface Error : ScheduleResult {
        data class RoomOccupied(
            val conflictingEvent: RoomEvent
        ) : Error

        data class CleaningServiceUnavailable(
            val occupyingRoomId: String,
            val cleaningSlotEndTime: LocalTime
        ) : Error
    }

}
package pl.apurtak.cinema.schedule

sealed interface CinemaScheduleEvent {
    data class RoomEventAdded(
        val roomId: String, val roomEvent: RoomEvent
    ) : CinemaScheduleEvent

    data class RoomEventCancelled(
        val roomId: String, val roomEvent: RoomEvent
    ) : CinemaScheduleEvent
}
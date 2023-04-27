package pl.apurtak.cinema.schedule.model

sealed interface CinemaScheduleEvent {
    val version: Long
    data class RoomEventAdded(
        override val version: Long,
        val roomId: String,
        val event: RoomEvent
    ) : CinemaScheduleEvent

    data class RoomEventCancelled(
        override val version: Long,
        val roomId: String,
        val roomEvent: RoomEvent
    ) : CinemaScheduleEvent
}
package pl.apurtak.cinema.schedule

import pl.apurtak.cinema.schedule.CinemaScheduleEvent.*
import java.time.LocalDate

data class CinemaSchedule(
    private val roomEventsById: Map<String, List<RoomEvent>>
) {
    fun process(scheduleCommand: CinemaScheduleCommand): ScheduleResult {
        return when (scheduleCommand) {
            is CinemaScheduleCommand.CancelRoomEvent -> processCancelRoomEventCommand(scheduleCommand)
            is CinemaScheduleCommand.ScheduleRoomEvent -> processScheduleRoomEventCommand(scheduleCommand)
        }
    }

    private fun processCancelRoomEventCommand(cancelRoomEventCommand: CinemaScheduleCommand.CancelRoomEvent): ScheduleResult {
        val roomEventToCancel = roomEventsById[cancelRoomEventCommand.roomId]
            ?.find { it.date == cancelRoomEventCommand.date && it.startTime == cancelRoomEventCommand.startTime }
        return ScheduleResult.Success(listOfNotNull(roomEventToCancel).map {
            RoomEventCancelled(
                cancelRoomEventCommand.roomId, it
            )
        })
    }

    private fun processScheduleRoomEventCommand(scheduleRoomEvent: CinemaScheduleCommand.ScheduleRoomEvent): ScheduleResult {
        val overlappingRoomEvent: RoomEvent? = findFirstOverlappingRoomEvent(scheduleRoomEvent)

        val cleaningServiceUnavailable: ScheduleResult.Error.CleaningServiceUnavailable? =
            cleaningServiceUnavailable(scheduleRoomEvent)
        return when {
            cleaningServiceUnavailable != null -> cleaningServiceUnavailable
            overlappingRoomEvent != null -> ScheduleResult.Error.RoomOccupied(overlappingRoomEvent)
            else -> ScheduleResult.Success(
                events = listOf(
                    RoomEventAdded(
                        roomId = scheduleRoomEvent.roomId, roomEvent = scheduleRoomEvent.event
                    )
                )
            )
        }
    }

    private fun cleaningServiceUnavailable(scheduleRoomEvent: CinemaScheduleCommand.ScheduleRoomEvent): ScheduleResult.Error.CleaningServiceUnavailable? {
        val showToBeScheduled = scheduleRoomEvent.event as? RoomEvent.Show
        if (showToBeScheduled == null) return null
        else {
            val showsWithOverlappingCleaningSlotsByRoomId = roomEventsById.mapValues { entry ->
                entry.value
                    .mapNotNull { it as? RoomEvent.Show }
                    .filter { it.date == scheduleRoomEvent.event.date }
                    .filter { it.hasOverlappingCleaningSlotWith(showToBeScheduled) }
            }
            return showsWithOverlappingCleaningSlotsByRoomId
                .filterValues { it.isNotEmpty() }
                .entries
                .firstOrNull()
                ?.let { entry ->
                    ScheduleResult.Error.CleaningServiceUnavailable(entry.key, entry.value.first().endTime)
                }

        }
    }


    private fun findFirstOverlappingRoomEvent(scheduleCommand: CinemaScheduleCommand.ScheduleRoomEvent): RoomEvent? {
        return roomEventsById[scheduleCommand.roomId]
            ?.find { roomEventsOverlap(scheduleCommand.event, it) }
    }

    private fun roomEventsOverlap(roomEvent1: RoomEvent, roomEvent2: RoomEvent): Boolean {
        return roomEvent1.startTime.isAfter(roomEvent2.startTime).and(roomEvent1.startTime.isBefore(roomEvent2.endTime))
            .or(
                roomEvent2.startTime.isAfter(roomEvent1.startTime)
                    .and(roomEvent2.startTime.isBefore(roomEvent1.endTime))
            )
    }

    fun applyEvent(scheduleEvent: CinemaScheduleEvent): CinemaSchedule {
        return when (scheduleEvent) {
            is RoomEventAdded -> CinemaSchedule(
                addRoomEvent(roomEventsById, scheduleEvent.roomId, scheduleEvent.roomEvent)
            )
            is RoomEventCancelled -> applyRoomEventCancelled(scheduleEvent)
        }
    }

    private fun addRoomEvent(
        roomEventsById: Map<String, List<RoomEvent>>,
        roomId: String,
        roomEvent: RoomEvent
    ): Map<String, List<RoomEvent>> {
        return roomEventsById.plus(Pair(roomId, (roomEventsById[roomId] ?: emptyList()).plus(roomEvent)))
    }

    private fun applyRoomEventCancelled(roomEventCancelled: RoomEventCancelled): CinemaSchedule {
        val allRoomEvents = roomEventsById[roomEventCancelled.roomId]
        val roomEventsWithoutCancelledEvent =
            allRoomEvents?.filterNot { it == roomEventCancelled.roomEvent } ?: emptyList()
        return CinemaSchedule(
            roomEventsById.plus(Pair(roomEventCancelled.roomId, roomEventsWithoutCancelledEvent))
        )
    }

    fun getRoomSchedule(roomId: String): List<RoomEvent> {
        return roomEventsById[roomId]?.sortedBy { it.date.atTime(it.startTime) } ?: emptyList()
    }

    fun getRoomSchedule(roomId: String, date: LocalDate): List<RoomEvent> {
        return getRoomSchedule(roomId).filter { it.date == date }
    }

    companion object {
        fun empty(): CinemaSchedule {
            return CinemaSchedule(emptyMap())
        }
    }
}
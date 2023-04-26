package pl.apurtak.cinema.schedule

import pl.apurtak.cinema.schedule.CinemaScheduleEvent.*
import java.time.LocalDate

data class CinemaSchedule(
    private val version: Long,
    private val roomEventsByRoomId: Map<String, List<RoomEvent>>
) {
    fun process(scheduleCommand: CinemaScheduleCommand): ScheduleResult {
        return when (scheduleCommand) {
            is CinemaScheduleCommand.CancelRoomEventCommand -> processCancelRoomEventCommand(scheduleCommand)
            is CinemaScheduleCommand.ScheduleRoomEventCommand -> processScheduleRoomEventCommand(scheduleCommand)
        }
    }

    private fun processCancelRoomEventCommand(cancelRoomEventCommand: CinemaScheduleCommand.CancelRoomEventCommand): ScheduleResult {
        val roomEventToCancel = roomEventsByRoomId[cancelRoomEventCommand.roomId]
            ?.find { it.date == cancelRoomEventCommand.date && it.startTime == cancelRoomEventCommand.startTime }
        return ScheduleResult.Success(listOfNotNull(roomEventToCancel).map {
            RoomEventCancelled(
                version + 1,
                cancelRoomEventCommand.roomId, it
            )
        })
    }

    private fun processScheduleRoomEventCommand(scheduleRoomEvent: CinemaScheduleCommand.ScheduleRoomEventCommand): ScheduleResult {
        val overlappingRoomEvent: RoomEvent? = findFirstOverlappingRoomEvent(scheduleRoomEvent)

        val cleaningServiceUnavailable: ScheduleResult.Error.CleaningServiceUnavailable? =
            cleaningServiceUnavailable(scheduleRoomEvent)
        return when {
            cleaningServiceUnavailable != null -> cleaningServiceUnavailable
            overlappingRoomEvent != null -> ScheduleResult.Error.RoomOccupied(overlappingRoomEvent)
            else -> ScheduleResult.Success(
                events = listOf(
                    RoomEventAdded(
                        version + 1,
                        roomId = scheduleRoomEvent.roomId, event = scheduleRoomEvent.event
                    )
                )
            )
        }
    }

    private fun cleaningServiceUnavailable(scheduleRoomEvent: CinemaScheduleCommand.ScheduleRoomEventCommand): ScheduleResult.Error.CleaningServiceUnavailable? {
        val showToBeScheduled = scheduleRoomEvent.event as? RoomEvent.Show
        if (showToBeScheduled == null) return null
        else {
            val showsWithOverlappingCleaningSlotsByRoomId = roomEventsByRoomId.mapValues { entry ->
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


    private fun findFirstOverlappingRoomEvent(scheduleCommand: CinemaScheduleCommand.ScheduleRoomEventCommand): RoomEvent? {
        return roomEventsByRoomId[scheduleCommand.roomId]
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
                scheduleEvent.version,
                addRoomEvent(roomEventsByRoomId, scheduleEvent.roomId, scheduleEvent.event)
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
        val allRoomEvents = roomEventsByRoomId[roomEventCancelled.roomId]
        val roomEventsWithoutCancelledEvent =
            allRoomEvents?.filterNot { it == roomEventCancelled.roomEvent } ?: emptyList()
        return CinemaSchedule(
            version + 1,
            roomEventsByRoomId.plus(Pair(roomEventCancelled.roomId, roomEventsWithoutCancelledEvent))
                .filterValues { it.isNotEmpty() }
        )
    }

    fun getRoomSchedule(roomId: String): List<RoomEvent> {
        return roomEventsByRoomId[roomId]?.sortedBy { it.date.atTime(it.startTime) } ?: emptyList()
    }

    fun getRoomSchedule(roomId: String, date: LocalDate): List<RoomEvent> {
        return getRoomSchedule(roomId).filter { it.date == date }
    }

    companion object {
        fun empty(): CinemaSchedule {
            return CinemaSchedule(0, emptyMap())
        }
    }
}
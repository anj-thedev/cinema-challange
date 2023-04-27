package pl.apurtak.cinema.schedule.model

import java.time.LocalDate
import java.time.LocalTime
import java.util.*

sealed interface RoomEvent {
    val date: LocalDate
    val startTime: LocalTime
    val endTime: LocalTime

    data class Show(
        val movieId: UUID,
        override val date: LocalDate,
        override val startTime: LocalTime,
        val durationMinutes: Int,
        val cleaningSlotDurationMinutes: Int
    ) : RoomEvent {
        override val endTime: LocalTime
            get() = startTime.plusMinutes(durationMinutes.toLong()).plusMinutes(cleaningSlotDurationMinutes.toLong())
        val cleaningSlotStartTime: LocalTime = startTime.plusMinutes(durationMinutes.toLong())
        val cleaningSlotEndTime: LocalTime = cleaningSlotStartTime.plusMinutes(cleaningSlotDurationMinutes.toLong())

        @OptIn(ExperimentalStdlibApi::class)
        fun hasOverlappingCleaningSlotWith(other: Show): Boolean {
            val cleaningSlotRange = cleaningSlotStartTime..<cleaningSlotEndTime
            return other.cleaningSlotStartTime in cleaningSlotRange || other.cleaningSlotEndTime in cleaningSlotRange
        }
    }

    data class Unavailability(
        override val date: LocalDate,
        override val startTime: LocalTime,
        override val endTime: LocalTime
    ) : RoomEvent
}
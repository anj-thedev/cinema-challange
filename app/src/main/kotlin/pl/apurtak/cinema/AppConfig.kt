package pl.apurtak.cinema

import pl.apurtak.cinema.schedule.service.CinemaScheduleConfig
import java.time.LocalTime

data class AppConfig(
    override val cleaningSlotDurationMinutes: Int,
    val allowedShowStartTimeRange: ClosedRange<LocalTime>,
    val allowedPremierStartTimeRange: ClosedRange<LocalTime>
) : CinemaScheduleConfig

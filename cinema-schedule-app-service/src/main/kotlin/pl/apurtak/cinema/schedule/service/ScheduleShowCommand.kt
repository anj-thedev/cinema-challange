package pl.apurtak.cinema.schedule.service

import java.time.LocalDate
import java.time.LocalTime

data class ScheduleShowCommand(
    val roomId: String,
    val movieName: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val isPremier: Boolean
)
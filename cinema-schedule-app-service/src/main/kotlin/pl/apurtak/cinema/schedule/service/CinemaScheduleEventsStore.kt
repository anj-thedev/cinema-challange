package pl.apurtak.cinema.schedule.service

import pl.apurtak.cinema.schedule.model.CinemaScheduleEvent

interface CinemaScheduleEventsStore {
    fun findEventsHistory(): List<CinemaScheduleEvent>
    fun addEvents(newEvents: List<CinemaScheduleEvent>)
}
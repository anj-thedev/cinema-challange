package pl.apurtak.cinema.schedule.service

import pl.apurtak.cinema.schedule.model.CinemaScheduleEvent

class InMemoryCinemaScheduleEventStore : CinemaScheduleEventsStore {
    private var events: List<CinemaScheduleEvent> = emptyList()

    override fun findEventsHistory(): List<CinemaScheduleEvent> {
        return events.sortedBy { it.version }
    }

    override fun addEvents(newEvents: List<CinemaScheduleEvent>) {
        this.events = this.events.plus(newEvents)
    }
}

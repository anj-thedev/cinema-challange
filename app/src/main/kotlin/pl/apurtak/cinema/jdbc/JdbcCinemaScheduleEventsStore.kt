package pl.apurtak.cinema.jdbc

import pl.apurtak.cinema.schedule.model.CinemaScheduleEvent
import pl.apurtak.cinema.schedule.service.CinemaScheduleEventsStore

/**
 * In-Memory impl - but in real life we could use for example a JDBC based event store, that would put unique index on event version
 * This JDBC impl could be placed in a separate module, or in a well named package
 */
class JdbcCinemaScheduleEventsStore : CinemaScheduleEventsStore {
    private var events: List<CinemaScheduleEvent> = emptyList()

    override fun findEventsHistory(): List<CinemaScheduleEvent> {
        return events.sortedBy { it.version }
    }

    override fun addEvents(newEvents: List<CinemaScheduleEvent>) {
        this.events = this.events.plus(newEvents)
    }
}

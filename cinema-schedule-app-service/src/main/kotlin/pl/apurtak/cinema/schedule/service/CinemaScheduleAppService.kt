package pl.apurtak.cinema.schedule.service

import pl.apurtak.cinema.moviescatalog.MoviesCatalog
import pl.apurtak.cinema.moviescatalog.model.Movie
import pl.apurtak.cinema.schedule.model.CinemaSchedule
import pl.apurtak.cinema.schedule.model.CinemaScheduleCommand.ScheduleRoomEventCommand
import pl.apurtak.cinema.schedule.model.RoomEvent
import pl.apurtak.cinema.schedule.model.ScheduleResult
import java.time.LocalDate
import java.util.UUID

class CinemaScheduleAppService(
    private val cinemaScheduleEventsStore: CinemaScheduleEventsStore,
    private val moviesCatalog: MoviesCatalog,
    private val scheduleCommandValidators: List<ScheduleCommandValidator>,
    private val cinemaScheduleConfig: CinemaScheduleConfig
) {
    fun scheduleShow(command: ScheduleShowCommand): ScheduleShowResult {
        val validationError = validate(command)
        if(validationError != null) return validationError

        val movie = moviesCatalog.findByName(command.movieName)
            ?: return ScheduleShowResult.Error("Movie ${command.movieName} not found in the catalog")

        val currentCinemaSchedule = getCurrentCinemaSchedule()
        val scheduleResult = currentCinemaSchedule.process(
            scheduleRoomEventDomainCommand(command, movie)
        )

        return when(scheduleResult) {
            is ScheduleResult.Error.CleaningServiceUnavailable -> ScheduleShowResult.Error("Cannot schedule show - cleaning service would be unavailable after it")
            is ScheduleResult.Error.RoomOccupied -> ScheduleShowResult.Error("Cannot schedule show - room is occupied in given time")
            is ScheduleResult.Success -> {
                cinemaScheduleEventsStore.addEvents(scheduleResult.events)
                ScheduleShowResult.Success(command.roomId, command.date, command.startTime, movie)
            }
        }
    }

    private fun validate(command: ScheduleShowCommand) =
        scheduleCommandValidators.asSequence()
            .mapNotNull { it.validate(command) }
            .firstOrNull()

    private fun getCurrentCinemaSchedule() = cinemaScheduleEventsStore.findEventsHistory()
        .foldRight(CinemaSchedule.empty()) { event, schedule -> schedule.applyEvent(event) }

    private fun scheduleRoomEventDomainCommand(
        command: ScheduleShowCommand,
        movie: Movie
    ) = ScheduleRoomEventCommand(
        command.roomId,
        RoomEvent.Show(
            movieId = movie.id,
            date = command.date,
            startTime = command.startTime,
            durationMinutes = movie.durationMinutes,
            cleaningSlotDurationMinutes = cinemaScheduleConfig.cleaningSlotDurationMinutes
        )
    )

    fun getRoomSchedule(roomId: String, date: LocalDate): List<EnrichedRoomEvent> {
        val currentCinemaSchedule = getCurrentCinemaSchedule()
        val movieByName = moviesCatalog.listMovies().associateBy { it.id }

        return currentCinemaSchedule.getRoomSchedule(roomId, date).map {
            enrichShowsWithMovieData(it, movieByName)
        }
    }

    private fun enrichShowsWithMovieData(roomEvent: RoomEvent, movieByName: Map<UUID, Movie>): EnrichedRoomEvent {
        return when(roomEvent) {
            is RoomEvent.Show -> {
                val movie = movieByName[roomEvent.movieId] ?: throw IllegalStateException("Movie with Id ${roomEvent.movieId} not found in catalog")
                EnrichedRoomEvent.Show(
                    movie, roomEvent.date, roomEvent.startTime
                )
            }
            is RoomEvent.Unavailability -> EnrichedRoomEvent.Unavailability(roomEvent.date, roomEvent.startTime, roomEvent.endTime)
        }
    }
}

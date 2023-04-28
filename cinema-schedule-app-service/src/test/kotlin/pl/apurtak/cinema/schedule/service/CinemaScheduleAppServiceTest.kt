@file:OptIn(ExperimentalStdlibApi::class)

package pl.apurtak.cinema.schedule.service

import assertk.assertThat
import assertk.assertions.containsOnly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.apurtak.cinema.moviescatalog.InMemoryMoviesCatalog
import pl.apurtak.cinema.moviescatalog.MoviesCatalog
import pl.apurtak.cinema.moviescatalog.model.Movie
import pl.apurtak.cinema.schedule.model.CinemaScheduleEvent
import pl.apurtak.cinema.schedule.model.RoomEvent
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class CinemaScheduleAppServiceTest {

    private lateinit var cinemaScheduleEventsStore: CinemaScheduleEventsStore
    private lateinit var cinemaScheduleAppService: CinemaScheduleAppService
    private lateinit var moviesCatalog: MoviesCatalog

    @BeforeEach
    fun init() {
        cinemaScheduleEventsStore = InMemoryCinemaScheduleEventStore()
        moviesCatalog = InMemoryMoviesCatalog()
        cinemaScheduleAppService = CinemaScheduleAppService(
            cinemaScheduleEventsStore = cinemaScheduleEventsStore,
            moviesCatalog = moviesCatalog,
            scheduleCommandValidators = emptyList(),
            cinemaScheduleConfig = CinemaScheduleSimpleConfig(15)
        )
    }

    @Test
    fun `should schedule a movie and persist events`() {
        // given
        moviesCatalog.add(SAMPLE_MOVIE)

        // when
        cinemaScheduleAppService.scheduleShow(
            ScheduleShowCommand(
                roomId = "1",
                movieName = SAMPLE_MOVIE.name,
                date = SAMPLE_DATE,
                startTime = SAMPLE_START_TIME,
                isPremier = false
            )
        )

        // then
        assertThat(cinemaScheduleEventsStore.findEventsHistory())
            .containsOnly(
                CinemaScheduleEvent.RoomEventAdded(
                    version = 1,
                    roomId = "1",
                    event = SAMPLE_SHOW
                )
            )
    }

    @Test
    fun `should get room schedule`() {
        // given
        moviesCatalog.add(SAMPLE_MOVIE)
        moviesCatalog.add(SAMPLE_MOVIE_2)
        cinemaScheduleAppService.scheduleShow(
            ScheduleShowCommand(
                roomId = "1",
                movieName = SAMPLE_MOVIE.name,
                date = SAMPLE_DATE,
                startTime = SAMPLE_START_TIME,
                isPremier = false
            )
        )
        cinemaScheduleAppService.scheduleShow(
            ScheduleShowCommand(
                roomId = "1",
                movieName = SAMPLE_MOVIE_2.name,
                date = SAMPLE_DATE,
                startTime = SAMPLE_START_TIME_2,
                isPremier = false
            )
        )


        // when
        val roomSchedule = cinemaScheduleAppService.getRoomSchedule("1", SAMPLE_DATE)
        // then
        assertThat(roomSchedule).containsOnly(
            EnrichedRoomEvent.Show(
                movie = SAMPLE_MOVIE,
                date = SAMPLE_DATE,
                startTime = SAMPLE_START_TIME,
                cleaningSlotTimeRange = SAMPLE_SHOW_CLEANING_SLOT
            ),
            EnrichedRoomEvent.Show(
                movie = SAMPLE_MOVIE_2,
                date = SAMPLE_DATE,
                startTime = SAMPLE_START_TIME_2,
                cleaningSlotTimeRange = SAMPLE_SHOW_2_CLEANING_SLOT
            )
        )
    }

    companion object {
        private val SAMPLE_DATE = LocalDate.of(2023, 4, 27)
        private val SAMPLE_START_TIME = LocalTime.of(18, 0)
        private val SAMPLE_SHOW = RoomEvent.Show(
            movieId = UUID.randomUUID(),
            date = SAMPLE_DATE,
            startTime = SAMPLE_START_TIME,
            durationMinutes = 110,
            cleaningSlotDurationMinutes = 15
        )
        private val SAMPLE_SHOW_CLEANING_SLOT = SAMPLE_SHOW.cleaningSlotStartTime..<SAMPLE_SHOW.cleaningSlotEndTime
        private val SAMPLE_START_TIME_2 = SAMPLE_START_TIME.plusHours(3)
        private val SAMPLE_SHOW_2 = SAMPLE_SHOW.copy(
            movieId = UUID.randomUUID(),
            startTime = SAMPLE_START_TIME_2,
            durationMinutes = 90,
            cleaningSlotDurationMinutes = 15
        )
        private val SAMPLE_SHOW_2_CLEANING_SLOT =
            SAMPLE_SHOW_2.cleaningSlotStartTime..<SAMPLE_SHOW_2.cleaningSlotEndTime
        private val SAMPLE_MOVIE = Movie(
            id = SAMPLE_SHOW.movieId,
            name = "Shrek",
            durationMinutes = SAMPLE_SHOW.durationMinutes,
            threeDimensionalGlassesNeeded = false
        )
        private val SAMPLE_MOVIE_2 = Movie(
            id = SAMPLE_SHOW_2.movieId,
            name = "Cinderella",
            durationMinutes = SAMPLE_SHOW_2.durationMinutes,
            threeDimensionalGlassesNeeded = false
        )

    }
}

data class CinemaScheduleSimpleConfig(override val cleaningSlotDurationMinutes: Int) : CinemaScheduleConfig

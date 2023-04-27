package pl.apurtak.cinema.schedule.model

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test
import pl.apurtak.cinema.schedule.model.CinemaScheduleCommand.*
import pl.apurtak.cinema.schedule.model.CinemaScheduleEvent.*
import pl.apurtak.cinema.schedule.model.CinemaScheduleFixtures.defaultDate
import pl.apurtak.cinema.schedule.model.CinemaScheduleFixtures.sampleMovieId1
import pl.apurtak.cinema.schedule.model.CinemaScheduleFixtures.sampleMovieId2
import pl.apurtak.cinema.schedule.model.CinemaScheduleFixtures.sampleShow
import java.time.LocalDate
import java.time.LocalTime

class CinemaScheduleTest {

    @Test
    fun `should schedule a movie show`() {
        // given
        val cinemaSchedule = CinemaSchedule.empty()

        // when
        val result = cinemaSchedule.process(
            ScheduleRoomEventCommand(
                roomId = "1", event = sampleShow()
            )
        )

        // then
        assertThat(result).isInstanceOf(ScheduleResult.Success::class).isEqualTo(
            ScheduleResult.Success(
                listOf(
                    RoomEventAdded(
                        version = 1,
                        roomId = "1",
                        event = sampleShow()
                    )
                )
            )
        )

    }

    @Test
    fun `should show room schedule for a given date`() {
        // given
        val cinemaSchedule = givenCinemaScheduleEvents(
            listOf(
                RoomEventAdded(
                    version = 1,
                    roomId = "1",
                    event = sampleShow(
                        movieId = sampleMovieId1, date = defaultDate, startTime = LocalTime.of(21, 30)
                    )
                ), RoomEventAdded(
                    version = 2, roomId = "1", event = sampleShow(
                        movieId = sampleMovieId2, date = defaultDate, startTime = LocalTime.of(18, 0)
                    )
                ), RoomEventAdded(
                    version = 3, roomId = "1", event = sampleShow(
                        movieId = sampleMovieId2, date = defaultDate.plusDays(1), startTime = LocalTime.of(18, 0)
                    )
                )
            )
        )

        // when
        val roomSchedule = cinemaSchedule.getRoomSchedule("1", defaultDate)

        // then
        assertThat(roomSchedule).containsExactly(
            sampleShow(
                movieId = sampleMovieId2, date = defaultDate, startTime = LocalTime.of(18, 0)
            ), sampleShow(
                movieId = sampleMovieId1, date = defaultDate, startTime = LocalTime.of(21, 30)
            )
        )
    }

    @Test
    fun `should show room schedule`() {
        // given
        val halfPastFivePm = LocalTime.of(17, 30)
        val eightPm = LocalTime.of(20, 0)
        val cinemaSchedule = givenCinemaScheduleEvents(
            listOf(
                RoomEventAdded(1, "1", sampleShow(sampleMovieId1, defaultDate, halfPastFivePm, 90)),
                RoomEventAdded(
                    2,
                    "1",
                    sampleShow(sampleMovieId2, defaultDate.minusDays(1), eightPm, 110)
                )
            )
        )

        // when
        val roomSchedule = cinemaSchedule.getRoomSchedule("1")

        // then
        assertThat(roomSchedule).containsExactly(
            sampleShow(sampleMovieId2, defaultDate.minusDays(1), eightPm, 110),
            sampleShow(sampleMovieId1, defaultDate, halfPastFivePm, 90)
        )
    }

    @Test
    fun `should not schedule two overlapping shows`() {
        // given
        val cinemaSchedule = givenCinemaScheduleEvents(
            listOf(
                RoomEventAdded(
                    1, "1", sampleShow(
                        movieId = sampleMovieId1, startTime = LocalTime.of(17, 30), durationMinutes = 90
                    )
                )
            )
        )

        // when
        val scheduleResult = cinemaSchedule.process(
            ScheduleRoomEventCommand(
                "1", sampleShow(movieId = sampleMovieId2, startTime = LocalTime.of(18, 59))
            )
        )

        // then
        assertThat(scheduleResult).isInstanceOf(ScheduleResult.Error.RoomOccupied::class)
            .transform { it.conflictingEvent }
            .isEqualTo(
                sampleShow(
                    movieId = sampleMovieId1, startTime = LocalTime.of(17, 30), durationMinutes = 90
                )
            )
    }

    @Test
    fun `should schedule two overlapping shows in different rooms`() {
        // given
        val cinemaSchedule = givenCinemaScheduleEvents(
            listOf(
                RoomEventAdded(
                    1, "1", sampleShow(
                        movieId = sampleMovieId1, startTime = LocalTime.of(17, 30), durationMinutes = 90
                    )
                )
            )
        )

        // when
        val scheduleResult = cinemaSchedule.process(
            ScheduleRoomEventCommand(
                "2", sampleShow(movieId = sampleMovieId2, startTime = LocalTime.of(17, 0))
            )
        )

        // then
        assertThat(scheduleResult).isInstanceOf(ScheduleResult.Success::class).transform { it.events }
            .containsExactly(
                RoomEventAdded(
                    2, "2",
                    sampleShow(movieId = sampleMovieId2, startTime = LocalTime.of(17, 0))
                )
            )
    }

    @Test
    fun `should cancel scheduled room event`() {
        // given
        val cinemaSchedule = givenCinemaScheduleEvents(
            listOf(
                RoomEventAdded(1, "1", sampleShow(sampleMovieId1, defaultDate, LocalTime.of(17, 0))),
                RoomEventAdded(2, "1", sampleShow(sampleMovieId2, defaultDate, LocalTime.of(20, 0)))
            )
        )

        // when
        val scheduleResult =
            cinemaSchedule.process(CancelRoomEventCommand("1", defaultDate, LocalTime.of(17, 0)))

        // then
        assertThat(scheduleResult).isInstanceOf(ScheduleResult.Success::class).transform { it.events }.containsExactly(
            RoomEventCancelled(
                3, "1", sampleShow(sampleMovieId1, defaultDate, LocalTime.of(17, 0))
            )
        )
    }

    @Test
    fun `should not include cancelled room event in schedule`() {
        // given
        val cinemaSchedule = givenCinemaScheduleEvents(
            listOf(
                RoomEventAdded(1, "1", sampleShow(sampleMovieId1, defaultDate, LocalTime.of(17, 0))),
                RoomEventAdded(2, "1", sampleShow(sampleMovieId2, defaultDate, LocalTime.of(20, 0))),
                RoomEventCancelled(
                    3, "1", sampleShow(sampleMovieId1, defaultDate, LocalTime.of(17, 0))
                )
            )
        )

        // when
        val roomSchedule = cinemaSchedule.getRoomSchedule("1", defaultDate)

        // then
        assertThat(roomSchedule).containsOnly(sampleShow(sampleMovieId2, defaultDate, LocalTime.of(20, 0)))
    }

    @Test
    fun `should not schedule a show overlapping with other show's cleaning slot`() {
        // given
        val halfPastFivePm = LocalTime.of(17, 30)
        val cinemaSchedule = givenCinemaScheduleEvents(
            listOf(
                RoomEventAdded(
                    1, "1",
                    sampleShow(
                        movieId = sampleMovieId1,
                        startTime = halfPastFivePm,
                        durationMinutes = 90,
                        cleaningSlotDurationMinutes = 15
                    )
                )
            )
        )

        // when
        val scheduleResult = cinemaSchedule.process(
            ScheduleRoomEventCommand(
                "1", sampleShow(movieId = sampleMovieId2, startTime = halfPastFivePm.plusMinutes(90))
            )
        )

        // then
        assertThat(scheduleResult).isInstanceOf(ScheduleResult.Error.RoomOccupied::class)
            .transform { it.conflictingEvent }
            .isEqualTo(
                sampleShow(
                    movieId = sampleMovieId1,
                    startTime = halfPastFivePm,
                    durationMinutes = 90,
                    cleaningSlotDurationMinutes = 15
                )
            )
    }

    @Test
    fun `should schedule a show just after other show's cleaning slot`() {
        // given
        val halfPastFivePm = LocalTime.of(17, 30)
        val cinemaSchedule = givenCinemaScheduleEvents(
            listOf(
                RoomEventAdded(
                    1, "1",
                    sampleShow(
                        movieId = sampleMovieId1,
                        startTime = halfPastFivePm,
                        durationMinutes = 90,
                        cleaningSlotDurationMinutes = 15
                    )
                )
            )
        )

        // when
        val scheduleResult = cinemaSchedule.process(
            ScheduleRoomEventCommand(
                "1",
                sampleShow(
                    movieId = sampleMovieId2,
                    startTime = halfPastFivePm.plusMinutes(90).plusMinutes(15)
                )
            )
        )

        // then
        assertThat(scheduleResult).isInstanceOf(ScheduleResult.Success::class)
            .transform { it.events }
            .containsOnly(
                RoomEventAdded(
                    2,
                    roomId = "1",
                    event = sampleShow(
                        movieId = sampleMovieId2,
                        startTime = halfPastFivePm.plusMinutes(90).plusMinutes(15)
                    )
                )
            )
    }

    @Test
    fun `should not schedule a show overlapping with other room's show's cleaning slot`() {
        // given
        val halfPastFivePm = LocalTime.of(17, 30)
        val cinemaSchedule = givenCinemaScheduleEvents(
            listOf(
                RoomEventAdded(
                    1, "1",
                    sampleShow(
                        movieId = sampleMovieId1,
                        startTime = halfPastFivePm,
                        durationMinutes = 90,
                        cleaningSlotDurationMinutes = 15
                    )
                )
            )
        )

        // when
        val scheduleResult = cinemaSchedule.process(
            ScheduleRoomEventCommand(
                "2",
                sampleShow(
                    movieId = sampleMovieId2,
                    startTime = halfPastFivePm,
                    durationMinutes = 90,
                    cleaningSlotDurationMinutes = 15
                )
            )
        )

        // then
        assertThat(scheduleResult).isInstanceOf(ScheduleResult.Error.CleaningServiceUnavailable::class)
            .all {
                transform { it.occupyingRoomId }.isEqualTo("1")
                transform { it.cleaningSlotEndTime }.isEqualTo(halfPastFivePm.plusMinutes(90).plusMinutes(15))
            }
    }

    @Test
    fun `should schedule temporal unavailability`() {
        // given
        val schedule = CinemaSchedule.empty()

        // when
        val scheduleResult = schedule.process(
            ScheduleRoomEventCommand(
                roomId = "1",
                event = RoomEvent.Unavailability(
                    date = LocalDate.of(2023, 4, 27),
                    startTime = LocalTime.of(17, 0),
                    endTime = LocalTime.of(19, 0)
                )
            )
        )

        // then
        assertThat(scheduleResult).isInstanceOf(ScheduleResult.Success::class)
            .transform { it.events }
            .containsOnly(
                RoomEventAdded(
                    version = 1,
                    roomId = "1",
                    event = RoomEvent.Unavailability(
                        date = LocalDate.of(2023, 4, 27),
                        startTime = LocalTime.of(17, 0),
                        endTime = LocalTime.of(19, 0)
                    )
                )
            )
    }

    @Test
    fun `should properly version cinema schedule`() {
        val schedule = givenCinemaScheduleEvents(
            listOf(
                RoomEventAdded(1, "1", sampleShow()),
                RoomEventAdded(2, "2", sampleShow()),
                RoomEventAdded(3, "3", sampleShow())
            )
        )
        val scheduleResult =
            schedule.process(
                CancelRoomEventCommand(
                    roomId = "2",
                    date = sampleShow().date,
                    startTime = sampleShow().startTime
                )
            )

        assertThat(scheduleResult).isInstanceOf(ScheduleResult.Success::class)
            .transform {
                it.events.fold(schedule) { schedule, cinemaScheduleEvent ->
                    schedule.applyEvent(
                        cinemaScheduleEvent
                    )
                }
            }
            .isEqualTo(
                CinemaSchedule(
                    version = 4,
                    roomEventsByRoomId = mapOf(
                        Pair("1", listOf(sampleShow())),
                        Pair("3", listOf(sampleShow()))
                    )
                )
            )
    }
}



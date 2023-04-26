package pl.apurtak.cinema.schedule

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test
import pl.apurtak.cinema.schedule.CinemaScheduleFixtures.defaultDate
import pl.apurtak.cinema.schedule.CinemaScheduleFixtures.sampleMovieId1
import pl.apurtak.cinema.schedule.CinemaScheduleFixtures.sampleMovieId2
import pl.apurtak.cinema.schedule.CinemaScheduleFixtures.sampleShow
import java.time.LocalTime

class CinemaScheduleTest {

    @Test
    fun `should schedule a movie show`() {
        // given
        val cinemaSchedule = CinemaSchedule.empty()

        // when
        val result = cinemaSchedule.process(
            CinemaScheduleCommand.ScheduleRoomEvent(
                roomId = "1", event = sampleShow()
            )
        )

        // then
        assertThat(result).isInstanceOf(ScheduleResult.Success::class).isEqualTo(
            ScheduleResult.Success(
                listOf(
                    CinemaScheduleEvent.RoomEventAdded(
                        roomId = "1", roomEvent = sampleShow()
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
                CinemaScheduleEvent.RoomEventAdded(
                    "1", sampleShow(
                        movieId = sampleMovieId1, date = defaultDate, startTime = LocalTime.of(21, 30)
                    )
                ), CinemaScheduleEvent.RoomEventAdded(
                    "1", sampleShow(
                        movieId = sampleMovieId2, date = defaultDate, startTime = LocalTime.of(18, 0)
                    )
                ), CinemaScheduleEvent.RoomEventAdded(
                    "1", sampleShow(
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
                CinemaScheduleEvent.RoomEventAdded("1", sampleShow(sampleMovieId1, defaultDate, halfPastFivePm, 90)),
                CinemaScheduleEvent.RoomEventAdded(
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
                CinemaScheduleEvent.RoomEventAdded(
                    "1", sampleShow(
                        movieId = sampleMovieId1, startTime = LocalTime.of(17, 30), durationMinutes = 90
                    )
                )
            )
        )

        // when
        val scheduleResult = cinemaSchedule.process(
            CinemaScheduleCommand.ScheduleRoomEvent(
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
                CinemaScheduleEvent.RoomEventAdded(
                    "1", sampleShow(
                        movieId = sampleMovieId1, startTime = LocalTime.of(17, 30), durationMinutes = 90
                    )
                )
            )
        )

        // when
        val scheduleResult = cinemaSchedule.process(
            CinemaScheduleCommand.ScheduleRoomEvent(
                "2", sampleShow(movieId = sampleMovieId2, startTime = LocalTime.of(17, 0))
            )
        )

        // then
        assertThat(scheduleResult).isInstanceOf(ScheduleResult.Success::class).transform { it.events }
            .containsExactly(
                CinemaScheduleEvent.RoomEventAdded(
                    "2",
                    sampleShow(movieId = sampleMovieId2, startTime = LocalTime.of(17, 0))
                )
            )
    }

    @Test
    fun `should cancel scheduled room event`() {
        // given
        val cinemaSchedule = givenCinemaScheduleEvents(
            listOf(
                CinemaScheduleEvent.RoomEventAdded("1", sampleShow(sampleMovieId1, defaultDate, LocalTime.of(17, 0))),
                CinemaScheduleEvent.RoomEventAdded("1", sampleShow(sampleMovieId2, defaultDate, LocalTime.of(20, 0)))
            )
        )

        // when
        val scheduleResult =
            cinemaSchedule.process(CinemaScheduleCommand.CancelRoomEvent("1", defaultDate, LocalTime.of(17, 0)))

        // then
        assertThat(scheduleResult).isInstanceOf(ScheduleResult.Success::class).transform { it.events }.containsExactly(
            CinemaScheduleEvent.RoomEventCancelled(
                "1", sampleShow(sampleMovieId1, defaultDate, LocalTime.of(17, 0))
            )
        )
    }

    @Test
    fun `should not include cancelled room event in schedule`() {
        // given
        val cinemaSchedule = givenCinemaScheduleEvents(
            listOf(
                CinemaScheduleEvent.RoomEventAdded("1", sampleShow(sampleMovieId1, defaultDate, LocalTime.of(17, 0))),
                CinemaScheduleEvent.RoomEventAdded("1", sampleShow(sampleMovieId2, defaultDate, LocalTime.of(20, 0))),
                CinemaScheduleEvent.RoomEventCancelled(
                    "1", sampleShow(sampleMovieId1, defaultDate, LocalTime.of(17, 0))
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
                CinemaScheduleEvent.RoomEventAdded(
                    "1",
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
            CinemaScheduleCommand.ScheduleRoomEvent(
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
                CinemaScheduleEvent.RoomEventAdded(
                    "1",
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
            CinemaScheduleCommand.ScheduleRoomEvent(
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
                CinemaScheduleEvent.RoomEventAdded(
                    roomId = "1",
                    roomEvent = sampleShow(
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
                CinemaScheduleEvent.RoomEventAdded(
                    "1",
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
            CinemaScheduleCommand.ScheduleRoomEvent(
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
    fun `should distinct show time and show time  plus cleaning time`() {
        // what's the case for this?
        TODO()
    }

    @Test
    fun `should schedule room unavailability`() {
        TODO()
    }

    @Test
    fun `remember to add event version to state and events classes`() {
        TODO()
    }
}



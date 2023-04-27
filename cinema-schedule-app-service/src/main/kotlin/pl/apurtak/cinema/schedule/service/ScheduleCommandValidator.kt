package pl.apurtak.cinema.schedule.service

interface ScheduleCommandValidator {
    fun validate(scheduleShowCommand: ScheduleShowCommand): ScheduleShowResult.Error?
}

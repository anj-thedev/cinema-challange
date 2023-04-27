package pl.apurtak.cinema.validators

import pl.apurtak.cinema.AppConfig
import pl.apurtak.cinema.schedule.service.ScheduleCommandValidator
import pl.apurtak.cinema.schedule.service.ScheduleShowCommand
import pl.apurtak.cinema.schedule.service.ScheduleShowResult

internal class ShowStartTimeValidator(private val appConfig: AppConfig) :
    ScheduleCommandValidator {
    override fun validate(scheduleShowCommand: ScheduleShowCommand): ScheduleShowResult.Error? {
        val allowedShowStartTimeRange = appConfig.allowedShowStartTimeRange
        return if(scheduleShowCommand.startTime in allowedShowStartTimeRange) {
            null
        } else ScheduleShowResult.Error("Show can start only between ${allowedShowStartTimeRange.start} and ${allowedShowStartTimeRange.endInclusive}")
    }
}

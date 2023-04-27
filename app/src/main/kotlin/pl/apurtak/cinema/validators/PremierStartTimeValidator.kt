package pl.apurtak.cinema.validators

import pl.apurtak.cinema.AppConfig
import pl.apurtak.cinema.schedule.service.ScheduleCommandValidator
import pl.apurtak.cinema.schedule.service.ScheduleShowCommand
import pl.apurtak.cinema.schedule.service.ScheduleShowResult

internal class PremierStartTimeValidator(private val appConfig: AppConfig) :
    ScheduleCommandValidator {
    override fun validate(scheduleShowCommand: ScheduleShowCommand): ScheduleShowResult.Error? {
        val allowedPremierStartTimeRange = appConfig.allowedPremierStartTimeRange
        return if (scheduleShowCommand.isPremier && (scheduleShowCommand.startTime in allowedPremierStartTimeRange).not()) {
            ScheduleShowResult.Error("A Premier show can only be schedule between ${allowedPremierStartTimeRange.start} " +
                    "and ${allowedPremierStartTimeRange.endInclusive}")
        } else null
    }

}

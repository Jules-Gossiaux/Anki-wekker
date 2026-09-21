package com.julesgossiaux.ankiwekker.alarm

import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object AlarmOccurrence {
    fun next(alarm: AlarmSettings, clock: Clock = Clock.systemUTC()): ZonedDateTime? {
        val zone = runCatching { ZoneId.of(alarm.zoneId) }.getOrElse { ZoneId.systemDefault() }
        val now = ZonedDateTime.now(clock.withZone(zone))
        val time = LocalTime.of(alarm.hour, alarm.minute)
        return (0..7).asSequence()
            .map { offset -> LocalDateTime.of(now.toLocalDate().plusDays(offset.toLong()), time).atZone(zone) }
            .firstOrNull { candidate ->
                candidate.isAfter(now) && candidate.dayOfWeek.value in alarm.activeDays
            }
    }
}

package com.julesgossiaux.ankiwekker.alarm

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmOccurrenceTest {
    private val utcClock = Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), ZoneOffset.UTC)

    @Test
    fun skipsInactiveDaysAndReturnsTheNextConfiguredOccurrence() {
        val alarm = AlarmSettings(
            hour = 7,
            minute = 30,
            activeDays = setOf(1, 3),
            zoneId = "UTC",
        )

        val result = AlarmOccurrence.next(alarm, utcClock)

        val occurrence = result ?: error("Expected an occurrence")
        assertEquals("2026-09-23T07:30Z[UTC]", occurrence.toString())
    }

    @Test
    fun followsTheAlarmTimeInItsConfiguredTimezone() {
        val alarm = AlarmSettings(
            hour = 9,
            minute = 0,
            activeDays = setOf(1),
            zoneId = "Europe/Brussels",
        )
        val clock = Clock.fixed(Instant.parse("2026-09-20T22:00:00Z"), ZoneOffset.UTC)

        val result = AlarmOccurrence.next(alarm, clock)

        val occurrence = result ?: error("Expected an occurrence")
        assertEquals(ZoneId.of("Europe/Brussels"), occurrence.zone)
        assertEquals(9, occurrence.hour)
        assertEquals(0, occurrence.minute)
    }

    @Test
    fun movesForwardAcrossTheSpringDstGap() {
        val alarm = AlarmSettings(
            hour = 2,
            minute = 30,
            activeDays = setOf(7),
            zoneId = "Europe/Brussels",
        )
        val clock = Clock.fixed(Instant.parse("2026-03-28T12:00:00Z"), ZoneOffset.UTC)

        val result = AlarmOccurrence.next(alarm, clock)

        val occurrence = result ?: error("Expected an occurrence")
        assertEquals(3, occurrence.hour)
        assertEquals(30, occurrence.minute)
    }
}

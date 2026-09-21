package com.julesgossiaux.ankiwekker

import org.junit.Assert.assertEquals
import org.junit.Test

class DaySelectionTest {
    @Test
    fun togglingAnUnselectedDayAddsIt() {
        assertEquals(setOf(1, 3), toggleDay(setOf(1), 3))
    }

    @Test
    fun togglingASelectedDayRemovesIt() {
        assertEquals(setOf(1), toggleDay(setOf(1, 3), 3))
    }
}

package com.julesgossiaux.ankiwekker.alarm

import java.time.ZoneId
import java.util.UUID

data class AlarmSettings(
    val id: String = UUID.randomUUID().toString(),
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
    val activeDays: Set<Int> = (1..7).toSet(),
    val zoneId: String = ZoneId.systemDefault().id,
    val selectedDeckIds: Set<String> = emptySet(),
)

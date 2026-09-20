package com.julesgossiaux.ankiwekker.alarm

data class AlarmSettings(
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
)

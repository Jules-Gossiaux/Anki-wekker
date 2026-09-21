package com.julesgossiaux.ankiwekker.permissions

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.julesgossiaux.ankiwekker.ankidroid.AnkiDroidGateway

data class PermissionState(
    val ankiDroid: Boolean,
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
) {
    val allGranted: Boolean get() = ankiDroid && notifications && exactAlarms && fullScreen
}

class PermissionCoordinator(private val context: Context) {
    fun readState(): PermissionState = PermissionState(
        ankiDroid = context.checkSelfPermission(AnkiDroidGateway.READ_WRITE_PERMISSION) == PackageManager.PERMISSION_GRANTED,
        notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        exactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms(),
        fullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent(),
    )

    fun exactAlarmSettingsIntent(): Intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:${context.packageName}")
    }

    fun fullScreenSettingsIntent(): Intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
        data = Uri.parse("package:${context.packageName}")
    }
}

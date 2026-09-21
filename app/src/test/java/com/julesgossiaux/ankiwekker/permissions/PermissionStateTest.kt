package com.julesgossiaux.ankiwekker.permissions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStateTest {
    @Test
    fun allPermissionsAreReadyOnlyWhenEveryRequirementIsGranted() {
        assertTrue(PermissionState(true, true, true, true).allGranted)
        assertFalse(PermissionState(true, true, false, true).allGranted)
    }
}

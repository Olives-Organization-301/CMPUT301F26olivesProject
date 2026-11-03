package com.example.code

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceIdentityTest {

    @Test
    fun testDeviceIdentityPersistence() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // Get device ID twice
        val id1 = DeviceIdentity.getOrCreate(appContext)
        val id2 = DeviceIdentity.getOrCreate(appContext)

        // Verify ID is not null or blank
        assertNotNull(id1)
        assertTrue(id1.isNotBlank())

        // Verify ID is persistent / stable across calls
        assertEquals("Device identity should be persistent for the installation", id1, id2)
    }
}

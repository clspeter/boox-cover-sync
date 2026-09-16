package tw.mustp.booxcoversync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualSyncLaunchGateTest {
    @Test
    fun `cold launch requests one sync`() {
        val gate = ManualSyncLaunchGate()
        gate.onCreate(configurationRecreated = false)

        assertTrue(gate.onResume())
        assertFalse(gate.onResume())
    }

    @Test
    fun `background return requests one sync`() {
        val gate = ManualSyncLaunchGate()
        gate.onCreate(configurationRecreated = false)
        gate.onResume()

        gate.onStop(changingConfigurations = false)

        assertTrue(gate.onResume())
        assertFalse(gate.onResume())
    }

    @Test
    fun `configuration recreation does not request another sync`() {
        val oldGate = ManualSyncLaunchGate()
        oldGate.onCreate(configurationRecreated = false)
        oldGate.onResume()
        oldGate.onStop(changingConfigurations = true)

        val newGate = ManualSyncLaunchGate()
        newGate.onCreate(configurationRecreated = true)

        assertFalse(newGate.onResume())
    }

    @Test
    fun `return from picker or settings is suppressed`() {
        val gate = ManualSyncLaunchGate()
        gate.onCreate(configurationRecreated = false)
        gate.onResume()

        gate.beforeExternalActivity()
        gate.onStop(changingConfigurations = false)

        assertFalse(gate.onResume())
        assertFalse(gate.onResume())
    }
}

package tw.mustp.booxcoversync

/**
 * Decides when a visible activity should perform its one manual launch sync.
 *
 * A configuration recreation receives a saved state and must not look up the
 * reader again. A real background return does, while returning from a picker
 * or settings page is explicitly suppressed.
 */
internal class ManualSyncLaunchGate {
    private var initialResumePending = true
    private var stoppedForReturn = false
    private var suppressNextResume = false

    fun onCreate(configurationRecreated: Boolean) {
        initialResumePending = !configurationRecreated
        stoppedForReturn = false
        suppressNextResume = false
    }

    fun beforeExternalActivity() {
        suppressNextResume = true
    }

    fun cancelExternalActivity() {
        suppressNextResume = false
    }

    fun onStop(changingConfigurations: Boolean) {
        if (!changingConfigurations) {
            stoppedForReturn = true
        }
    }

    /** Returns true exactly when a one-shot launch sync should start. */
    fun onResume(): Boolean {
        if (initialResumePending) {
            initialResumePending = false
            stoppedForReturn = false
            return true
        }

        if (suppressNextResume) {
            suppressNextResume = false
            stoppedForReturn = false
            return false
        }

        if (stoppedForReturn) {
            stoppedForReturn = false
            return true
        }

        return false
    }
}

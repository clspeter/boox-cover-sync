package tw.mustp.booxcoversync.autosync

import android.database.ContentObserver
import android.database.MatrixCursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import tw.mustp.booxcoversync.reader.BooxMetadataContentObserver
import tw.mustp.booxcoversync.reader.BooxMetadataProviderLocator
import tw.mustp.booxcoversync.reader.NeoReaderLocation

@RunWith(RobolectricTestRunner::class)
class AutoSyncMetadataCatchUpTest {
    @Test
    fun `stale new candidate keeps remaining finite retry and catches current book`() {
        val root = Files.createTempDirectory("boox-metadata-catch-up").toFile()
        val lastSynced = File(root, "last-synced.epub").apply { writeText("last synced") }
        val previous = File(root, "previous.epub").apply { writeText("previous") }
        val current = File(root, "current.epub").apply { writeText("current") }
        var visibleBook = previous
        var lastAccess = 200L
        val locator = BooxMetadataProviderLocator(
            query = { _, _, _ -> metadataCursor(visibleBook, lastAccess) },
            externalStorageRoot = root,
        )
        val scheduler = FakeScheduler()
        val store = FakeFingerprintStore().apply {
            value = AutoSyncUriFingerprint.of(privateUri(lastSynced.name))
        }
        val candidates = mutableListOf<Candidate>()
        val coordinator = AutoSyncCoordinator(
            scheduler = scheduler,
            locator = locator,
            fingerprintStore = store,
            onNewLocation = { location, fingerprint, generation ->
                candidates += Candidate(location, fingerprint, generation)
            },
        )
        var registeredObserver: ContentObserver? = null
        val observer = BooxMetadataContentObserver(
            handler = Handler(Looper.getMainLooper()),
            register = { _, _, registered -> registeredObserver = registered },
            unregister = {},
            onProviderChanged = coordinator::onReaderWindowChanged,
        )

        assertTrue(observer.start())
        registeredObserver!!.onChange(false, BooxMetadataProviderLocator.providerUriForTest())
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        scheduler.advanceBy(2_000L)

        val stale = candidates.single()
        assertEquals(privateUri(previous.name), stale.location.contentUri)
        coordinator.onProcessingCompleted(stale.fingerprint, stale.generation, success = true)
        assertEquals(stale.fingerprint, store.value)

        // Metadata catches up inside the existing +5 second retry window, but
        // it does not emit another callback. The remaining bounded retry must
        // still locate the book the user actually opened.
        scheduler.advanceBy(1_000L)
        visibleBook = current
        lastAccess = 300L
        scheduler.advanceBy(4_000L)

        assertEquals(
            listOf(privateUri(previous.name), privateUri(current.name)),
            candidates.map { it.location.contentUri },
        )
    }

    @Test
    fun `one second window event stream cannot starve the two second lookup`() {
        val root = Files.createTempDirectory("boox-metadata-debounce").toFile()
        val current = File(root, "current.epub").apply { writeText("current") }
        var queryCount = 0
        val locator = BooxMetadataProviderLocator(
            query = { _, _, _ ->
                queryCount += 1
                metadataCursor(current, 100L)
            },
            externalStorageRoot = root,
        )
        val scheduler = FakeScheduler()
        val coordinator = AutoSyncCoordinator(
            scheduler = scheduler,
            locator = locator,
            fingerprintStore = FakeFingerprintStore(),
            onNewLocation = { _, _, _ -> },
        )

        coordinator.onReaderWindowChanged()
        repeat(30) {
            scheduler.advanceBy(1_000L)
            coordinator.onReaderWindowChanged()
        }

        assertTrue(
            "30 seconds of window events continuously postponed the metadata lookup",
            queryCount > 0,
        )
    }

    @Test
    fun `successful candidates consume only the remaining bounded retries`() {
        val root = Files.createTempDirectory("boox-metadata-bounded").toFile()
        val books = listOf("first.epub", "second.epub", "third.epub").map { name ->
            File(root, name).apply { writeText(name) }
        }
        var queryCount = 0
        val locator = BooxMetadataProviderLocator(
            query = { _, _, _ ->
                val book = books[queryCount.coerceAtMost(books.lastIndex)]
                queryCount += 1
                metadataCursor(book, queryCount.toLong())
            },
            externalStorageRoot = root,
        )
        val scheduler = FakeScheduler()
        val dispatched = mutableListOf<Uri>()
        lateinit var coordinator: AutoSyncCoordinator
        coordinator = AutoSyncCoordinator(
            scheduler = scheduler,
            locator = locator,
            fingerprintStore = FakeFingerprintStore(),
            onNewLocation = { location, fingerprint, generation ->
                dispatched += location.contentUri
                coordinator.onProcessingCompleted(fingerprint, generation, success = true)
            },
        )

        coordinator.onReaderWindowChanged()
        scheduler.advanceBy(60_000L)

        assertEquals(3, queryCount)
        assertEquals(books.map { privateUri(it.name) }, dispatched)
        assertEquals(0, scheduler.activeTaskCount())
    }

    @Test
    fun `screen off cancels retry scheduled after successful processing`() {
        assertSuccessfulRetryCancellation { coordinator -> coordinator.onScreenOff() }
    }

    @Test
    fun `disabling cancels retry scheduled after successful processing`() {
        assertSuccessfulRetryCancellation { coordinator -> coordinator.setEnabled(false) }
    }

    @Test
    fun `window event during retry wait replaces it with a two second lookup`() {
        val root = Files.createTempDirectory("boox-metadata-retry-event").toFile()
        val current = File(root, "current.epub").apply { writeText("current") }
        var available = false
        var queryCount = 0
        val locator = BooxMetadataProviderLocator(
            query = { _, _, _ ->
                queryCount += 1
                if (available) metadataCursor(current, 100L) else null
            },
            externalStorageRoot = root,
        )
        val scheduler = FakeScheduler()
        val dispatched = mutableListOf<Uri>()
        val coordinator = AutoSyncCoordinator(
            scheduler = scheduler,
            locator = locator,
            fingerprintStore = FakeFingerprintStore(),
            onNewLocation = { location, _, _ -> dispatched += location.contentUri },
        )

        coordinator.onReaderWindowChanged()
        scheduler.advanceBy(2_000L)
        assertEquals(1, queryCount)

        scheduler.advanceBy(1_000L)
        available = true
        coordinator.onReaderWindowChanged()
        scheduler.advanceBy(1_999L)
        assertEquals(1, queryCount)
        scheduler.advanceBy(1L)

        assertEquals(5_000L, scheduler.elapsedMillis)
        assertEquals(2, queryCount)
        assertEquals(listOf(privateUri(current.name)), dispatched)
    }

    private fun assertSuccessfulRetryCancellation(cancel: (AutoSyncCoordinator) -> Unit) {
        val root = Files.createTempDirectory("boox-metadata-cancel-retry").toFile()
        val current = File(root, "current.epub").apply { writeText("current") }
        var queryCount = 0
        val locator = BooxMetadataProviderLocator(
            query = { _, _, _ ->
                queryCount += 1
                metadataCursor(current, 100L)
            },
            externalStorageRoot = root,
        )
        val scheduler = FakeScheduler()
        var candidate: Candidate? = null
        val coordinator = AutoSyncCoordinator(
            scheduler = scheduler,
            locator = locator,
            fingerprintStore = FakeFingerprintStore(),
            onNewLocation = { location, fingerprint, generation ->
                candidate = Candidate(location, fingerprint, generation)
            },
        )

        coordinator.onReaderWindowChanged()
        scheduler.advanceBy(2_000L)
        val completed = requireNotNull(candidate)
        coordinator.onProcessingCompleted(completed.fingerprint, completed.generation, success = true)
        assertEquals(1, scheduler.activeTaskCount())

        cancel(coordinator)
        scheduler.advanceBy(30_000L)

        assertEquals(1, queryCount)
        assertEquals(0, scheduler.activeTaskCount())
    }

    private fun metadataCursor(book: File, lastAccess: Long): MatrixCursor =
        MatrixCursor(BooxMetadataProviderLocator.projectionForTest()).apply {
            addRow(arrayOf(lastAccess, "epub", book.absolutePath))
        }

    private fun privateUri(fileName: String): Uri = Uri.parse(
        "content://com.onyx.kreader.onyx.fileprovider/external/$fileName",
    )

    private data class Candidate(
        val location: NeoReaderLocation,
        val fingerprint: String,
        val generation: Long,
    )

    private class FakeFingerprintStore : AutoSyncFingerprintStore {
        var value: String? = null

        override fun read(): String? = value

        override fun write(fingerprint: String) {
            value = fingerprint
        }
    }

    private class FakeScheduler : AutoSyncScheduler {
        private data class Task(
            val dueAtMillis: Long,
            val action: () -> Unit,
            var cancelled: Boolean = false,
        )

        private val tasks = mutableListOf<Task>()
        var elapsedMillis: Long = 0L
            private set

        override fun schedule(delayMillis: Long, task: () -> Unit): AutoSyncCancellable {
            val scheduled = Task(elapsedMillis + delayMillis, task)
            tasks += scheduled
            return AutoSyncCancellable { scheduled.cancelled = true }
        }

        fun advanceBy(deltaMillis: Long) {
            require(deltaMillis >= 0L)
            val targetMillis = elapsedMillis + deltaMillis
            while (true) {
                val next = tasks
                    .filterNot { it.cancelled }
                    .filter { it.dueAtMillis <= targetMillis }
                    .minByOrNull { it.dueAtMillis }
                    ?: break
                next.cancelled = true
                elapsedMillis = next.dueAtMillis
                next.action()
            }
            elapsedMillis = targetMillis
        }

        fun activeTaskCount(): Int = tasks.count { !it.cancelled }
    }
}

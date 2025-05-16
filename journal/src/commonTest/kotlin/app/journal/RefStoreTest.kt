package app.journal

import app.journal.supporting.InMemoryRefStore
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RefStoreTest {
    @Test
    fun `swap - fails if ref is invalid`() = runTest {
        val refs = provideRefStore()
        assertFailsWith<IllegalArgumentException> {
            refs.swap("", someHash(), someHash())
        }
    }

    @Test
    fun `swap - fails if old hash is invalid - for new ref`() = runTest {
        val refs = provideRefStore()
        assertFailsWith<ConcurrencyException> {
            refs.swap(someRef(), someHash(), someHash())
        }
    }

    @Test
    fun `swap - fails if old hash is invalid - for existing ref`() = runTest {
        val refs = provideRefStore()
        val ref = someRef()
        refs.swap(ref, someHash(), null)

        assertFailsWith<ConcurrencyException> {
            refs.swap(ref, someHash(), someHash())
        }
    }

    @Test
    fun `swap - fails if new hash is null`() {
        // Should be never possible
    }

    @Test
    fun `swap - new ref can be stored by providing null old hash`() = runTest {
        // setup
        val refs = provideRefStore()
        val ref = someRef()
        val want = someHash()

        refs.swap(ref, want, null)

        val got = refs.read(ref)
        assertEquals(want, got)
    }

    @Test
    fun `swap - does nothing if old is equal to new`() {
    }

    @Test
    fun `swap - updates existing hash if old hash matches`() = runTest {
        val refs = provideRefStore()
        val ref = someRef()
        val old = someHash()
        val want = someHash()
        refs.swap(ref, old, null)

        refs.swap(ref, want, old)

        val got = refs.read(ref)
        assertEquals(want, got)
    }

    @Test
    fun `read - fails if ref is invalid`() = runTest {
        val refs = provideRefStore()
        assertEquals(null, refs.read(""))
    }

    @Test
    fun `read - returns null if ref not present`() = runTest {
        val refs = provideRefStore()
        assertEquals(null, refs.read(someRef()))
    }

    @Test
    fun `read - returns correct hash for existing ref`() = runTest {
        val refs = provideRefStore()
        val ref = someRef()
        val want = someHash()
        refs.swap(ref, want, null)

        val got = refs.read(ref)

        assertEquals(want, got)
    }

    @Test
    fun `concurrency - swap can be concurrently for different refs`() = runTest {
        val refs = provideRefStore()
        awaitAll(
            async { refs.swap(someRef(), someHash(), null) },
            async { refs.swap(someRef(), someHash(), null) },
            async { refs.swap(someRef(), someHash(), null) },
            async { refs.swap(someRef(), someHash(), null) },
            async { refs.swap(someRef(), someHash(), null) },
            async { refs.swap(someRef(), someHash(), null) },
            async { refs.swap(someRef(), someHash(), null) },
        )
    }

    @Test
    fun `concurrency - only one of the swap succeeds when called concurrently for the same ref`() =
        runTest {
            val refs = provideRefStore()
            val ref = someRef()
            val success = atomic(0)
            val failed = atomic(0)
            val startTrigger = atomic(false)
            val concurrency = 10
            val all: MutableList<Deferred<*>> = mutableListOf()
            for (i in 0 until concurrency) {
                all.add(
                    async {
                        while (!startTrigger.value) continue
                        runCatching { refs.swap(ref, someHash(), null) }
                            .onSuccess { success.incrementAndGet() }
                            .onFailure { failed.incrementAndGet() }
                    }
                )
            }
            startTrigger.value = true
            awaitAll(*all.toTypedArray())
            assertEquals(1, success.value)
            assertEquals(concurrency - 1, failed.value)
        }

    @Test
    fun `concurrency - swap can be called concurrently with reads for different ref`() = runTest {
        val refs = provideRefStore()
        awaitAll(
            async { refs.swap(someRef(), someHash(), null) },
            async { refs.swap(someRef(), someHash(), null) },
            async { refs.swap(someRef(), someHash(), null) },
            async { refs.swap(someRef(), someHash(), null) },
            async { refs.read(someRef()) },
            async { refs.read(someRef()) },
            async { refs.read(someRef()) },
            async { refs.read(someRef()) },
            async { refs.read(someRef()) },
        )
    }
}
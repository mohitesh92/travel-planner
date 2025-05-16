package app.journal.supporting.memory

import app.journal.ConcurrencyException
import app.journal.Hash
import app.journal.RefStore
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.getValue

class InMemoryRefStore : RefStore {

    private val aggregates by lazy { mutableMapOf<String, AtomicRef<Hash>>() }

    override suspend fun swap(
        aggregateId: String,
        newRef: Hash,
        oldRef: Hash?
    ) {
        if (aggregateId.isEmpty()) throw IllegalArgumentException("Aggregate $aggregateId cannot be empty")

        if (oldRef == newRef) return

        val existing = aggregates[aggregateId]
        if (oldRef == null) {
            if (existing != null) {
                throw ConcurrencyException(
                    "Concurrency conflict: expected version $oldRef, but current version is null"
                )
            }
            aggregates[aggregateId] = atomic(newRef)
            return
        }

        if (existing == null) {
            throw ConcurrencyException(
                "Concurrency conflict: expected version $oldRef, but current version is null"
            )
        }

        if (!existing.compareAndSet(oldRef, newRef)) {
            throw ConcurrencyException(
                "Concurrency conflict: expected version $oldRef, but current version is ${existing.value}"
            )
        }
    }

    override suspend fun read(aggregateId: String): Hash? {
        return withContext(Dispatchers.IO) {
            aggregates[aggregateId]?.value?.let { (it) }
        }
    }
}
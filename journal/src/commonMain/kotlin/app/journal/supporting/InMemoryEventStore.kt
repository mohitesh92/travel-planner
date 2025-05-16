package app.journal.supporting

import app.journal.ConcurrencyException
import app.journal.Event
import app.journal.EventStore
import app.journal.Hash
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of EventStore for testing purposes.
 * This class stores events in memory and provides the basic functionality
 * of committing events and retrieving them by aggregate ID.
 */
class InMemoryEventStore : EventStore {
    // Map of aggregate ID to list of events for that aggregate
    private val eventStore = mutableMapOf<String, MutableList<Event>>()

    // Map of aggregate ID to the latest version (hash) of that aggregate
    private val versionStore = mutableMapOf<String, Hash>()

    private val mutex = Mutex()

    override suspend fun commit(
        aggregateId: String,
        event: Event,
        expectedVersion: Hash
    ): Hash {
        // Acquire the mutex lock to ensure thread safety
        mutex.withLock {
            // Check if this is a new aggregate or an update to an existing one
            if (versionStore.containsKey(aggregateId)) {
                // This is an update to an existing aggregate
                val currentVersion = versionStore[aggregateId]

                // Optimistic concurrency check - only proceed if the expected version matches the current version
                if (currentVersion != expectedVersion) {
                    throw ConcurrencyException(
                        "Concurrency conflict: expected version $expectedVersion, but current version is $currentVersion"
                    )
                }
            } else {
                // For a new aggregate, make sure we're initializing with the zero hash
                val zeroHash = Hash("0".repeat(64))
                if (expectedVersion != zeroHash) {
                    throw ConcurrencyException(
                        "Concurrency conflict: expected initial version to be $zeroHash, but got $expectedVersion"
                    )
                }
            }

            // Add events to the store
            val eventList = eventStore.getOrPut(aggregateId) { mutableListOf() }
            eventList.add(event)

            // Update the version to the hash of the last event
            versionStore[aggregateId] = event.hash()

            return versionStore[aggregateId] ?: Hash("0".repeat(64))
        }
    }

    override suspend fun getEventsForAggregate(aggregateId: String): List<Event> {
        // Return a copy of the list of events for the aggregate, or an empty list if none exist
        return eventStore[aggregateId]?.toList() ?: emptyList()
    }

    override fun getAllEvents() = kotlinx.coroutines.flow.flow {
        // Flatten all events from all aggregates and emit them
        eventStore.values.flatten().sortedBy { it.timestamp }.forEach { emit(it) }
    }
}
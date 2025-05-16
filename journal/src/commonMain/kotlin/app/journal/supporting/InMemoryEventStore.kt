package app.journal.supporting

import app.journal.ConcurrencyException
import app.journal.Event
import app.journal.EventStore
import app.journal.Hash
import app.journal.RefStore
import kotlinx.coroutines.sync.Mutex

/**
 * In-memory implementation of EventStore for testing purposes.
 * This class stores events in memory and provides the basic functionality
 * of committing events and retrieving them by aggregate ID.
 */
class InMemoryEventStore(
    private val refStore: RefStore,
) : EventStore {
    // Map of aggregate ID to list of events for that aggregate
    private val eventStore = mutableMapOf<String, MutableList<Event>>()

    private val mutex = Mutex()

    override suspend fun commit(
        aggregateId: String,
        event: Event,
        expectedVersion: Hash
    ): Hash {
        // Acquire the mutex lock to ensure thread safety
        // Check if this is a new aggregate or an update to an existing one
        val currentVersion = refStore.read(aggregateId)
        if (currentVersion != null) {
            // This is an update to an existing aggregate
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

        val newVersion = event.hash()

        // Update the version to the hash of the last event
        runCatching { refStore.swap(aggregateId = aggregateId, newRef = newVersion, oldRef = currentVersion) }
            .onFailure {
                // remove the event from the store if the swap fails
                eventStore[aggregateId]?.remove(event)
                // If the swap fails, it means the expected version was incorrect
                throw ConcurrencyException(
                    "Concurrency conflict: expected version $expectedVersion, but current version is $currentVersion"
                )
            }

        return newVersion
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
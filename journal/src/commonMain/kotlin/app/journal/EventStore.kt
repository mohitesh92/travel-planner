package app.journal

import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Interface for the event store, which is responsible for persisting and retrieving events.
 * The event store is the source of truth in an event sourcing system.
 */
interface EventStore {
    /**
     * Saves a sequence of events for a specific aggregate.
     * 
     * @param aggregateId The ID of the aggregate these events belong to
     * @param event The event to save
     * @param expectedVersion The expected current version of the aggregate (for optimistic concurrency)
     * @throws ConcurrencyException If the expected version doesn't match the actual version
     * @return The new version (hash) of the aggregate after the commit
     */
    @Throws(ConcurrencyException::class, CancellationException::class)
    suspend fun commit(aggregateId: String, event: Event, expectedVersion: Hash) : Hash

    /**
     * Retrieves all events for a specific aggregate.
     * 
     * @param aggregateId The ID of the aggregate
     * @return A list of events for the aggregate, ordered by version
     */
    suspend fun getEventsForAggregate(aggregateId: String): List<Event>
    
    /**
     * Retrieves all events in the system.
     * 
     * @return A flow of all events, typically ordered by timestamp
     */
    fun getAllEvents(): Flow<Event>
}

/**
 * Exception thrown when a concurrency violation is detected.
 */
class ConcurrencyException(message: String) : Exception(message)
package app.journal

import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Exception thrown when a concurrency violation is detected.
 */
class ConcurrencyException(message: String) : Exception(message)

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
     * @param filter The filter to apply when retrieving events
     * @return A list of events for the aggregate, ordered by version
     */
    suspend fun events(filter: EventFilter): List<Event>
    
    /**
     * Retrieves all events in the system.
     * 
     * @return A flow of all events, typically ordered by timestamp
     */
    fun getAllEvents(): Flow<Event>
}

/**
 * Filter criteria for querying events from the EventStore.
 *
 * @property aggregateId The ID of the aggregate to filter events for
 * @property type Optional event type to filter by
 * @property start Optional minimum timestamp to include events from
 * @property end Optional maximum timestamp to include events until
 */
data class EventFilter(
    val aggregateId: String,
    val type: String? = null,
    val start: Long? = null,
    val end: Long? = null,
)

/**
 * Extension function to help create an EventFilter for a specific aggregate ID.
 * This makes the code more readable in common scenarios.
 *
 * @param aggregateId The ID of the aggregate to filter events for
 * @return An EventFilter with the specified aggregate ID
 */
fun eventsFor(aggregateId: String): EventFilter = EventFilter(aggregateId = aggregateId)
package app.journal.supporting.sqlite

import app.journal.ConcurrencyException
import app.journal.Event
import app.journal.EventFilter
import app.journal.EventStore
import app.journal.Hash
import app.journal.PolymorphicJson
import app.journal.RefStore
import app.journal.db.JournalDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * SQLite implementation of the EventStore interface.
 * This implementation uses SQLDelight to interact with a SQLite database.
 */
class SqliteStore(
    private val database: JournalDatabase,
    private val json: PolymorphicJson,
) : EventStore, RefStore {

    private val journalDatabaseQueries by lazy { database.journalDatabaseQueries }

    override suspend fun commit(
        aggregateId: String,
        event: Event,
        expectedVersion: Hash
    ): Hash {
        // Check the current version from the RefStore
        val currentVersion = read(aggregateId)

        if (currentVersion != null) {
            // This is an update to an existing aggregate
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

        // Calculate the new version hash
        val newVersion = event.hash()

        // Use a transaction to ensure atomicity
        database.transaction {
            // Insert the event
            database.journalDatabaseQueries.insertEvent(
                id = event.id,
                aggregate_id = event.aggregateId,
                timestamp = event.timestamp,
                parent = event.currentVersion,
                type = event.type,
                data_ = json.encodeToString(event)
            )

            swapInternal(
                aggregateId = event.aggregateId,
                newRef = newVersion,
                oldRef = currentVersion
            )

            val updated = database.journalDatabaseQueries.changes().executeAsOne() > 0
            if (!updated) {
                // The version reference update failed due to concurrent modification
                throw ConcurrencyException(
                    "Concurrency conflict: version was changed by another process"
                )
            }
        }

        return newVersion
    }

    /**
     * Retrieves events that match the given filter criteria.
     *
     * @param filter Filter criteria for the events to retrieve
     * @return A list of events matching the filter criteria, ordered by timestamp
     */
    override suspend fun events(filter: EventFilter): List<Event> {
        val results = mutableListOf<Event>()

        // Build the query based on the filter
        if (filter.type != null) {
            if (filter.start != null && filter.end != null) {
                // Filter by type and time range
                database.journalDatabaseQueries.getEventsByTypeAndTimeRange(
                    aggregateId = filter.aggregateId,
                    type = filter.type,
                    start = filter.start,
                    end = filter.end
                ).executeAsList()
            } else if (filter.start != null) {
                // Filter by type and start time
                database.journalDatabaseQueries.getEventsByTypeAndStartTime(
                    aggregate_id = filter.aggregateId,
                    type = filter.type,
                    timestamp = filter.start
                ).executeAsList()
            } else if (filter.end != null) {
                // Filter by type and end time
                database.journalDatabaseQueries.getEventsByTypeAndEndTime(
                    aggregate_id = filter.aggregateId,
                    type = filter.type,
                    timestamp = filter.end
                ).executeAsList()
            } else {
                // Filter by type only
                database.journalDatabaseQueries.getEventsByType(
                    aggregate_id = filter.aggregateId,
                    type = filter.type
                ).executeAsList()
            }
        } else {
            if (filter.start != null && filter.end != null) {
                // Filter by time range
                database.journalDatabaseQueries.getEventsByTimeRange(
                    aggregateId = filter.aggregateId,
                    start = filter.start,
                    end = filter.end
                ).executeAsList()
            } else if (filter.start != null) {
                // Filter by start time
                database.journalDatabaseQueries.getEventsByStartTime(
                    aggregate_id = filter.aggregateId,
                    timestamp = filter.start
                ).executeAsList()
            } else if (filter.end != null) {
                // Filter by end time
                database.journalDatabaseQueries.getEventsByEndTime(
                    aggregate_id = filter.aggregateId,
                    timestamp = filter.end
                ).executeAsList()
            } else {
                // No filtering, get all events for the aggregate
                database.journalDatabaseQueries.getEventsForAggregate(
                    aggregate_id = filter.aggregateId
                ).executeAsList()
            }
        }.forEach { row ->
            // Deserialize each event from its JSON representation
            try {
                results.add(json.decodeFromString(row.data_))
            } catch (e: Exception) {
                // Log the error but continue processing other events
                println("Error deserializing event ${row.id}: ${e.message}")
            }
        }

        return results
    }

    /**
     * Retrieves all events in the system as a Flow.
     *
     * @return A flow of all events, ordered by timestamp
     */
    override fun getAllEvents(): Flow<Event> = flow {
        database.journalDatabaseQueries.getAllEvents().executeAsList().forEach { row ->
            try {
                val event: Event = json.decodeFromString(row.data_)
                emit(event)
            } catch (e: Exception) {
                // Log the error but continue processing other events
                println("Error deserializing event ${row.id}: ${e.message}")
            }
        }
    }

    override suspend fun swap(
        aggregateId: String,
        newRef: Hash,
        oldRef: Hash?
    ) {
        if (aggregateId.isEmpty()) {
            throw IllegalArgumentException("Aggregate ID cannot be empty")
        }
        database.transaction {
            swapInternal(aggregateId, newRef, oldRef)
        }
    }

    private fun swapInternal(
        aggregateId: String,
        newRef: Hash,
        oldRef: Hash?
    ) {
        if (oldRef == null) {
            // We're expecting this to be a new aggregate with no existing reference
            val existingRef = journalDatabaseQueries.getRef(aggregateId).executeAsOneOrNull()
            if (existingRef != null) {
                throw ConcurrencyException(
                    "Concurrency conflict: expected no existing reference, but found $existingRef"
                )
            }

            // Insert the new reference
            journalDatabaseQueries.insertRef(
                aggregate_id = aggregateId,
                version = newRef
            )
        } else {
            // Update the reference if it matches the expected value
            journalDatabaseQueries.updateRefIfMatch(
                newVersion = newRef,
                aggregateId = aggregateId,
                oldVersion = oldRef,
            )

            // Check if the update succeeded (returns 1 if a row was updated)
            val rowsUpdated = journalDatabaseQueries.changes().executeAsOne()
            if (rowsUpdated == 0L) {
                throw ConcurrencyException(
                    "Concurrency conflict: expected version $oldRef, but current version is different"
                )
            }
        }
    }

    override suspend fun read(aggregateId: String): Hash? {
        return database.journalDatabaseQueries.getRef(aggregateId).executeAsOneOrNull()
    }
}
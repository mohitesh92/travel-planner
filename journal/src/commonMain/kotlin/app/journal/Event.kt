package app.journal

import kotlinx.serialization.json.Json

typealias PolymorphicJson = Json

/**
 * Base interface for all events in the system.
 * Events are immutable records of something that has happened in the past.
 * An event is hashable, meaning it can be used as a key in hash-based collections.
 */
interface Event : Hashable {
    /** Unique identifier for this event instance */
    val id: String
    
    /** Identifier of the aggregate this event belongs to */
    val aggregateId: String
    
    /** When the event occurred */
    val timestamp: Long

    /** The version of the aggregate when this event was created, null for first event */
    val currentVersion: Hash?

    /** The unique type of this event. Can be used if we want to filter one type of events */
    val type: String
}
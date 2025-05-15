package app.journal

/**
 * Base interface for all events in the system.
 * Events are immutable records of something that has happened in the past.
 */
interface Event {
    /** Unique identifier for this event instance */
    val id: String
    
    /** Identifier of the aggregate this event belongs to */
    val aggregateId: String
    
    /** When the event occurred */
    val timestamp: Long
    
    /** Sequential version number for the aggregate */
    val version: Long
}
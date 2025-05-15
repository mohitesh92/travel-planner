package app.journal

/**
 * Base interface for all commands in the system.
 * Commands represent an intention to change the state of the system.
 * Unlike events which represent something that has already happened,
 * commands can be rejected if they violate business rules.
 */
interface Command {
    /** Unique identifier for this command instance */
    val id: String
    
    /** Identifier of the aggregate this command targets */
    val aggregateId: String
}
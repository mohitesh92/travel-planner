package app.journal

/**
 * Interface for event handlers, which process events after they have been persisted.
 * Event handlers are typically used to update read models/projections or
 * to trigger side effects (notifications, integrations, etc.).
 */
interface EventHandler {
    
    /**
     * Handles an event that has been persisted to the event store.
     * 
     * @param event The event to handle
     */
    suspend fun handle(event: Event)
}
package app.journal

/**
 * Interface for an event bus that distributes events to registered handlers.
 * The event bus decouples event producers from consumers and
 * allows for multiple handlers to react to the same event.
 */
interface EventBus {
    
    /**
     * Publishes an event to all registered handlers.
     * 
     * @param event The event to publish
     */
    suspend fun publish(event: Event)
    
    /**
     * Publishes multiple events to all registered handlers.
     * 
     * @param events The events to publish
     */
    suspend fun publishAll(events: List<Event>) {
        events.forEach { publish(it) }
    }
    
    /**
     * Registers an event handler to receive published events.
     * 
     * @param handler The event handler to register
     */
    fun subscribe(handler: EventHandler)
    
    /**
     * Unregisters an event handler.
     * 
     * @param handler The event handler to unregister
     */
    fun unsubscribe(handler: EventHandler)
}
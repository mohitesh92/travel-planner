package app.journal

import app.journal.supporting.InMemoryEventStore
import app.journal.supporting.InMemoryRefStore
import kotlin.random.Random

/**
 * Test implementation of Event for testing purposes
 */
data class TestEvent(
    override val id: String,
    override val aggregateId: String,
    override val timestamp: Long,
    override val currentVersion: Hash?,
    val data: String
) : Event {
    override fun hash(): Hash {
        // Simple implementation for testing
        return toString().hash
    }
}

fun someHash(): Hash {
    // Generate a random hash for testing
    return getRandomString(10).hash
}

fun getRandomString(length: Int) : String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

fun provideEventStore(): EventStore {
    return InMemoryEventStore(
        refStore = provideRefStore()
    )
}

fun provideRefStore(): RefStore {
    return InMemoryRefStore()
}

fun someRef(): String {
    return "ref-" + Random.nextInt().toString()
}
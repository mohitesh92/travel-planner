package app.journal

import app.journal.supporting.InMemoryEventStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventStoreTest {
    
    @Test
    fun `should successfully commit and retrieve events`() = runTest {
        // Arrange
        val store = InMemoryEventStore()
        val aggregateId = "aggregate-1"
        val zeroHash = Hash("0".repeat(64))
        val event1 = TestEvent(
            id = "event-1",
            aggregateId = aggregateId,
            timestamp = 1000L,
            currentVersion = null,
            data = "First event"
        )
        val event2 = TestEvent(
            id = "event-2",
            aggregateId = aggregateId,
            timestamp = 2000L,
            currentVersion = event1.hash(),
            data = "Second event"
        )
        
        // Act
        store.commit(aggregateId, listOf(event1), zeroHash)
        store.commit(aggregateId, listOf(event2), event1.hash())
        val retrievedEvents = store.getEventsForAggregate(aggregateId)
        
        // Assert
        assertEquals(2, retrievedEvents.size, "Should retrieve exactly 2 events")
        assertEquals(event1.id, retrievedEvents[0].id, "First event should match")
        assertEquals(event2.id, retrievedEvents[1].id, "Second event should match")
    }
    
    @Test
    fun `should throw concurrency exception when expected version doesn't match`() = runTest {
        // Arrange
        val store = InMemoryEventStore()
        val aggregateId = "aggregate-1"
        val zeroHash = Hash("0".repeat(64))
        val correctHash = Hash("1".repeat(64)) // This matches what our TestEvent.hash() returns
        val wrongHash = Hash("2".repeat(64))   // This is different
        val event = TestEvent(
            id = "event-1",
            aggregateId = aggregateId,
            timestamp = 1000L,
            currentVersion = null,
            data = "First event"
        )
        
        // First commit succeeds
        store.commit(aggregateId, listOf(event), zeroHash)
        
        // Act & Assert - Second commit with wrong version fails
        assertFailsWith<ConcurrencyException> {
            // After first commit, the version will be event.hash() (1111...)
            // So we intentionally use wrongHash (2222...) to cause a concurrency exception
            store.commit(aggregateId, listOf(
                TestEvent(
                    id = "event-2",
                    aggregateId = aggregateId,
                    timestamp = 2000L,
                    currentVersion = correctHash, 
                    data = "Second event"
                )
            ), wrongHash)
        }
    }
    
    @Test
    fun `should enforce new aggregates start with zero hash`() = runTest {
        // Arrange
        val store = InMemoryEventStore()
        val aggregateId = "aggregate-1"
        val nonZeroHash = Hash("1".repeat(64))
        val event = TestEvent(
            id = "event-1",
            aggregateId = aggregateId,
            timestamp = 1000L,
            currentVersion = null,
            data = "First event"
        )
        
        // Act & Assert
        assertFailsWith<ConcurrencyException> {
            // Try to commit with non-zero hash for new aggregate
            store.commit(aggregateId, listOf(event), nonZeroHash)
        }
    }
    
    @Test
    fun `should get all events in order`() = runTest {
        // Arrange
        val store = InMemoryEventStore()
        val zeroHash = Hash("0".repeat(64))
        
        // Create events for two different aggregates
        val aggregate1 = "aggregate-1"
        val aggregate2 = "aggregate-2"
        
        val event1 = TestEvent(
            id = "event-1",
            aggregateId = aggregate1,
            timestamp = 1000L,
            currentVersion = null,
            data = "Aggregate 1, Event 1"
        )
        
        val event2 = TestEvent(
            id = "event-2",
            aggregateId = aggregate2,
            timestamp = 2000L,
            currentVersion = null,
            data = "Aggregate 2, Event 1"
        )
        
        val event3 = TestEvent(
            id = "event-3",
            aggregateId = aggregate1,
            timestamp = 3000L,
            currentVersion = event1.hash(),
            data = "Aggregate 1, Event 2"
        )
        
        // Act
        store.commit(aggregate1, listOf(event1), zeroHash)
        store.commit(aggregate2, listOf(event2), zeroHash)
        store.commit(aggregate1, listOf(event3), event1.hash())
        
        // Collect all events from the flow
        val allEvents = mutableListOf<Event>()
        store.getAllEvents().collect { allEvents.add(it) }
        
        // Assert
        assertEquals(3, allEvents.size, "Should retrieve all 3 events")
        // Events should be sorted by timestamp
        assertEquals(event1.id, allEvents[0].id, "First event should be the earliest by timestamp")
        assertEquals(event2.id, allEvents[1].id, "Second event should be the middle by timestamp")
        assertEquals(event3.id, allEvents[2].id, "Third event should be the latest by timestamp")
    }
}

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
        val hashString = "1".repeat(64)
        return Hash(hashString)
    }
}
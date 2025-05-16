package app.journal

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EventStoreTest {
    
    @Test
    fun `should filter events by type`() = runTest {
        // Arrange
        val store = provideEventStore()
        val aggregateId = "aggregate-1"
        val zeroHash = Hash("0".repeat(64))
        
        val event1 = TestEvent(
            id = "event-1",
            aggregateId = aggregateId,
            timestamp = 1000L,
            currentVersion = null,
            data = "First event",
            type = "test.type.a"
        )
        
        val event2 = TestEvent(
            id = "event-2",
            aggregateId = aggregateId,
            timestamp = 2000L,
            currentVersion = event1.hash(),
            data = "Second event",
            type = "test.type.b"
        )
        
        val event3 = TestEvent(
            id = "event-3",
            aggregateId = aggregateId,
            timestamp = 3000L,
            currentVersion = event2.hash(),
            data = "Third event",
            type = "test.type.a"
        )
        
        // Commit all events
        val hash1 = store.commit(aggregateId, event1, zeroHash)
        val hash2 = store.commit(aggregateId, event2, hash1)
        store.commit(aggregateId, event3, hash2)
        
        // Act - Filter by type A
        val typeAEvents = store.events(EventFilter(
            aggregateId = aggregateId,
            type = "test.type.a"
        ))
        
        // Assert
        assertEquals(2, typeAEvents.size, "Should find 2 events of type A")
        assertEquals(event1.id, typeAEvents[0].id, "First event of type A should match")
        assertEquals(event3.id, typeAEvents[1].id, "Second event of type A should match")
        
        // Act - Filter by type B
        val typeBEvents = store.events(EventFilter(
            aggregateId = aggregateId,
            type = "test.type.b"
        ))
        
        // Assert
        assertEquals(1, typeBEvents.size, "Should find 1 event of type B")
        assertEquals(event2.id, typeBEvents[0].id, "Event of type B should match")
    }
    
    @Test
    fun `should filter events by timestamp range`() = runTest {
        // Arrange
        val store = provideEventStore()
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
        
        val event3 = TestEvent(
            id = "event-3",
            aggregateId = aggregateId,
            timestamp = 3000L,
            currentVersion = event2.hash(),
            data = "Third event"
        )
        
        // Commit all events
        val hash1 = store.commit(aggregateId, event1, zeroHash)
        val hash2 = store.commit(aggregateId, event2, hash1)
        store.commit(aggregateId, event3, hash2)
        
        // Act - Filter by start time only
        val eventsAfter1500 = store.events(EventFilter(
            aggregateId = aggregateId,
            start = 1500L
        ))
        
        // Assert
        assertEquals(2, eventsAfter1500.size, "Should find 2 events after 1500")
        assertEquals(event2.id, eventsAfter1500[0].id, "First event after 1500 should be event2")
        assertEquals(event3.id, eventsAfter1500[1].id, "Second event after 1500 should be event3")
        
        // Act - Filter by end time only
        val eventsBefore2500 = store.events(EventFilter(
            aggregateId = aggregateId,
            end = 2500L
        ))
        
        // Assert
        assertEquals(2, eventsBefore2500.size, "Should find 2 events before 2500")
        assertEquals(event1.id, eventsBefore2500[0].id, "First event before 2500 should be event1")
        assertEquals(event2.id, eventsBefore2500[1].id, "Second event before 2500 should be event2")
        
        // Act - Filter by range
        val eventsInRange = store.events(EventFilter(
            aggregateId = aggregateId,
            start = 1500L,
            end = 2500L
        ))
        
        // Assert
        assertEquals(1, eventsInRange.size, "Should find 1 event in the range 1500-2500")
        assertEquals(event2.id, eventsInRange[0].id, "Event in range should be event2")
    }
    
    @Test
    fun `should filter events by combined criteria`() = runTest {
        // Arrange
        val store = provideEventStore()
        val aggregateId = "aggregate-1"
        val zeroHash = Hash("0".repeat(64))
        
        val event1 = TestEvent(
            id = "event-1",
            aggregateId = aggregateId,
            timestamp = 1000L,
            currentVersion = null,
            data = "First event",
            type = "test.type.a"
        )
        
        val event2 = TestEvent(
            id = "event-2",
            aggregateId = aggregateId,
            timestamp = 2000L,
            currentVersion = event1.hash(),
            data = "Second event",
            type = "test.type.b"
        )
        
        val event3 = TestEvent(
            id = "event-3",
            aggregateId = aggregateId,
            timestamp = 3000L,
            currentVersion = event2.hash(),
            data = "Third event",
            type = "test.type.a"
        )
        
        val event4 = TestEvent(
            id = "event-4",
            aggregateId = aggregateId,
            timestamp = 4000L,
            currentVersion = event3.hash(),
            data = "Fourth event",
            type = "test.type.b"
        )
        
        // Commit all events
        val hash1 = store.commit(aggregateId, event1, zeroHash)
        val hash2 = store.commit(aggregateId, event2, hash1)
        val hash3 = store.commit(aggregateId, event3, hash2)
        store.commit(aggregateId, event4, hash3)
        
        // Act - Filter by type and time range
        val filteredEvents = store.events(EventFilter(
            aggregateId = aggregateId,
            type = "test.type.a",
            start = 2000L
        ))
        
        // Assert
        assertEquals(1, filteredEvents.size, "Should find 1 event matching combined criteria")
        assertEquals(event3.id, filteredEvents[0].id, "Event matching criteria should be event3")
    }
    
    @Test
    fun `should successfully commit and retrieve events`() = runTest {
        // Arrange
        val store = provideEventStore()
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
        val currentHash = store.commit(aggregateId, event1, zeroHash)
        store.commit(aggregateId, (event2), currentHash)
        val retrievedEvents = store.events(eventsFor(aggregateId))
        
        // Assert
        assertEquals(2, retrievedEvents.size, "Should retrieve exactly 2 events")
        assertEquals(event1.id, retrievedEvents[0].id, "First event should match")
        assertEquals(event2.id, retrievedEvents[1].id, "Second event should match")
    }
    
    @Test
    fun `should throw concurrency exception when expected version doesn't match`() = runTest {
        // Arrange
        val store = provideEventStore()
        val aggregateId = "aggregate-1"
        val zeroHash = Hash("0".repeat(64))
        val wrongHash = someHash()  // This is different
        val event = TestEvent(
            id = "event-1",
            aggregateId = aggregateId,
            timestamp = 1000L,
            currentVersion = null,
            data = "First event"
        )
        
        // First commit succeeds
        val correctHash = store.commit(aggregateId, event, zeroHash)
        
        // Act & Assert - Second commit with wrong version fails
        assertFailsWith<ConcurrencyException> {
            // Try to commit with a different hash
            store.commit(
                aggregateId = aggregateId,
                event = (
                    TestEvent(
                        id = "event-2",
                        aggregateId = aggregateId,
                        timestamp = 2000L,
                        currentVersion = correctHash,
                        data = "Second event"
                    )
                ),
                expectedVersion = wrongHash
            )
        }
    }

    @Test
    fun `should enforce new aggregates start with zero hash`() = runTest {
        // Arrange
        val store = provideEventStore()
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
            store.commit(aggregateId, event, nonZeroHash)
        }
    }
    
    @Test
    fun `should get all events in order`() = runTest {
        // Arrange
        val store = provideEventStore()
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
        store.commit(aggregate1, event1, zeroHash)
        store.commit(aggregate2, event2, zeroHash)
        store.commit(aggregate1, event3, event1.hash())
        
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
    
    @Test
    fun `should handle concurrent commits to the same aggregateId`() = runTest {
        // Arrange
        val store = provideEventStore()
        val aggregateId = "concurrent-aggregate"
        val zeroHash = Hash("0".repeat(64))

        // First event to establish the aggregate
        val initialEvent = TestEvent(
            id = "initial-event",
            aggregateId = aggregateId,
            timestamp = 1000L,
            currentVersion = null,
            data = "Initial event"
        )
        
        // Commit the initial event
        val eventHash = store.commit(aggregateId, (initialEvent), zeroHash)
        
        // Create two events that will be committed concurrently
        val event1 = TestEvent(
            id = "concurrent-event-1",
            aggregateId = aggregateId,
            timestamp = 2000L,
            currentVersion = eventHash,
            data = "Concurrent event 1"
        )
        
        val event2 = TestEvent(
            id = "concurrent-event-2",
            aggregateId = aggregateId,
            timestamp = 3000L,
            currentVersion = eventHash,
            data = "Concurrent event 2"
        )
        
        // Track success/failure results
        var firstResult = false
        var secondResult = false
        
        // Act - Launch two coroutines to commit events concurrently
        listOf(
            async {
                try {
                    store.commit(aggregateId, event1, eventHash)
                    firstResult = true // Success
                } catch (e: ConcurrencyException) {
                    firstResult = false // Failure
                }
            },
            async {
                try {
                    // Add a small delay to increase the likelihood of a race condition
                    store.commit(aggregateId, event2, eventHash)
                    secondResult = true // Success
                } catch (e: ConcurrencyException) {
                    secondResult = false // Failure
                }
            }
        ).awaitAll()

        println("First commit result: $firstResult")
        println("Second commit result: $secondResult")
        
        // Assert - One operation should succeed and one should fail
        assertTrue(
            (firstResult && !secondResult) || (!firstResult && secondResult),
            "One commit should succeed and one should fail"
        )
        
        // Verify the event store state
        val events = store.events(eventsFor(aggregateId))
        assertEquals(2, events.size, "Should have two events (initial + one of the concurrent ones)")
        assertEquals(initialEvent.id, events[0].id, "First event should be the initial event")
        
        // The second event should be either event1 or event2, depending on which commit succeeded
        val committedEventId = events[1].id
        assertTrue(
            committedEventId == event1.id || committedEventId == event2.id,
            "Second event should be one of the concurrent events"
        )
    }
}
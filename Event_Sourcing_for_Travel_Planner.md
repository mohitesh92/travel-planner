# Building a Travel Planner with Event Sourcing: A Git-Inspired Approach

## Introduction

Modern mobile applications need robust architectures that can handle complex state management, support offline functionality, and enable collaborative features. In this article, we'll explore how we can leverage event sourcing concepts to build a powerful, resilient travel planning application.

We'll take inspiration from Git's distributed version control system to implement a travel planner that maintains a complete history of all changes, supports collaboration, and works seamlessly offline.

## What is Event Sourcing?

Event sourcing is an architectural pattern where:

1. **State changes are recorded as immutable events**
2. **The current state is derived by replaying events**
3. **The event log becomes the system's source of truth**

Instead of storing the current state directly, we store the sequence of events that led to that state. This gives us a complete audit trail, time-travel capabilities, and improved concurrency control.

## The Git Parallel: Why It Works for Travel Planning

Git is essentially an event sourcing system for code. When we consider a travel planner app, the parallels become clear:

- **Trips = Repositories**: Each trip is a separate repository with its own history
- **Itinerary changes = Commits**: Changes to a trip are saved as commits
- **Collaboration = Shared repositories**: Multiple travelers can contribute to a shared trip
- **Conflict resolution**: When two travelers make conflicting changes, we need to resolve them

This mental model is intuitive for developers and can be translated into user-friendly concepts for travelers.

## Core Components of Our Event Sourcing Architecture

### 1. Event

Events are immutable facts that have occurred in our system. They represent state changes and are named in past tense.

```kotlin
interface Event {
    val id: String               // Unique event identifier
    val aggregateId: String      // Which trip this event belongs to
    val timestamp: Long          // When the event occurred  
    val version: Long            // Sequential version number
}

data class TripCreatedEvent(
    override val id: String,
    override val aggregateId: String,
    override val timestamp: Long,
    override val version: Long,
    val name: String,
    val destination: String,
    val startDate: Long,
    val endDate: Long
) : Event
```

The `version` field acts like a commit ID in Git—it ensures we maintain the correct sequence and helps with concurrency control. Just as Git requires you to be on the same commit as the remote before pushing changes, our system requires you to be on the expected version before applying new events.

### 2. Command

Commands are intentions to change state. They're named in imperative form and may be rejected if they violate business rules.

```kotlin
interface Command {
    val id: String
    val aggregateId: String
}

data class CreateTripCommand(
    override val id: String,
    override val aggregateId: String,
    val name: String,
    val destination: String,
    val startDate: Long,
    val endDate: Long
) : Command
```

### 3. Aggregate

Aggregates encapsulate domain entities and ensure business rule consistency. They process commands and emit events.

```kotlin
abstract class Aggregate<E : Event>(val id: String) {
    var version: Long = 0
    protected val changes = mutableListOf<E>()
    
    fun uncommittedChanges(): List<E> = changes.toList()
    fun markChangesAsCommitted() = changes.clear()
    abstract fun apply(event: E)
}

class TripAggregate(id: String) : Aggregate<Event>(id) {
    private var name: String = ""
    private var destination: String = ""
    private var startDate: Long = 0
    private var endDate: Long = 0
    private val activities = mutableListOf<String>()
    
    fun process(command: CreateTripCommand): Event {
        // Validate command
        require(startDate <= endDate) { "Start date must be before end date" }
        
        // Create and apply event
        val event = TripCreatedEvent(
            id = UUID.randomUUID().toString(),
            aggregateId = command.aggregateId,
            timestamp = System.currentTimeMillis(),
            version = version + 1,
            name = command.name,
            destination = command.destination,
            startDate = command.startDate,
            endDate = command.endDate
        )
        apply(event)
        changes.add(event)
        return event
    }
    
    override fun apply(event: Event) {
        when (event) {
            is TripCreatedEvent -> {
                name = event.name
                destination = event.destination
                startDate = event.startDate
                endDate = event.endDate
                version = event.version
            }
            // Handle other event types...
        }
    }
}
```

### 4. Event Store

The event store is the persistent storage for all events—the source of truth in our system.

```kotlin
interface EventStore {
    suspend fun saveEvents(aggregateId: String, events: List<Event>, expectedVersion: Long)
    suspend fun getEventsForAggregate(aggregateId: String): List<Event>
    suspend fun getAllEvents(): Flow<Event>
}
```

### 5. Command Handler

Command handlers orchestrate processing of commands, ensuring they're valid and storing resulting events.

```kotlin
interface CommandHandler<C : Command> {
    suspend fun handle(command: C): Result<List<Event>>
}
```

### 6. Event Handler/Projector

Projectors build read models optimized for queries by consuming events.

```kotlin
interface EventHandler {
    suspend fun handle(event: Event)
}
```

### 7. Repository

Repositories reconstruct aggregate state by replaying events.

```kotlin
interface AggregateRepository<T : Aggregate<*>> {
    suspend fun getById(id: String): T
    suspend fun save(aggregate: T)
}
```

### 8. State

The application state derived from events.

```kotlin
data class TripDto(
    val id: String,
    val name: String,
    val destination: String,
    val startDate: Long,
    val endDate: Long,
    val activities: List<ActivityDto>
)
```

### 9. Event Bus

Distributes events to interested handlers.

```kotlin
interface EventBus {
    suspend fun publish(event: Event)
    fun subscribe(handler: EventHandler)
}
```

## The Git-Inspired Travel Planning Experience

Using this architecture, we can create a powerful travel planning experience:

### 1. Trip Creation and History

When a user creates a new trip, we're essentially creating a new "repository" with an initial commit. Every change to the trip (adding activities, changing dates, etc.) becomes a new commit with a descriptive message.

Users can view the complete history of their trip planning process, see who made each change, and even revert to previous versions if needed.

### 2. Collaborative Planning

Trips can be shared with fellow travelers, giving them different permission levels (owner, editor, viewer). When multiple people plan together, their changes are tracked separately and can be merged.

This is perfect for group trips where different people might be responsible for different aspects of planning.

### 3. Offline Support

One of the biggest advantages of our Git-inspired approach is robust offline support. Users can make changes to their trips while offline, essentially creating "local commits" that will be synchronized when connectivity returns.

Conflict resolution happens automatically for non-overlapping changes, while meaningful conflicts can be presented to users for resolution.

### 4. Branching for Alternative Plans

More advanced users can create branches for alternative itineraries. For example, a "rainy-day-plan" branch could contain indoor activities to swap in if the weather is poor.

These alternatives can be compared side-by-side and merged as needed.

## Implementation Strategy

Our implementation will be housed in a dedicated `journal` module within our Kotlin Multiplatform project, allowing the event sourcing system to be used across both Android and iOS.

We'll start by defining the core interfaces, then implement the basic functionality needed for a minimum viable product:

1. Create trips
2. Add activities and bookings
3. Share trips with other users
4. Support basic offline operations

Once that foundation is in place, we can add more advanced features like branching, conflict resolution UI, and detailed history visualization.

## Benefits of This Approach

1. **Complete History**: Every change is tracked with attribution
2. **Collaboration**: Multiple users can work on the same trip
3. **Offline Support**: The app works perfectly without connectivity
4. **Conflict Resolution**: Clean handling of concurrent edits
5. **Time Travel**: Ability to view or restore previous versions
6. **Event Replay**: Ability to reconstruct state at any point
7. **Audit Trail**: Who changed what and when
8. **Scalability**: Event sourcing architectures scale well

## Conclusion

By combining event sourcing principles with Git-inspired concepts, we can create a travel planning application that is robust, collaborative, and works reliably even without constant connectivity.

This architecture might seem complex at first, but it enables powerful features that would be difficult to implement with traditional CRUD approaches. The investment in this foundation will pay dividends as the application grows in complexity and user base.
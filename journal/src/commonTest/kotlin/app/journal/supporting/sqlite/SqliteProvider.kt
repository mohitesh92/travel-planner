package app.journal.supporting.sqlite

import app.cash.sqldelight.db.SqlDriver
import app.journal.Event
import app.journal.EventStore
import app.journal.HashAdapter
import app.journal.RefStore
import app.journal.TestEvent
import app.journal.db.Events
import app.journal.db.JournalDatabase
import app.journal.db.Refs
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic

/**
 * Provides an in-memory SQLite-based implementation of EventStore and RefStore for testing.
 *
 * @return A SqliteStore instance that implements both EventStore and RefStore interfaces
 */
fun provideSqliteStore(): SqliteStore {
    // Create an in-memory database for testing
    val driver: SqlDriver = provideSqlDriver()

    // Create the schema
    JournalDatabase.Schema.create(driver)

    // Create the database
    val database = JournalDatabase(
        driver = driver,
        refsAdapter = Refs.Adapter(
            versionAdapter = HashAdapter
        ),
        eventsAdapter = Events.Adapter(
            parentAdapter = HashAdapter,
        ),
    )

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        useArrayPolymorphism = true
        serializersModule = SerializersModule {
            polymorphic(Event::class) {
                subclass(TestEvent::class, TestEvent.serializer())
            }
        }
    }

    // Return the store
    return SqliteStore(database, json)
}

expect fun provideSqlDriver(): SqlDriver

/**
 * Provides an in-memory SQLite-based implementation of EventStore for testing.
 * This is a convenience function to use the same provider pattern as the in-memory tests.
 *
 * @return A SqliteStore instance cast to EventStore
 */
fun provideSqliteEventStore(): EventStore {
    return provideSqliteStore()
}

/**
 * Provides an in-memory SQLite-based implementation of RefStore for testing.
 * This is a convenience function to use the same provider pattern as the in-memory tests.
 *
 * @return A SqliteStore instance cast to RefStore
 */
fun provideSqliteRefStore(): RefStore {
    return provideSqliteStore()
}
package app.journal

import app.journal.db.JournalDatabase
import app.journal.supporting.memory.InMemoryEventStore
import app.journal.supporting.memory.InMemoryRefStore
import app.journal.supporting.sqlite.JournalDatabaseFactory
import app.journal.supporting.sqlite.SqliteStore

/**
 * Repository class that manages access to the EventStore and RefStore.
 * This class can create either in-memory or persistent implementations of the stores.
 */
class JournalRepository private constructor(
    private val eventStore: EventStore,
) {
    companion object {
        /**
         * Creates an instance of JournalRepository with in-memory implementations.
         * This is suitable for testing or small-scale usage.
         *
         * @return A JournalRepository with in-memory storage
         */
        fun createInMemory(): JournalRepository {
            val refStore = InMemoryRefStore()
            val eventStore = InMemoryEventStore(refStore)
            return JournalRepository(eventStore)
        }

        /**
         * Creates an instance of JournalRepository with SQLite-based persistent implementations.
         * This is suitable for production usage.
         *
         * @param databaseFactory A factory to create the SQLite database
         * @return A JournalRepository with persistent storage
         */
        fun createPersistent(databaseFactory: JournalDatabaseFactory, json: PolymorphicJson): JournalRepository {
            val database = databaseFactory.createDatabase()
            return createWithDatabase(database, json)
        }

        /**
         * Creates an instance of JournalRepository with the given database.
         * This is useful for testing with a specific database instance.
         *
         * @param database The SQLite database to use
         * @return A JournalRepository using the provided database
         */
        fun createWithDatabase(database: JournalDatabase, json: PolymorphicJson): JournalRepository {
            val store = SqliteStore(database, json)
            return JournalRepository(store)
        }
    }
}
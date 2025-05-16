package app.journal.supporting.sqlite

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.journal.HashAdapter
import app.journal.db.Events
import app.journal.db.JournalDatabase
import app.journal.db.Refs

/**
 * Android implementation of JournalDatabaseFactory.
 * Uses AndroidSqliteDriver for SQLite access.
 */
actual class JournalDatabaseFactory(private val context: Context) {
    /**
     * Creates a JournalDatabase instance with an Android-specific SQLite driver.
     *
     * @return A JournalDatabase instance ready to use
     */
    actual fun createDatabase(): JournalDatabase {
        val driver = AndroidSqliteDriver(
            schema = JournalDatabase.Schema,
            context = context,
            name = DATABASE_NAME
        )
        return JournalDatabase(
            driver = driver,
            refsAdapter = Refs.Adapter(
                versionAdapter = HashAdapter
            ),
            eventsAdapter = Events.Adapter(
                parentAdapter = HashAdapter,
            ),
        )
    }

    companion object {
        private const val DATABASE_NAME = "journal.db"
    }
}

/**
 * A factory function that requires Android Context, which must be provided
 * at the application level.
 */
actual fun createJournalDatabaseFactory(): JournalDatabaseFactory {
    // This function cannot be implemented correctly for Android without a context
    // The application should provide the context when creating the factory
    throw IllegalStateException(
        "For Android, please use the JournalDatabaseFactory constructor and provide a Context"
    )
}
package app.journal.supporting.sqlite

import app.journal.db.JournalDatabase

/**
 * Factory for creating platform-specific database instances.
 * This is an expect class that will have platform-specific implementations
 * to handle the differences between Android, iOS, and other platforms.
 */
expect class JournalDatabaseFactory {
    /**
     * Creates a JournalDatabase instance using platform-specific SQLite driver.
     * 
     * @return A JournalDatabase instance ready to use
     */
    fun createDatabase(): JournalDatabase
}

/**
 * Helper function to create a database factory based on the current platform.
 * 
 * @return A JournalDatabaseFactory appropriate for the current platform
 */
expect fun createJournalDatabaseFactory(): JournalDatabaseFactory
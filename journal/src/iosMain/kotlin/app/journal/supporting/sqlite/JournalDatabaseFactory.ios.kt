@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package app.journal.supporting.sqlite

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.journal.db.JournalDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * iOS implementation of JournalDatabaseFactory.
 * Uses NativeSqliteDriver for SQLite access.
 */
actual class JournalDatabaseFactory {
    /**
     * Creates a JournalDatabase instance with an iOS-specific SQLite driver.
     * 
     * @return A JournalDatabase instance ready to use
     */
    actual fun createDatabase(): JournalDatabase {
        val driver = NativeSqliteDriver(
            schema = JournalDatabase.Schema,
            name = DATABASE_NAME
        )
        return JournalDatabase(driver)
    }
    
    companion object {
        private const val DATABASE_NAME = "journal.db"
    }
}

/**
 * Creates a JournalDatabaseFactory for iOS.
 * 
 * @return A JournalDatabaseFactory instance
 */
actual fun createJournalDatabaseFactory(): JournalDatabaseFactory {
    return JournalDatabaseFactory()
}
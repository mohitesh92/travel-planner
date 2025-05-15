package app.journal

/**
 * Interface for command handlers, which are responsible for processing commands.
 * Command handlers validate commands, load the appropriate aggregate,
 * and coordinate saving the resulting events.
 * 
 * @param C The type of command this handler processes
 */
interface CommandHandler<C : Command> {
    
    /**
     * Handles a command and produces events.
     * 
     * @param command The command to process
     * @return A Result containing the list of generated events on success, or an exception on failure
     */
    suspend fun handle(command: C): Result<List<Event>>
}

/**
 * Exception thrown when an unsupported command is received.
 */
class UnsupportedCommandException(message: String) : Exception(message)
package app.journal

interface RefStore {
    /**
     * Store a reference to an object.
     *
     * @param aggregateId The unique identifier for the reference
     * @param newRef The reference to store
     * @return The unique identifier for the stored reference
     */
    suspend fun swap(aggregateId: String, newRef: Hash, oldRef: Hash?)

    /**
     * Retrieve a reference by its unique identifier.
     *
     * @param aggregateId The unique identifier of the reference
     * @return The stored reference, or null if not found
     */
    suspend fun read(aggregateId: String): Hash?
}
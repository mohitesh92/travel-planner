package app.journal

import kotlin.jvm.JvmInline

/**
 * A hash is a unique identifier for a piece of data.
 */
@JvmInline
value class Hash(
    private val value: String
) : Comparable<Hash> {
    init {
        // SHA256 returns a 64 character hex string
        require(value.length == 64)

        // SHA256 returns a hex string
        require(value.all { it in '0'..'9' || it in 'a'..'f' })
    }

    override fun compareTo(other: Hash): Int {
        return value.compareTo(other.value)
    }

    operator fun compareTo(other: String): Int {
        return value.compareTo(other)
    }

    override fun toString(): String {
        return value
    }
}

val String.hash: Hash
    get() = Hash(sha256(this))

interface Hashable {
    fun hash(): Hash
}

expect fun sha256(data: String): String

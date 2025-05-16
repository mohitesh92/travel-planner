package app.journal

import java.security.MessageDigest

actual fun sha256(data: String): String {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val digest = messageDigest.digest(data.toByteArray())
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}
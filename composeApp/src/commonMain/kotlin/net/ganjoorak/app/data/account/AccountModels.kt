package net.ganjoorak.app.data.account

import kotlinx.serialization.Serializable

/** A signed-in account, matching the website's AccountUser. */
@Serializable
data class AccountUser(
    val username: String,
    val displayName: String = "",
    val createdAt: Long = 0,
    val isPublic: Boolean = false,
    val avatar: String = "",
    val hasRecoveryCode: Boolean = false,
    val bio: String = "",
    val banner: String = "",
    val accent: String = "",
    val background: String = "",
)

class AccountException(message: String, val status: Int = 0) : Exception(message)

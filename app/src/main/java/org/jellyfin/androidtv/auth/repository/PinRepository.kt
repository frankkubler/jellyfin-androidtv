package org.jellyfin.androidtv.auth.repository

import org.jellyfin.androidtv.auth.store.AuthenticationStore
import java.security.MessageDigest
import java.util.UUID

/**
 * Repository managing local PIN protection for stored user profiles.
 * The PIN is stored as a SHA-256 hash of (pin + userId) to prevent plaintext exposure.
 */
interface PinRepository {
	fun hasPin(serverId: UUID, userId: UUID): Boolean
	fun setPin(serverId: UUID, userId: UUID, pin: String): Boolean
	fun removePin(serverId: UUID, userId: UUID): Boolean
	fun validatePin(serverId: UUID, userId: UUID, pin: String): Boolean
}

class PinRepositoryImpl(
	private val authenticationStore: AuthenticationStore,
) : PinRepository {
	override fun hasPin(serverId: UUID, userId: UUID): Boolean {
		return authenticationStore.getUser(serverId, userId)?.pinHash != null
	}

	override fun setPin(serverId: UUID, userId: UUID, pin: String): Boolean {
		val user = authenticationStore.getUser(serverId, userId) ?: return false
		val hash = hashPin(pin, userId)
		return authenticationStore.putUser(serverId, userId, user.copy(pinHash = hash))
	}

	override fun removePin(serverId: UUID, userId: UUID): Boolean {
		val user = authenticationStore.getUser(serverId, userId) ?: return false
		return authenticationStore.putUser(serverId, userId, user.copy(pinHash = null))
	}

	override fun validatePin(serverId: UUID, userId: UUID, pin: String): Boolean {
		val storedHash = authenticationStore.getUser(serverId, userId)?.pinHash ?: return false
		return hashPin(pin, userId) == storedHash
	}

	private fun hashPin(pin: String, userId: UUID): String {
		val input = pin + userId.toString()
		val digest = MessageDigest.getInstance("SHA-256")
		val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
		return hashBytes.joinToString("") { "%02x".format(it) }
	}
}

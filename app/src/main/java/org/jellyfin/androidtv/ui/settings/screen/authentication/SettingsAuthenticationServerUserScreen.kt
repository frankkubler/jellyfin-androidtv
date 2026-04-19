package org.jellyfin.androidtv.ui.settings.screen.authentication

import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.PinRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import java.util.UUID

/** Action to perform after PIN verification */
private enum class PinVerifyAction { CHANGE, REMOVE }

@Composable
fun SettingsAuthenticationServerUserScreen(serverId: UUID, userId: UUID) {
	val router = LocalRouter.current
	val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
	val serverRepository = koinInject<ServerRepository>()
	val serverUserRepository = koinInject<ServerUserRepository>()
	val authenticationRepository = koinInject<AuthenticationRepository>()
	val pinRepository = koinInject<PinRepository>()

	LaunchedEffect(serverRepository) { serverRepository.loadStoredServers() }

	val server by remember(serverRepository.storedServers) {
		serverRepository.storedServers.map { it.find { server -> server.id == serverId } }
	}.collectAsState(null)

	val user = remember(server) { server?.let(serverUserRepository::getStoredServerUsers)?.find { user -> user.id == userId } }

	var hasPin by remember(user) {
		mutableStateOf(pinRepository.hasPin(serverId, userId))
	}

	// PIN verification dialog state
	var verifyAction by remember { mutableStateOf<PinVerifyAction?>(null) }
	var verifyPin by remember { mutableStateOf("") }
	var verifyError by remember { mutableStateOf(false) }

	// Show PIN verification dialog when an action requires it
	if (verifyAction != null) {
		androidx.compose.ui.window.Dialog(
			onDismissRequest = {
				verifyAction = null
				verifyPin = ""
				verifyError = false
			}
		) {
			androidx.compose.foundation.layout.Box(
				modifier = Modifier
					.fillMaxWidth()
					.padding(24.dp)
			) {
				Column(modifier = Modifier.fillMaxWidth()) {
					Text(stringResource(R.string.pin_verify_title))
					Spacer(modifier = Modifier.height(12.dp))
					AndroidView(
						modifier = Modifier.fillMaxWidth(),
						factory = { context ->
							android.widget.EditText(context).apply {
								inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
								filters = arrayOf(InputFilter.LengthFilter(4))
								hint = context.getString(R.string.pin_hint)
								gravity = android.view.Gravity.CENTER
								requestFocus()
								addTextChangedListener(object : TextWatcher {
									override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
									override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
									override fun afterTextChanged(s: Editable?) {
										verifyPin = s?.toString() ?: ""
										verifyError = false
									}
								})
							}
						}
					)
					if (verifyError) {
						Spacer(modifier = Modifier.height(4.dp))
						Text(stringResource(R.string.pin_incorrect))
					}
					Spacer(modifier = Modifier.height(12.dp))
					androidx.compose.foundation.layout.Row {
						ListButton(
							headingContent = { Text(stringResource(R.string.pin_confirm_button)) },
							onClick = {
								if (!pinRepository.validatePin(serverId, userId, verifyPin)) {
									verifyError = true
									verifyPin = ""
								} else {
									val action = verifyAction
									verifyAction = null
									verifyPin = ""
									verifyError = false
									when (action) {
										PinVerifyAction.CHANGE -> router.push(
											Routes.AUTHENTICATION_SERVER_USER_PIN,
											mapOf("serverId" to serverId.toString(), "userId" to userId.toString())
										)
										PinVerifyAction.REMOVE -> lifecycleScope.launch {
											pinRepository.removePin(serverId, userId)
											hasPin = false
										}
										null -> Unit
									}
								}
							}
						)
						ListButton(
							headingContent = { Text(stringResource(R.string.lbl_cancel)) },
							onClick = {
								verifyAction = null
								verifyPin = ""
								verifyError = false
							}
						)
					}
				}
			}
		}
	}

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(server?.name?.uppercase().orEmpty()) },
				headingContent = { Text(user?.name.orEmpty()) },
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_logout), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.lbl_sign_out)) },
				captionContent = { Text(stringResource(R.string.lbl_sign_out_content)) },
				onClick = {
					lifecycleScope.launch {
						if (user != null) authenticationRepository.logout(user)
						router.back()
					}
				}
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.lbl_remove)) },
				captionContent = { Text(stringResource(R.string.lbl_remove_user_content)) },
				onClick = {
					lifecycleScope.launch {
						if (user != null) serverUserRepository.deleteStoredUser(user)
						router.back()
					}
				}
			)
		}

		if (!hasPin) {
			item {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_lock), contentDescription = null) },
					headingContent = { Text(stringResource(R.string.pin_setup)) },
					captionContent = { Text(stringResource(R.string.pin_setup_description)) },
					onClick = {
						router.push(
							Routes.AUTHENTICATION_SERVER_USER_PIN,
							mapOf("serverId" to serverId.toString(), "userId" to userId.toString())
						)
					}
				)
			}
		} else {
			item {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_lock), contentDescription = null) },
					headingContent = { Text(stringResource(R.string.pin_change)) },
					captionContent = { Text(stringResource(R.string.pin_change_description)) },
					onClick = {
						verifyPin = ""
						verifyError = false
						verifyAction = PinVerifyAction.CHANGE
					}
				)
			}

			item {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
					headingContent = { Text(stringResource(R.string.pin_remove)) },
					captionContent = { Text(stringResource(R.string.pin_remove_description)) },
					onClick = {
						verifyPin = ""
						verifyError = false
						verifyAction = PinVerifyAction.REMOVE
					}
				)
			}
		}
	}
}


@Composable
fun SettingsAuthenticationServerUserScreen(serverId: UUID, userId: UUID) {
	val router = LocalRouter.current
	val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
	val serverRepository = koinInject<ServerRepository>()
	val serverUserRepository = koinInject<ServerUserRepository>()
	val authenticationRepository = koinInject<AuthenticationRepository>()
	val pinRepository = koinInject<PinRepository>()

	LaunchedEffect(serverRepository) { serverRepository.loadStoredServers() }

	val server by remember(serverRepository.storedServers) {
		serverRepository.storedServers.map { it.find { server -> server.id == serverId } }
	}.collectAsState(null)

	val user = remember(server) { server?.let(serverUserRepository::getStoredServerUsers)?.find { user -> user.id == userId } }

	var hasPin by remember(user) {
		mutableStateOf(pinRepository.hasPin(serverId, userId))
	}

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(server?.name?.uppercase().orEmpty()) },
				headingContent = { Text(user?.name.orEmpty()) },
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_logout), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.lbl_sign_out)) },
				captionContent = { Text(stringResource(R.string.lbl_sign_out_content)) },
				onClick = {
					lifecycleScope.launch {
						if (user != null) authenticationRepository.logout(user)
						router.back()
					}
				}
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.lbl_remove)) },
				captionContent = { Text(stringResource(R.string.lbl_remove_user_content)) },
				onClick = {
					lifecycleScope.launch {
						if (user != null) serverUserRepository.deleteStoredUser(user)
						router.back()
					}
				}
			)
		}

		if (!hasPin) {
			item {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_lock), contentDescription = null) },
					headingContent = { Text(stringResource(R.string.pin_setup)) },
					captionContent = { Text(stringResource(R.string.pin_setup_description)) },
					onClick = {
						router.push(
							Routes.AUTHENTICATION_SERVER_USER_PIN,
							mapOf("serverId" to serverId.toString(), "userId" to userId.toString())
						)
					}
				)
			}
		} else {
			item {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_lock), contentDescription = null) },
					headingContent = { Text(stringResource(R.string.pin_change)) },
					captionContent = { Text(stringResource(R.string.pin_change_description)) },
					onClick = {
						router.push(
							Routes.AUTHENTICATION_SERVER_USER_PIN,
							mapOf("serverId" to serverId.toString(), "userId" to userId.toString())
						)
					}
				)
			}

			item {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
					headingContent = { Text(stringResource(R.string.pin_remove)) },
					captionContent = { Text(stringResource(R.string.pin_remove_description)) },
					onClick = {
						lifecycleScope.launch {
							pinRepository.removePin(serverId, userId)
							hasPin = false
						}
					}
				)
			}
		}
	}
}

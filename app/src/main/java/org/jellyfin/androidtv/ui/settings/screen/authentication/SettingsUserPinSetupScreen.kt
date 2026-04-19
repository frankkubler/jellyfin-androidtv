package org.jellyfin.androidtv.ui.settings.screen.authentication

import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.Editable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.PinRepository
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import java.util.UUID

@Composable
fun SettingsUserPinSetupScreen(serverId: UUID, userId: UUID) {
	val router = LocalRouter.current
	val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
	val pinRepository = koinInject<PinRepository>()

	var newPin by remember { mutableStateOf("") }
	var confirmPin by remember { mutableStateOf("") }
	var errorMessage by remember { mutableStateOf<String?>(null) }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_login).uppercase()) },
				headingContent = { Text(stringResource(R.string.pin_setup_title)) },
			)
		}

		item {
			Column(modifier = Modifier.fillMaxWidth()) {
				Text(
					text = stringResource(R.string.pin_new_label),
					modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
				)
				AndroidView(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp, vertical = 4.dp),
					factory = { context ->
						android.widget.EditText(context).apply {
							inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
							filters = arrayOf(InputFilter.LengthFilter(4))
							hint = context.getString(R.string.pin_hint)
							addTextChangedListener(object : TextWatcher {
								override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
								override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
								override fun afterTextChanged(s: Editable?) { newPin = s?.toString() ?: "" }
							})
						}
					}
				)
			}
		}

		item {
			Column(modifier = Modifier.fillMaxWidth()) {
				Text(
					text = stringResource(R.string.pin_confirm_label),
					modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
				)
				AndroidView(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp, vertical = 4.dp),
					factory = { context ->
						android.widget.EditText(context).apply {
							inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
							filters = arrayOf(InputFilter.LengthFilter(4))
							hint = context.getString(R.string.pin_hint)
							addTextChangedListener(object : TextWatcher {
								override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
								override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
								override fun afterTextChanged(s: Editable?) { confirmPin = s?.toString() ?: "" }
							})
						}
					}
				)
			}
		}

		errorMessage?.let { msg ->
			item {
				Text(
					text = msg,
					modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
				)
			}
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.pin_save)) },
				onClick = {
					when {
						newPin.length != 4 -> errorMessage = "PIN must be exactly 4 digits"
						newPin != confirmPin -> errorMessage = "PINs do not match"
						else -> {
							lifecycleScope.launch {
								pinRepository.setPin(serverId, userId, newPin)
								router.back()
							}
						}
					}
				}
			)
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_cancel)) },
				onClick = { router.back() }
			)
		}
	}
}

package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.ApiClientErrorLoginState
import org.jellyfin.androidtv.auth.model.AuthenticatedState
import org.jellyfin.androidtv.auth.model.AuthenticatingState
import org.jellyfin.androidtv.auth.model.PrivateUser
import org.jellyfin.androidtv.auth.model.RequireSignInState
import org.jellyfin.androidtv.auth.model.ServerUnavailableState
import org.jellyfin.androidtv.auth.model.ServerVersionNotSupported
import org.jellyfin.androidtv.auth.repository.PinRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.databinding.FragmentUserPinBinding
import org.jellyfin.androidtv.ui.startup.StartupViewModel
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class UserPinFragment : Fragment() {
	companion object {
		const val ARG_SERVER_ID = "server_id"
		const val ARG_USER_ID = "user_id"
		const val ARG_USER_NAME = "user_name"
	}

	private val startupViewModel: StartupViewModel by activityViewModel()
	private val pinRepository: PinRepository by inject()
	private var _binding: FragmentUserPinBinding? = null
	private val binding get() = _binding!!

	private val serverIdArgument get() = arguments?.getString(ARG_SERVER_ID)?.toUUIDOrNull()
	private val userIdArgument get() = arguments?.getString(ARG_USER_ID)?.toUUIDOrNull()
	private val userNameArgument get() = arguments?.getString(ARG_USER_NAME)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		_binding = FragmentUserPinBinding.inflate(inflater, container, false)

		binding.pinUsername.text = userNameArgument

		with(binding.pinInput) {
			setOnEditorActionListener { _, actionId, _ ->
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					verifyPin()
					true
				} else {
					false
				}
			}
		}

		binding.confirm.setOnClickListener { verifyPin() }
		binding.cancel.setOnClickListener { parentFragmentManager.popBackStack() }

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.pinInput.requestFocus()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun verifyPin() {
		val serverId = serverIdArgument ?: return
		val userId = userIdArgument ?: return
		val enteredPin = binding.pinInput.text.toString()

		if (enteredPin.length != 4) {
			binding.error.setText(R.string.pin_length_error)
			binding.pinInput.text.clear()
			return
		}

		if (!pinRepository.validatePin(serverId, userId, enteredPin)) {
			binding.error.setText(R.string.pin_incorrect)
			binding.pinInput.text.clear()
			return
		}

		// PIN validated — proceed with authentication
		val server = startupViewModel.getServer(serverId) ?: return
		val users = startupViewModel.users.value
		val user = users.filterIsInstance<PrivateUser>().find { it.id == userId } ?: return

		startupViewModel.authenticate(server, user).onEach { state ->
			when (state) {
				AuthenticatingState -> Unit
				AuthenticatedState -> Unit
				RequireSignInState -> {
					requireActivity().supportFragmentManager.commit {
						replace<UserLoginFragment>(
							R.id.content_view,
							null,
							bundleOf(
								UserLoginFragment.ARG_SERVER_ID to serverId.toString(),
								UserLoginFragment.ARG_USERNAME to userNameArgument,
							)
						)
						addToBackStack(null)
					}
				}
				ServerUnavailableState,
				is ApiClientErrorLoginState -> Toast.makeText(
					context, R.string.server_connection_failed, Toast.LENGTH_LONG
				).show()
				is ServerVersionNotSupported -> Toast.makeText(
					context,
					getString(
						R.string.server_issue_outdated_version,
						state.server.version,
						ServerRepository.recommendedServerVersion.toString()
					),
					Toast.LENGTH_LONG
				).show()
			}
		}.launchIn(lifecycleScope)
	}
}

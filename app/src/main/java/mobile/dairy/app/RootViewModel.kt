package mobile.dairy.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import mobile.dairy.app.data.AuthRepository
import mobile.dairy.app.data.PrefsRepository
import mobile.dairy.app.domain.AppPrefs
import mobile.dairy.app.services.LocalPrefs
import mobile.dairy.app.services.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    prefsRepository: PrefsRepository,
    localPrefs: LocalPrefs,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val user = authRepository.authState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUser)

    @OptIn(ExperimentalCoroutinesApi::class)
    val prefs: StateFlow<AppPrefs> = user.flatMapLatest { u ->
        if (u == null) flowOf(AppPrefs()) else prefsRepository.appPrefs()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs())

    val onboarded = localPrefs.onboarded
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val locked = MutableStateFlow(false)

    init {
        // Lock the diary whenever the app leaves the foreground (if enabled).
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                if (prefs.value.lockEnabled) locked.value = true
            }
        })
        viewModelScope.launch {
            // Ensure reminders exist on first launch.
            reminderScheduler.rescheduleAll()
        }
    }

    fun unlock() { locked.value = false }
    fun signOut() = authRepository.signOut()
}

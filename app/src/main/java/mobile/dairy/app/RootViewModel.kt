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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

import mobile.dairy.app.services.FirestoreSyncManager

@HiltViewModel
class RootViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    prefsRepository: PrefsRepository,
    private val localPrefs: LocalPrefs,
    private val reminderScheduler: ReminderScheduler,
    private val syncManager: FirestoreSyncManager,
    private val database: mobile.dairy.app.data.AppDatabase,
    private val sessionManager: mobile.dairy.app.data.SessionManager
) : ViewModel() {

    val user = authRepository.authState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUser)

    val activeLocalUserId = localPrefs.activeLocalUserId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val prefs: StateFlow<AppPrefs> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        prefsRepository.appPrefs()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs())

    val onboarded = localPrefs.onboarded
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val onlineMode = localPrefs.isOnlineMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val hasSeenTour = localPrefs.hasSeenTour
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val locked = MutableStateFlow(false)
    val globalLoading = MutableStateFlow(false)
    val initialLoading = MutableStateFlow(true)

    fun setLoading(isLoading: Boolean) {
        globalLoading.value = isLoading
    }

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
            // Dismiss initial loading after auth & initial state settle
            delay(600)
            initialLoading.value = false
        }
        
        viewModelScope.launch {
            combine(user, onlineMode) { u, isOnline ->
                Pair(u, isOnline)
            }.collect { (currentUser, isOnline) ->
                if (isOnline && currentUser != null) {
                    val activeLocalId = localPrefs.activeLocalUserId.firstOrNull()
                    if (activeLocalId != null && activeLocalId != "guest") {
                        globalLoading.value = true
                        try {
                            // Migrate all records from activeLocalId to currentUser.uid
                            syncManager.migrateLocalUserToCloud(activeLocalId, currentUser.uid, database)
                            // Clear the activeLocalUserId so we don't do it again
                            localPrefs.setActiveLocalUserId(null)
                            syncManager.sync()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            globalLoading.value = false
                        }
                    }
                }
            }
        }
    }

    fun unlock() { locked.value = false }
    fun finishTour() { viewModelScope.launch { localPrefs.setHasSeenTour() } }
    fun resetTour() { viewModelScope.launch { localPrefs.resetHasSeenTour() } }
    fun signOut() {
        viewModelScope.launch {
            localPrefs.setActiveLocalUserId(null)
            authRepository.signOut()
        }
    }
}

package mobile.dairy.app.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import mobile.dairy.app.data.PrefsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * FCM endpoint for server pushes from the shared Cloud Functions backend
 * (goal-milestone celebrations, Monday weekly summary).
 */
@AndroidEntryPoint
class BloomMessagingService : FirebaseMessagingService() {

    @Inject lateinit var prefsRepository: PrefsRepository
    @Inject lateinit var localPrefs: LocalPrefs

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            runCatching { prefsRepository.saveDeviceToken(token) } // no-op when signed out
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "Bloom"
        val body = message.notification?.body ?: return
        val privateMode = runBlocking { localPrefs.reminderSettingsNow().privateMode }
        Notifier.post(this, message.messageId.hashCode(), title, body, privateMode, Notifier.CHANNEL_CELEBRATIONS)
    }
}

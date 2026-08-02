package mobile.dairy.app

import android.app.Application
import mobile.dairy.app.services.Notifier
import mobile.dairy.app.services.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BloomApp : Application() {
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
    }
}

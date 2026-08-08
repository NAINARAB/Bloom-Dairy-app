package mobile.dairy.app.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily reminders via AlarmManager (inexact — gentle nudges don't need
 * exact-alarm permissions). Cancel-all-then-reschedule keeps the schedule
 * perfectly in sync with settings; BootReceiver restores after reboot.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localPrefs: LocalPrefs,
) {
    private val alarm get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val copies = mapOf(
        "MORNING_PLANNING" to ("Plan your day \uD83C\uDF31" to "What is the one goal that matters most today?"),
        "MIDDAY_FOCUS" to ("Midday check \uD83C\uDFAF" to "How is your focus going? A short reset helps."),
        "EVENING_CHECKIN" to ("Evening reflection \uD83C\uDF19" to "Two quiet minutes for today's check-in."),
        "EXPENSE_REMINDER" to ("Money minute \uD83E\uDE99" to "Log today's spending and anything you saved or avoided."),
        "GOAL_PROGRESS_CHECKIN" to ("Goal Progress Update \uD83C\uDFAF" to "How much progress did you make on your main goal today?"),
        "GOAL_TIME_CHECKIN" to ("Goal Time Tracker \u23F1\uFE0F" to "You planned to spend time on your goal. Were you able to complete it?"),
        "GOAL_OBSTACLE_CHECKIN" to ("Overcoming Obstacles \uD83D\uDCAA" to "What stopped you from focusing today? Log your reflection."),
        "GOAL_TOMORROW_ACTION" to ("Small Steps Forward \uD83D\uDE80" to "What is one small action you can take tomorrow for your goal?"),
    )

    suspend fun rescheduleAll() {
        val settings = localPrefs.reminderSettingsNow()
        // Cancel every known slot first.
        copies.keys.forEachIndexed { index, key -> alarm.cancel(pending(index, key)) }
        if (!settings.enabled) return

        copies.keys.forEachIndexed { index, key ->
            if (settings.disabledCategories.contains(key)) return@forEachIndexed

            val minuteOfDay = settings.timeMinutes[key] ?: (settings.times[key]?.times(60)) ?: return@forEachIndexed
            val h = minuteOfDay / 60
            val m = minuteOfDay % 60
            
            if (inQuietHours(h, settings.quietStart, settings.quietEnd)) return@forEachIndexed
            
            val at = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            alarm.setInexactRepeating(
                AlarmManager.RTC_WAKEUP, at.timeInMillis, AlarmManager.INTERVAL_DAY, pending(index, key)
            )
        }
    }

    private fun pending(requestCode: Int, category: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra("category", category)
        return PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun inQuietHours(hour: Int, start: Int?, end: Int?): Boolean {
        if (start == null || end == null) return false
        return if (start <= end) hour in start until end else hour >= start || hour < end
    }

    fun copyFor(category: String): Pair<String, String>? = copies[category]
}

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var localPrefs: LocalPrefs
    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val category = intent.getStringExtra("category") ?: return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = localPrefs.reminderSettingsNow()
                if (settings.enabled) {
                    scheduler.copyFor(category)?.let { (title, body) ->
                        Notifier.post(context, category.hashCode(), title, body, settings.privateMode)
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { scheduler.rescheduleAll() } finally { result.finish() }
        }
    }
}

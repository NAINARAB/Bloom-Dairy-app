package mobile.dairy.app.services

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import mobile.dairy.app.core.Constants
import mobile.dairy.app.core.Dates
import mobile.dairy.app.data.InsightRepository
import mobile.dairy.app.domain.AppUsage
import mobile.dairy.app.domain.ScreenTimeDay
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Screen time via UsageStatsManager — what needed a custom Expo module in the
 * React Native version is plain platform code here.
 *
 * Requires the special "Usage access" toggle (PACKAGE_USAGE_STATS); it cannot
 * be requested with a runtime dialog, so [openUsageAccessSettings] deep-links
 * to the system switch. The whole feature stays opt-in and revocable.
 */
@Singleton
class ScreenTimeService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val insightRepository: InsightRepository,
) {

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings() {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Reads today's usage and upserts users/{uid}/screenTime/{today}. */
    suspend fun syncToday(): SyncResult {
        if (!hasPermission()) return SyncResult.NO_PERMISSION

        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = System.currentTimeMillis()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager

        var totalMs = 0L
        val apps = ArrayList<AppUsage>()
        for ((pkg, stat) in usm.queryAndAggregateUsageStats(midnight, now)) {
            val fg = stat.totalTimeInForeground
            if (fg < 60_000L) continue // ignore < 1 minute
            totalMs += fg
            val label = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
            apps.add(
                AppUsage(
                    name = label,
                    packageName = pkg,
                    minutes = fg / 60_000.0,
                    kind = Constants.classifyApp(pkg),
                )
            )
        }

        var unlocks = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val events = usm.queryEvents(midnight, now)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) unlocks += 1
            }
        }

        insightRepository.saveScreenTime(
            ScreenTimeDay(
                date = Dates.todayKey(),
                totalMinutes = totalMs / 60_000.0,
                unlocks = unlocks,
                apps = apps.sortedByDescending { it.minutes }.take(15),
                source = "android-usage-stats",
            )
        )
        return SyncResult.SYNCED
    }

    enum class SyncResult { SYNCED, NO_PERMISSION }
}

package mobile.dairy.app.core

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import mobile.dairy.app.domain.AppUsage
import mobile.dairy.app.domain.ScreenTimeDay
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object UsageStatsHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsageSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Fetches the last 7 days of ScreenTimeDay using Android's UsageStatsManager.
     */
    fun getLast7DaysUsage(context: Context): List<ScreenTimeDay> {
        if (!hasUsageStatsPermission(context)) return emptyList()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        
        val today = LocalDate.now()
        val result = mutableListOf<ScreenTimeDay>()

        // Keywords to guess if an app is distracting
        val distractingKeywords = listOf("instagram", "tiktok", "facebook", "twitter", "x", "snapchat", "reddit", "youtube", "clash", "game", "reel")
        val productiveKeywords = listOf("dairy", "notion", "docs", "drive", "email", "gmail", "calendar", "asana", "trello", "teams", "slack", "zoom")

        // Pre-fetch system launcher packages to filter them out
        val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val launcherPackages = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }

        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            val startMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            // End of day is start of next day
            val endMillis = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val events = usm.queryEvents(startMillis, endMillis)
            val event = android.app.usage.UsageEvents.Event()
            
            val appUsageMap = mutableMapOf<String, Long>()
            val appStartTimes = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName
                val timestamp = event.timeStamp
                
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    appStartTimes[pkg] = timestamp
                } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED || 
                           event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED) {
                    appStartTimes[pkg]?.let { startTime ->
                        val timeSpent = timestamp - startTime
                        if (timeSpent > 0) {
                            appUsageMap[pkg] = appUsageMap.getOrDefault(pkg, 0L) + timeSpent
                        }
                        appStartTimes.remove(pkg)
                    }
                }
            }
            
            val currentMillis = System.currentTimeMillis()
            val actualEndMillis = minOf(endMillis, currentMillis)
            appStartTimes.forEach { (pkg, startTime) ->
                val timeSpent = actualEndMillis - startTime
                if (timeSpent > 0) {
                    appUsageMap[pkg] = appUsageMap.getOrDefault(pkg, 0L) + timeSpent
                }
            }
            
            var totalMins = 0.0
            val appUsages = mutableListOf<AppUsage>()

            appUsageMap.forEach { (pkg, timeInForeground) ->
                if (timeInForeground > 0) {
                    // Filter out system launchers and the app itself (optional, but let's keep the app itself so they see it)
                    if (!launcherPackages.contains(pkg) && !pkg.contains("com.android.systemui") && !pkg.contains("launcher")) {
                        val minutes = timeInForeground / 1000.0 / 60.0
                        
                        if (minutes >= 1.0) { // Only track apps used for at least 1 minute
                            val appName = try {
                                val info = pm.getApplicationInfo(pkg, 0)
                                pm.getApplicationLabel(info).toString()
                            } catch (e: Exception) {
                                pkg // fallback to package name
                            }
                            
                            val nameLower = appName.lowercase()
                            val kind = when {
                                distractingKeywords.any { nameLower.contains(it) } -> "distracting"
                                productiveKeywords.any { nameLower.contains(it) } -> "productive"
                                else -> "neutral"
                            }

                            appUsages.add(AppUsage(name = appName, packageName = pkg, minutes = minutes, kind = kind))
                            totalMins += minutes
                        }
                    }
                }
            }

            result.add(
                ScreenTimeDay(
                    date = dateStr,
                    totalMinutes = totalMins,
                    apps = appUsages.sortedByDescending { it.minutes }
                )
            )
        }
        return result
    }
}

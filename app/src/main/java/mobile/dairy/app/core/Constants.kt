package mobile.dairy.app.core

/** App vocabulary — pure Kotlin, no Android imports, shared with unit tests. */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector

enum class Mood(val label: String, val emoji: String, val icon: ImageVector, val valence: Double, val colorHex: Long) {
    HAPPY("Happy", "\uD83D\uDE0A", Icons.Default.SentimentVerySatisfied, 1.0, 0xFFE8B34B),
    CALM("Calm", "\uD83D\uDE0C", Icons.Default.SelfImprovement, 0.7, 0xFF5FB4A2),
    MOTIVATED("Motivated", "\uD83D\uDD25", Icons.Default.Whatshot, 0.9, 0xFFE0784A),
    EXCITED("Excited", "\uD83E\uDD29", Icons.Default.Celebration, 1.0, 0xFFD96AA0),
    GRATEFUL("Grateful", "\uD83D\uDE4F", Icons.Default.VolunteerActivism, 0.9, 0xFF8C9F5F),
    CONFIDENT("Confident", "\uD83D\uDCAA", Icons.Default.FitnessCenter, 0.8, 0xFF5A8FD8),
    SAD("Sad", "\uD83D\uDE14", Icons.Default.SentimentDissatisfied, -0.8, 0xFF7A8BB1),
    ANGRY("Angry", "\uD83D\uDE20", Icons.Default.SentimentVeryDissatisfied, -0.9, 0xFFD45B50),
    STRESSED("Stressed", "\uD83D\uDE23", Icons.Default.MoodBad, -0.8, 0xFFB96AC9),
    TIRED("Tired", "\uD83E\uDD71", Icons.Default.Bedtime, -0.4, 0xFF9C8F7F),
    CONFUSED("Confused", "\uD83D\uDE15", Icons.Default.HelpOutline, -0.4, 0xFF8A8FA3),
    LONELY("Lonely", "\uD83E\uDEE5", Icons.Default.PersonOff, -0.8, 0xFF6E7BA8);

    val key: String get() = name.lowercase()

    companion object {
        fun fromKey(key: String): Mood? = entries.firstOrNull { it.key == key }
        fun valenceOf(keys: List<String>): Double {
            val moods = keys.mapNotNull(::fromKey)
            if (moods.isEmpty()) return 0.0
            return moods.sumOf { it.valence } / moods.size
        }
    }
}

enum class NotificationCategory(val label: String, val description: String, val defaultHour: Int?) {
    MORNING_PLANNING("Morning planning", "Plan your main goal for the day", 8),
    MIDDAY_FOCUS("Midday focus nudge", "A gentle check on your focus session", 13),
    EVENING_CHECKIN("Evening reflection", "Your two-minute daily check-in", 21),
    EXPENSE_REMINDER("Expense entry", "Log today's spending and savings", 20),
    GOAL_UPDATES("Goal progress", "Milestone updates and progress questions", null),
    SCREEN_TIME_ALERTS("Screen-time alerts", "When screen time rises above your usual", null),
    STREAK_CELEBRATIONS("Streak celebrations", "Celebrate consistency milestones", null),
    WEEKLY_SUMMARY("Weekly summary", "Your week in review, every Monday", null),
    MOTIVATION("Motivation", "Occasional supportive messages", null),
}

object Constants {
    const val PRIVATE_NOTIFICATION_BODY = "Your daily reflection is ready."

    val GOAL_MILESTONES = listOf(25, 50, 75, 100)
    val STREAK_CELEBRATION_DAYS = listOf(3, 5, 7, 10, 14, 21, 30, 50, 75, 100)

    val CAUSE_SUGGESTIONS = listOf(
        "Family", "Friends", "Work", "Health", "Money", "Sleep", "Traffic", "Weather", "My own thoughts",
    )
    val DECISION_SUGGESTIONS = listOf(
        "Woke up early", "Skipped junk food", "Said no politely", "Took a walk", "Avoided an impulse buy", "Asked for help",
    )
    val IMPROVEMENT_SUGGESTIONS = listOf(
        "Sleep earlier", "Less scrolling", "Start the hard task first", "Drink more water", "Be more patient",
    )
    val GRATITUDE_SUGGESTIONS = listOf(
        "My family", "My health", "A good meal", "A kind friend", "Quiet time", "My work",
    )

    private val DISTRACTING = listOf(
        "instagram", "facebook", "twitter", "tiktok", "snapchat", "youtube", "netflix", "hotstar", "primevideo", "reddit",
    )
    private val PRODUCTIVE = listOf(
        "notion", "docs", "sheets", "slack", "gmail", "outlook", "code", "github", "udemy", "coursera", "duolingo", "kindle", "calendar",
    )

    fun classifyApp(packageName: String): String {
        val p = packageName.lowercase()
        return when {
            DISTRACTING.any { p.contains(it) } -> "distracting"
            PRODUCTIVE.any { p.contains(it) } -> "productive"
            else -> "neutral"
        }
    }

    const val WELLBEING_DISCLAIMER =
        "Bloom is a self-reflection companion, not a medical or mental-health service. " +
            "If you are going through a difficult time, please reach out to a professional or someone you trust."
}

package mobile.dairy.app.domain

import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Firestore document models. Every field has a default so the Firestore SDK
 * can construct them reflectively (no-arg constructor requirement).
 *
 * The schema is identical to the one in firestore.rules / other Bloom clients:
 * users/{uid}/entries|dailyRatings|goals(+updates)|goalUpdateLog|expenses|
 * savings|screenTime|insights|prefs|devices.
 */

@Serializable
data class CategoryDef(
    var key: String = "",
    var label: String = "",
    var emoji: String = ""
)

@Serializable
data class JournalQuestionDef(
    var id: String = "",
    var title: String = "",
    var subtitle: String? = null,
    var type: String = "text", // text, emoji, person, rating_group, date, dropdown, number, toggle, slider
    var isActive: Boolean = true,
    var isMandatory: Boolean = false,
    var isCustom: Boolean = false,
    var options: List<String> = emptyList(), // For dropdowns
    var maxChars: Int? = null
)

@Serializable
data class PersonRef(
    var name: String = "",
    /** a mood key, or "stronger" */
    var feeling: String = "happy",
)

@Entity(tableName = "local_users")
data class LocalUser(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val phone: String = "",
    val passwordHash: String = "",
    val firebaseUid: String? = null,
    val createdAt: Long = 0,
)

@Serializable
@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey
    var id: String = "",
    var date: String = "",
    var createdAt: Long = 0,
    var updatedAt: Long = 0,
    var moods: List<String> = emptyList(),
    var moodCauses: List<String> = emptyList(),
    var bestPart: String? = null,
    var hardestPart: String? = null,
    var goodDecision: String? = null,
    var improvement: String? = null,
    var goodThings: String? = null,
    var mistakes: String? = null,
    var lessons: String? = null,
    var dayReason: String? = null,
    var gratitude: List<String> = emptyList(),
    var people: List<PersonRef> = emptyList(),
    var tags: List<String> = emptyList(),
    var note: String? = null,
    var favorite: Boolean = false,
    var important: Boolean = false,
    var customAnswers: Map<String, String> = emptyMap(),
    var isSynced: Boolean = false,
    var alterId: Int = 1,
    var userId: String = "",
)

@Entity(tableName = "daily_ratings")
data class DailyRating(
    @PrimaryKey
    var date: String = "",
    var overall: Int? = null,
    var happiness: Int? = null,
    var energy: Int? = null,
    var productivity: Int? = null,
    var focus: Int? = null,
    var discipline: Int? = null,
    var goalEffort: Int? = null,
    var stress: Int? = null,
    var sleep: Int? = null,
    var financialDiscipline: Int? = null,
    var updatedAt: Long = 0,
    var isSynced: Boolean = false,
    var alterId: Int = 1,
    var userId: String = "",
)

data class Milestone(
    var id: String = "",
    var title: String = "",
    var done: Boolean = false,
    var dueDate: String? = null,
)

data class GoalTask(
    var id: String = "",
    var title: String = "",
    var done: Boolean = false,
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey
    var id: String = "",
    var title: String = "",
    var description: String? = null,
    var type: String = "short",          // short | long
    var priority: String = "medium",     // low | medium | high
    var deadline: String? = null,
    var status: String = "active",       // active | paused | completed | archived
    var progress: Int = 0,               // 0..100
    var milestones: List<Milestone> = emptyList(),
    var dailyTasks: List<GoalTask> = emptyList(),
    var plannedMinutesPerDay: Int? = null,
    var createdAt: Long = 0,
    var updatedAt: Long = 0,
    var isSynced: Boolean = false,
    var alterId: Int = 1,
    var userId: String = "",
)

@Entity(tableName = "goal_updates")
data class GoalUpdate(
    @PrimaryKey
    var id: String = "",
    var goalId: String = "",
    var date: String = "",
    var progress: Int = 0,
    var status: String? = null,
    var minutes: Int = 0,
    var effort: Int? = null,
    var focus: Int? = null,
    var obstacles: String? = null,
    var achievement: String? = null,
    var lesson: String? = null,
    var note: String? = null,
    var createdAt: Long = 0,
    var isSynced: Boolean = false,
    var alterId: Int = 1,
    var userId: String = "",
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey
    var id: String = "",
    var date: String = "",
    var amount: Double = 0.0,
    var category: String = "other",
    var necessity: String = "necessary", // necessary | unnecessary
    var note: String? = null,
    var createdAt: Long = 0,
    var isSynced: Boolean = false,
    var alterId: Int = 1,
    var userId: String = "",
)

@Entity(tableName = "savings")
data class Saving(
    @PrimaryKey
    var id: String = "",
    var date: String = "",
    var amount: Double = 0.0,
    var category: String = "other",
    var kind: String = "earned",         // earned | avoided
    var note: String? = null,
    var createdAt: Long = 0,
    var isSynced: Boolean = false,
    var alterId: Int = 1,
    var userId: String = "",
)

data class AppUsage(
    var name: String = "",
    var packageName: String? = null,
    var minutes: Double = 0.0,
    var kind: String = "neutral",        // productive | neutral | distracting
)

data class ScreenTimeDay(
    var date: String = "",
    var totalMinutes: Double = 0.0,
    var unlocks: Int? = null,
    var apps: List<AppUsage> = emptyList(),
    var source: String = "android-usage-stats",
    var updatedAt: Long = 0,
)

@Entity(tableName = "insights")
data class Insight(
    @PrimaryKey
    var id: String = "",
    var date: String = "",
    var type: String = "",
    var message: String = "",
    var emoji: String = "",
    var icon: String? = null,
    var priority: Int = 0,
    var seen: Boolean = false,
    var createdAt: Long = 0,
    var isSynced: Boolean = false,
    var alterId: Int = 1,
    var userId: String = "",
)

@Entity(tableName = "app_prefs")
data class AppPrefs(
    @PrimaryKey
    var id: String = "app_prefs", // Single row pattern
    var theme: String = "system",        // system | light | dark
    var accent: String = "violet",       // violet | teal | rose | amber | sky
    var cardGradient: String = "midnight", // midnight | sunset | ocean | aurora | forest | berry | ember | royal
    var fontSize: Int = 14,              // 12..18 px/sp
    var boldText: Boolean = false,
    var currency: String = "INR",
    var dailyBudget: Double? = null,
    var monthlyBudget: Double = 30000.0,
    var lockEnabled: Boolean = false,
    var privateNotifications: Boolean = false,
    var enableMotivationalNotifications: Boolean = true,
    var screenTimeEnabled: Boolean = false,
    var quoteIndices: Map<String, Int> = emptyMap(),
    var transactionCategories: List<CategoryDef> = listOf(
        CategoryDef("food", "Food & dining", "🍱"),
        CategoryDef("food-delivery", "Food delivery", "🛵"),
        CategoryDef("groceries", "Groceries", "🛒"),
        CategoryDef("transport", "Transport", "🚌"),
        CategoryDef("shopping", "Shopping", "🛍️"),
        CategoryDef("bills", "Bills & recharge", "🧾"),
        CategoryDef("entertainment", "Entertainment", "🎬"),
        CategoryDef("health", "Health", "🩻"),
        CategoryDef("education", "Education", "📚"),
        CategoryDef("family", "Family & gifts", "🎁"),
        CategoryDef("other", "Other", "💠")
    ),
    var journalQuestions: List<JournalQuestionDef> = listOf(
        // Mandatory (6)
        JournalQuestionDef("q_feeling", "How are you feeling?", "Pick everything that fits — days are rarely one thing.", "emoji", isMandatory = true, isActive = true),
        JournalQuestionDef("q_feeling_reason", "What made you feel this way?", "Situations, habits — tap or add your own.", "text", isMandatory = true, isActive = true),
        JournalQuestionDef("q_people", "Who made your day?", "People who made you feel stronger, happy, or even stressed.", "person", isMandatory = true, isActive = true),
        JournalQuestionDef("q_lessons", "Lessons learned?", "What did today teach you?", "text", isMandatory = true, isActive = true),
        JournalQuestionDef("q_rating_performance", "Performance", null, "rating_group", isMandatory = true, isActive = true),
        JournalQuestionDef("q_rating_wellbeing", "Wellbeing", null, "rating_group", isMandatory = true, isActive = true),
        JournalQuestionDef("q_anything_else", "Anything else for today?", "A free note, a memory, a thought for future-you.", "text", isMandatory = true, isActive = true),
        
        // Optional (7)
        JournalQuestionDef("q_best_part", "Best part of your day?", null, "text", isMandatory = false, isActive = false),
        JournalQuestionDef("q_hardest_part", "And the most difficult part?", "Naming it is often half the weight.", "text", isMandatory = false, isActive = false),
        JournalQuestionDef("q_good_things", "Good things today?", "Successes, kindnesses, or just things that went well.", "text", isMandatory = false, isActive = false),
        JournalQuestionDef("q_mistakes", "Any mistakes or bad choices?", "Honesty with yourself is the first step to growth.", "text", isMandatory = false, isActive = false),
        JournalQuestionDef("q_gratitude", "What are you grateful for?", "Small things count.", "text", isMandatory = false, isActive = false),
        JournalQuestionDef("q_good_decision", "One good decision you made?", null, "text", isMandatory = false, isActive = false),
        JournalQuestionDef("q_improve_tomorrow", "What would you improve tomorrow?", "Small and realistic beats grand and forgotten.", "text", isMandatory = false, isActive = false)
    ),
    var isSynced: Boolean = false,
    var alterId: Int = 1,
    var userId: String = "",
)

data class DeviceToken(
    var token: String = "",
    var platform: String = "android",
    var updatedAt: Long = 0,
)

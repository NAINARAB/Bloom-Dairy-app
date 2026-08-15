package mobile.dairy.app.domain

import mobile.dairy.app.core.Constants
import mobile.dairy.app.core.Dates
import mobile.dairy.app.core.Format
import mobile.dairy.app.core.Mood
import mobile.dairy.app.domain.Insight
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Personal motivation assistant — deterministic, rule-based, pure Kotlin.
 *
 * Design rules (from the product spec):
 *  - appreciate genuine effort, encourage after hard days
 *  - never judge, blame, or use harsh language
 *  - suggest small, realistic improvements
 *  - celebrate streaks and milestones
 *  - surface patterns (spending, screen time, mood influencers)
 *
 * Runs on-device (privacy) and is fully unit-testable.
 */

data class InsightContext(
    val today: String,
    val currency: String = "INR",
    val dailyBudget: Double? = null,
    val prefs: AppPrefs = AppPrefs(),
    val entries: List<JournalEntry> = emptyList(),
    val ratings: List<DailyRating> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val goalUpdates: List<GoalUpdate> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val savings: List<Saving> = emptyList(),
    val screenTime: List<ScreenTimeDay> = emptyList(),
    val quotes: Map<String, List<String>> = emptyMap(),
)

object InsightEngine {

    private fun getQuote(ctx: InsightContext, category: String, replacements: Map<String, String>, defaultText: String): String {
        val list = ctx.quotes[category]
        if (list.isNullOrEmpty()) return defaultText
        val index = ctx.prefs.quoteIndices[category] ?: 0
        var text = list[index % list.size]
        replacements.forEach { (k, v) -> text = text.replace("{$k}", v) }
        return text
    }

    /** Stable pseudo-random pick so the same day always shows the same variant. */
    fun <T> pickVariant(seed: String, variants: List<T>): T {
        var h = 0
        for (c in seed) h = (h * 31 + c.code)
        return variants[abs(h) % variants.size]
    }

    private fun mk(date: String, type: String, emoji: String, icon: String?, message: String, priority: Int) =
        Insight(id = "$date-$type", date = date, type = type, emoji = emoji, icon = icon, message = message, priority = priority, createdAt = System.currentTimeMillis())

    /** Milestones crossed by a progress change, e.g. 20 -> 60 crosses [25, 50]. */
    fun crossedMilestones(prev: Int, next: Int): List<Int> =
        Constants.GOAL_MILESTONES.filter { prev < it && next >= it }

    fun generateDailyInsights(ctx: InsightContext): List<Insight> {
        val all = effortRules(ctx) + goalMilestoneRule(ctx) + goalStreakRule(ctx) +
            moneyRules(ctx) + screenTimeRule(ctx) + moodPatternRule(ctx) + goalReminderRule(ctx)
        // Supportive, never overwhelming: max 4 per day, highest priority first.
        return all.sortedByDescending { it.priority }.take(4)
    }

    /* -------------------------------------------------------------- */
    /* Rules                                                            */
    /* -------------------------------------------------------------- */

    private fun effortRules(ctx: InsightContext): List<Insight> {
        val rating = ctx.ratings.firstOrNull { it.date == ctx.today } ?: return emptyList()
        val hardDay = (rating.overall ?: 10) <= 4 || (rating.stress ?: 0) >= 8
        val triedAnyway = (rating.goalEffort ?: 0) >= 6 ||
            ctx.goalUpdates.any { it.date == ctx.today && it.minutes > 0 }

        return when {
            hardDay && triedAnyway -> listOf(mk(ctx.today, "effort_hard_day", "\uD83C\uDF31", "Eco", getQuote(ctx, "effort_hard_day", emptyMap(), "You made progress even though the day was difficult. That effort matters."), 90))
            hardDay -> listOf(mk(ctx.today, "encouragement", "\uD83C\uDF08", "SentimentSatisfied", pickVariant(ctx.today + "hard", listOf(
                "Today did not go as planned, but one bad day does not define your journey.",
                "Some days are for resting and resetting. Tomorrow is a fresh page.",
                "Be as kind to yourself tonight as you would be to a good friend.",
            )), 85))
            (rating.productivity ?: 10) <= 3 -> listOf(mk(ctx.today, "encouragement", "\uD83E\uDDE9", "Extension", pickVariant(ctx.today + "unprod", listOf(
                "An unproductive day is information, not a verdict. What is one small action for tomorrow?",
                "Slow days happen to everyone. Pick one tiny task for tomorrow morning and start there.",
            )), 70))
            (rating.overall ?: 0) >= 8 -> listOf(mk(ctx.today, "effort_good_day", "\u2728", "AutoAwesome", getQuote(ctx, "effort_good_day", emptyMap(), "A genuinely good day. Notice what made it work so you can repeat it."), 60))
            else -> emptyList()
        }
    }

    private fun goalStreakRule(ctx: InsightContext): List<Insight> {
        val workedDays = ctx.goalUpdates.filter { it.minutes > 0 || (it.effort ?: 0) >= 5 }.map { it.date }
        val streak = Streaks.current(workedDays, ctx.today)
        return if (streak >= 3) listOf(mk(ctx.today, "goals_streak", "\uD83D\uDD25", "Whatshot",
            getQuote(ctx, "goals_streak", mapOf("streak" to streak.toString()), "You have focused on your goal for $streak consecutive days. Keep going."), 80))
        else emptyList()
    }

    private fun goalMilestoneRule(ctx: InsightContext): List<Insight> {
        val out = ArrayList<Insight>()
        for (u in ctx.goalUpdates.filter { it.date == ctx.today }) {
            val goal = ctx.goals.firstOrNull { it.id == u.goalId } ?: continue
            val prev = ctx.goalUpdates
                .filter { it.goalId == u.goalId && it.createdAt < u.createdAt }
                .maxByOrNull { it.createdAt }?.progress ?: 0
            for (m in crossedMilestones(prev, u.progress)) {
                val icon = if (m == 100) "EmojiEvents" else "MilitaryTech"
                val msg = if (m == 100)
                    "\u201C${goal.title}\u201D is complete. Take a moment to appreciate how far you came."
                else
                    getQuote(ctx, "goals_milestone", mapOf("milestone" to m.toString(), "goal_title" to goal.title), "You reached $m% of \u201C${goal.title}\u201D. Steady steps, real progress.")
                out.add(mk(ctx.today, "goals_milestone", if (m == 100) "\uD83C\uDFC6" else "\uD83C\uDF96\uFE0F", icon, msg, 95))
            }
        }
        return out
    }

    private fun goalReminderRule(ctx: InsightContext): List<Insight> {
        for (g in ctx.goals.filter { it.status == "active" }) {
            val last = ctx.goalUpdates.filter { it.goalId == g.id }.maxByOrNull { it.date }
            val sinceKey = last?.date ?: Dates.key(java.time.Instant.ofEpochMilli(g.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
            val staleDays = Dates.daysBetween(sinceKey, ctx.today)
            if (staleDays >= 3) {
                return listOf(mk(ctx.today, "goals_reminder", "\uD83E\uDDED", "Explore", getQuote(ctx, "goals_reminder", mapOf("goal_title" to g.title, "stale_days" to staleDays.toString()), "\u201C${g.title}\u201D has been waiting quietly for $staleDays days. What is one small action you can take tomorrow?"), 55)) // one gentle reminder at a time, never a pile-on
            }
        }
        return emptyList()
    }

    private fun moneyRules(ctx: InsightContext): List<Insight> {
        val out = ArrayList<Insight>()
        val today = Finance.summarizeDay(ctx.today, ctx.expenses, ctx.savings)

        if (today.avoided > 0) {
            out.add(mk(ctx.today, "money_avoided", "\uD83D\uDEE1\uFE0F", "Security",
                getQuote(ctx, "money_avoided", mapOf("amount" to Format.money(today.avoided, ctx.currency)), "You avoided ${Format.money(today.avoided, ctx.currency)} in unnecessary spending today."), 75))
        }

        val spike = Finance.weekOverWeekByCategory(ctx.today, ctx.expenses)
            .firstOrNull { it.changePct >= 30 && it.current >= 100 }
        if (spike != null) {
            val label = ctx.prefs.transactionCategories.find { it.key == spike.category }?.label?.lowercase() ?: "that category"
            out.add(mk(ctx.today, "money_trend", "\uD83D\uDCC8", "TrendingUp",
                getQuote(ctx, "money_trend", mapOf("category" to label), "Your $label spending is higher than last week. Worth a quick look \u2014 no judgement."), 50))
        }

        val budget = ctx.dailyBudget
        if (budget != null && budget > 0) {
            val streak = Finance.budgetStreak(Finance.dailySpendMap(ctx.expenses), budget, ctx.today)
            if (streak >= 3) {
                out.add(mk(ctx.today, "money_streak", "\uD83C\uDFAF", "FilterCenterFocus",
                    getQuote(ctx, "money_streak", mapOf("streak" to streak.toString()), "You stayed within your daily budget for $streak days. That is real financial discipline."), 65))
            }
        }
        return out
    }

    private fun screenTimeRule(ctx: InsightContext): List<Insight> {
        val todayRec = ctx.screenTime.firstOrNull { it.date == ctx.today } ?: return emptyList()
        val weekAgo = Dates.addDays(ctx.today, -7)
        val prior = ctx.screenTime.filter { it.date != ctx.today && it.date >= weekAgo }
        if (prior.size < 3) return emptyList()
        val avg = prior.sumOf { it.totalMinutes } / prior.size
        return if (todayRec.totalMinutes > avg * 1.25 && todayRec.totalMinutes - avg >= 30) {
            listOf(mk(ctx.today, "screen_time_high", "\uD83D\uDCF5", "PhonelinkOff",
                getQuote(ctx, "screen_time_high", mapOf("today_mins" to Format.minutes(todayRec.totalMinutes), "avg_mins" to Format.minutes(avg)), "Your screen time increased today (${Format.minutes(todayRec.totalMinutes)} vs your usual ${Format.minutes(avg)}). Consider keeping your phone away during your next focus session."), 60))
        } else emptyList()
    }

    private fun moodPatternRule(ctx: InsightContext): List<Insight> {
        val (positive, negative) = moodInfluencers(ctx.entries)
        positive.firstOrNull()?.let { top ->
            if (top.count >= 3) return listOf(mk(ctx.today, "mood_pattern_positive", "\uD83D\uDCA1", "Lightbulb",
                getQuote(ctx, "mood_pattern_positive", mapOf("influencer" to top.name), "\u201C${top.name}\u201D keeps showing up on your good days. More of that, when you can."), 45))
        }
        negative.firstOrNull()?.let { low ->
            if (low.count >= 3) return listOf(mk(ctx.today, "mood_pattern_negative", "\uD83E\uDDF5", "Link",
                getQuote(ctx, "mood_pattern_negative", mapOf("influencer" to low.name), "\u201C${low.name}\u201D often appears on heavier days. Noticing the pattern is the first step \u2014 you decide what to do with it."), 45))
        }
        return emptyList()
    }

    fun checkinCompleteMessage(ctx: InsightContext): String {
        val rating = ctx.ratings.firstOrNull { it.date == ctx.today }
        val hardDay = (rating?.overall ?: 10) <= 4 || (rating?.stress ?: 0) >= 8
        val triedAnyway = (rating?.goalEffort ?: 0) >= 6 ||
            ctx.goalUpdates.any { it.date == ctx.today && it.minutes > 0 }

        if (hardDay && triedAnyway) {
            return "You made progress even though the day was difficult. That effort matters."
        }

        val entries = ctx.entries.sortedByDescending { it.date }
        if (entries.isEmpty()) return "Welcome to Bloom! Your first entry is saved."
        val streak = Streaks.current(entries.map { it.date }, ctx.today)
        return if (streak >= 3) "Check-in complete \u2014 that is $streak days in a row. Consistency like this is how change happens."
        else "Check-in complete. Every entry is a step towards knowing yourself better."
    }

    fun moodInfluencers(entries: List<JournalEntry>): Pair<List<Influencer>, List<Influencer>> {
        val allInfluencers = (entries.flatMap { it.moodCauses } + entries.flatMap { it.people.map { it.name } }).distinct()
        val pos = mutableListOf<Influencer>()
        val neg = mutableListOf<Influencer>()

        for (name in allInfluencers) {
            val related = entries.filter { it.moodCauses.contains(name) || it.people.any { p -> p.name == name } }
            // Map special feelings like "stronger" to high valence for analysis
            val personMoods = related.flatMap { e ->
                e.people.filter { it.name == name }.map {
                    if (it.feeling == "stronger") "motivated" else it.feeling
                }
            }
            // If it's a person with tagged feelings, those feelings are the primary signal.
            val moodsToAnalyze = if (personMoods.isNotEmpty()) personMoods else related.flatMap { it.moods }
            val valence = Mood.valenceOf(moodsToAnalyze)
            if (valence >= 0.4) pos.add(Influencer(name, related.size))
            if (valence <= -0.4) neg.add(Influencer(name, related.size))
        }
        return pos.sortedByDescending { it.count } to neg.sortedByDescending { it.count }
    }

    data class Influencer(val name: String, val count: Int)

    fun roundPct(v: Double): Int = v.roundToInt()

    fun generateWeeklySummary(ctx: InsightContext, from: String, to: String): WeeklySummary {
        val weekEntries = ctx.entries.filter { it.date in from..to }
        val weekRatings = ctx.ratings.filter { it.date in from..to }
        val weekUpdates = ctx.goalUpdates.filter { it.date in from..to }
        val weekExpenses = ctx.expenses.filter { it.date in from..to }

        val checkins = weekEntries.size
        val avgOverall = if (weekRatings.isNotEmpty()) {
            val valid = weekRatings.mapNotNull { it.overall }
            if (valid.isNotEmpty()) valid.average() else null
        } else null
        val goalMinutes = weekUpdates.sumOf { it.minutes }
        val spent = weekExpenses.sumOf { it.amount }

        val bestDay = weekRatings.maxByOrNull { it.overall ?: 0 }?.date
        val hardestDay = weekRatings.minByOrNull { it.overall ?: 10 }?.date

        val message = when {
            checkins >= 5 && (avgOverall ?: 0.0) >= 7.0 -> "A strong, positive week. You're building great momentum."
            checkins >= 5 -> "Consistent effort this week. You're showing up for yourself."
            checkins > 0 -> "You've made some progress. Let's aim for more consistency next week."
            else -> "A quiet week. Tomorrow is a fresh start."
        }

        return WeeklySummary(checkins, avgOverall, goalMinutes, spent, bestDay, hardestDay, message)
    }
}

data class WeeklySummary(
    val checkins: Int,
    val avgOverall: Double?,
    val goalMinutes: Int,
    val spent: Double,
    val bestDay: String?,
    val hardestDay: String?,
    val message: String,
)

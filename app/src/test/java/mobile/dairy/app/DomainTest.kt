package mobile.dairy.app

import mobile.dairy.app.domain.DailyRating
import mobile.dairy.app.domain.Expense
import mobile.dairy.app.domain.Finance
import mobile.dairy.app.domain.GoalUpdate
import mobile.dairy.app.domain.InsightContext
import mobile.dairy.app.domain.InsightEngine
import mobile.dairy.app.domain.JournalEntry
import mobile.dairy.app.domain.PersonRef
import mobile.dairy.app.domain.Saving
import mobile.dairy.app.domain.ScreenTimeDay
import mobile.dairy.app.domain.Streaks
import mobile.dairy.app.core.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val TODAY = "2026-07-10"

class DomainTest {

    /* ---------------- streaks ---------------- */

    @Test fun `streak counts consecutive days ending today`() {
        assertEquals(3, Streaks.current(listOf("2026-07-08", "2026-07-09", "2026-07-10"), TODAY))
    }

    @Test fun `streak survives an unlogged today`() {
        assertEquals(3, Streaks.current(listOf("2026-07-07", "2026-07-08", "2026-07-09"), TODAY))
    }

    @Test fun `streak breaks on a gap and crosses months`() {
        assertEquals(2, Streaks.current(listOf("2026-07-05", "2026-07-09", "2026-07-10"), TODAY))
        assertEquals(3, Streaks.current(listOf("2026-06-29", "2026-06-30", "2026-07-01"), "2026-07-01"))
    }

    @Test fun `best streak finds the longest run`() {
        assertEquals(3, Streaks.best(listOf("2026-07-01", "2026-07-02", "2026-07-05", "2026-07-06", "2026-07-07")))
    }

    /* ---------------- finance ---------------- */

    private fun exp(date: String, amount: Double, category: String = "food", necessity: String = "necessary") =
        Expense(id = "$date-$amount", date = date, amount = amount, category = category, necessity = necessity)

    private fun sav(date: String, amount: Double, kind: String) =
        Saving(id = "$date-$kind", date = date, amount = amount, kind = kind)

    @Test fun `summarizeDay splits buckets`() {
        val s = Finance.summarizeDay(
            TODAY,
            listOf(exp(TODAY, 200.0), exp(TODAY, 150.0, "shopping", "unnecessary"), exp("2026-07-09", 999.0)),
            listOf(sav(TODAY, 300.0, "avoided"), sav(TODAY, 500.0, "saved")),
        )
        assertEquals(350.0, s.spent, 0.001)
        assertEquals(200.0, s.necessary, 0.001)
        assertEquals(150.0, s.unnecessary, 0.001)
        assertEquals(500.0, s.earned, 0.001)
        assertEquals(300.0, s.avoided, 0.001)
    }

    @Test fun `week over week flags a category spike`() {
        val expenses = listOf(
            exp("2026-07-07", 400.0, "food-delivery"),
            exp("2026-07-09", 300.0, "food-delivery"),
            exp("2026-06-30", 250.0, "food-delivery"),
            exp("2026-07-02", 150.0, "food-delivery"),
        )
        val fd = Finance.weekOverWeekByCategory(TODAY, expenses).first { it.category == "food-delivery" }
        assertEquals(700.0, fd.current, 0.001)
        assertEquals(400.0, fd.previous, 0.001)
        assertEquals(75, InsightEngine.roundPct(fd.changePct))
    }

    @Test fun `budget streak counts within-budget days and ignores unlogged today`() {
        val m = Finance.dailySpendMap(listOf(
            exp("2026-07-07", 400.0), exp("2026-07-08", 500.0), exp("2026-07-09", 450.0), exp(TODAY, 300.0),
        ))
        assertEquals(4, Finance.budgetStreak(m, 500.0, TODAY))
        assertEquals(1, Finance.budgetStreak(m, 449.0, TODAY))
        val m2 = Finance.dailySpendMap(listOf(exp("2026-07-08", 100.0), exp("2026-07-09", 100.0)))
        assertEquals(2, Finance.budgetStreak(m2, 500.0, TODAY))
    }

    @Test fun `INR grouping uses lakh style`() {
        assertEquals("\u20B912,34,567", Format.money(1234567.0, "INR"))
        assertEquals("\u20B9300", Format.money(300.0, "INR"))
    }

    /* ---------------- insight engine ---------------- */

    private fun entry(date: String, moods: List<String>, people: List<PersonRef> = emptyList()) =
        JournalEntry(id = date, date = date, moods = moods, people = people)

    @Test fun `crossed milestones detects thresholds`() {
        assertEquals(listOf(25, 50), InsightEngine.crossedMilestones(20, 60))
        assertEquals(emptyList<Int>(), InsightEngine.crossedMilestones(50, 50))
        assertEquals(listOf(100), InsightEngine.crossedMilestones(99, 100))
    }

    @Test fun `praises effort on a difficult day`() {
        val ctx = InsightContext(today = TODAY, ratings = listOf(DailyRating(date = TODAY, overall = 3, goalEffort = 7)))
        assertTrue(InsightEngine.generateDailyInsights(ctx).any { it.type == "effort" })
    }

    @Test fun `goal streak uses the exact spec wording`() {
        val updates = listOf("2026-07-06", "2026-07-07", "2026-07-08", "2026-07-09", TODAY)
            .mapIndexed { i, d -> GoalUpdate(id = "u$i", goalId = "g1", date = d, progress = i * 10, minutes = 30, createdAt = i.toLong()) }
        val streak = InsightEngine.generateDailyInsights(InsightContext(today = TODAY, goalUpdates = updates))
            .first { it.type == "streak" }
        assertEquals("You have focused on your goal for 5 consecutive days. Keep going.", streak.message)
    }

    @Test fun `avoided spending message with INR`() {
        val ctx = InsightContext(today = TODAY, savings = listOf(sav(TODAY, 300.0, "avoided")))
        val msg = InsightEngine.generateDailyInsights(ctx).first { it.type == "money-avoided" }.message
        assertEquals("You avoided \u20B9300 in unnecessary spending today.", msg)
    }

    @Test fun `screen time alert needs a real baseline`() {
        fun st(date: String, min: Double) = ScreenTimeDay(date = date, totalMinutes = min)
        val thin = InsightContext(today = TODAY, screenTime = listOf(st("2026-07-08", 100.0), st("2026-07-09", 100.0), st(TODAY, 400.0)))
        assertFalse(InsightEngine.generateDailyInsights(thin).any { it.type == "screen-time" })
        val full = InsightContext(today = TODAY, screenTime = listOf(
            st("2026-07-07", 100.0), st("2026-07-08", 100.0), st("2026-07-09", 100.0), st(TODAY, 200.0)))
        assertTrue(InsightEngine.generateDailyInsights(full).any { it.type == "screen-time" })
    }

    @Test fun `output capped at four insights`() {
        val updates = listOf("2026-07-07", "2026-07-08", "2026-07-09", TODAY)
            .mapIndexed { i, d -> GoalUpdate(id = "u$i", goalId = "g1", date = d, progress = 25 * (i + 1), minutes = 30, createdAt = i.toLong()) }
        val ctx = InsightContext(
            today = TODAY,
            ratings = listOf(DailyRating(date = TODAY, overall = 3, goalEffort = 8)),
            savings = listOf(sav(TODAY, 300.0, "avoided")),
            goalUpdates = updates,
            goals = listOf(mobile.dairy.app.domain.Goal(id = "g1", title = "Ship Bloom", progress = 100)),
        )
        assertTrue(InsightEngine.generateDailyInsights(ctx).size <= 4)
    }

    @Test fun `resilience message when effort met a hard day`() {
        val ctx = InsightContext(
            today = TODAY,
            ratings = listOf(DailyRating(date = TODAY, overall = 3, goalEffort = 7)),
            entries = listOf(entry(TODAY, listOf("stressed"))),
        )
        assertEquals("You made progress even though the day was difficult. That effort matters.",
            InsightEngine.checkinCompleteMessage(ctx))
    }

    @Test fun `completion message mentions the streak`() {
        val ctx = InsightContext(today = TODAY, entries = listOf(
            entry("2026-07-07", listOf("calm")), entry("2026-07-08", listOf("calm")),
            entry("2026-07-09", listOf("happy")), entry(TODAY, listOf("happy"))))
        assertTrue(InsightEngine.checkinCompleteMessage(ctx).contains("4 days in a row"))
    }

    @Test fun `mood influencers rank people by tagged feelings`() {
        val entries = listOf(
            entry("2026-07-08", listOf("happy"), listOf(PersonRef("Amma", "happy"))),
            entry("2026-07-09", listOf("happy"), listOf(PersonRef("Amma", "stronger"))),
            entry(TODAY, listOf("calm"), listOf(PersonRef("Amma", "happy"), PersonRef("Traffic", "angry"))),
        )
        val (positive, negative) = InsightEngine.moodInfluencers(entries)
        assertEquals("Amma", positive.first().name)
        assertEquals(3, positive.first().count)
        assertEquals("Traffic", negative.first().name)
    }

    @Test fun `weekly summary aggregates the week`() {
        val ctx = InsightContext(
            today = TODAY,
            entries = listOf(entry("2026-07-06", listOf("happy")), entry("2026-07-07", listOf("tired"))),
            ratings = listOf(DailyRating(date = "2026-07-06", overall = 8), DailyRating(date = "2026-07-07", overall = 4)),
            goalUpdates = listOf(GoalUpdate(id = "u1", goalId = "g1", date = "2026-07-07", progress = 10, minutes = 90)),
            expenses = listOf(exp("2026-07-07", 500.0)),
        )
        val s = InsightEngine.generateWeeklySummary(ctx, "2026-07-06", "2026-07-12")
        assertEquals(2, s.checkins)
        assertEquals(6.0, s.avgOverall!!, 0.001)
        assertEquals(90, s.goalMinutes)
        assertEquals(500.0, s.spent, 0.001)
        assertEquals("2026-07-06", s.bestDay)
        assertEquals("2026-07-07", s.hardestDay)
        assertTrue(s.message.length > 10)
    }
}

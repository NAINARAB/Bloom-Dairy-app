package mobile.dairy.app.domain

import mobile.dairy.app.core.Dates

data class DayMoney(
    val date: String,
    val spent: Double,
    val necessary: Double,
    val unnecessary: Double,
    val earned: Double,
    val avoided: Double,
)

data class RangeTotals(
    val spent: Double,
    val necessary: Double,
    val unnecessary: Double,
    val earned: Double,
    val avoided: Double,
    /** avoided + earned: the money kept through deliberate decisions or earned */
    val keptThroughDecisions: Double,
)

data class CategoryTrend(
    val category: String,
    val current: Double,
    val previous: Double,
    val changePct: Double,
)

object Finance {

    fun summarizeDay(date: String, expenses: List<Expense>, savings: List<Saving>): DayMoney {
        var spent = 0.0; var necessary = 0.0; var unnecessary = 0.0; var earned = 0.0; var avoided = 0.0
        for (e in expenses) {
            if (e.date != date) continue
            spent += e.amount
            if (e.necessity == "necessary") necessary += e.amount else unnecessary += e.amount
        }
        for (s in savings) {
            if (s.date != date) continue
            // support legacy 'saved' by mapping to 'earned'
            if (s.kind == "earned" || s.kind == "saved") earned += s.amount else avoided += s.amount
        }
        return DayMoney(date, spent, necessary, unnecessary, earned, avoided)
    }

    fun totalsInRange(from: String, to: String, expenses: List<Expense>, savings: List<Saving>): RangeTotals {
        var spent = 0.0; var necessary = 0.0; var unnecessary = 0.0; var earned = 0.0; var avoided = 0.0
        for (e in expenses) {
            if (e.date < from || e.date > to) continue
            spent += e.amount
            if (e.necessity == "necessary") necessary += e.amount else unnecessary += e.amount
        }
        for (s in savings) {
            if (s.date < from || s.date > to) continue
            if (s.kind == "earned" || s.kind == "saved") earned += s.amount else avoided += s.amount
        }
        return RangeTotals(spent, necessary, unnecessary, earned, avoided, earned + avoided)
    }

    fun categoryTotals(from: String, to: String, expenses: List<Expense>): Map<String, Double> {
        val out = HashMap<String, Double>()
        for (e in expenses) {
            if (e.date < from || e.date > to) continue
            out[e.category] = (out[e.category] ?: 0.0) + e.amount
        }
        return out
    }

    /** This week (Mon..today) vs the same span of last week, per category. */
    fun weekOverWeekByCategory(today: String, expenses: List<Expense>, minAmount: Double = 50.0): List<CategoryTrend> {
        val curStart = Dates.startOfWeek(today)
        val prevStart = Dates.addDays(curStart, -7)
        val prevEnd = Dates.addDays(today, -7)
        val cur = categoryTotals(curStart, today, expenses)
        val prev = categoryTotals(prevStart, prevEnd, expenses)
        val cats = cur.keys + prev.keys
        return cats.mapNotNull { c ->
            val a = cur[c] ?: 0.0
            val b = prev[c] ?: 0.0
            if (a < minAmount && b < minAmount) return@mapNotNull null
            val changePct = if (b == 0.0) 100.0 else (a - b) / b * 100.0
            CategoryTrend(c, a, b, changePct)
        }.sortedByDescending { it.changePct }
    }

    fun dailySpendMap(expenses: List<Expense>): Map<String, Double> {
        val m = HashMap<String, Double>()
        for (e in expenses) m[e.date] = (m[e.date] ?: 0.0) + e.amount
        return m
    }

    /** Consecutive days (ending today or yesterday) within the daily budget. */
    fun budgetStreak(dailySpend: Map<String, Double>, budget: Double, today: String): Int {
        fun within(d: String) = (dailySpend[d] ?: 0.0) <= budget
        var cursor = if (dailySpend.containsKey(today) && within(today)) today else Dates.addDays(today, -1)
        var n = 0
        while (dailySpend.containsKey(cursor) && within(cursor)) {
            n += 1
            cursor = Dates.addDays(cursor, -1)
        }
        return n
    }
}

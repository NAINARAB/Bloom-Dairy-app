package mobile.dairy.app.domain

import mobile.dairy.app.core.Dates

object Streaks {

    /**
     * Consecutive-day streak ending today (or yesterday, so an unfinished
     * "today" doesn't zero the streak before the evening check-in).
     */
    fun current(days: Collection<String>, today: String): Int {
        val set = days.toHashSet()
        var cursor = if (set.contains(today)) today else Dates.addDays(today, -1)
        var n = 0
        while (set.contains(cursor)) {
            n += 1
            cursor = Dates.addDays(cursor, -1)
        }
        return n
    }

    fun best(days: Collection<String>): Int {
        val sorted = days.toSortedSet().toList()
        var best = 0
        var run = 0
        var prev: String? = null
        for (day in sorted) {
            run = if (prev != null && Dates.addDays(prev, 1) == day) run + 1 else 1
            if (run > best) best = run
            prev = day
        }
        return best
    }

    fun isMilestone(streak: Int, milestones: List<Int>): Boolean = milestones.contains(streak)
}

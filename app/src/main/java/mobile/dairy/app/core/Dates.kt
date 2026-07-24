package mobile.dairy.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * All persistence uses local-day keys "YYYY-MM-DD" — identical to the
 * Firestore documents written by other Bloom clients.
 */
object Dates {
    private val FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayKey(): String = LocalDate.now().format(FMT)

    fun key(date: LocalDate): String = date.format(FMT)

    fun parse(key: String): LocalDate = LocalDate.parse(key, FMT)

    fun addDays(key: String, n: Long): String = key(parse(key).plusDays(n))

    fun startOfWeek(key: String): String {
        val d = parse(key)
        return key(d.minusDays(((d.dayOfWeek.value + 6) % 7).toLong())) // Monday
    }

    fun startOfMonth(key: String): String = key.substring(0, 7) + "-01"

    fun daysBetween(a: String, b: String): Long = ChronoUnit.DAYS.between(parse(a), parse(b))

    fun friendly(key: String, today: String = todayKey()): String {
        if (key == today) return "Today"
        if (key == addDays(today, -1)) return "Yesterday"
        val d = parse(key)
        val dow = d.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        val month = d.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        return "$dow, ${d.dayOfMonth} $month"
    }

    fun monthLabel(key: String): String {
        val d = parse(key)
        val month = d.month.name.lowercase().replaceFirstChar { it.uppercase() }
        return "$month ${d.year}"
    }

    fun fromMillis(ms: Long): String {
        return key(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate())
    }

    fun greetingForHour(hour: Int): String = when {
        hour < 5 -> "Up late"
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Winding down"
    }
}

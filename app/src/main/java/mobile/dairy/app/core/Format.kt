package mobile.dairy.app.core

import kotlin.math.abs
import kotlin.math.roundToInt

object Format {

    /** Indian grouping (12,34,567) for INR; western grouping otherwise. */
    fun groupDigits(n: Double, indian: Boolean): String {
        val negative = n < 0
        val abs = abs(n)
        val hasDecimals = abs % 1.0 != 0.0
        val fixed = if (hasDecimals) String.format("%.2f", abs) else abs.toLong().toString()
        val parts = fixed.split(".")
        val intPart = parts[0]
        val grouped = if (indian) {
            if (intPart.length <= 3) intPart
            else {
                val last3 = intPart.takeLast(3)
                val rest = intPart.dropLast(3)
                val sb = StringBuilder()
                for ((i, c) in rest.withIndex()) {
                    sb.append(c)
                    val remaining = rest.length - i - 1
                    if (remaining > 0 && remaining % 2 == 0) sb.append(',')
                }
                "$sb,$last3"
            }
        } else {
            val sb = StringBuilder()
            for ((i, c) in intPart.withIndex()) {
                sb.append(c)
                val remaining = intPart.length - i - 1
                if (remaining > 0 && remaining % 3 == 0) sb.append(',')
            }
            sb.toString()
        }
        val sign = if (negative) "-" else ""
        return if (parts.size > 1) "$sign$grouped.${parts[1]}" else "$sign$grouped"
    }

    fun money(amount: Double, currency: String = "INR"): String {
        val symbol = when (currency) {
            "INR" -> "\u20B9"
            "USD" -> "$"
            "EUR" -> "\u20AC"
            "GBP" -> "\u00A3"
            "AED" -> "AED "
            "SGD" -> "S$"
            else -> ""
        }
        val rounded = (amount * 100).roundToInt() / 100.0
        return symbol + groupDigits(rounded, currency == "INR")
    }

    fun minutes(min: Double): String {
        val m = min.roundToInt()
        if (m < 60) return "${m}m"
        val h = m / 60
        val rem = m % 60
        return if (rem == 0) "${h}h" else "${h}h ${rem}m"
    }
}

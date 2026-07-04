package com.tracker.core

import java.util.Calendar

internal object QueryWindowCalculator {
    fun calculate(days: Int, nowMillis: Long): Pair<Long, Long> {
        require(days >= 1) { "days must be >= 1, was $days" }
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }
        return calendar.timeInMillis to nowMillis
    }
}

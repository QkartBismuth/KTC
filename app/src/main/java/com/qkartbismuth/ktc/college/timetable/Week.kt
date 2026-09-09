package com.qkartbismuth.ktc.college.timetable

import androidx.annotation.Keep

/**
 * {
 *    "days": [...],
 *    "week_number": 1
 * }
 */
@Keep
data class Week(
    val days: List<Day>,
    val week_number: Int
)

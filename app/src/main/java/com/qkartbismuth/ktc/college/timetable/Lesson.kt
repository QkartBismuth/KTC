package com.qkartbismuth.ktc.college.timetable

import androidx.annotation.Keep

/**
 * {
 *      "title": "Проектирование и дизайн информационных систем",
 *      "teacher": "Гринь Д.Х.",
 *      "classroom": "3.10",
 *      "actual_classroom": "3.12",
 *      "status": 0,
 *      "note": "Замена кабинета",
 *      "time": ["1", "8:15", "9:45"]
 * }
 */
@Keep
data class Lesson(
    val title: String,
    val teacher: String,
    val classroom: String,
    val time: LessonTime,
    /** 0 - normal, 1 - cancelled, 2 - rescheduled/changed */
    val status: Int = 0,
    /** true when the API marks the lesson as cancelled. */
    val cancelled: Boolean = false,
    /** Actual classroom after a reschedule, null when unchanged. */
    val actual_classroom: String? = null,
    /** Optional note explaining the change. */
    val note: String? = null
)
package com.qkartbismuth.ktc.college.teacher

import androidx.annotation.Keep

/**
 * @param number lesson number
 * @param title lesson title
 * @param classroom auditory
 * @param group group name
 */
@Keep
data class TeacherLesson(
    val number: String,
    val title: String,
    val classroom: String,
    val group: String,
    /** 0 - normal, 1 - cancelled, 2 - rescheduled/changed */
    val status: Int = 0,
    /** true when the API marks the lesson as cancelled. */
    val cancelled: Boolean = false,
    /** Actual classroom after a reschedule, null when unchanged. */
    val actual_classroom: String? = null,
    /** Optional note explaining the change. */
    val note: String? = null
)
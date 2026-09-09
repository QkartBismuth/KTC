package com.qkartbismuth.ktc.college.teacher

import androidx.annotation.Keep

/**
 * Keeps teachers list with them IDs and names.
 */
@Keep
data class TeachersList(
    val teachers: ArrayList<Teacher>
)

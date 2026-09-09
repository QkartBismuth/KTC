package com.qkartbismuth.ktc

import androidx.annotation.Keep

/**
 * Constants class keeps all app constants
 */
@Keep
sealed class Constants {
    companion object {
        const val PACKAGE = "com.qkartbismuth.ktc"

        const val TIMETABLE_STATE = "state"
        const val TIMETABLE_BRANCH = "branch"
        const val TIMETABLE_GROUP = "group"
        const val TIMETABLE_GROUP_TITLE = "group_title"
        const val TIMETABLE_WEEK = "week"
        const val TIMETABLE_IS_STUDENT = "is_student"
        const val TIMETABLE_TEACHER_ID = "teacher_id"

        // AppDynamicTheme
        const val CURRENT_THEME = "current_theme"
    }
}
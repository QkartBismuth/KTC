package com.qkartbismuth.ktc.college.timetable

import androidx.annotation.Keep

/**
 * {
 *      "course": 1,
 *      "groups": [
 *          {
 *              "id": 264,
 *              "title": "РП.09.21.1"
 *          }, ...
 *      ]
 * }
 */
@Keep
data class Course(
    val course: Int,
    val groups: Groups
)

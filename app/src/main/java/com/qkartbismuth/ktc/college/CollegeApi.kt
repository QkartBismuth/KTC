package com.qkartbismuth.ktc.college

import androidx.annotation.Keep
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Provides working with KTC api.
 */
@Keep
class CollegeApi {
    companion object {
        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        private const val MY_API = "http://mob.kansk-tc.ru/ktc-api"
        private const val BRANCHES = "$MY_API/branches"
        private const val COURSES = "$MY_API/courses"
        private const val TIMETABLE = "$MY_API/timetable"
        private const val TEACHER_TIMETABLE = "$MY_API/teacher-timetable"
        private const val TEACHERS_LIST = "$MY_API/teachers-list"

        /**
         * Sends GET request to url.
         */
        private fun sendRequest(url: String, callback: CollegeCallback) {
            val request: Request.Builder = Request.Builder()
                .get()
                .url(url)
            client.newCall(request.build()).enqueue(callback)
        }

        /**
         * Fetches all college branches
         */
        fun fetchBranches(callback: CollegeCallback) {
            sendRequest(BRANCHES, callback)
        }

        /**
         * Fetch all branch courses
         * @param branchId unique branch ID.
         */
        fun fetchCourses(branchId: Int, callback: CollegeCallback) {
            sendRequest("$COURSES/$branchId", callback)
        }

        /**
         * Fetches the timetable for specified group and week.
         * @param groupId course group ID.
         * @param week week number. by default fetches current week.
         */
        fun fetchTimetable(groupId: Int, callback: CollegeCallback, week: Int? = null) {
            if (week == null)
                sendRequest("$TIMETABLE/$groupId", callback)
            else
                sendRequest("$TIMETABLE/$groupId/$week", callback)
        }

        /**
         * Fetches the teachers list in branch.
         * @param branchId branch ID.
         */
        fun fetchTeachersList(branchId: Int, callback: CollegeCallback) {
            sendRequest("$TEACHERS_LIST/$branchId", callback)
        }

        /**
         * Fetches the teacher's timetable.
         * @param branchId branch ID.
         * @param teacherId unique teacher ID.
         */
        fun fetchTeacherTimetable(branchId: Int, teacherId: Int, callback: CollegeCallback) {
            sendRequest("$TEACHER_TIMETABLE/$branchId/$teacherId", callback)
        }
    }
}
package com.qkartbismuth.ktc.college.classroom

import com.qkartbismuth.ktc.college.CollegeApi
import com.qkartbismuth.ktc.college.CollegeCallback
import com.qkartbismuth.ktc.college.timetable.Courses
import com.qkartbismuth.ktc.college.timetable.LessonTime
import com.qkartbismuth.ktc.college.timetable.Week
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Response
import java.io.IOException
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/**
 * A single lesson that takes place in a classroom.
 *
 * @param day weekday title as the server sends it, e.g. "Пн".
 * @param time lesson time [number, from, to].
 * @param group group name, e.g. "БД.09.23.1".
 */
data class ClassroomLesson(
    val day: String,
    val time: LessonTime,
    val group: String,
    val teacher: String,
    val title: String,
    val classroom: String
)

private data class GroupRef(val id: Int, val title: String)

/**
 * Builds an in-memory index of every lesson in every classroom of a branch.
 *
 * The server has no "classroom timetable" endpoint, so the index is assembled
 * from the current-week timetables of all groups in the branch. It is built
 * once and then kept in memory for the whole screen lifecycle.
 */
class ClassroomIndex(
    branchId: Int,
    private val onReady: () -> Unit,
    private val onError: () -> Unit
) {
    private val lessons: MutableList<ClassroomLesson> = Collections.synchronizedList(ArrayList())

    @Volatile
    var ready = false
        private set

    init {
        fetchGroups(branchId)
    }

    /**
     * Returns the sorted list of classrooms that have lessons on [day].
     */
    fun classrooms(day: String): List<String> {
        if (!ready) return emptyList()
        return lessons
            .filter { it.day == day }
            .map { it.classroom }
            .distinct()
            .sorted()
    }

    /**
     * Returns the lessons (of any group) that take place in [classroom]
     * on [day], ordered by time and group.
     */
    fun lessonsFor(classroom: String, day: String): List<ClassroomLesson> {
        if (!ready) return emptyList()
        return lessons
            .filter { it.day == day && it.classroom == classroom }
            .sortedWith(compareBy({ it.time[1] }, { it.group }))
    }

    /**
     * Fetches the course/group list of the branch and then
     * the current-week timetable of every group.
     */
    private fun fetchGroups(branchId: Int) {
        CollegeApi.fetchCourses(branchId, object : CollegeCallback {
            override fun onResponse(call: Call, response: Response) {
                val courses: Courses
                try {
                    val json = response.body?.string() ?: return fail()
                    courses = Gson().fromJson(json, Courses::class.java)
                } catch (e: Exception) {
                    return fail()
                }
                val groups = ArrayList<GroupRef>()
                for (course in courses)
                    for (group in course.groups)
                        groups.add(GroupRef(group.id, group.title))
                if (groups.isEmpty()) {
                    fail()
                } else {
                    loadAll(groups)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                fail()
            }
        })
    }

    /**
     * Loads the current-week timetable of every group in parallel.
     * Broken groups are skipped; [onReady] fires after the last attempt.
     */
    private fun loadAll(groups: List<GroupRef>) {
        val counter = AtomicInteger(groups.size)
        for (group in groups) {
            CollegeApi.fetchTimetable(group.id, object : CollegeCallback {
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val json = response.body?.string()
                        if (response.isSuccessful && json != null && json.isNotBlank()) {
                            val week = Gson().fromJson(json, Week::class.java)
                            for (day in week.days) {
                                for (lesson in day.lessons) {
                                    if (lesson.cancelled || lesson.status == 1) continue
                                    if (lesson.classroom.isBlank()) continue
                                    lessons.add(ClassroomLesson(
                                        day = day.title,
                                        time = lesson.time,
                                        group = group.title,
                                        teacher = lesson.teacher,
                                        title = lesson.title,
                                        classroom = lesson.classroom
                                    ))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // skip a broken group timetable
                    } finally {
                        done(counter)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    done(counter)
                }
            })
        }
    }

    private fun done(counter: AtomicInteger) {
        if (counter.decrementAndGet() == 0) {
            ready = true
            onReady()
        }
    }

    private fun fail() {
        if (!ready) onError()
    }
}
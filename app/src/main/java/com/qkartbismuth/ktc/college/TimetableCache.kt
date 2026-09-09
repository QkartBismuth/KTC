package com.qkartbismuth.ktc.college

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Stores the last successful timetable fetch for the currently selected
 * group (and the currently selected teacher) so the app can show it
 * while offline.
 *
 * Only a single entry is kept: whether a group or a teacher timetable is
 * chosen, the other one is replaced. Every successful fetch overwrites
 * the stored copy.
 */
class TimetableCache(context: Context) {
    private val cacheDir = context.cacheDir
    private val prefs = context.getSharedPreferences("timetable_meta", Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()

    companion object {
        const val MAX_AGE_MS = 30 * 60 * 1000L
        private const val CACHE_FILE = "cached_timetable.json"
        private const val TEACHER_CACHE_FILE = "cached_teacher_timetable.json"
        private const val PREF_RAW = "raw"
        private const val PREF_TEACHER_RAW = "raw_teacher"
        private const val PREF_FETCHED_AT = "fetched_at"
        private const val PREF_TEACHER_FETCHED_AT = "fetched_at_teacher"
    }

    private fun cacheFile(forTeacher: Boolean): File =
        File(cacheDir, if (forTeacher) TEACHER_CACHE_FILE else CACHE_FILE)

    /**
     * Serialized raw JSON of a successful lecture (Week) fetch.
     */
    fun saveRawTimetable(json: String) {
        prefs.edit().putString(PREF_RAW, json).putLong(PREF_FETCHED_AT, System.currentTimeMillis()).apply()
        cacheFile(false).writeText(json)
    }

    fun saveRawTeacherTimetable(json: String) {
        prefs.edit().putString(PREF_TEACHER_RAW, json).putLong(PREF_TEACHER_FETCHED_AT, System.currentTimeMillis()).apply()
        cacheFile(true).writeText(json)
    }

    fun raw(forTeacher: Boolean): String? =
        prefs.getString(if (forTeacher) PREF_TEACHER_RAW else PREF_RAW, null)
            ?: cacheFile(forTeacher).takeIf { it.exists() }?.readText()

    fun fetchedAt(forTeacher: Boolean): Long =
        prefs.getLong(if (forTeacher) PREF_TEACHER_FETCHED_AT else PREF_FETCHED_AT, 0L)

    /**
     * True if there is a stored copy older than 30 minutes. When true,
     * a background refresh should be performed.
     */
    fun isExpired(forTeacher: Boolean): Boolean {
        val age = System.currentTimeMillis() - fetchedAt(forTeacher)
        return fetchedAt(forTeacher) == 0L || age >= MAX_AGE_MS
    }

    fun <T> fromCache(forTeacher: Boolean, clazz: Class<T>): T? {
        val json = raw(forTeacher) ?: return null
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            null
        }
    }
}
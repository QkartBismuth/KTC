package com.qkartbismuth.ktc.ui.widgets

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.qkartbismuth.ktc.Preferences
import com.qkartbismuth.ktc.R
import com.qkartbismuth.ktc.college.CollegeApi
import com.qkartbismuth.ktc.college.CollegeCallback
import com.qkartbismuth.ktc.college.timetable.Day
import com.qkartbismuth.ktc.college.timetable.Week
import com.qkartbismuth.ktc.ui.activities.MainActivity
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.Call
import okhttp3.Response
import java.lang.Exception
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


/**
 * Implementation of App Widget functionality.
 */
class TimetableWidget : AppWidgetProvider() {
    private var preferences: Preferences? = null

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // The widget may start the process on its own, so the in-memory
        // preferences (group/teacher/branch) must be read from disk first.
        preferences = Preferences(context).also { it.load() }
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * Update timetable for current widget
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun updateWidgetPendingIntent(
        context: Context?,
        appWidgetId: Int
    ): PendingIntent {
        val intent = Intent(context!!, TimetableWidget::class.java)
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, TimetableWidget::class.java))
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)

        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            when {
                Build.VERSION.SDK_INT >= 31 -> PendingIntent.FLAG_MUTABLE
                else -> PendingIntent.FLAG_UPDATE_CURRENT
            })
    }

    /**
     * Open app with pending intent
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun openAppPendingIntent(
        context: Context?,
        appWidgetId: Int
    ): PendingIntent {
        val intent = Intent(context!!, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            when {
                Build.VERSION.SDK_INT >= 31 -> PendingIntent.FLAG_MUTABLE
                else -> PendingIntent.FLAG_UPDATE_CURRENT
            })
    }

    @SuppressLint("RemoteViewLayout", "SimpleDateFormat")
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.widget_timetable)
        views.setOnClickPendingIntent(
            R.id.timetable_widget_reload,
            updateWidgetPendingIntent(context, appWidgetId)
        )
        views.setOnClickPendingIntent(
            R.id.timetable_widget_background,
            openAppPendingIntent(context, appWidgetId)
        )
        // Load last group ID
        val groupId = Preferences.group.id
        if (groupId <= 0) {
            views.setTextViewText(
                R.id.timetable_widget_title,
                context.getString(R.string.widget_no_group)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        val calendar = Calendar.getInstance()
        val weekday = calendar.get(Calendar.DAY_OF_WEEK)
        // Lesson times come from the API as "H:mm", so parse them the same way.
        val timeFormat = SimpleDateFormat("H:mm", Locale.getDefault())
        val now = timeFormat.parse(
            "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}"
        )?.time

        CollegeApi.fetchTimetable(groupId, object : CollegeCallback {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call, response: Response) {
                // Parse JSON
                val json = response.body?.string()
                val timetable: Week?
                try {
                    timetable = Gson().fromJson(json, Week::class.java)
                } catch (e: JsonSyntaxException) {
                    views.setTextViewText(
                        R.id.timetable_widget_title,
                        context.getString(R.string.toast_timetable_error)
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return
                } catch (e: Exception) {
                    views.setTextViewText(
                        R.id.timetable_widget_title,
                        context.getString(R.string.toast_unknown_error)
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return
                }
                if (timetable == null) {
                    views.setTextViewText(
                        R.id.timetable_widget_title,
                        context.getString(R.string.widget_no_data)
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return
                }
                // Gson can leave days null when the API returns an empty object.
                @Suppress("SENSELESS_COMPARISON")
                val hasDays = timetable.days != null && timetable.days.size > weekday - 2
                // Get current day timetable
                val day: Day? = when {
                    weekday == 1 -> null
                    !hasDays -> null
                    else -> timetable.days[weekday - 2]
                }
                if (day == null) {
                    views.setTextViewText(
                        R.id.timetable_widget_title,
                        context.getString(R.string.widget_day_off)
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return
                }

                // Setup widget
                views.setTextViewText(R.id.timetable_widget_title, day.title)
                // removeAllViews is available since API 23 only.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    views.removeAllViews(R.id.timetable_widget_lessons)

                // Setup views
                var lesson: RemoteViews
                for (l in day.lessons) {
                    // cancelled lessons are not shown
                    if (l.status == 1) continue
                    // Load lesson data
                    lesson = RemoteViews(context.packageName, R.layout.widget_lesson)
                    with (lesson) {
                        setTextViewText(R.id.lesson_title, l.title)
                        setTextViewText(R.id.lesson_classroom, l.actual_classroom ?: l.classroom)
                        setTextViewText(R.id.lesson_number, l.time[0])
                        setTextViewText(R.id.lesson_from, l.time[1])
                        setTextViewText(R.id.lesson_to, l.time[2])
                        setTextViewText(R.id.lesson_teacher, l.teacher)
                    }

                    // Highlight the lesson that is in progress right now.
                    try {
                        val from = timeFormat.parse(l.time[1])?.time
                        val to = timeFormat.parse(l.time[2])?.time
                        if (now != null && from != null && to != null && now in from..to)
                            lesson.setInt(
                                R.id.lesson_background,
                                "setBackgroundResource",
                                R.color.current_lesson
                            )
                    } catch (e: ParseException) {
                        // Malformed time in the API response: skip highlighting.
                    }

                    views.addView(R.id.timetable_widget_lessons, lesson)
                }
                // Update widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }, null)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
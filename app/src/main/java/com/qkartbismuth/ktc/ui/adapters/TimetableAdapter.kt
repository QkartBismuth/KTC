package com.qkartbismuth.ktc.ui.adapters

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qkartbismuth.ktc.R
import com.qkartbismuth.ktc.college.timetable.Day
import com.qkartbismuth.ktc.college.timetable.Week
import com.qkartbismuth.ktc.databinding.LayoutLessonBinding
import com.qkartbismuth.ktc.databinding.LayoutTimetableBinding
import com.qkartbismuth.ktc.ui.fragments.TimetableFragment
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provides RecyclerView.Adapter behavior for timetable.
 */
class TimetableAdapter(
    private val timetableFragment: TimetableFragment,
    private val week: Week
) : RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {
    @SuppressLint("SimpleDateFormat")
    private var dateFormat = SimpleDateFormat("mm:ss")
    private var weekday: Day? = null
    private var now = "00:00"
    var currentPos: Int? = null
    /**
     * Provides RecyclerView.ViewHolder behavior.
     * Also includes TimetableBinding.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = LayoutTimetableBinding.bind(view)
    }

    init {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        now = "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}"
        currentPos = when (day) {
            1 -> null
            2 -> 0
            else -> day-2
        }
        weekday = when (day) {
            1 -> null
            2 -> week.days[0]
            else -> week.days[day-2]
        }
    }

    @SuppressLint("SimpleDateFormat")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_timetable, parent, false))
    }

    /**
     * Binds every day in week.
     * Binds every lesson in day.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val day = week.days[position]
        holder.binding.dayHeader.text = day.title

        holder.binding.root.setBackgroundResource(
            if (currentPos == position) R.drawable.shape_background_current_day
            else R.drawable.shape_background_secondary
        )

        // Reset the card: keep only the header, drop lessons added earlier
        // so recycled ViewHolders don't accumulate duplicates.
        val header = holder.binding.dayRoot
        holder.binding.root.removeAllViews()
        holder.binding.root.addView(header)

        var lesson: LayoutLessonBinding
        var visibleCount = 0
        for (l in day.lessons) {
            // cancelled lessons are shown greyed-out with a strikethrough
            val cancelled = l.cancelled || l.status == 1
            visibleCount++

            lesson = LayoutLessonBinding.inflate(
                LayoutInflater.from(timetableFragment.context),
                holder.binding.root, false)
            with (lesson) {
                lessonTitle.text = l.title
                lessonNumber.text = l.time[0]
                lessonFrom.text = l.time[1]
                lessonTo.text = l.time[2]
                lessonTeacher.text = l.teacher

                // rescheduled lessons show the actual classroom
                val classroom = l.actual_classroom ?: l.classroom
                lessonClassroom.text = classroom

                when {
                    cancelled -> bindCancelled()
                    l.status == 2 -> {
                        lesson.root.setBackgroundResource(R.color.lesson_changed)
                        bindNote(l.note)
                    }
                    else -> {
                        lesson.root.setBackgroundResource(android.R.color.transparent)
                        bindNote(null)
                    }
                }

                // check time (skip cancelled lessons)
                if (!cancelled) {
                    val from = dateFormat.parse(l.time[1])?.time!!
                    val current = dateFormat.parse(now)?.time!!
                    val to = dateFormat.parse(l.time[2])?.time!!
                    if (current in from..to && weekday?.title == day.title)
                        lesson.root.setBackgroundResource(R.color.current_lesson)
                }

                holder.binding.root.addView(root)
            }
        }
        bindLoadIndicator(holder.binding.dayLoadIndicator, visibleCount)
    }

    /**
     * Styles a cancelled lesson: muted colors, strikethrough title
     * and an "Отменено" note.
     */
    private fun LayoutLessonBinding.bindCancelled() {
        val ctx = timetableFragment.requireContext()
        val muted = ctx.getColor(R.color.lesson_note_text)
        lessonNumber.setTextColor(muted)
        lessonTitle.setTextColor(muted)
        lessonTitle.paintFlags = lessonTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        lessonTeacher.setTextColor(muted)
        lessonClassroom.setTextColor(muted)
        root.setBackgroundResource(android.R.color.transparent)
        lessonNote.text = ctx.getString(R.string.lesson_cancelled)
        lessonNote.setTextColor(ctx.getColor(R.color.error))
        lessonNote.visibility = View.VISIBLE
    }

    /**
     * Colorizes the day card indicator by the number of visible lessons:
     * green for 1-2, yellow for 3, red for 4+.
     */
    private fun bindLoadIndicator(indicator: View, count: Int) {
        if (count == 0) {
            indicator.visibility = View.GONE
            return
        }
        indicator.visibility = View.VISIBLE
        val colorId = when {
            count >= 4 -> R.color.day_load_high
            count == 3 -> R.color.day_load_medium
            else -> R.color.day_load_low
        }
        indicator.backgroundTintList = ColorStateList.valueOf(
            timetableFragment.requireContext().getColor(colorId)
        )
    }

    private fun LayoutLessonBinding.bindNote(note: String?) {
        lessonNote.visibility =
            if (note.isNullOrBlank()) View.GONE else View.VISIBLE
        if (!note.isNullOrBlank()) {
            lessonNote.text = note
            lessonNote.setTextColor(
                timetableFragment.requireContext().getColor(R.color.lesson_note_text)
            )
        }
    }

    /**
     * @return days count.
     */
    override fun getItemCount(): Int = week.days.count()
}
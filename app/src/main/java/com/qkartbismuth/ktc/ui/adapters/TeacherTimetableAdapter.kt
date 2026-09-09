package com.qkartbismuth.ktc.ui.adapters

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qkartbismuth.ktc.R
import com.qkartbismuth.ktc.college.teacher.TeacherTimetable
import com.qkartbismuth.ktc.databinding.LayoutTeacherLessonBinding
import com.qkartbismuth.ktc.databinding.LayoutTimetableBinding
import com.qkartbismuth.ktc.ui.fragments.TimetableFragment
import java.util.*

/**
 * Implementation of TimetableAdapter but for teachers
 */
class TeacherTimetableAdapter(
    private val timetableFragment: TimetableFragment,
    private val items: TeacherTimetable,
) : RecyclerView.Adapter<TeacherTimetableAdapter.ViewHolder>() {

    var currentPos: Int? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = LayoutTimetableBinding.bind(view)
    }

    init {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        currentPos = when (day) {
            1 -> null
            2 -> 0
            else -> day-2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_timetable, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val day = items.week[position]

        binding.dayHeader.text = day.title

        binding.root.setBackgroundResource(
            if (currentPos == position) R.drawable.shape_background_current_day
            else R.drawable.shape_background_secondary
        )

        // ох уж эти костыли ...
        val header = binding.dayRoot
        binding.root.removeAllViews()
        binding.root.addView(header)

        var lesson: LayoutTeacherLessonBinding
        var visibleCount = 0
        for (l in day.lessons) {
            // empty slots are placeholders, cancelled lessons are shown greyed-out
            if (l.group == "") continue
            val cancelled = l.cancelled || l.status == 1
            visibleCount++

            lesson = LayoutTeacherLessonBinding.inflate(
                LayoutInflater.from(timetableFragment.context),
                null, false)
            with (lesson) {
                // rescheduled lessons show the actual classroom
                tlessonClassroom.text = l.actual_classroom ?: l.classroom
                tlessonGroup.text = l.group
                tlessonTitle.text = l.title
                tlessonNumber.text = l.number

                when {
                    cancelled -> bindCancelled()
                    l.status == 2 -> bindNote(l.note)
                    else -> bindNote(null)
                }

                binding.root.addView(root)
            }
        }
        bindLoadIndicator(binding.dayLoadIndicator, visibleCount)
    }

    private fun LayoutTeacherLessonBinding.bindCancelled() {
        val ctx = timetableFragment.requireContext()
        val muted = ctx.getColor(R.color.lesson_note_text)
        tlessonNumber.setTextColor(muted)
        tlessonTitle.setTextColor(muted)
        tlessonTitle.paintFlags = tlessonTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        tlessonGroup.setTextColor(muted)
        tlessonClassroom.setTextColor(muted)
        root.setBackgroundResource(android.R.color.transparent)
        tlessonNote.text = ctx.getString(R.string.lesson_cancelled)
        tlessonNote.setTextColor(ctx.getColor(R.color.error))
        tlessonNote.visibility = View.VISIBLE
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

    private fun LayoutTeacherLessonBinding.bindNote(note: String?) {
        tlessonNote.visibility =
            if (note.isNullOrBlank()) View.GONE else View.VISIBLE
        if (!note.isNullOrBlank()) {
            tlessonNote.text = note
            tlessonNote.setTextColor(
                timetableFragment.requireContext().getColor(R.color.lesson_note_text)
            )
        }
    }

    override fun getItemCount(): Int = items.week.count()
}
package com.qkartbismuth.ktc.ui.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qkartbismuth.ktc.Preferences
import com.qkartbismuth.ktc.R
import com.qkartbismuth.ktc.college.timetable.Courses
import com.qkartbismuth.ktc.databinding.LayoutCourseBinding
import com.qkartbismuth.ktc.ui.fragments.TimetableFragment
import com.google.android.material.chip.Chip

/**
 * Provides RecyclerView.Adapter behavior for courses.
 */
class CourseAdapter(
    private val timetableFragment: TimetableFragment,
    private val items: Courses
) : RecyclerView.Adapter<CourseAdapter.ViewHolder>() {
    /**
     * Provides RecyclerView.ViewHolder behavior.
     * Also includes CourseBinding.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = LayoutCourseBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_course, parent, false)
        )
    }

    /**
     * Builds groups for every course.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val item = items[position]
        val context = timetableFragment.requireContext()

        binding.courseTitle.text = context.getString(R.string.course_number, item.course)
        for (group in item.groups) {
            val chip = Chip(context)
            chip.text = group.title
            chip.chipBackgroundColor = ColorStateList.valueOf(
                context.getColor(R.color.secondary_container)
            )
            chip.chipStrokeWidth = 0f
            chip.setTextColor(context.getColor(R.color.on_secondary_container))
            chip.setOnClickListener {
                timetableFragment.fetchTimetable(group.id)
                Preferences.group = group
            }
            binding.courseGroup.addView(chip)
        }
    }

    /**
     * @return items count
     */
    override fun getItemCount(): Int = items.size
}
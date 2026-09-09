package com.qkartbismuth.ktc.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qkartbismuth.ktc.R
import com.qkartbismuth.ktc.college.teacher.TeachersList
import com.qkartbismuth.ktc.databinding.LayoutTeacherBinding
import com.qkartbismuth.ktc.ui.fragments.TimetableFragment


class TeachersListAdapter(
    private val timetableFragment: TimetableFragment,
    private val items: TeachersList
) : RecyclerView.Adapter<TeachersListAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = LayoutTeacherBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_teacher, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.teacherName.text = items.teachers[position].name
        holder.binding.teacherCard.setOnClickListener {
            timetableFragment.fetchTeacherTimetable(items.teachers[position].id)
        }
    }

    override fun getItemCount(): Int = items.teachers.size
}
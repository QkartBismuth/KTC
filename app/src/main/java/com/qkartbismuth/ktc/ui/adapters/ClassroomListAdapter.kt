package com.qkartbismuth.ktc.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qkartbismuth.ktc.R
import com.qkartbismuth.ktc.college.classroom.ClassroomLesson
import com.qkartbismuth.ktc.databinding.LayoutClassroomBinding
import com.qkartbismuth.ktc.databinding.LayoutClassroomLessonBinding

/**
 * A classroom with its today lessons. Used to build expandable sections.
 */
data class ClassroomSection(
    val classroom: String,
    val lessons: List<ClassroomLesson>
)

/**
 * List of classrooms. Tapping a classroom expands/collapses its today lessons.
 */
class ClassroomListAdapter(
    private val sections: List<ClassroomSection>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val expanded = HashSet<String>()

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_LESSON = 1
    }

    class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = LayoutClassroomBinding.bind(view)
    }

    class LessonHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = LayoutClassroomLessonBinding.bind(view)
    }

    override fun getItemCount(): Int {
        var count = 0
        for (s in sections)
            count += 1 + if (s.classroom in expanded) s.lessons.size else 0
        return count
    }

    override fun getItemViewType(position: Int): Int =
        if (resolve(position).second) TYPE_HEADER else TYPE_LESSON

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER)
            HeaderHolder(inflater.inflate(R.layout.layout_classroom, parent, false))
        else
            LessonHolder(inflater.inflate(R.layout.layout_classroom_lesson, parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (index, isHeader) = resolve(position)
        val section = sections[index]
        if (isHeader) {
            val binding = (holder as HeaderHolder).binding
            binding.roomName.text = section.classroom
            binding.roomCounter.text =
                holder.itemView.context.getString(R.string.classroom_lesson_count, section.lessons.size)
            val open = section.classroom in expanded
            binding.roomChevron.rotation = if (open) 90f else 0f
            binding.root.setOnClickListener {
                if (!expanded.add(section.classroom))
                    expanded.remove(section.classroom)
                notifyDataSetChanged()
            }
            return
        }
        val lesson = section.lessons[position - (1 + indexOffsetBefore(index))]
        val binding = (holder as LessonHolder).binding
        binding.clessonTime.text = lesson.time[1] + " – " + lesson.time[2]
        binding.clessonGroup.text = lesson.group
        binding.clessonTeacher.text = lesson.teacher
        binding.clessonTitle.text = lesson.title
    }

    /** Position -> (section index, isHeader). */
    private fun resolve(position: Int): Pair<Int, Boolean> {
        var offset = 0
        for (i in sections.indices) {
            val header = offset
            if (position == header) return i to true
            val lessonCount = if (sections[i].classroom in expanded) sections[i].lessons.size else 0
            if (position <= header + lessonCount) return i to false
            offset = header + 1 + lessonCount
        }
        return 0 to true
    }

    /** Number of rows before the header of section [index]. */
    private fun indexOffsetBefore(index: Int): Int {
        var offset = 0
        for (i in 0 until index)
            offset += 1 + if (sections[i].classroom in expanded) sections[i].lessons.size else 0
        return offset
    }
}
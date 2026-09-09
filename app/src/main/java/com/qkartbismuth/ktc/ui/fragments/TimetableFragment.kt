package com.qkartbismuth.ktc.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.qkartbismuth.ktc.Preferences
import com.qkartbismuth.ktc.R
import com.qkartbismuth.ktc.college.CollegeApi
import com.qkartbismuth.ktc.college.CollegeCallback
import com.qkartbismuth.ktc.college.TimetableCache
import com.qkartbismuth.ktc.college.classroom.ClassroomIndex
import com.qkartbismuth.ktc.college.teacher.TeacherTimetable
import com.qkartbismuth.ktc.college.teacher.TeachersList
import com.qkartbismuth.ktc.college.timetable.*
import com.qkartbismuth.ktc.databinding.FragmentTimetableBinding
import com.qkartbismuth.ktc.ui.adapters.*
import com.qkartbismuth.ktc.utils.IOFragmentBackPressed
import com.qkartbismuth.ktc.ui.decoration.SpacingItemDecoration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.Call
import okhttp3.Response
import java.io.IOException
import java.lang.Exception
import java.util.Calendar

/**
 * Provides working with KTC timetable.
 * Includes branches, courses with groups and timetable for any week.
 */
class TimetableFragment : IOFragmentBackPressed() {
    private var _binding: FragmentTimetableBinding? = null
    private lateinit var preferences: Preferences
    private lateinit var itemDecoration: RecyclerView.ItemDecoration
    private lateinit var cache: TimetableCache

    private val binding get() = _binding!!

    /**
     * Enables auto-refresh of the current week timetable.
     * Disabled while the user is browsing other weeks.
     */
    private var autoRefresh = false
    private val refreshHandler = Handler(Looper.getMainLooper())

    // Classroom tab (teacher mode → "Кабинеты")
    private var classroomIndex: ClassroomIndex? = null
    private var currentTeachers: TeachersList? = null
    private var selectedTeacherTab = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimetableBinding.inflate(inflater, container, false)

        binding.timetable.layoutManager = LinearLayoutManager(context)
        itemDecoration = SpacingItemDecoration(16, 12)
        binding.timetable.addItemDecoration(itemDecoration)

        // Load state
        preferences = Preferences(requireContext())
        preferences.load()
        cache = TimetableCache(requireContext())
        loadState()

        // Analog for back button
        binding.back.setOnClickListener {
            // back button disabled when state isn't 0
            binding.back.isEnabled = !onBackPressed()
        }

        // Next week
        binding.next.setOnClickListener {
            if (Preferences.week < 57)
                changeWeek(1)
        }

        // previous week
        binding.previous.setOnClickListener {
            if (Preferences.week > 1)
                changeWeek(-1)
        }

        // toggle isStudent
        binding.toggleTimetable.setOnClickListener {
            Preferences.isStudent = !Preferences.isStudent
            if (!Preferences.isStudent && Preferences.timetableState == 2 && Preferences.teacherId == 0) {
                Preferences.timetableState = 1
            }
            if (Preferences.isStudent && Preferences.timetableState == 2 && Preferences.group.id == 0) {
                Preferences.timetableState = 1
            }
            loadState()
        }

        binding.themePicker.setOnClickListener {
            showThemeDialog()
        }

        // Classroom search on the "Кабинеты" teacher tab
        binding.teacherTabs.addOnTabSelectedListener(teacherTabListener)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        refreshHandler.removeCallbacks(autoRefreshTask)
        refreshHandler.postDelayed(autoRefreshTask, TimetableCache.MAX_AGE_MS)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(autoRefreshTask)
    }

    /**
     * Background refresh of the current week (both student and teacher
     * modes). Silent: does not show toasts on failure.
     */
    private val autoRefreshTask = object : Runnable {
        override fun run() {
            when (Preferences.timetableState) {
                2 -> if (Preferences.isStudent)
                    fetchTimetable(Preferences.group.id, silent = true)
                else
                    fetchTeacherTimetable(Preferences.teacherId, silent = true)
                else -> Unit
            }
            refreshHandler.postDelayed(this, TimetableCache.MAX_AGE_MS)
        }
    }

    /**
     * destroy bindings.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        refreshHandler.removeCallbacksAndMessages(null)
        _binding = null
    }

    /**
     * Calls when back button pressed.
     */
    override fun onBackPressed(): Boolean {
        // Provides behavior on Back pressed.
        when (Preferences.timetableState) {
            1 -> fetchBranches()
            2 ->
                if (Preferences.isStudent)
                    fetchCourses(Preferences.branch.id)
                else
                    fetchTeacherList(Preferences.branch.id)
            else -> return false
        }
        return true
    }

    private fun changeWeek(i: Int) {
        // Provides week changing behavior.
        autoRefresh = false
        Preferences.week += i
        binding.next.isEnabled = false
        binding.previous.isEnabled = false
        fetchTimetable(Preferences.group.id, Preferences.week)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadState() {
        // Fetches timetable from loaded state.
        when (Preferences.timetableState) {
            // branches
            0 -> fetchBranches()
            // courses
            1 ->
                if (Preferences.isStudent)
                    fetchCourses(Preferences.branch.id)
                else
                    fetchTeacherList(Preferences.branch.id)
            // students timetable
            2 -> {
                autoRefresh = true
                showCachedTimetable(notifyOffline = false)
                if (Preferences.isStudent)
                    fetchTimetable(Preferences.group.id)
                else
                    fetchTeacherTimetable(Preferences.teacherId)
            }
        }
        binding.toggleTimetable.setImageDrawable(resources.getDrawable(
            if (Preferences.isStudent) R.drawable.ic_graduation_cap else R.drawable.ic_globe_alt,
            requireContext().theme)
        )
    }

    /**
     * Shows the last saved timetable immediately, if any.
     * @return true if a cached timetable has been displayed.
     * @param notifyOffline true to notify the user that the data is from the cache.
     */
    private fun showCachedTimetable(notifyOffline: Boolean): Boolean {
        val shown = if (Preferences.isStudent) {
            val data = cache.fromCache(false, Week::class.java) ?: return false
            displayStudentTimetable(data)
        } else {
            val data = cache.fromCache(true, TeacherTimetable::class.java) ?: return false
            displayTeacherTimetable(data)
        }
        if (shown && notifyOffline) toast(R.string.toast_offline_timetable)
        return shown
    }

    /**
     * Fetches branches and shows it.
     */
    private fun fetchBranches() {
        Preferences.timetableState = 0
        CollegeApi.fetchBranches(object : CollegeCallback {
            override fun onResponse(call: Call, response: Response) {
                // Parse JSON
                val json = response.body?.string()
                val branches: Branches
                try {
                    branches = Gson().fromJson(json, Branches::class.java)
                } catch (e: JsonSyntaxException) {
                    toast(R.string.toast_branch_error)
                    return
                } catch (e: Exception) {
                    toast(R.string.toast_unknown_error)
                    return
                }

                activity!!.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    with(binding) {
                        back.isEnabled = true
                        timetableToolbar.visibility = View.GONE
                        branchHeader.visibility = View.VISIBLE
                        themePicker.visibility = View.VISIBLE
                        teacherTabs.visibility = View.GONE
                        classroomSearch.visibility = View.GONE
                        timetable.adapter = BranchAdapter(
                            this@TimetableFragment, branches
                        )
                    }
                    preferences.saveTimetable()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                super.onFailure(call, e)
                toast(R.string.toast_unknown_error)
            }
        })
    }

    /**
     * Fetches courses for specified branch.
     * @param branchId unique branch ID.
     */
    fun fetchCourses(branchId: Int) {
        Preferences.timetableState = 1
        CollegeApi.fetchCourses(branchId, object : CollegeCallback {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call, response: Response) {
                // Parse JSON
                val json = response.body?.string()
                val courses: Courses
                try {
                    courses = Gson().fromJson(json, Courses::class.java)
                } catch (e: JsonSyntaxException) {
                    toast(R.string.toast_courses_error)
                    return
                } catch (e: Exception) {
                    toast(R.string.toast_unknown_error)
                    return
                }

                activity!!.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    with (binding) {
                        back.isEnabled = true
                        timetableToolbar.visibility = View.VISIBLE
                        branchHeader.visibility = View.GONE
                        themePicker.visibility = View.GONE
                        next.visibility = View.GONE
                        previous.visibility = View.GONE
                        teacherTabs.visibility = View.GONE
                        classroomSearch.visibility = View.GONE
                        timetableTitle.text = getString(R.string.courses_title)
                        timetable.adapter = CourseAdapter(
                            this@TimetableFragment, courses
                        )
                    }
                    preferences.saveTimetable()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                super.onFailure(call, e)
                toast(R.string.toast_unknown_error)
            }
        })
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun displayStudentTimetable(data: Week): Boolean {
        if (_binding == null) return false
        with (binding) {
            back.isEnabled = true
            next.isEnabled = true
            previous.isEnabled = true
            timetableTitle.text = "${Preferences.group.title}\n${data.week_number} неделя"
            timetableToolbar.visibility = View.VISIBLE
            branchHeader.visibility = View.GONE
            themePicker.visibility = View.GONE
            teacherTabs.visibility = View.GONE
            classroomSearch.visibility = View.GONE
            next.visibility = View.VISIBLE
            previous.visibility = View.VISIBLE
            val adapter = TimetableAdapter(
                this@TimetableFragment, data
            )
            timetable.adapter = adapter
            if (adapter.currentPos != null)
                timetable.layoutManager?.scrollToPosition(adapter.currentPos!!)
        }
        return true
    }

    @SuppressLint("SetTextI18n")
    private fun displayTeacherTimetable(data: TeacherTimetable): Boolean {
        if (_binding == null) return false
        with (binding) {
            back.isEnabled = true
            timetableTitle.text = data.title
            timetableToolbar.visibility = View.VISIBLE
            branchHeader.visibility = View.GONE
            themePicker.visibility = View.GONE
            teacherTabs.visibility = View.GONE
            classroomSearch.visibility = View.GONE
            next.visibility = View.GONE
            previous.visibility = View.GONE
            timetable.adapter = TeacherTimetableAdapter(
                this@TimetableFragment, data
            )
        }
        return true
    }

    /**
     * Fetches timetable for specified groupId.
     * @param groupId unique group ID.
     * @param week by default is current week.
     * @param silent true for background refresh without failure toasts.
     */
    fun fetchTimetable(groupId: Int, week: Int? = null, silent: Boolean = false) {
        Preferences.timetableState = 2
        CollegeApi.fetchTimetable(groupId, object : CollegeCallback {
            @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
            override fun onResponse(call: Call, response: Response) {
                // Parse JSON
                val json = response.body?.string()
                val data: Week
                try {
                    data = Gson().fromJson(json, Week::class.java)
                } catch (e: JsonSyntaxException) {
                    if (!silent) toast(R.string.toast_timetable_error)
                    return
                } catch (e: Exception) {
                    if (!silent) toast(R.string.toast_unknown_error)
                    return
                }

                // Saving the current week so it can be shown offline
                if (autoRefresh && json != null && data.week_number == Preferences.week) {
                    val changed = cache.raw(false) != null && cache.raw(false) != json
                    cache.saveRawTimetable(json)
                    if (changed) toast(R.string.toast_timetable_updated)
                }
                Preferences.week = data.week_number

                activity!!.runOnUiThread {
                    displayStudentTimetable(data)
                }
                preferences.saveTimetable()
            }

            override fun onFailure(call: Call, e: IOException) {
                super.onFailure(call, e)
                if (silent) return
                activity?.runOnUiThread {
                    if (showCachedTimetable(notifyOffline = false)) return@runOnUiThread
                    toast(R.string.toast_unknown_error)
                }
            }
        }, week)
    }

    /**
     * Fetches timetable for specified teacher.
     * @param teacherId unique teacher ID.
     * @param silent true for background refresh without failure toasts.
     */
    fun fetchTeacherTimetable(teacherId: Int = -1, silent: Boolean = false) {
        Preferences.timetableState = 2
        CollegeApi.fetchTeacherTimetable(
            Preferences.branch.id,
            if (teacherId < 0) Preferences.teacherId else teacherId,
            object : CollegeCallback {
                @SuppressLint("SetTextI18n")
                override fun onResponse(call: Call, response: Response) {
                    // Parse JSON
                    val json = response.body?.string()
                    val data: TeacherTimetable
                    try {
                        data = Gson().fromJson(json, TeacherTimetable::class.java)
                    } catch (e: JsonSyntaxException) {
                        if (!silent) toast(R.string.toast_timetable_error)
                        return
                    } catch (e: Exception) {
                        if (!silent) toast(R.string.toast_unknown_error)
                        return
                    }
                    if (teacherId >= 0) Preferences.teacherId = teacherId

                    // Saving the current teacher timetable so it can be shown offline
                    if (autoRefresh && json != null) {
                        val changed = cache.raw(true) != null && cache.raw(true) != json
                        cache.saveRawTeacherTimetable(json)
                        if (changed) toast(R.string.toast_timetable_updated)
                    }

                    activity!!.runOnUiThread {
                        displayTeacherTimetable(data)
                    }
                    preferences.saveTimetable()
                }

                override fun onFailure(call: Call, e: IOException) {
                    super.onFailure(call, e)
                    if (silent) return
                    activity?.runOnUiThread {
                        if (showCachedTimetable(notifyOffline = false)) return@runOnUiThread
                        toast(R.string.toast_unknown_error)
                    }
                }
            })
    }

    /**
     * Fetches list of teachers
     * @param branchId unique branch ID.
     */
    fun fetchTeacherList(branchId: Int) {
        Preferences.timetableState = 1
        CollegeApi.fetchTeachersList(branchId, object : CollegeCallback {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call, response: Response) {
                // Parse JSON
                val json = response.body?.string()
                val teachers: TeachersList
                try {
                    teachers = Gson().fromJson(json, TeachersList::class.java)
                } catch (e: JsonSyntaxException) {
                    toast(R.string.toast_teacher_error)
                    return
                } catch (e: Exception) {
                    toast(R.string.toast_unknown_error)
                    return
                }

                activity!!.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    with (binding) {
                        back.isEnabled = true
                        timetableToolbar.visibility = View.VISIBLE
                        branchHeader.visibility = View.GONE
                        themePicker.visibility = View.GONE
                        next.visibility = View.GONE
                        previous.visibility = View.GONE
                        timetableTitle.text = getString(R.string.teachers_title)
                        currentTeachers = teachers
                        showTeacherTabs()
                    }
                    preferences.saveTimetable()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                super.onFailure(call, e)
                toast(R.string.toast_unknown_error)
            }
        })
    }

    /**
     * Shows the teacher tabs and displays the selected tab content.
     * Resets the classroom index when the screen reopens.
     */
    private fun showTeacherTabs() {
        classroomIndex = null
        binding.teacherTabs.visibility = View.VISIBLE
        binding.teacherTabs.removeOnTabSelectedListener(teacherTabListener)
        binding.teacherTabs.selectTab(binding.teacherTabs.getTabAt(selectedTeacherTab))
        binding.teacherTabs.addOnTabSelectedListener(teacherTabListener)
        showTeacherTab()
    }

    private val teacherTabListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            selectedTeacherTab = tab.position
            showTeacherTab()
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {}
        override fun onTabReselected(tab: TabLayout.Tab) {}
    }

    /**
     * Switches between the teachers list and the classroom list.
     */
    private fun showTeacherTab() {
        if (selectedTeacherTab == 0) {
            binding.classroomSearch.visibility = View.GONE
            binding.timetable.adapter = currentTeachers?.let {
                TeachersListAdapter(this@TimetableFragment, it)
            }
            return
        }
        binding.classroomSearch.visibility = View.VISIBLE
        binding.classroomDay.text = getString(R.string.classroom_day, todayTitle())
        showClassroomList()
    }

    /**
     * Shows all classrooms with their today lessons.
     */
    private fun showClassroomList() {
        if (_binding == null) return

        val index = classroomIndex
        if (index == null) {
            binding.classroomEmpty.visibility = View.VISIBLE
            binding.classroomEmpty.text = getString(R.string.classroom_loading)
            binding.timetable.adapter = null
            classroomIndex = ClassroomIndex(
                Preferences.branch.id,
                onReady = {
                    activity?.runOnUiThread {
                        if (_binding != null && selectedTeacherTab == 1) showClassroomList()
                    }
                },
                onError = {
                    activity?.runOnUiThread {
                        if (_binding == null) return@runOnUiThread
                        binding.classroomEmpty.visibility = View.VISIBLE
                        binding.classroomEmpty.text = getString(R.string.classroom_not_found)
                        binding.timetable.adapter = null
                    }
                }
            )
            return
        }

        if (!index.ready) {
            binding.classroomEmpty.visibility = View.VISIBLE
            binding.classroomEmpty.text = getString(R.string.classroom_loading)
            binding.timetable.adapter = null
            return
        }

        val day = todayTitle()
        val sections = index.classrooms(day).map { classroom ->
            ClassroomSection(classroom, index.lessonsFor(classroom, day))
        }
        if (sections.isEmpty()) {
            binding.classroomEmpty.visibility = View.VISIBLE
            binding.classroomEmpty.text = getString(R.string.classroom_not_found)
            binding.timetable.adapter = null
        } else {
            binding.classroomEmpty.visibility = View.GONE
            binding.timetable.adapter = ClassroomListAdapter(sections)
        }
    }

    /**
     * Returns the Russian weekday title used by the API ("Пн".."Вс").
     */
    private fun todayTitle(): String {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return arrayOf("", "Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")[day]
    }

    /**
     * Shows a dialog with the available themes.
     * Applies and recreates the activity on selection.
     */
    private fun showThemeDialog() {
        val options = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_dark),
            getString(R.string.theme_oled)
        )
        val values = arrayOf("default", "dark", "oled")
        val checked = values.indexOf(Preferences.currentTheme).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.theme_title)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                Preferences.currentTheme = values[which].also {
                    preferences.saveTheme()
                }
                dialog.dismiss()
                requireActivity().recreate()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun toast(resId: Int) {
        requireActivity().runOnUiThread {
            Toast.makeText(
                requireContext(), resId, Toast.LENGTH_SHORT
            ).show()
        }
    }
}
package com.example.gradetrack

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * Shared bottom-navigation click handling and styling for faculty screens.
 */
object FacultyBottomNavHelper {

    enum class Tab { HOME, STUDENTS, SUBJECTS, GRADES, SCHEDULE, PROFILE }

    fun setup(activity: Activity, facultyId: String?, current: Tab?) {
        bind(activity, R.id.btnFacultyHome, Tab.HOME, FacultyDashboardActivity::class.java, facultyId, current)
        bind(activity, R.id.btnManageStudents, Tab.STUDENTS, ManageStudentsActivity::class.java, facultyId, current)
        bind(activity, R.id.btnManageSubjects, Tab.SUBJECTS, ManageSubjectsActivity::class.java, facultyId, current)
        bind(activity, R.id.btnManageGrade, Tab.GRADES, GradeManagementActivity::class.java, facultyId, current)
        bind(activity, R.id.btnManageSchedule, Tab.SCHEDULE, ManageScheduleActivity::class.java, facultyId, current)
        bind(activity, R.id.btnFacultyProfile, Tab.PROFILE, FacultyProfileActivity::class.java, facultyId, current)
    }

    private fun bind(
        activity: Activity,
        viewId: Int,
        tab: Tab,
        target: Class<*>,
        facultyId: String?,
        current: Tab?
    ) {
        val view = activity.findViewById<View>(viewId) ?: return

        val activeColor = activity.getColor(R.color.primary)
        val inactiveColor = activity.getColor(R.color.text_secondary)
        val isSelected = (tab == current)

        val vg = view as? ViewGroup
        val iv = vg?.getChildAt(0) as? ImageView
        val tv = vg?.getChildAt(1) as? TextView

        if (iv != null) {
            iv.imageTintList = ColorStateList.valueOf(if (isSelected) activeColor else inactiveColor)
            iv.alpha = if (isSelected) 1.0f else 0.75f
        }
        if (tv != null) {
            tv.setTextColor(if (isSelected) activeColor else inactiveColor)
            tv.alpha = if (isSelected) 1.0f else 0.75f
        }

        view.setOnClickListener {
            if (tab == current) return@setOnClickListener

            val intent = Intent(activity, target).apply {
                putExtra("FACULTY_ID", facultyId)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            activity.startActivity(intent)
            activity.overridePendingTransition(0, 0)

            if (current != null) activity.finish()
        }
    }
}

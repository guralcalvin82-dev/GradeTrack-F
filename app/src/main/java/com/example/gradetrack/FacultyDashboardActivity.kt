package com.example.gradetrack

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FacultyDashboardActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var facultyId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faculty_dashboard)

        dbHelper = DatabaseHelper(this)
        facultyId = intent.getStringExtra("FACULTY_ID")?.trim().orEmpty()

        if (facultyId.isEmpty()) {
            Toast.makeText(this, "Faculty ID missing. Please log in again.", Toast.LENGTH_LONG).show()
            logout()
            return
        }

        FacultyBottomNavHelper.setup(this, facultyId, FacultyBottomNavHelper.Tab.HOME)

        loadFacultyHomeData()

        findViewById<View>(R.id.btnLogout).setOnClickListener { logout() }

        // Quick Shortcuts
        findViewById<View>(R.id.btnQuickStudents)?.setOnClickListener { startTab(ManageStudentsActivity::class.java) }
        findViewById<View>(R.id.btnQuickSubjects)?.setOnClickListener { startTab(ManageSubjectsActivity::class.java) }
        findViewById<View>(R.id.btnQuickGrades)?.setOnClickListener { startTab(GradeManagementActivity::class.java) }
        findViewById<View>(R.id.btnQuickSchedule)?.setOnClickListener { startTab(ManageScheduleActivity::class.java) }
        findViewById<View>(R.id.btnQuickProfile)?.setOnClickListener { startTab(FacultyProfileActivity::class.java) }
    }

    private fun loadFacultyHomeData() {
        val name = dbHelper.getUserName(facultyId)
        if (name.isNotBlank()) {
            findViewById<TextView>(R.id.tvWelcomeFaculty)?.text = "Welcome, $name"
        }

        val classes = dbHelper.getClassesByFaculty(facultyId)
        val handledCount = classes.map { it.subjectCode }.distinct().size
        val totalClasses = classes.size

        findViewById<TextView>(R.id.tvCountHandledSubjects)?.text = handledCount.toString()
        findViewById<TextView>(R.id.tvCountTotalClasses)?.text = totalClasses.toString()
    }

    private fun startTab(target: Class<*>) {
        val intent = Intent(this, target).apply {
            putExtra("FACULTY_ID", facultyId)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun logout() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

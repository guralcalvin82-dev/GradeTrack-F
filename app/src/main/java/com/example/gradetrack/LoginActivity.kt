package com.example.gradetrack

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = DatabaseHelper(this)

        val etUserId = findViewById<TextInputEditText>(R.id.etUserId)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val cbAgreement = findViewById<CheckBox>(R.id.cbAgreement)
        val tvReadTerms = findViewById<TextView>(R.id.tvReadTerms)
        val tvAgreementText = findViewById<TextView>(R.id.tvAgreementText)

        val showPrivacyDialog = {
            AlertDialog.Builder(this)
                .setTitle("Data Privacy Disclaimer & Consent")
                .setMessage(
                    "By using GradeTrack, you agree and consent to the collection, storage, and processing of your personal information, Student/Faculty ID, course, section, and academic grades within this application.\n\n" +
                    "Your data is securely managed by the system administrator and used strictly for academic tracking, scheduling, and grading purposes in accordance with data privacy standards."
                )
                .setPositiveButton("I Accept") { _, _ ->
                    cbAgreement.isChecked = true
                }
                .setNegativeButton("Close", null)
                .show()
        }

        tvReadTerms?.setOnClickListener { showPrivacyDialog() }
        tvAgreementText?.setOnClickListener { showPrivacyDialog() }

        btnLogin.setOnClickListener {
            val userId = etUserId.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (userId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter User ID and Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!cbAgreement.isChecked) {
                Toast.makeText(
                    this,
                    "Please check and accept the Data Privacy Agreement before logging in.",
                    Toast.LENGTH_LONG
                ).show()
                showPrivacyDialog()
                return@setOnClickListener
            }

            login(userId, password)
        }
    }

    private fun login(userId: String, password: String) {
        try {
            dbHelper.readableDatabase.rawQuery(
                """
            SELECT user_id, role, name
            FROM ${DatabaseHelper.TABLE_USERS}
            WHERE (LOWER(user_id) = LOWER(?)
                   OR LOWER(COALESCE(email, '')) = LOWER(?)
                   OR LOWER(name) = LOWER(?))
              AND password = ?
            LIMIT 1
            """.trimIndent(),
                arrayOf(userId, userId, userId, password)
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    Toast.makeText(this, "Invalid User ID or Password", Toast.LENGTH_SHORT).show()
                    return
                }
                val actualUserId = cursor.getString(0)
                val role = cursor.getString(1) ?: "STUDENT"
                val name = cursor.getString(2) ?: ""

                val intent = when {
                    role.equals("ADMIN", ignoreCase = true) -> {
                        Toast.makeText(this, "Welcome Admin $name", Toast.LENGTH_LONG).show()
                        Intent(this, AdminDashboardActivity::class.java)
                            .putExtra("ADMIN_ID", actualUserId)
                    }
                    role.equals("FACULTY", ignoreCase = true) -> {
                        Toast.makeText(
                            this,
                            "Welcome ${facultyTitle(actualUserId)} $name",
                            Toast.LENGTH_LONG
                        ).show()
                        Intent(this, FacultyDashboardActivity::class.java)
                            .putExtra("FACULTY_ID", actualUserId)
                    }
                    else -> {
                        Intent(this, StudentDashboardActivity::class.java)
                            .putExtra("STUDENT_ID", actualUserId)
                    }
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        } catch (e: Exception) {
            Log.e("Login", "Login failed", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun facultyTitle(facultyId: String): String =
        if (facultyId in setOf("FAC2026-002", "FAC2026-005")) "Ma'am" else "Sir"

}

package com.example.gradetrack

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class FacultyProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var facultyId: String = ""

    private var ivFacultyProfilePic: ImageView? = null
    private var tvAvatar: TextView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // fallback
            }
            val uriString = it.toString()
            val success = dbHelper.updateUserProfileImage(facultyId, uriString)
            if (success) {
                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                displayImage(uriString)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faculty_profile)

        dbHelper = DatabaseHelper(this)

        facultyId = intent.getStringExtra("FACULTY_ID")?.trim().orEmpty()
        if (facultyId.isEmpty()) {
            Toast.makeText(this, "Faculty ID missing. Please log in again.", Toast.LENGTH_LONG).show()
            logout()
            return
        }

        FacultyBottomNavHelper.setup(this, facultyId, FacultyBottomNavHelper.Tab.PROFILE)

        ivFacultyProfilePic = findViewById(R.id.ivFacultyProfilePic)
        tvAvatar = findViewById(R.id.tvAvatar)

        findViewById<TextView>(R.id.tvFacultyId)?.text = facultyId

        // Photo Picker Listeners
        findViewById<View>(R.id.cardProfilePic)?.setOnClickListener { pickImageLauncher.launch("image/*") }
        findViewById<View>(R.id.tvChangePhoto)?.setOnClickListener { pickImageLauncher.launch("image/*") }

        findViewById<View>(R.id.btnChangePassword)?.setOnClickListener { showChangePasswordDialog() }
        findViewById<View>(R.id.btnLogout).setOnClickListener { logout() }

        loadFacultyProfile()
    }

    private fun showChangePasswordDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etCurrentPass = EditText(this).apply {
            hint = "Current Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etNewPass = EditText(this).apply {
            hint = "New Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etConfirmPass = EditText(this).apply {
            hint = "Confirm New Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(etCurrentPass)
        layout.addView(etNewPass)
        layout.addView(etConfirmPass)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val currentPass = etCurrentPass.text.toString().trim()
                val newPass = etNewPass.text.toString().trim()
                val confirmPass = etConfirmPass.text.toString().trim()

                if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                    Toast.makeText(this, "Please fill in all password fields.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPass != confirmPass) {
                    Toast.makeText(this, "New passwords do not match.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val db = dbHelper.readableDatabase
                var isCurrentValid = false
                db.rawQuery("SELECT 1 FROM ${DatabaseHelper.TABLE_USERS} WHERE user_id = ? AND password = ?", arrayOf(facultyId, currentPass)).use { c ->
                    isCurrentValid = c.moveToFirst()
                }

                if (!isCurrentValid) {
                    Toast.makeText(this, "Current password is incorrect.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val values = ContentValues().apply {
                    put("password", newPass)
                }

                val updated = dbHelper.writableDatabase.update(
                    DatabaseHelper.TABLE_USERS,
                    values,
                    "user_id = ?",
                    arrayOf(facultyId)
                ) > 0

                if (updated) {
                    Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to update password.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (facultyId.isNotEmpty()) {
            loadFacultyProfile()
        }
    }

    private fun loadFacultyProfile() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT u.name, 
                   COALESCE(u.email, t.email, ''), 
                   COALESCE(NULLIF(u.department, ''), t.department, u.course, 'Information Systems'), 
                   COALESCE(u.course, ''), 
                   COALESCE(NULLIF(u.position, ''), t.position, 'Instructor'),
                   COALESCE(u.contact_num, ''),
                   COALESCE(u.address, ''),
                   COALESCE(u.image_uri, '')
            FROM ${DatabaseHelper.TABLE_USERS} u
            LEFT JOIN ${DatabaseHelper.TABLE_TEACHERS} t ON LOWER(t.teacher_id) = LOWER(u.user_id)
            WHERE LOWER(u.user_id) = LOWER(?)
            """.trimIndent(),
            arrayOf(facultyId)
        )
        if (cursor.moveToFirst()) {
            val name = cursor.getString(0) ?: ""
            val email = cursor.getString(1) ?: ""
            val dept = cursor.getString(2) ?: ""
            val course = cursor.getString(3) ?: ""
            val pos = cursor.getString(4) ?: ""
            val contactNum = cursor.getString(5) ?: ""
            val address = cursor.getString(6) ?: ""
            val imageUri = cursor.getString(7) ?: ""

            val finalDepartment = if (dept.isNotBlank()) dept else course.ifBlank { "Information Systems" }
            val finalPosition = if (pos.isNotBlank()) pos else "Instructor"

            if (name.isNotBlank()) {
                findViewById<TextView>(R.id.tvFullName)?.text = name
                val initials = name.split(" ")
                    .filter { it.isNotBlank() }
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                    .take(2)
                tvAvatar?.text = initials.ifBlank { name.take(2).uppercase() }
            }
            findViewById<TextView>(R.id.tvFacultyId)?.text = facultyId
            findViewById<TextView>(R.id.tvEmail)?.text = email.ifBlank { "N/A" }
            findViewById<TextView>(R.id.tvDepartment)?.text = finalDepartment
            findViewById<TextView>(R.id.tvPosition)?.text = finalPosition
            findViewById<TextView>(R.id.tvContactNum)?.text = contactNum.ifBlank { "N/A" }
            findViewById<TextView>(R.id.tvAddress)?.text = address.ifBlank { "N/A" }

            val roleSubtitle = "$finalPosition • $finalDepartment"
            findViewById<TextView>(R.id.tvFacultyRole)?.text = roleSubtitle

            displayImage(imageUri)
        }
        cursor.close()
    }

    private fun displayImage(imageUri: String) {
        if (imageUri.isNotBlank()) {
            try {
                ivFacultyProfilePic?.setImageURI(Uri.parse(imageUri))
                ivFacultyProfilePic?.visibility = View.VISIBLE
                tvAvatar?.visibility = View.GONE
            } catch (e: Exception) {
                ivFacultyProfilePic?.visibility = View.GONE
                tvAvatar?.visibility = View.VISIBLE
            }
        } else {
            ivFacultyProfilePic?.visibility = View.GONE
            tvAvatar?.visibility = View.VISIBLE
        }
    }

    private fun logout() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

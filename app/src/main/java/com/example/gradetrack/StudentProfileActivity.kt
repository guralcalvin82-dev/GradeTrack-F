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
import com.google.android.material.button.MaterialButton

class StudentProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var studentId: String = ""

    private lateinit var ivStudentProfilePic: ImageView
    private lateinit var tvAvatarInitials: TextView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // fallback
            }
            val uriString = it.toString()
            val success = dbHelper.updateUserProfileImage(studentId, uriString)
            if (success) {
                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                displayImage(uriString)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_profile)

        dbHelper = DatabaseHelper(this)
        studentId = intent.getStringExtra("STUDENT_ID") ?: ""

        ivStudentProfilePic = findViewById(R.id.ivStudentProfilePic)
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials)

        // Bottom navigation bar (Home / Grades / Schedule / Profile)
        BottomNavHelper.setup(this, studentId, BottomNavHelper.Tab.PROFILE)

        // Photo Picker Listeners
        findViewById<View>(R.id.cardProfilePic).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        findViewById<View>(R.id.tvChangePhoto).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<View>(R.id.btnEditProfile)?.setOnClickListener {
            showEditProfileDialog()
        }

        findViewById<View>(R.id.btnChangePassword)?.setOnClickListener {
            showChangePasswordDialog()
        }

        // Handle Logout
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        loadStudentProfile()
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

                // Verify current password
                val db = dbHelper.readableDatabase
                var isCurrentValid = false
                db.rawQuery("SELECT 1 FROM ${DatabaseHelper.TABLE_USERS} WHERE user_id = ? AND password = ?", arrayOf(studentId, currentPass)).use { c ->
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
                    arrayOf(studentId)
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

    private fun showEditProfileDialog() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT name, COALESCE(email,''), COALESCE(contact_num,''), COALESCE(address,'') FROM ${DatabaseHelper.TABLE_USERS} WHERE user_id = ?",
            arrayOf(studentId)
        )

        var curName = ""
        var curEmail = ""
        var curContact = ""
        var curAddress = ""

        if (cursor.moveToFirst()) {
            curName = cursor.getString(0) ?: ""
            curEmail = cursor.getString(1) ?: ""
            curContact = cursor.getString(2) ?: ""
            curAddress = cursor.getString(3) ?: ""
        }
        cursor.close()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etName = EditText(this).apply {
            hint = "Full Name"
            setText(curName)
        }
        val etEmail = EditText(this).apply {
            hint = "Email Address"
            setText(curEmail)
        }
        val etContact = EditText(this).apply {
            hint = "Contact Number"
            setText(curContact)
        }
        val etAddress = EditText(this).apply {
            hint = "Home Address"
            setText(curAddress)
        }

        layout.addView(etName)
        layout.addView(etEmail)
        layout.addView(etContact)
        layout.addView(etAddress)

        AlertDialog.Builder(this)
            .setTitle("Edit Personal Information")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim()
                val newEmail = etEmail.text.toString().trim()
                val newContact = etContact.text.toString().trim()
                val newAddress = etAddress.text.toString().trim()

                if (newName.isBlank()) {
                    Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val values = ContentValues().apply {
                    put("name", newName)
                    put("email", newEmail)
                    put("contact_num", newContact)
                    put("address", newAddress)
                }

                val updated = dbHelper.writableDatabase.update(
                    DatabaseHelper.TABLE_USERS,
                    values,
                    "user_id = ?",
                    arrayOf(studentId)
                ) > 0

                if (updated) {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    loadStudentProfile()
                } else {
                    Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadStudentProfile() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT name, user_id, COALESCE(course,''), COALESCE(section,''), COALESCE(birthdate,''), COALESCE(address,''), COALESCE(email,''), COALESCE(contact_num,''), COALESCE(department,''), COALESCE(image_uri,'') FROM ${DatabaseHelper.TABLE_USERS} WHERE user_id = ?",
            arrayOf(studentId)
        )

        if (cursor.moveToFirst()) {
            val name = cursor.getString(0) ?: ""
            val userId = cursor.getString(1) ?: ""
            val course = cursor.getString(2) ?: ""
            val section = cursor.getString(3) ?: ""
            val birthdate = cursor.getString(4) ?: ""
            val address = cursor.getString(5) ?: ""
            val email = cursor.getString(6) ?: ""
            val contactNum = cursor.getString(7) ?: ""
            val department = cursor.getString(8) ?: ""
            val imageUri = cursor.getString(9) ?: ""

            // 1. Overview Header
            findViewById<TextView>(R.id.tvHeaderName).text = name
            val courseSectionText = listOf(course, section).filter { it.isNotBlank() }.joinToString(" • ")
            findViewById<TextView>(R.id.tvHeaderCourse).text = courseSectionText

            // 2. Profile Details
            findViewById<TextView>(R.id.tvName).text = name
            findViewById<TextView>(R.id.tvStudentId).text = userId

            val courseDeptText = if (department.isNotBlank()) "$course ($department)" else course
            findViewById<TextView>(R.id.tvCourse).text = courseDeptText.ifBlank { "N/A" }

            findViewById<TextView>(R.id.tvContactNum).text = contactNum.ifBlank { "N/A" }
            findViewById<TextView>(R.id.tvAddress).text = address.ifBlank { "N/A" }
            findViewById<TextView>(R.id.tvEmail).text = email.ifBlank { "N/A" }

            if (name.isNotBlank()) {
                val initials = name.split(" ")
                    .filter { it.isNotBlank() }
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                    .take(2)
                tvAvatarInitials.text = initials.ifBlank { name.take(2).uppercase() }
            }

            displayImage(imageUri)
        }
        cursor.close()
    }

    private fun displayImage(imageUri: String) {
        if (imageUri.isNotBlank()) {
            try {
                ivStudentProfilePic.setImageURI(Uri.parse(imageUri))
                ivStudentProfilePic.visibility = View.VISIBLE
                tvAvatarInitials.visibility = View.GONE
            } catch (e: Exception) {
                ivStudentProfilePic.visibility = View.GONE
                tvAvatarInitials.visibility = View.VISIBLE
            }
        } else {
            ivStudentProfilePic.visibility = View.GONE
            tvAvatarInitials.visibility = View.VISIBLE
        }
    }
}

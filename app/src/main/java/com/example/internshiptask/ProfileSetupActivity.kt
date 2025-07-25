package com.example.internshiptask

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.InputStream

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var nameET: TextInputEditText
    private lateinit var bioET: TextInputEditText
    private lateinit var profileImageView: ImageView
    private lateinit var selectImageBtn: Button
    private lateinit var saveProfileBtn: MaterialButton

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                Glide.with(this).load(selectedImageUri).into(profileImageView)
            } else {
                Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        nameET = findViewById(R.id.nameEditText)
        bioET = findViewById(R.id.bioEditText)
        profileImageView = findViewById(R.id.profileImageView)
        selectImageBtn = findViewById(R.id.selectImageBtn)
        saveProfileBtn = findViewById(R.id.saveProfileBtn)

        selectImageBtn.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .galleryOnly()
                .compress(1024)
                .maxResultSize(512, 512)
                .createIntent { intent -> imagePickerLauncher.launch(intent) }
        }

        saveProfileBtn.setOnClickListener {
            saveProfile()
        }
    }

    private fun convertImageToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveProfile() {
        val name = nameET.text.toString().trim()
        val bio = bioET.text.toString().trim()
        val uid = auth.currentUser?.uid ?: return

        if (name.isEmpty()) {
            nameET.error = "Name required"
            nameET.requestFocus()
            return
        }

        if (bio.isEmpty()) {
            bioET.error = "Bio required"
            bioET.requestFocus()
            return
        }

        saveProfileBtn.isEnabled = false

        val imageBase64 = selectedImageUri?.let { convertImageToBase64(it) } ?: ""

        val userMap = hashMapOf(
            "name" to name,
            "bio" to bio,
            "profileImageBase64" to imageBase64
        )

        firestore.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving profile: ${it.message}", Toast.LENGTH_LONG).show()
                saveProfileBtn.isEnabled = true
            }
    }
}

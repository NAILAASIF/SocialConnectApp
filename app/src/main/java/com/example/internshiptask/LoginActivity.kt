package com.example.internshiptask

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var emailET: TextInputEditText
    private lateinit var passwordET: TextInputEditText
    private lateinit var loginBtn: MaterialButton
    private lateinit var rememberMeCheckBox: CheckBox
    private lateinit var forgotPassBtn: Button
    private lateinit var signupBtn: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var loadingDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        sharedPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        // Setup loading dialog
        setupLoadingDialog()

        // UI references
        emailET = findViewById(R.id.UsernameET)
        passwordET = findViewById(R.id.PasswordET)
        loginBtn = findViewById(R.id.GoBtn)
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox)
        forgotPassBtn = findViewById(R.id.ForgotPassbtn)
        signupBtn = findViewById(R.id.signup_screenbtn)

        loadSavedLogin()

        val prefilledEmail = intent.getStringExtra("email")
        if (!prefilledEmail.isNullOrEmpty()) {
            emailET.setText(prefilledEmail)
            passwordET.requestFocus()
        }

        loginBtn.setOnClickListener { loginUser() }

        signupBtn.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        forgotPassBtn.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            intent.putExtra("email", emailET.text.toString().trim())
            startActivity(intent)
        }
    }

    private fun loginUser() {
        val email = emailET.text.toString().trim()
        val password = passwordET.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailET.error = getString(R.string.invalid_email)
            emailET.requestFocus()
            return
        }

        loadingDialog.show()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                loadingDialog.dismiss()
                if (task.isSuccessful) {

                    if (rememberMeCheckBox.isChecked) {
                        saveLogin(email, password)
                    } else {
                        clearSavedLogin()
                    }
                    postLoginNavigation()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun postLoginNavigation() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())

            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name")
                    val bio = document.getString("bio")
                    val profileImage = document.getString("profileImageBase64")

                    val intent = if (!name.isNullOrEmpty() && !bio.isNullOrEmpty() && !profileImage.isNullOrEmpty()) {
                        Intent(this, MainActivity::class.java)
                    } else {
                        Intent(this, ProfileSetupActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun saveLogin(email: String, password: String) {
        sharedPrefs.edit().apply {
            putString("email", email)
            putString("password", password)
            putBoolean("remember", true)
            apply()
        }
    }

    private fun loadSavedLogin() {
        val remember = sharedPrefs.getBoolean("remember", false)
        if (remember) {
            emailET.setText(sharedPrefs.getString("email", "")?.trim())
            passwordET.setText(sharedPrefs.getString("password", "") ?: "")
            rememberMeCheckBox.isChecked = true
        }
    }

    private fun clearSavedLogin() {
        sharedPrefs.edit().clear().apply()
    }

    private fun setupLoadingDialog() {
        loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
    }
}

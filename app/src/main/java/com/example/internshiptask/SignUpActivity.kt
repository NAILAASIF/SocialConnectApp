package com.example.internshiptask

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var fullnameET: TextInputEditText
    private lateinit var usernameET: TextInputEditText
    private lateinit var emailET: TextInputEditText
    private lateinit var phoneET: TextInputEditText
    private lateinit var passwordET: TextInputEditText
    private lateinit var confirmPasswordET: TextInputEditText
    private lateinit var signupBtn: MaterialButton
    private lateinit var loginBtn: Button
    private lateinit var loadingDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        setupLoadingDialog()

        // Initialize UI
        fullnameET = findViewById(R.id.fullnameET)
        usernameET = findViewById(R.id.usernameET)
        emailET = findViewById(R.id.emailET)
        phoneET = findViewById(R.id.phoneET)
        passwordET = findViewById(R.id.passwordET)
        confirmPasswordET = findViewById(R.id.confirmPasswordET)
        signupBtn = findViewById(R.id.SignupBtn)
        loginBtn = findViewById(R.id.loginBtn)

        signupBtn.setOnClickListener { registerUser() }
        loginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupLoadingDialog() {
        loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
    }

    private fun registerUser() {
        val fullname = fullnameET.text.toString().trim()
        val username = usernameET.text.toString().trim()
        val email = emailET.text.toString().trim()
        val phone = phoneET.text.toString().trim()
        val password = passwordET.text.toString().trim()
        val confirmPassword = confirmPasswordET.text.toString().trim()

        if (fullname.isEmpty() || username.isEmpty() || email.isEmpty() ||
            phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailET.error = getString(R.string.invalid_email)
            emailET.requestFocus()
            return
        }

        if (password.length < 6 ||
            !password.matches(Regex(".*[A-Z].*")) ||
            !password.matches(Regex(".*\\d.*")) ||
            !password.matches(Regex(".*[@#\$%^&+=].*"))) {
            passwordET.error = getString(R.string.password_strength_error)
            passwordET.requestFocus()
            return
        }

        if (password != confirmPassword) {
            confirmPasswordET.error = getString(R.string.passwords_do_not_match)
            confirmPasswordET.requestFocus()
            return
        }

        loadingDialog.show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                loadingDialog.dismiss()
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val userMap = hashMapOf(
                        "uid" to uid,
                        "name" to fullname,
                        "username" to username,
                        "email" to email,
                        "phone" to phone,
                        "bio" to "",
                        "profileImageUrl" to "" // Use Firebase Storage later
                    )

                    FirebaseFirestore.getInstance().collection("users")
                        .document(uid)
                        .set(userMap)
                        .addOnSuccessListener {
                            auth.currentUser?.sendEmailVerification()
                            Toast.makeText(this, "Sign Up Successful! Verify your email.", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                        }

                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}

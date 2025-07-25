package com.example.internshiptask

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var sendResetButton: MaterialButton
    private lateinit var backToLoginButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.etEmail)
        sendResetButton = findViewById(R.id.btnSendReset)
        backToLoginButton = findViewById(R.id.btnBackToLogin)

        val incomingEmail = intent.getStringExtra("email")
        if (!incomingEmail.isNullOrEmpty()) {
            emailEditText.setText(incomingEmail)
        }

        sendResetButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.error = "Enter a valid email"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            val progressDialog = ProgressDialog(this)
            progressDialog.setMessage("Sending reset link...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Reset link sent! Check your email.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        backToLoginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}

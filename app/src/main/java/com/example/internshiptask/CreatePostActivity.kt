package com.example.internshiptask

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class CreatePostActivity : AppCompatActivity() {

    private lateinit var postText: EditText
    private lateinit var postImage: ImageView
    private lateinit var selectImageBtn: Button
    private lateinit var postBtn: Button
    private var selectedImageUri: Uri? = null
    private var progressDialog: ProgressDialog? = null

    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                selectedImageUri = it.data?.data
                postImage.setImageURI(selectedImageUri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        postText = findViewById(R.id.postContentEditText)
        postImage = findViewById(R.id.postImageView)
        selectImageBtn = findViewById(R.id.selectImageBtn)
        postBtn = findViewById(R.id.uploadPostBtn)

        selectImageBtn.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .galleryOnly()
                .createIntent { intent -> imagePickerLauncher.launch(intent) }
        }

        postBtn.setOnClickListener {
            uploadPost()
        }
    }

    private fun uploadPost() {
        val content = postText.text.toString().trim()
        if (content.isEmpty()) {
            postText.error = "Post content required"
            return
        }

        postBtn.isEnabled = false
        showProgressDialog("Uploading post...")

        if (selectedImageUri != null) {
            uploadImageToImgBB(selectedImageUri!!) { imageUrl ->
                if (imageUrl.isNotEmpty()) {
                    savePost(content, imageUrl)
                } else {
                    hideProgressDialog()
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                    postBtn.isEnabled = true
                }
            }
        } else {
            savePost(content, "")
        }
    }

    private fun uploadImageToImgBB(uri: Uri, callback: (String) -> Unit) {
        val apiKey = "2841feea1d89b556ef3d120fe765ae4c"
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val fileBytes = contentResolver.openInputStream(uri)?.readBytes()
        if (fileBytes == null) {
            callback("")
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "post.jpg",
                RequestBody.create("image/*".toMediaTypeOrNull(), fileBytes)
            )
            .build()

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload?key=$apiKey")
            .post(requestBody)
            .build()

        val call = client.newCall(request)

        // Cancel on dismiss
        progressDialog?.setOnCancelListener {
            call.cancel()
            Toast.makeText(this, "Upload canceled", Toast.LENGTH_SHORT).show()
            postBtn.isEnabled = true
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideProgressDialog()
                    callback("")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string()
                try {
                    val json = JSONObject(resStr ?: "")
                    val imageUrl = json.getJSONObject("data").getString("url")
                    runOnUiThread { callback(imageUrl) }
                } catch (e: Exception) {
                    runOnUiThread {
                        hideProgressDialog()
                        callback("")
                    }
                }
            }
        })
    }
    private fun savePost(content: String, imageUrl: String) {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        userRef.get().addOnSuccessListener { userDoc ->
            val userName = userDoc.getString("name") ?: FirebaseAuth.getInstance().currentUser?.email ?: "User"
            val profileImage = userDoc.getString("profileImageBase64") ?: ""

            val postId = UUID.randomUUID().toString()
            val postMap = mapOf(
                "id" to postId,
                "userId" to uid,
                "userName" to userName,
                "profileImage" to profileImage,
                "content" to content,
                "imageUrl" to imageUrl,
                "timestamp" to Timestamp.now(),
                "likedBy" to listOf<String>()
            )

            FirebaseFirestore.getInstance().collection("posts")
                .document(postId)
                .set(postMap)
                .addOnSuccessListener {
                    hideProgressDialog()
                    Toast.makeText(this, "Post uploaded successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    Toast.makeText(this, "Failed to save post", Toast.LENGTH_SHORT).show()
                    postBtn.isEnabled = true
                }
        }
    }



    private fun showProgressDialog(message: String) {
        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage(message)
        progressDialog?.setCancelable(true)
        progressDialog?.show()
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
    }
}

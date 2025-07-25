package com.example.internshiptask

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.internshiptask.adapters.PostAdapter
import com.example.internshiptask.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileBio: TextView
    private lateinit var userPostsRecyclerView: RecyclerView
    private lateinit var noPostsTV: TextView
    private lateinit var editProfileBtn: Button
    private lateinit var followBtn: Button
    private lateinit var chatIcon: ImageView
    private lateinit var headerTitle: TextView

    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private lateinit var userIdToLoad: String
    private var receiverName: String = ""
    private var receiverImageBase64: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // ✅ Initialize Views
        headerTitle = findViewById(R.id.headerTitle)
        chatIcon = findViewById(R.id.chatIcon)
        profileImage = findViewById(R.id.profileImage)
        profileName = findViewById(R.id.profileName)
        profileBio = findViewById(R.id.profileBio)
        userPostsRecyclerView = findViewById(R.id.userPostsRecyclerView)
        noPostsTV = findViewById(R.id.noPostsTextView)
        editProfileBtn = findViewById(R.id.editProfileBtn)
        followBtn = findViewById(R.id.followButton)

        headerTitle.text = "Profile"

        // ✅ Get userId from Intent or fallback to current user
        userIdToLoad = intent.getStringExtra("userId") ?: currentUid

        if (userIdToLoad.isEmpty()) {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ✅ Setup RecyclerView
        postAdapter = PostAdapter(this, postList) { userId ->
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
        userPostsRecyclerView.layoutManager = LinearLayoutManager(this)
        userPostsRecyclerView.adapter = postAdapter

        // ✅ Show buttons based on profile type
        if (userIdToLoad == currentUid) {
            editProfileBtn.visibility = View.VISIBLE
            followBtn.visibility = View.GONE
            chatIcon.visibility = View.GONE
            editProfileBtn.setOnClickListener {
                startActivity(Intent(this, ProfileSetupActivity::class.java))
            }
        } else {
            editProfileBtn.visibility = View.GONE
            followBtn.visibility = View.VISIBLE
            chatIcon.visibility = View.VISIBLE
            checkFollowStatus()
            followBtn.setOnClickListener {
                if (followBtn.text == "Follow") followUser() else unfollowUser()
            }
        }

        // ✅ Chat Icon Click
        chatIcon.setOnClickListener {
            if (userIdToLoad != currentUid) {
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("receiverId", userIdToLoad)
                intent.putExtra("receiverName", receiverName)
                intent.putExtra("receiverImage", receiverImageBase64)
                startActivity(intent)
            } else {
                Toast.makeText(this, "You cannot chat with yourself", Toast.LENGTH_SHORT).show()
            }
        }

        loadUserProfile()
        loadUserPosts()
    }

    override fun onResume() {
        super.onResume()
        if (userIdToLoad != currentUid) {
            checkFollowStatus()
        }
    }

    private fun loadUserProfile() {
        db.collection("users").document(userIdToLoad)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (doc != null && doc.exists()) {
                    receiverName = doc.getString("name") ?: "No Name"
                    profileName.text = receiverName
                    profileBio.text = doc.getString("bio") ?: "No Bio"

                    receiverImageBase64 = doc.getString("profileImageBase64") ?: ""
                    if (receiverImageBase64.isNotEmpty()) {
                        try {
                            val bytes = Base64.decode(receiverImageBase64, Base64.DEFAULT)
                            Glide.with(this).load(bytes).circleCrop().into(profileImage)
                        } catch (e: Exception) {
                            profileImage.setImageResource(R.drawable.ic_launcher_foreground)
                        }
                    } else {
                        profileImage.setImageResource(R.drawable.ic_launcher_foreground)
                    }
                }
            }
    }

    private fun loadUserPosts() {
        db.collection("posts")
            .whereEqualTo("userId", userIdToLoad)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { snapshot ->
                postList.clear()
                snapshot.documents.forEach {
                    it.toObject(Post::class.java)?.let { postList.add(it) }
                }
                postAdapter.notifyDataSetChanged()
                noPostsTV.visibility = if (postList.isEmpty()) View.VISIBLE else View.GONE
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load posts", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkFollowStatus() {
        db.collection("users").document(userIdToLoad)
            .collection("followers").document(currentUid)
            .get()
            .addOnSuccessListener { doc ->
                followBtn.text = if (doc.exists()) "Unfollow" else "Follow"
            }
    }

    private fun followUser() {
        if (currentUid.isEmpty() || userIdToLoad.isEmpty()) {
            Toast.makeText(this, "Invalid user IDs", Toast.LENGTH_SHORT).show()
            return
        }

        followBtn.isEnabled = false

        // Get current user details
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { currentUserDoc ->
                val currentUserName = currentUserDoc.getString("name") ?: "Unknown"

                // Get the profile user details
                db.collection("users").document(userIdToLoad).get()
                    .addOnSuccessListener { profileUserDoc ->
                        val profileUserName = profileUserDoc.getString("name") ?: "Unknown"

                        val timestamp = System.currentTimeMillis()

                        val followerData = mapOf("uid" to currentUid, "name" to currentUserName, "timestamp" to timestamp)
                        val followingData = mapOf("uid" to userIdToLoad, "name" to profileUserName, "timestamp" to timestamp)

                        db.collection("users").document(userIdToLoad)
                            .collection("followers").document(currentUid).set(followerData)
                            .addOnSuccessListener {
                                db.collection("users").document(currentUid)
                                    .collection("following").document(userIdToLoad).set(followingData)
                                    .addOnSuccessListener {
                                        followBtn.text = "Unfollow"
                                        followBtn.isEnabled = true
                                        Toast.makeText(this, "Followed successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        followBtn.isEnabled = true
                                    }
                            }
                            .addOnFailureListener {
                                followBtn.isEnabled = true
                            }
                    }
            }
    }

    private fun unfollowUser() {
        followBtn.isEnabled = false
        db.collection("users").document(userIdToLoad)
            .collection("followers").document(currentUid).delete()
            .addOnSuccessListener {
                db.collection("users").document(currentUid)
                    .collection("following").document(userIdToLoad).delete()
                    .addOnSuccessListener {
                        followBtn.text = "Follow"
                        followBtn.isEnabled = true
                    }
            }.addOnFailureListener {
                followBtn.isEnabled = true
            }
    }
}

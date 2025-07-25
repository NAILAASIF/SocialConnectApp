package com.example.internshiptask

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.internshiptask.adapters.CommentAdapter
import com.example.internshiptask.models.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CommentActivity : AppCompatActivity() {

    private lateinit var commentBox: EditText
    private lateinit var sendBtn: Button
    private lateinit var commentRecyclerView: RecyclerView
    private lateinit var adapter: CommentAdapter
    private val commentList = mutableListOf<Comment>()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var postId: String
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        postId = intent.getStringExtra("postId") ?: return

        commentBox = findViewById(R.id.commentEditText)
        sendBtn = findViewById(R.id.sendCommentBtn)
        commentRecyclerView = findViewById(R.id.commentRecyclerView)

        adapter = CommentAdapter(commentList, postId)
        commentRecyclerView.layoutManager = LinearLayoutManager(this)
        commentRecyclerView.adapter = adapter

        loadComments()

        sendBtn.setOnClickListener {
            val text = commentBox.text.toString().trim()
            if (text.isNotEmpty()) {
                sendComment(text)
            } else {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendComment(text: String) {
        val commentRef = db.collection("posts").document(postId)
            .collection("comments").document()

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val userName = userDoc.getString("name") ?: FirebaseAuth.getInstance().currentUser?.email ?: "Anonymous"
            val profileImage = userDoc.getString("profileImageBase64") ?: ""

            val comment = Comment(
                id = commentRef.id,
                userId = uid,
                userName = userName,
                profileImage = profileImage,
                content = text,
                timestamp = System.currentTimeMillis()
            )

            commentRef.set(comment)
                .addOnSuccessListener {
                    commentBox.setText("")
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to send comment", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun loadComments() {
        db.collection("posts").document(postId)
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load comments", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    commentList.clear()
                    for (doc in snapshot) {
                        val comment = doc.toObject(Comment::class.java)
                        commentList.add(comment)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }
}

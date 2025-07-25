package com.example.internshiptask

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.internshiptask.adapters.UserAdapter
import com.example.internshiptask.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userList: ArrayList<User>
    private lateinit var userAdapter: UserAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)
        recyclerView = findViewById(R.id.recyclerViewUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        userList = ArrayList()
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        userAdapter = UserAdapter(userList) { selectedUser ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("receiverId", selectedUser.uid)
            intent.putExtra("receiverName", selectedUser.name)
            intent.putExtra("receiverImage", selectedUser.profileImageBase64) // ✅ FIXED
            startActivity(intent)
        }

        recyclerView.adapter = userAdapter
        loadUsers()
    }
    private fun loadUsers() {
        val currentUserId = auth.currentUser?.uid

        db.collection("users").get()
            .addOnSuccessListener { documents ->
                userList.clear()
                for (document in documents) {
                    val user = document.toObject(User::class.java)

                    // ✅ Set UID from Firestore doc ID
                    user.uid = document.id

                    android.util.Log.d("UserListActivity", "Fetched user: ${user.uid}, name: ${user.name}")

                    if (!user.uid.isNullOrEmpty() && user.uid != currentUserId) {
                        userList.add(user)
                    }
                }
                android.util.Log.d("UserListActivity", "Total Users Loaded: ${userList.size}")
                userAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                android.util.Log.e("UserListActivity", "Error loading users: ${it.message}")
            }
    }

}

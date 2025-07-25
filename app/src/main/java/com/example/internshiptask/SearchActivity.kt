package com.example.internshiptask

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.internshiptask.adapters.UserAdapter
import com.example.internshiptask.models.User
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : AppCompatActivity() {

    private lateinit var searchButton: ImageButton
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<User>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Initialize Views
        searchButton = findViewById(R.id.searchButton)
        searchEditText = findViewById(R.id.searchEditText)
        recyclerView = findViewById(R.id.recyclerViewSearch)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(userList) { selectedUser ->
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userId", selectedUser.uid) // ✅ Passing correct UID
            startActivity(intent)
        }
        recyclerView.adapter = userAdapter

        // ✅ Real-time listener for all users
        db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            val updatedUsers = snapshot.documents.mapNotNull {
                val user = it.toObject(User::class.java)
                user?.apply { uid = it.id }
            }

            userList.clear()
            userList.addAll(updatedUsers)
            userAdapter.notifyDataSetChanged()
        }

        // ✅ Search button click
        searchButton.setOnClickListener {
            val queryText = searchEditText.text.toString().trim()
            if (queryText.isNotEmpty()) {
                searchUsers(queryText)
            } else {
                Toast.makeText(this, "Enter a name to search", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Optional: Live filtering as you type
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString().trim()
                if (text.isNotEmpty()) {
                    searchUsers(text)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun searchUsers(query: String) {
        db.collection("users")
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .get()
            .addOnSuccessListener { snapshot ->
                val searchResults = snapshot.documents.mapNotNull {
                    val user = it.toObject(User::class.java)
                    user?.apply { uid = it.id }
                }

                userList.clear()
                userList.addAll(searchResults)
                userAdapter.notifyDataSetChanged()

                if (searchResults.isEmpty()) {
                    Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Search failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

package com.example.internshiptask

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.internshiptask.adapters.PostAdapter
import com.example.internshiptask.models.Post
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyTextView: TextView
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private val postList = mutableListOf<Post>()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ Toolbar
        val toolbar = findViewById<Toolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        recyclerView = findViewById(R.id.postRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        postAdapter = PostAdapter(this, postList) { userId ->
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        recyclerView.adapter = postAdapter

        progressBar = findViewById(R.id.loadingProgressBar)
        emptyTextView = findViewById(R.id.emptyTextView)
        shimmerLayout = findViewById(R.id.shimmerLayout)

        // ✅ Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    true
                }
                R.id.nav_create -> {
                    startActivity(Intent(this, CreatePostActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    FirebaseAuth.getInstance().currentUser?.uid?.let {
                        val intent = Intent(this, ProfileActivity::class.java)
                        intent.putExtra("userId", it)
                        startActivity(intent)
                    }
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_home

        listenForPosts()
    }

    private fun listenForPosts() {
        shimmerLayout.startShimmer()
        shimmerLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyTextView.visibility = View.GONE

        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Toast.makeText(this, "Error loading posts", Toast.LENGTH_SHORT).show()
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    return@addSnapshotListener
                }

                for (change in snapshot.documentChanges) {
                    val post = change.document.toObject(Post::class.java)
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            postList.add(0, post) // Add on top
                            postAdapter.notifyItemInserted(0)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val index = postList.indexOfFirst { it.id == post.id }
                            if (index != -1) {
                                postList[index] = post
                                postAdapter.notifyItemChanged(index)
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val index = postList.indexOfFirst { it.id == post.id }
                            if (index != -1) {
                                postList.removeAt(index)
                                postAdapter.notifyItemRemoved(index)
                            }
                        }
                    }
                }

                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                emptyTextView.visibility = if (postList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_chat -> {
                startActivity(Intent(this, UserListActivity::class.java))
                true
            }
            R.id.action_profile -> {
                FirebaseAuth.getInstance().currentUser?.uid?.let {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("userId", it)
                    startActivity(intent)
                }
                true
            }
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_home
    }
}

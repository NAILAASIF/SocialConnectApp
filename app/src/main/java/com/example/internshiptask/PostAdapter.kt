package com.example.internshiptask.adapters

import android.content.Context
import android.text.InputType
import android.util.Base64
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.internshiptask.CommentActivity
import com.example.internshiptask.R
import com.example.internshiptask.models.Post
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(
    private val context: Context,
    private val postList: MutableList<Post>,
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val firestore = FirebaseFirestore.getInstance()

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userImage: ImageView = view.findViewById(R.id.postUserImage)
        val userNameTV: TextView = view.findViewById(R.id.postUserName)
        val contentTV: TextView = view.findViewById(R.id.postContent)
        val timestampTV: TextView = view.findViewById(R.id.postTimestamp)
        val imageIV: ImageView = view.findViewById(R.id.postImage)
        val likeIcon: ImageView = view.findViewById(R.id.likeIcon)
        val likeCountTV: TextView = view.findViewById(R.id.likeCount)
        val commentIcon: ImageView = view.findViewById(R.id.commentIcon)
        val editBtn: ImageView = view.findViewById(R.id.editPostIcon)
        val deleteBtn: ImageView = view.findViewById(R.id.deletePostIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        holder.contentTV.text = post.content

        // ✅ Format timestamp
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        holder.timestampTV.text = try {
            sdf.format(post.timestamp.toDate())
        } catch (e: Exception) {
            "Unknown"
        }

        // ✅ Load Post Image
        if (post.imageUrl.isNotEmpty()) {
            holder.imageIV.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(post.imageUrl)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_error)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.imageIV)
        } else {
            holder.imageIV.visibility = View.GONE
        }

        // ✅ Real-Time User Data (Name + Profile Image)
        FirebaseFirestore.getInstance().collection("users")
            .document(post.userId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val name = snapshot.getString("name") ?: "User"
                    val profileImageBase64 = snapshot.getString("profileImageBase64") ?: ""

                    holder.userNameTV.text = name

                    if (profileImageBase64.isNotEmpty()) {
                        try {
                            val imageBytes = Base64.decode(profileImageBase64, Base64.DEFAULT)
                            Glide.with(holder.itemView.context)
                                .asBitmap()
                                .load(imageBytes)
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .circleCrop()
                                .into(holder.userImage)
                        } catch (e: Exception) {
                            holder.userImage.setImageResource(R.drawable.ic_launcher_foreground)
                        }
                    } else {
                        holder.userImage.setImageResource(R.drawable.ic_launcher_foreground)
                    }
                }
            }

        // ✅ Profile Click
        holder.userImage.setOnClickListener { onUserClick(post.userId) }
        holder.userNameTV.setOnClickListener { onUserClick(post.userId) }

        // ✅ Likes
        val isLiked = post.likedBy.contains(uid)
        holder.likeIcon.setImageResource(
            if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
        )
        holder.likeCountTV.text = post.likedBy.size.toString()
        holder.likeIcon.setOnClickListener { toggleLike(post, position) }

        // ✅ Comments
        holder.commentIcon.setOnClickListener {
            val intent = android.content.Intent(context, CommentActivity::class.java)
            intent.putExtra("postId", post.id)
            context.startActivity(intent)
        }

        // ✅ Edit/Delete for Own Posts
        if (post.userId == uid) {
            holder.editBtn.visibility = View.VISIBLE
            holder.deleteBtn.visibility = View.VISIBLE
        } else {
            holder.editBtn.visibility = View.GONE
            holder.deleteBtn.visibility = View.GONE
        }
        holder.editBtn.setOnClickListener { showEditDialog(post, position) }
        holder.deleteBtn.setOnClickListener { confirmDelete(post, position) }
    }


    override fun getItemCount(): Int = postList.size

    private fun toggleLike(post: Post, position: Int) {
        val updatedLikedBy = post.likedBy.toMutableList()
        if (updatedLikedBy.contains(uid)) {
            updatedLikedBy.remove(uid)
        } else {
            updatedLikedBy.add(uid)
        }

        postList[position] = post.copy(likedBy = updatedLikedBy)
        notifyItemChanged(position)

        val postRef = firestore.collection("posts").document(post.id)

        if (updatedLikedBy.isEmpty()) {
            postRef.update("likedBy", FieldValue.delete())
        } else {
            postRef.update("likedBy", updatedLikedBy)
        }
    }

    private fun showEditDialog(post: Post, position: Int) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(post.content)
            setSelection(post.content.length)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Edit Post")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) {
                    updatePost(post, position, newText)
                } else {
                    Toast.makeText(context, "Content cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePost(post: Post, position: Int, newText: String) {
        postList[position] = post.copy(content = newText, timestamp = Timestamp.now())
        notifyItemChanged(position)

        firestore.collection("posts").document(post.id)
            .update(mapOf(
                "content" to newText,
                "timestamp" to Timestamp.now()
            )).addOnFailureListener {
                Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete(post: Post, position: Int) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Yes") { _, _ -> deletePost(post, position) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost(post: Post, position: Int) {
        firestore.collection("posts").document(post.id)
            .delete()
            .addOnSuccessListener {
                if (position < postList.size) {
                    postList.removeAt(position)
                    notifyItemRemoved(position)
                }
                Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}

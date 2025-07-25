package com.example.internshiptask.adapters

import android.content.Intent
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.internshiptask.ProfileActivity
import com.example.internshiptask.R
import com.example.internshiptask.models.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class CommentAdapter(private val commentList: List<Comment>, private val postId: String) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val prettyTime = PrettyTime(Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]
        holder.bind(comment)
    }

    override fun getItemCount(): Int = commentList.size

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val commentUserImage: ImageView = itemView.findViewById(R.id.commentUserImage)
        private val commentUser: TextView = itemView.findViewById(R.id.commentUser)
        private val commentText: TextView = itemView.findViewById(R.id.commentText)
        private val commentTime: TextView = itemView.findViewById(R.id.commentTime)
        private val deleteIcon: ImageView = itemView.findViewById(R.id.deleteCommentIcon)

        fun bind(comment: Comment) {
            commentText.text = comment.content
            commentTime.text = prettyTime.format(Date(comment.timestamp))

            // ✅ Fetch Latest User Details in Real-Time
            db.collection("users").document(comment.userId)
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        val updatedName = doc.getString("name") ?: "Unknown"
                        val updatedProfileImage = doc.getString("profileImageBase64") ?: ""

                        commentUser.text = updatedName

                        if (updatedProfileImage.isNotEmpty()) {
                            try {
                                val imageBytes = Base64.decode(updatedProfileImage, Base64.DEFAULT)
                                Glide.with(itemView.context)
                                    .asBitmap()
                                    .load(imageBytes)
                                    .placeholder(R.drawable.ic_launcher_foreground)
                                    .circleCrop()
                                    .into(commentUserImage)
                            } catch (e: Exception) {
                                commentUserImage.setImageResource(R.drawable.ic_launcher_foreground)
                            }
                        } else {
                            commentUserImage.setImageResource(R.drawable.ic_launcher_foreground)
                        }
                    }
                }

            // ✅ Click username to open profile
            commentUser.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, ProfileActivity::class.java)
                intent.putExtra("userId", comment.userId)
                context.startActivity(intent)
            }

            // ✅ Show delete icon only if user owns the comment
            if (comment.userId == uid) {
                deleteIcon.visibility = View.VISIBLE
                deleteIcon.setOnClickListener {
                    showDeleteDialog(comment)
                }
            } else {
                deleteIcon.visibility = View.GONE
            }
        }


        private fun showDeleteDialog(comment: Comment) {
            val context = itemView.context
            AlertDialog.Builder(context)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Yes") { _, _ ->
                    db.collection("posts").document(postId)
                        .collection("comments").document(comment.id)
                        .delete()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}

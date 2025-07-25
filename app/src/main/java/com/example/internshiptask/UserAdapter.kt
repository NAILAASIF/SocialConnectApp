package com.example.internshiptask.adapters

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.internshiptask.R
import com.example.internshiptask.models.User
import com.google.android.material.imageview.ShapeableImageView

class UserAdapter(
    private val users: MutableList<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.userName)
        val userImage: ShapeableImageView = itemView.findViewById(R.id.userImage)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserClick(users[position])
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.nameText.text = user.name

        // Load profile image using Glide
        if (!user.profileImageBase64.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(user.profileImageBase64, Base64.DEFAULT)
                Glide.with(holder.itemView.context)
                    .asBitmap()
                    .load(imageBytes)
                    .placeholder(R.drawable.ic_launcher_foreground) // default placeholder
                    .circleCrop() // make image circular
                    .into(holder.userImage)
            } catch (e: Exception) {
                holder.userImage.setImageResource(R.drawable.ic_launcher_foreground)
            }
        } else {
            holder.userImage.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    override fun getItemCount(): Int = users.size

    // ✅ Update entire list dynamically
    fun updateUsers(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }
}

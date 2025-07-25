package com.example.internshiptask.models

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val profileImage: String = "",
    val imageUrl: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val likedBy: List<String> = emptyList()
)

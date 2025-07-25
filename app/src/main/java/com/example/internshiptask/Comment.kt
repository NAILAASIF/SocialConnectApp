package com.example.internshiptask.models

data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val profileImage: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

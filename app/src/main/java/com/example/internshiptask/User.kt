package com.example.internshiptask.models

data class User(
    var uid: String = "",
    var name: String = "",
    var username: String = "",
    var email: String = "",
    var phone: String = "",
    var bio: String = "",
    var profileImageBase64: String = "",
    var fcmToken: String = ""
)


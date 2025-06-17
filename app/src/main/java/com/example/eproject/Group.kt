package com.example.eproject
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Group(
    val id: String = "",
    val name: String = "",
    val creatorId: String = "",
    val description: String = ""
)
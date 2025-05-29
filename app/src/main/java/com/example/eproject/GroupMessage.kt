package com.example.eproject

// GroupMessage.kt
data class GroupMessage(
    val id: String,
    val groupId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long
)
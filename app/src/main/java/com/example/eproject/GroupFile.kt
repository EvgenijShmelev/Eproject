package com.example.eproject

data class GroupFile(
    val id: String,
    val groupId: String,
    val fileName: String,
    val filePath: String,
    val uploaderId: String,
    val uploadTime: Long
)
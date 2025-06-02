package com.example.eproject

data class Group(
    val id: String,
    val name: String,
    val creatorId: String,
    val description: String = ""
)
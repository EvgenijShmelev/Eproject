package com.example.eproject

enum class UserRole {
    HEAD,
    MEMBER
}

data class GroupMember(
    val groupId: String,
    val userId: String,
    val role: UserRole
)
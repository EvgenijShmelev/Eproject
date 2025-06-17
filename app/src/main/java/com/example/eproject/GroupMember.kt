package com.example.eproject

enum class UserRole {
    HEAD,
    MEMBER,
    GUEST,
    ADMIN
}

data class GroupMember(
    val groupId: String,
    val userId: String,
    val role: UserRole
)
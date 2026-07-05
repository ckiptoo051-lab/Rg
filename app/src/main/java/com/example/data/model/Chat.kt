package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileName: String = "Brian",
    val profilePhotoUri: String? = null,
    val profileInitialsColor: Int = 0xFFFF5722.toInt(), // default orange-pink
    val isOnline: Boolean = true,
    val lastSeenText: String = "online",
    val createdAt: Long = System.currentTimeMillis()
)

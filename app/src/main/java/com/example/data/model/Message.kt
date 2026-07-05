package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Chat::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chatId"])]
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val text: String,
    val time: String,
    val isOutgoing: Boolean,
    val status: String = "READ", // PENDING, SENT, DELIVERED, READ, FAILED
    val reactions: String? = null, // Comma-separated reactions like "👍,❤️"
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val mediaUris: String? = null, // Comma-separated image URIs or drawables
    val isDateHeader: Boolean = false
)

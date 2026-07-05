package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.repository.MockChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MockChatRepository
    val chats: StateFlow<List<Chat>>

    private val _activeChatId = MutableStateFlow<Long?>(null)
    val activeChatId = _activeChatId.asStateFlow()

    val activeChat: StateFlow<Chat?> = _activeChatId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getChatById(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val activeMessages: StateFlow<List<Message>> = _activeChatId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getMessagesForChat(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MockChatRepository(database.chatDao(), database.messageDao())

        chats = repository.allChats.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed default chats and messages if empty
        viewModelScope.launch {
            repository.allChats.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultData()
                } else if (_activeChatId.value == null) {
                    _activeChatId.value = list.first().id
                }
            }
        }
    }

    private suspend fun seedDefaultData() {
        val defaultChat = Chat(
            profileName = "Brian",
            profileInitialsColor = 0xFFEC407A.toInt(), // Nice pinkish/rose color from screenshot
            isOnline = false,
            lastSeenText = "last seen June 13, 2026",
            createdAt = System.currentTimeMillis()
        )
        val chatId = repository.insertChat(defaultChat)
        
        var baseTime = System.currentTimeMillis() - 60000

        // Seed default messages matching the user's screenshot exactly
        val msg1 = Message(
            chatId = chatId,
            text = "Hi",
            time = "6:52 PM",
            isOutgoing = true,
            status = "FAILED",
            timestamp = baseTime
        )
        repository.insertMessage(msg1)

        // Today Header
        baseTime += 1000
        val headerToday = Message(
            chatId = chatId,
            text = "Today",
            time = "",
            isOutgoing = false,
            status = "READ",
            timestamp = baseTime,
            isDateHeader = true
        )
        repository.insertMessage(headerToday)

        // 4 Deleted messages in a row
        repeat(4) { idx ->
            baseTime += 1000
            val deletedMsg = Message(
                chatId = chatId,
                text = "You deleted this message",
                time = "11:01 AM",
                isOutgoing = true,
                status = "READ",
                timestamp = baseTime,
                isDeleted = true
            )
            repository.insertMessage(deletedMsg)
        }

        // 1 Image Grid message with 6 images (4 unique in 2x2 grid, 5th and 6th trigger +2 overlay)
        baseTime += 1000
        val gridMsg = Message(
            chatId = chatId,
            text = "",
            time = "11:05 AM",
            isOutgoing = true,
            status = "SENT",
            timestamp = baseTime,
            mediaUris = "img_deal_1,img_deal_2,img_deal_3,img_deal_4,img_deal_1,img_deal_2"
        )
        repository.insertMessage(gridMsg)

        _activeChatId.value = chatId
    }

    fun selectChat(chatId: Long) {
        _activeChatId.value = chatId
    }

    fun createNewChat(name: String, initialsColor: Int, isOnline: Boolean = true, lastSeenText: String = "online") {
        viewModelScope.launch {
            val newChat = Chat(
                profileName = name,
                profileInitialsColor = initialsColor,
                isOnline = isOnline,
                lastSeenText = lastSeenText
            )
            val id = repository.insertChat(newChat)
            _activeChatId.value = id
        }
    }

    fun updateActiveChatProfile(name: String, photoUri: String?, isOnline: Boolean, lastSeenText: String) {
        val currentChat = activeChat.value ?: return
        viewModelScope.launch {
            val updated = currentChat.copy(
                profileName = name,
                profilePhotoUri = photoUri,
                isOnline = isOnline,
                lastSeenText = lastSeenText
            )
            repository.updateChat(updated)
        }
    }

    fun addMessageToActiveChat(
        text: String, 
        isOutgoing: Boolean, 
        timeInput: String = "", 
        statusInput: String = "READ",
        isDeleted: Boolean = false,
        mediaUris: String? = null,
        isDateHeader: Boolean = false
    ) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            val finalTime = if (timeInput.isBlank() && !isDateHeader) {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                sdf.format(Date())
            } else {
                timeInput
            }
            val newMessage = Message(
                chatId = chatId,
                text = text,
                time = finalTime,
                isOutgoing = isOutgoing,
                status = statusInput,
                isDeleted = isDeleted,
                mediaUris = mediaUris,
                isDateHeader = isDateHeader
            )
            repository.insertMessage(newMessage)
        }
    }

    fun editMessageInActiveChat(
        messageId: Long, 
        text: String, 
        isOutgoing: Boolean, 
        time: String, 
        status: String, 
        reactions: String?,
        isDeleted: Boolean = false,
        mediaUris: String? = null,
        isDateHeader: Boolean = false
    ) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            val updated = Message(
                id = messageId,
                chatId = chatId,
                text = text,
                time = time,
                isOutgoing = isOutgoing,
                status = status,
                reactions = reactions,
                isDeleted = isDeleted,
                mediaUris = mediaUris,
                isDateHeader = isDateHeader
            )
            repository.updateMessage(updated)
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessageById(messageId)
        }
    }

    fun deleteActiveChat() {
        val currentChat = activeChat.value ?: return
        viewModelScope.launch {
            repository.deleteChat(currentChat)
            _activeChatId.value = null
        }
    }

    fun clearActiveChatMessages() {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.clearMessagesForChat(chatId)
        }
    }
}

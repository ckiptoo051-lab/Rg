package com.example.data.repository

import com.example.data.database.ChatDao
import com.example.data.database.MessageDao
import com.example.data.model.Chat
import com.example.data.model.Message
import kotlinx.coroutines.flow.Flow

class MockChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    val allChats: Flow<List<Chat>> = chatDao.getAllChats()

    fun getChatById(id: Long): Flow<Chat?> = chatDao.getChatById(id)

    fun getMessagesForChat(chatId: Long): Flow<List<Message>> = messageDao.getMessagesForChat(chatId)

    suspend fun insertChat(chat: Chat): Long = chatDao.insertChat(chat)

    suspend fun updateChat(chat: Chat) = chatDao.updateChat(chat)

    suspend fun deleteChat(chat: Chat) = chatDao.deleteChat(chat)

    suspend fun deleteChatById(id: Long) = chatDao.deleteChatById(id)

    suspend fun insertMessage(message: Message): Long = messageDao.insertMessage(message)

    suspend fun updateMessage(message: Message) = messageDao.updateMessage(message)

    suspend fun deleteMessage(message: Message) = messageDao.deleteMessage(message)

    suspend fun deleteMessageById(id: Long) = messageDao.deleteMessageById(id)

    suspend fun clearMessagesForChat(chatId: Long) = messageDao.clearMessagesForChat(chatId)
}

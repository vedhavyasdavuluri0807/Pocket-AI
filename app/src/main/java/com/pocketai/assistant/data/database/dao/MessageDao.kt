package com.pocketai.assistant.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pocketai.assistant.data.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesSnapshot(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessage(conversationId: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET content = :content, status = :status WHERE id = :id")
    suspend fun updateContentAndStatus(id: String, content: String, status: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearConversationMessages(conversationId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: String): Int
}


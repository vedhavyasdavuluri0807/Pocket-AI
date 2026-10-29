package com.pocketai.assistant.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketai.assistant.data.database.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun getConversationFlow(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversation(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)
}


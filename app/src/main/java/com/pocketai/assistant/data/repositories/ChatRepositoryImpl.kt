package com.pocketai.assistant.data.repositories

import com.pocketai.assistant.data.database.dao.ConversationDao
import com.pocketai.assistant.data.database.dao.MessageDao
import com.pocketai.assistant.data.database.entity.ConversationEntity
import com.pocketai.assistant.data.database.entity.MessageEntity
import com.pocketai.assistant.data.preferences.PreferencesManager
import com.pocketai.assistant.domain.chat.ChatRepository
import com.pocketai.assistant.domain.inference.GenerationEvent
import com.pocketai.assistant.domain.inference.GenerationRequest
import com.pocketai.assistant.domain.model.AttachmentItem
import com.pocketai.assistant.domain.model.AttachmentRef
import com.pocketai.assistant.domain.model.ChatMessage
import com.pocketai.assistant.domain.model.MessageRole
import com.pocketai.assistant.domain.model.MessageStatus
import com.pocketai.assistant.inference.ModelManager
import com.pocketai.assistant.inference.PromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ChatRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val preferencesManager: PreferencesManager,
    private val modelManager: ModelManager
) : ChatRepository {

    private val conversationId = ConversationEntity.DEFAULT_CONVERSATION_ID

    init {
        // Ensure conversation exists
        CoroutineScope(Dispatchers.IO).launch {
            if (conversationDao.getConversation(conversationId) == null) {
                conversationDao.insertOrUpdate(ConversationEntity(id = conversationId))
            }
        }
    }

    override fun getMessagesFlow(): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMessagesSnapshot(): List<ChatMessage> = withContext(Dispatchers.IO) {
        messageDao.getMessagesSnapshot(conversationId).map { it.toDomain() }
    }

    override suspend fun sendUserMessage(
        content: String,
        attachments: List<AttachmentItem>
    ): Flow<GenerationEvent> = channelFlow {
        withContext(Dispatchers.IO) {
            val userMsgId = UUID.randomUUID().toString()
            val attachmentRefs = attachments.map {
                AttachmentRef(
                    id = it.id,
                    uriString = it.uriString,
                    fileName = it.displayName,
                    mimeType = it.mimeType,
                    sizeBytes = it.sizeBytes,
                    extractedText = it.extractedText
                )
            }

            val userEntity = MessageEntity(
                id = userMsgId,
                conversationId = conversationId,
                role = MessageRole.USER.name,
                content = content,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SUCCESS.name,
                attachmentsJson = serializeAttachments(attachmentRefs)
            )
            messageDao.insertMessage(userEntity)

            val assistantMsgId = UUID.randomUUID().toString()
            val assistantEntity = MessageEntity(
                id = assistantMsgId,
                conversationId = conversationId,
                role = MessageRole.ASSISTANT.name,
                content = "",
                timestamp = System.currentTimeMillis() + 1,
                status = MessageStatus.STREAMING.name
            )
            messageDao.insertMessage(assistantEntity)

            val prefs = preferencesManager.userPreferencesFlow.first()
            val history = messageDao.getMessagesSnapshot(conversationId).map { it.toDomain() }
            val arch = modelManager.currentModelInfo.value?.architecture ?: "llama"
            val prompt = PromptBuilder.buildChatPrompt(history, prefs, arch)

            val request = GenerationRequest(
                prompt = prompt,
                maxTokens = prefs.maxTokens,
                temperature = prefs.temperature
            )

            val fullTextBuilder = StringBuilder()
            var lastPersistTime = System.currentTimeMillis()

            try {
                modelManager.engine.generate(request).collect { event ->
                    when (event) {
                        is GenerationEvent.Token -> {
                            fullTextBuilder.append(event.text)
                            send(event)

                            val now = System.currentTimeMillis()
                            if (now - lastPersistTime > 250) {
                                messageDao.updateContentAndStatus(
                                    assistantMsgId,
                                    fullTextBuilder.toString(),
                                    MessageStatus.STREAMING.name
                                )
                                lastPersistTime = now
                            }
                        }
                        is GenerationEvent.Completed -> {
                            messageDao.updateContentAndStatus(
                                assistantMsgId,
                                event.fullText,
                                MessageStatus.SUCCESS.name
                            )
                            send(event)
                        }
                        is GenerationEvent.Error -> {
                            val entity = messageDao.getMessageById(assistantMsgId)
                            if (entity != null) {
                                messageDao.updateMessage(
                                    entity.copy(
                                        content = fullTextBuilder.toString(),
                                        status = MessageStatus.ERROR.name,
                                        errorMessage = event.message
                                    )
                                )
                            }
                            send(event)
                        }
                        is GenerationEvent.Interrupted -> {
                            val entity = messageDao.getMessageById(assistantMsgId)
                            if (entity != null) {
                                messageDao.updateMessage(
                                    entity.copy(
                                        content = fullTextBuilder.toString(),
                                        status = MessageStatus.CANCELLED.name
                                    )
                                )
                            }
                            send(event)
                        }
                    }
                }
            } catch (e: CancellationException) {
                messageDao.updateContentAndStatus(
                    assistantMsgId,
                    fullTextBuilder.toString(),
                    MessageStatus.CANCELLED.name
                )
                send(GenerationEvent.Interrupted)
            } catch (e: Exception) {
                val entity = messageDao.getMessageById(assistantMsgId)
                if (entity != null) {
                    messageDao.updateMessage(
                        entity.copy(
                            content = fullTextBuilder.toString(),
                            status = MessageStatus.ERROR.name,
                            errorMessage = e.localizedMessage
                        )
                    )
                }
                send(GenerationEvent.Error(e.localizedMessage ?: "Unknown error occurred", e))
            }
        }
    }

    override suspend fun stopGeneration() {
        modelManager.engine.stopGeneration()
    }

    override suspend fun retryLastAssistantResponse(): Flow<GenerationEvent>? = withContext(Dispatchers.IO) {
        val latest = messageDao.getLatestMessage(conversationId) ?: return@withContext null
        if (latest.role == MessageRole.ASSISTANT.name) {
            messageDao.deleteMessageById(latest.id)
        }

        val lastUserMsg = messageDao.getLatestMessage(conversationId) ?: return@withContext null
        if (lastUserMsg.role != MessageRole.USER.name) return@withContext null

        channelFlow {
            val assistantMsgId = UUID.randomUUID().toString()
            val assistantEntity = MessageEntity(
                id = assistantMsgId,
                conversationId = conversationId,
                role = MessageRole.ASSISTANT.name,
                content = "",
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.STREAMING.name
            )
            messageDao.insertMessage(assistantEntity)

            val prefs = preferencesManager.userPreferencesFlow.first()
            val history = messageDao.getMessagesSnapshot(conversationId).map { it.toDomain() }
            val arch = modelManager.currentModelInfo.value?.architecture ?: "llama"
            val prompt = PromptBuilder.buildChatPrompt(history, prefs, arch)

            val request = GenerationRequest(
                prompt = prompt,
                maxTokens = prefs.maxTokens,
                temperature = prefs.temperature
            )

            val fullTextBuilder = StringBuilder()
            var lastPersist = System.currentTimeMillis()

            try {
                modelManager.engine.generate(request).collect { event ->
                    when (event) {
                        is GenerationEvent.Token -> {
                            fullTextBuilder.append(event.text)
                            send(event)
                            val now = System.currentTimeMillis()
                            if (now - lastPersist > 250) {
                                messageDao.updateContentAndStatus(
                                    assistantMsgId,
                                    fullTextBuilder.toString(),
                                    MessageStatus.STREAMING.name
                                )
                                lastPersist = now
                            }
                        }
                        is GenerationEvent.Completed -> {
                            messageDao.updateContentAndStatus(
                                assistantMsgId,
                                event.fullText,
                                MessageStatus.SUCCESS.name
                            )
                            send(event)
                        }
                        is GenerationEvent.Error -> {
                            val entity = messageDao.getMessageById(assistantMsgId)
                            if (entity != null) {
                                messageDao.updateMessage(
                                    entity.copy(
                                        content = fullTextBuilder.toString(),
                                        status = MessageStatus.ERROR.name,
                                        errorMessage = event.message
                                    )
                                )
                            }
                            send(event)
                        }
                        is GenerationEvent.Interrupted -> {
                            val entity = messageDao.getMessageById(assistantMsgId)
                            if (entity != null) {
                                messageDao.updateMessage(
                                    entity.copy(
                                        content = fullTextBuilder.toString(),
                                        status = MessageStatus.CANCELLED.name
                                    )
                                )
                            }
                            send(event)
                        }
                    }
                }
            } catch (e: Exception) {
                messageDao.updateContentAndStatus(
                    assistantMsgId,
                    fullTextBuilder.toString(),
                    MessageStatus.ERROR.name
                )
                send(GenerationEvent.Error(e.localizedMessage ?: "Retry failed", e))
            }
        }
    }

    override suspend fun deleteMessage(id: String) = withContext(Dispatchers.IO) {
        messageDao.deleteMessageById(id)
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        messageDao.clearConversationMessages(conversationId)
    }

    override suspend fun getMessageCount(): Int = withContext(Dispatchers.IO) {
        messageDao.getMessageCount(conversationId)
    }

    private fun MessageEntity.toDomain(): ChatMessage {
        val parsedRole = try {
            MessageRole.valueOf(role)
        } catch (_: Exception) {
            MessageRole.USER
        }
        val parsedStatus = try {
            MessageStatus.valueOf(status)
        } catch (_: Exception) {
            MessageStatus.SUCCESS
        }

        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = parsedRole,
            content = content,
            timestamp = timestamp,
            status = parsedStatus,
            errorMessage = errorMessage,
            attachments = deserializeAttachments(attachmentsJson)
        )
    }

    private fun serializeAttachments(list: List<AttachmentRef>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("uriString", item.uriString)
            obj.put("fileName", item.fileName)
            obj.put("mimeType", item.mimeType)
            obj.put("sizeBytes", item.sizeBytes)
            obj.put("extractedText", item.extractedText ?: JSONObject.NULL)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeAttachments(json: String): List<AttachmentRef> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<AttachmentRef>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AttachmentRef(
                        id = obj.optString("id", ""),
                        uriString = obj.optString("uriString", ""),
                        fileName = obj.optString("fileName", ""),
                        mimeType = obj.optString("mimeType", ""),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        extractedText = if (obj.isNull("extractedText")) null else obj.optString("extractedText")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}


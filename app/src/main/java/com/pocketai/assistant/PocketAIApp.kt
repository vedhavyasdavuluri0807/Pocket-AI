package com.pocketai.assistant

import android.app.Application
import androidx.room.Room
import com.pocketai.assistant.attachments.AttachmentProcessor
import com.pocketai.assistant.data.database.AppDatabase
import com.pocketai.assistant.data.preferences.PreferencesManager
import com.pocketai.assistant.data.repositories.ChatRepositoryImpl
import com.pocketai.assistant.domain.chat.ChatRepository
import com.pocketai.assistant.domain.inference.LocalModelEngine
import com.pocketai.assistant.inference.ModelManager
import com.pocketai.assistant.inference.engine.LlamaCppEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PocketAIApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var localModelEngine: LocalModelEngine
        private set

    lateinit var modelManager: ModelManager
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var attachmentProcessor: AttachmentProcessor
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "pocket_ai.db"
        ).fallbackToDestructiveMigration()
            .build()

        preferencesManager = PreferencesManager(applicationContext)

        // Initialize native llama.cpp runtime and CPU backends
        com.pocketai.assistant.inference.engine.LlamaNativeBridge.init(applicationContext)

        localModelEngine = LlamaCppEngine(applicationContext)

        modelManager = ModelManager(
            context = applicationContext,
            engine = localModelEngine,
            preferencesManager = preferencesManager,
            applicationScope = applicationScope
        )

        chatRepository = ChatRepositoryImpl(
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            preferencesManager = preferencesManager,
            modelManager = modelManager
        )

        attachmentProcessor = AttachmentProcessor(applicationContext)
    }
}


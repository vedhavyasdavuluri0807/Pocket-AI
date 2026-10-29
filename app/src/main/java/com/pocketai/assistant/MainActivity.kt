package com.pocketai.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.pocketai.assistant.domain.model.UserPreferences
import com.pocketai.assistant.navigation.NavGraph
import com.pocketai.assistant.navigation.Screen
import com.pocketai.assistant.ui.chat.ChatViewModel
import com.pocketai.assistant.ui.chat.ChatViewModelFactory
import com.pocketai.assistant.ui.models.ModelManagementViewModel
import com.pocketai.assistant.ui.models.ModelManagementViewModelFactory
import com.pocketai.assistant.ui.settings.SettingsViewModel
import com.pocketai.assistant.ui.settings.SettingsViewModelFactory
import com.pocketai.assistant.ui.theme.PocketAITheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PocketAIApp

        val initialPrefs = runBlocking {
            app.preferencesManager.userPreferencesFlow.first()
        }

        setContent {
            val userPrefs by app.preferencesManager.userPreferencesFlow.collectAsState(initial = initialPrefs)

            PocketAITheme(themeMode = userPrefs.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    val chatViewModel: ChatViewModel by viewModels {
                        ChatViewModelFactory(
                            chatRepository = app.chatRepository,
                            modelManager = app.modelManager,
                            preferencesManager = app.preferencesManager,
                            attachmentProcessor = app.attachmentProcessor
                        )
                    }

                    val modelViewModel: ModelManagementViewModel by viewModels {
                        ModelManagementViewModelFactory(
                            application = app,
                            modelManager = app.modelManager
                        )
                    }

                    val settingsViewModel: SettingsViewModel by viewModels {
                        SettingsViewModelFactory(
                            preferencesManager = app.preferencesManager,
                            chatRepository = app.chatRepository,
                            modelManager = app.modelManager
                        )
                    }

                    val startDestination = if (userPrefs.hasCompletedOnboarding) {
                        Screen.Chat.route
                    } else {
                        Screen.Onboarding.route
                    }

                    NavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        chatViewModel = chatViewModel,
                        modelViewModel = modelViewModel,
                        settingsViewModel = settingsViewModel,
                        onCompleteOnboarding = {
                            lifecycleScope.launch {
                                app.preferencesManager.setOnboardingCompleted(true)
                            }
                        }
                    )
                }
            }
        }
    }
}


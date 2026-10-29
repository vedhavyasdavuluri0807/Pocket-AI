package com.pocketai.assistant.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pocketai.assistant.ui.chat.ChatScreen
import com.pocketai.assistant.ui.chat.ChatViewModel
import com.pocketai.assistant.ui.models.ModelManagementScreen
import com.pocketai.assistant.ui.models.ModelManagementViewModel
import com.pocketai.assistant.ui.onboarding.OnboardingScreen
import com.pocketai.assistant.ui.settings.PersonalizationScreen
import com.pocketai.assistant.ui.settings.SettingsScreen
import com.pocketai.assistant.ui.settings.SettingsViewModel
import com.pocketai.assistant.ui.settings.StoragePrivacyScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    chatViewModel: ChatViewModel,
    modelViewModel: ModelManagementViewModel,
    settingsViewModel: SettingsViewModel,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onContinueClick = {
                    onCompleteOnboarding()
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Chat.route) {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToModels = {
                    navController.navigate(Screen.ModelManagement.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToModels = {
                    navController.navigate(Screen.ModelManagement.route)
                },
                onNavigateToPersonalization = {
                    navController.navigate(Screen.Personalization.route)
                },
                onNavigateToStoragePrivacy = {
                    navController.navigate(Screen.StoragePrivacy.route)
                }
            )
        }

        composable(Screen.ModelManagement.route) {
            ModelManagementScreen(
                viewModel = modelViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Personalization.route) {
            PersonalizationScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.StoragePrivacy.route) {
            StoragePrivacyScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}


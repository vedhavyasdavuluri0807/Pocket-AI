package com.pocketai.assistant.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Chat : Screen("chat")
    object Settings : Screen("settings")
    object ModelManagement : Screen("model_management")
    object Personalization : Screen("personalization")
    object StoragePrivacy : Screen("storage_privacy")
}


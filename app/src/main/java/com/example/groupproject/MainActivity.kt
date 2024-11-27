package com.example.groupproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.groupproject.ui.theme.GroupProjectTheme

// Главный класс приложения
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Применяем тему для приложения
            GroupProjectTheme {

                // Создаем контроллер навигации
                val navController = rememberNavController()

                // Определяем навигационные маршруты через NavHost
                NavHost(
                    navController = navController, // Передаем контроллер навигации
                    startDestination = "welcome"  // Указываем стартовый экран
                ) {
                    // Экран приветствия
                    composable("welcome") {
                        WelcomeScreen(
                            onLoginClick = { navController.navigate("login") },  // Переход на экран входа
                            onRegistrationClick = { navController.navigate("register") } // Переход на экран регистрации
                        )
                    }
                    // Экран входа
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = { _ ->
                                // Успешный вход. Переход на главный экран
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true } // Удаляем экран входа из стека
                                }
                            }
                        )
                    }
                    // Экран регистрации
                    composable("register") {
                        RegistrationScreen(
                            onRegistrationSuccess = { _ ->
                                // Успешная регистрация. Переход на главный экран
                                navController.navigate("home") {
                                    popUpTo("register") { inclusive = true } // Удаляем экран регистрации из стека
                                }
                            }
                        )
                    }
                    // Главный экран
                    composable("home") {
                        // Передаем userId для дальнейшего использования (пока захардкожен)
                        HomeScreen(navController = navController, userId = 1)
                    }
                    // Экран продуктов пользователя
                    composable("userProducts/{userId}") { backStackEntry ->
                        // Получаем userId из маршрута
                        val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
                        if (userId != null) {
                            // Если userId корректен, отображаем экран продуктов
                            UserProductScreen(userId = userId)
                        } else {
                            // В случае ошибки отображаем экран ошибки
                            ErrorScreen()
                        }
                    }
                }
            }
        }
    }
}

// Новый композируемый экран для отображения ошибки
@Composable
fun ErrorScreen() {
    Text("Ошибка: Неверный идентификатор пользователя")
}

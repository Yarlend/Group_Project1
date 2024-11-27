package com.example.groupproject

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(onLoginClick: () -> Unit, onRegistrationClick: () -> Unit) {
    // Основной контейнер для вертикального размещения элементов
    Column(modifier = Modifier.padding(16.dp)) {
        // Отображение приветственного текста
        Text(
            text = "Welcome!",
            modifier = Modifier.padding(bottom = 16.dp) // Отступ
        )
        // Кнопка для перехода на экран авторизации
        Button(onClick = { onLoginClick() }) {
            Text(text = "LogIn") // Текст на кнопке
        }

        // Кнопка для перехода на экран регистрации
        Button(onClick = { onRegistrationClick() }) {
            Text(text = "Registration") // Текст на кнопке
        }
    }
}

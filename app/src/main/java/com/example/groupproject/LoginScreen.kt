package com.example.groupproject

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import getCsrfTokenAndSessionId
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

// Экран логина
@Composable
fun LoginScreen(onLoginSuccess: (Int) -> Unit) { // Колбэк возвращает userId при успешном входе
    // Поля для ввода логина и пароля
    var username by remember { mutableStateOf("") } // Локально запоминаем логин
    var password by remember { mutableStateOf("") } // Локально запоминаем пароль

    // Переменная для отображения результата логина
    var loginResult by remember { mutableStateOf("") }

    // Верстка экрана
    Column(modifier = Modifier.padding(16.dp)) {

        // Поле для ввода логина
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") }, // Надпись "Username" внутри поля
            modifier = Modifier.padding(bottom = 8.dp) // Отступ снизу
        )

        // Поле для ввода пароля
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") }, // Надпись "Password" внутри поля
            modifier = Modifier.padding(bottom = 16.dp),
            visualTransformation = PasswordVisualTransformation() // Скрытие вводимого текста
        )

        // Кнопка "LogIn"
        Button(onClick = {
            // При нажатии отправляем запрос на сервер
            loginUser(username, password) { response, userId ->
                loginResult = response // Отображаем сообщение о результате логина
                if (response.contains("login successful", ignoreCase = true) && userId != null) {
                    onLoginSuccess(userId) // Если вход успешен, передаем userId
                }
            }
        }) {
            Text(text = "LogIn") // Текст кнопки
        }

        // Отображение результата логина
        Text(text = loginResult, modifier = Modifier.padding(top = 16.dp))
    }
}

// Функция для выполнения запроса логина в фоновом потоке
private fun loginUser(username: String, password: String, callback: (String, Int?) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch { // Выполняем код в фоновом потоке
        try {
            // Отправляем запрос и получаем ответ
            val response = login(username, password)
            withContext(Dispatchers.Main) { // Возвращаемся на главный поток
                callback(response.first, response.second) // Передаем результат через колбэк
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                // Обрабатываем ошибку
                callback("Ошибка: ${e.message}", null)
            }
        }
    }
}

// Основная функция отправки POST-запроса для логина
fun login(username: String, password: String): Pair<String, Int?> {
    val csrfTokenData = getCsrfTokenAndSessionId()

    if (csrfTokenData == null) {
        println("Ошибка: Не удалось получить CSRF токен и session ID.")
        return "Ошибка: Не удалось получить CSRF токен." to null
    }

    val (csrfToken, sessionId) = csrfTokenData

    val url = URL("https://mobileee.pythonanywhere.com/api/login/")
    val urlConnection = url.openConnection() as HttpURLConnection
    urlConnection.requestMethod = "POST"
    urlConnection.setRequestProperty("Content-Type", "application/json")
    urlConnection.setRequestProperty("X-CSRFTOKEN", csrfToken)
    urlConnection.setRequestProperty("Cookie", "csrftoken=$csrfToken; sessionid=$sessionId")
    urlConnection.setRequestProperty("Referer", "https://mobileee.pythonanywhere.com/") // Добавляем Referer
    urlConnection.doOutput = true

    val jsonInputString = """
        {
            "username": "$username",
            "password": "$password"
        }
    """

    return try {
        val outputStream: OutputStream = urlConnection.outputStream
        val writer = OutputStreamWriter(outputStream)
        writer.write(jsonInputString)
        writer.flush()

        val responseCode = urlConnection.responseCode
        val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
            urlConnection.inputStream
        } else {
            urlConnection.errorStream
        }

        val reader = BufferedReader(InputStreamReader(inputStream))
        val response = StringBuilder()
        reader.forEachLine { response.append(it) }
        reader.close()

        // Извлечение user_id из cookies
        val cookies = urlConnection.headerFields["Set-Cookie"]
        val userId = cookies?.find { it.startsWith("user_id=") }
            ?.substringAfter("user_id=")
            ?.substringBefore(";")
            ?.toIntOrNull()

        val responseText = response.toString().trim()
        println("Response: $responseText")

        // Проверяем, что вход был успешным
        if (responseText.contains("Login success", ignoreCase = true) && userId != null) {
            "login successful" to userId
        } else {
            "Ошибка: Невозможно выполнить логин" to null
        }

    } catch (e: Exception) {
        e.printStackTrace()
        "Ошибка: ${e.message}" to null
    } finally {
        urlConnection.disconnect()
    }
}

fun main() {
    // Данные для тестирования
    val testUsername = "user1" // Замените на реальные данные для теста
    val testPassword = "123" // Замените на реальные данные для теста

    // Запускаем проверку в отдельном потоке
    runBlocking {
        withContext(Dispatchers.IO) {
            try {
                // Вызываем функцию логина
                val (response, userId) = login(testUsername, testPassword)

                // Выводим результат в консоль
                println("Ответ сервера: $response")
                if (userId != null && userId > 0) {
                    println("User ID: $userId")
                } else {
                    println("Не удалось получить userId")
                }
            } catch (e: Exception) {
                println("Ошибка: ${e.message}")
            }
        }
    }
}

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
    // URL для отправки запроса
    val url = URL("https://mobileee.pythonanywhere.com/api/login/")
    val urlConnection = url.openConnection() as HttpURLConnection
    urlConnection.requestMethod = "POST" // Метод запроса — POST
    urlConnection.setRequestProperty("Content-Type", "application/json") // Заголовок запроса
    urlConnection.doOutput = true // Разрешаем запись данных в тело запроса

    // Формируем JSON-объект с логином и паролем
    val jsonInputString = """
        {
            "username": "$username",
            "password": "$password"
        }
    """

    return try {
        // Записываем JSON в тело запроса
        val outputStream: OutputStream = urlConnection.outputStream
        val writer = OutputStreamWriter(outputStream)
        writer.write(jsonInputString)
        writer.flush()

        // Получаем код ответа от сервера
        val responseCode = urlConnection.responseCode

        // Выбираем поток (ответ или ошибка) в зависимости от кода
        val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
            urlConnection.inputStream
        } else {
            urlConnection.errorStream
        }

        // Читаем ответ от сервера
        val reader = BufferedReader(InputStreamReader(inputStream))
        val response = StringBuilder()
        reader.forEachLine { response.append(it) }
        reader.close()

        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Если вход успешен, парсим JSON-ответ
            val responseText = response.toString().trim()
            val jsonResponse = JSONObject(responseText)
            val userId = jsonResponse.optInt("id", -1) // Извлекаем userId из ответа
            "login successful" to userId
        } else {
            // Если ошибка, возвращаем код и текст ответа
            "Ошибка: код $responseCode, Ответ: $response" to null
        }

    } catch (e: Exception) {
        e.printStackTrace()
        // В случае ошибки возвращаем ее сообщение
        "Ошибка: ${e.message}" to null
    } finally {
        // Закрываем соединение
        urlConnection.disconnect()
    }
}

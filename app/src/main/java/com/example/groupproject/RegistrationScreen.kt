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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

// Основной экран для регистрации пользователя
@Composable
fun RegistrationScreen(onRegistrationSuccess: (Int) -> Unit) { // Колбэк возвращает userId
    var username by remember { mutableStateOf("") } // Состояние для логина
    var password by remember { mutableStateOf("") } // Состояние для пароля
    var confirmPassword by remember { mutableStateOf("") } // Подтверждение пароля
    var registrationResult by remember { mutableStateOf("") } // Результат регистрации (успех/ошибка)

    // Верстка экрана
    Column(modifier = Modifier.padding(16.dp)) {
        // Поле для ввода логина
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Логин") },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Поле для ввода пароля
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            modifier = Modifier.padding(bottom = 8.dp),
            visualTransformation = PasswordVisualTransformation() // Скрытие вводимых символов
        )

        // Поле для подтверждения пароля
        TextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Подтверждение пароля") },
            modifier = Modifier.padding(bottom = 16.dp),
            visualTransformation = PasswordVisualTransformation() // Скрытие вводимых символов
        )

        // Кнопка "Зарегистрироваться"
        Button(onClick = {
            if (password == confirmPassword) { // Проверяем совпадение паролей
                // Если пароли совпадают, отправляем данные на сервер
                registerUser(username, password) { response, userId ->
                    registrationResult = response // Отображаем сообщение о результате
                    if (response.contains("registration successful", ignoreCase = true) && userId != null) {
                        onRegistrationSuccess(userId) // Если успех, передаем userId
                    }
                }
            } else {
                // Если пароли не совпадают, выводим сообщение
                registrationResult = "Пароли не совпадают"
            }
        }) {
            Text(text = "Зарегистрироваться") // Текст на кнопке
        }

        // Отображение результата регистрации
        Text(text = registrationResult, modifier = Modifier.padding(top = 16.dp))
    }
}

// Асинхронная функция для отправки запроса на сервер
private fun registerUser(username: String, password: String, callback: (String, Int?) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch { // Выполняем запрос в фоновом потоке
        try {
            val response = register(username, password) // Отправляем запрос
            withContext(Dispatchers.Main) { // Возвращаемся в главный поток
                callback(response.first, response.second) // Передаем результат через колбэк
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                // Если произошла ошибка, передаем сообщение об ошибке
                callback("Ошибка: ${e.message}", null)
            }
        }
    }
}

// Функция для отправки POST-запроса на сервер
fun register(username: String, password: String): Pair<String, Int?> {
    val url = URL("https://mobileee.pythonanywhere.com/api/register/") // URL API для регистрации
    val urlConnection = url.openConnection() as HttpURLConnection
    urlConnection.requestMethod = "POST" // Используем метод POST
    urlConnection.setRequestProperty("Content-Type", "application/json") // Указываем тип данных
    urlConnection.doOutput = true // Разрешаем запись данных в запрос

    // Формируем JSON-объект с логином и паролем
    val jsonInputString = """
        {
            "username": "$username",
            "password": "$password"
        }
    """

    return try {
        // Отправляем JSON в тело запроса
        val outputStream: OutputStream = urlConnection.outputStream
        val writer = OutputStreamWriter(outputStream)
        writer.write(jsonInputString)
        writer.flush()

        // Получаем код ответа от сервера
        val responseCode = urlConnection.responseCode

        // В зависимости от кода ответа выбираем, какой поток использовать
        val inputStream = if (responseCode == HttpURLConnection.HTTP_CREATED) { // HTTP 201 Created
            urlConnection.inputStream
        } else {
            urlConnection.errorStream
        }

        // Читаем ответ сервера
        val reader = BufferedReader(InputStreamReader(inputStream))
        val response = StringBuilder()
        reader.forEachLine { response.append(it) }
        reader.close()

        if (responseCode == HttpURLConnection.HTTP_CREATED) {
            // Если регистрация успешна, парсим JSON-ответ
            val responseText = response.toString().trim()
            val jsonResponse = JSONObject(responseText)
            val userId = jsonResponse.optInt("id", -1) // Получаем userId из ответа
            "registration successful" to userId
        } else {
            // Если ошибка, возвращаем код ответа и текст ошибки
            "Ошибка: код $responseCode, Ответ: $response" to null
        }

    } catch (e: Exception) {
        e.printStackTrace()
        // Если произошла ошибка, возвращаем сообщение об ошибке
        "Ошибка: ${e.message}" to null
    } finally {
        // Закрываем соединение
        urlConnection.disconnect()
    }
}

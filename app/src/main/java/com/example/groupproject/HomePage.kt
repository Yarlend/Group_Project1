package com.example.groupproject

import CookieManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.navigation.compose.rememberNavController
import getCsrfTokenAndSessionId
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun HomeScreen(navController: NavController, userId: Int?) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Загружаем данные продуктов при старте
    LaunchedEffect(Unit) {
        try {
            products = fetchProducts()
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Список продуктов
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Список продуктов:", style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    isLoading -> {
                        CircularProgressIndicator()
                        Text(text = "Загрузка данных продуктов...", modifier = Modifier.padding(top = 8.dp))
                    }
                    errorMessage != null -> {
                        Text(text = "Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error)
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(products) { product ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${product.name} - ${product.unit}",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            userId?.let { id ->
                                                scope.launch {
                                                    try {
                                                        addProductToPurchases(
                                                            userId = id,
                                                            productId = product.id,
                                                            quantity = 1L // Количество по умолчанию
                                                        )
                                                        snackbarHostState.showSnackbar("Продукт успешно добавлен!")
                                                    } catch (e: Exception) {
                                                        errorMessage = "Ошибка добавления продукта: ${e.message}"
                                                    }
                                                }
                                            } ?: run {
                                                errorMessage = "Не удалось получить userId"
                                            }
                                        }
                                    ) {
                                        Text("Добавить")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Кнопки внизу экрана
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { /* Действие для кнопки Продукты */ }) {
                    Text("П")
                }
                Button(onClick = { /* Действие для кнопки Рекомендации */ }) {
                    Text("Р")
                }
                Button(onClick = {
                    userId?.let { id ->
                        navController.navigate("userProducts/$id")
                    } ?: run {
                        errorMessage = "Не удалось получить userId"
                    }
                }) {
                    Text("ПП")
                }
                Button(onClick = { /* Действие для кнопки Голосовое распознавание */ }) {
                    Text("ГР")
                }
                Button(onClick = { /* Действие для кнопки Инфо об аккаунте */ }) {
                    Text("Инфо")
                }
            }
        }
    }
}

fun addProductToPurchases(userId: Int, productId: Int, quantity: Long) {
    val csrfData = getCsrfTokenAndSessionId()
    if (csrfData != null) {
        val (csrftoken, sessionid) = csrfData

        try {
            // Формируем тело запроса
            val purchase = mapOf(
                "quantity" to quantity,
                "user" to userId,
                "product" to productId
            )
            val json = Gson().toJson(purchase)
            val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

            println("Отправляем запрос с телом: $json") // Лог тела запроса

            // Создаем HTTP клиент и выполняем запрос
            val client = OkHttpClient.Builder()
                .cookieJar(CookieManager())  // Используем CookieManager для управления cookie
                .build()

            val request = Request.Builder()
                .url("https://mobileee.pythonanywhere.com/api/purchase/")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-CSRFTOKEN", csrftoken) // Добавляем CSRF-токен
                .addHeader("Cookie", "sessionid=$sessionid") // Добавляем sessionid
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                println("HTTP Код ответа: ${response.code}") // Лог HTTP-кода
                println("Тело ответа: $responseBody") // Лог тела ответа

                if (!response.isSuccessful) {
                    throw Exception("Ошибка при добавлении покупки: ${response.message} - $responseBody")
                } else {
                    println("Успешно добавлено: $responseBody") // Лог успешного ответа
                }
            }
        } catch (e: Exception) {
            println("Ошибка добавления продукта: ${e.message}") // Лог исключения
            e.printStackTrace() // Для подробного стека вызовов
        }
    } else {
        println("Ошибка: Не удалось получить CSRF токен или session id")
    }
}

suspend fun fetchProducts(): List<Product> {
    return withContext(Dispatchers.IO) {
        val productResponse = performGetRequest("https://mobileee.pythonanywhere.com/api/product/")
        val unitResponse = performGetRequest("https://mobileee.pythonanywhere.com/api/unit/")

        if (productResponse != null && unitResponse != null) {
            val gson = Gson()

            val productListType = object : TypeToken<List<Product>>() {}.type
            val products: List<Product> = gson.fromJson(productResponse, productListType)

            val unitListType = object : TypeToken<List<Unit1>>() {}.type
            val units: List<Unit1> = gson.fromJson(unitResponse, unitListType)

            val unitMap = units.associate { it.id to it.name }

            return@withContext products.map { product ->
                product.unit = unitMap[product.unit.toInt()] ?: "Unknown"
                product
            }
        } else {
            throw Exception("Не удалось загрузить данные с сервера.")
        }
    }
}

fun performGetRequest(url: String): String? {
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
            return response.body?.string()
        } else {
            throw Exception("Ошибка запроса: ${response.code}")
        }
    }
}

data class Product(val id: Int, val name: String, var unit: String)
data class Unit1(val id: Int, val name: String)
data class CartItem(val userId: Int, val productId: Int, val quantity: Int)

@Preview(showBackground = true)
@Composable
fun DefaultPreview2() {
    // Создаем временный NavController для превью
    val navController = rememberNavController()

    // Отображаем HomeScreen с этим NavController и передаем userId
    HomeScreen(navController, userId = 1)  // Пример с userId = 1
}

fun main() {
    // Пример вызова для добавления продукта в покупки
    val userId = 1
    val productId = 1
    val quantity = 1L

    addProductToPurchases(userId, productId, quantity)
}

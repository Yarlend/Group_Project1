package com.example.groupproject

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Модель данных для продукта
data class UserProduct(
    val id: Int,
    val quantity: Int,
    val user: Int,
    val product: Int
)

// Интерфейс Retrofit для получения данных
interface ApiService {
    @GET("api/userproduct/")
    suspend fun getUserProducts(@Query("by_user_id") userId: Int): List<UserProduct>

    // Метод для добавления товара в корзину
    @POST("api/purchase/")
    suspend fun addToCart(@Body cartItem: CartItem): Response<Unit>
}


// Настройка Retrofit
object RetrofitInstance {
    private const val BASE_URL = "https://mobileee.pythonanywhere.com/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

@Composable
fun UserProductScreen(userId: Int) {
    var userProducts by remember { mutableStateOf<List<UserProduct>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        try {
            // Проверим, правильно ли передается userId
            println("Fetching user products for userId: $userId")
            userProducts = RetrofitInstance.api.getUserProducts(userId)
        } catch (e: Exception) {
            errorMessage = "Ошибка: ${e.message}"
            println("Error fetching products: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (isLoading) {
            CircularProgressIndicator()
            Text(text = "Загрузка данных...", modifier = Modifier.padding(top = 8.dp))
        } else if (errorMessage != null) {
            Text(text = errorMessage ?: "Неизвестная ошибка", color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(userProducts) { product ->
                    ProductItem(product)
                }
            }
        }
    }
}

@Composable
fun ProductItem(product: UserProduct) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = "Продукт: ${product.product}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Единица измерения: ${product.user}", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Количество: ${product.quantity}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
    }
}


package com.example.groupproject

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query


// Интерфейс Retrofit для получения данных
interface ApiService {
    @GET("api/userproduct/")
    suspend fun getUserProducts(@Query("by_user_id") userId: Int): List<UserProduct>

    @GET("api/unit/")
    suspend fun getUnits(): List<Unit1>

    // Метод для добавления товара в корзину
    @POST("api/purchase/")
    suspend fun addToCart(@Body cartItem: CartItem): Response<Unit1>

    // Исправленный метод PUT для обновления количества товара
    @PUT("api/userproduct/{id}/")
    suspend fun updateProductQuantity(
        @Path("id") productId: Int,
        @Body product: UserProduct // Используйте правильный тип данных для обновления товара
    ): Response<UserProduct> // Ответ может быть объектом UserProduct, если это нужно
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
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }

    var units by remember { mutableStateOf<List<Unit1>>(emptyList()) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        try {
            // Проверим, правильно ли передается userId
            println("Fetching user products for userId: $userId")
            userProducts = RetrofitInstance.api.getUserProducts(userId)
            products = fetchProducts1()
            units = RetrofitInstance.api.getUnits()
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
                    ProductItem(product, products, units)
                }
            }
        }
    }
}

@Composable
fun ProductItem(product: UserProduct, products: List<Product>, units: List<Unit1>) {
    Column(modifier = Modifier.padding(8.dp)) {

        val targetId = product.product
        var targetName = "null"
        var targetUnitName = "null"
        var targetUnitId = 0

        for (value in products) {
            if (value.id == targetId) {
                targetUnitId = value.unit
                targetName = value.name
            }
        }

        for (value in units) {
            if (value.id == targetUnitId){
                targetUnitName = value.name
            }
        }

        Text(text = "Product: $targetName", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Unit of measurement: $targetUnitName", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Amount: ${product.quantity}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

suspend fun main() {
    val userProducts1 = RetrofitInstance.api.getUserProducts(1)
    println(userProducts1)
}


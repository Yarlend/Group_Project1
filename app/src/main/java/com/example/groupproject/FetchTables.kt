package com.example.groupproject

import CookieManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import getCsrfTokenAndSessionId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

suspend fun fetchProducts1(): List<Product> {
    return withContext(Dispatchers.IO) {
        val productResponse = performGetRequest("https://mobileee.pythonanywhere.com/api/product/")

        val products: List<Product>

        if (productResponse != null) {
            val gson = Gson()

            val productListType = object : TypeToken<List<Product>>() {}.type

            products = gson.fromJson(productResponse, productListType)

            return@withContext products
        } else {
            throw Exception("Не удалось загрузить данные с сервера.")
        }
    }
}



suspend fun main() {
    var response11 = fetchProducts1()
    println("Products list: $response11")
    println("Type response11: ${response11[0]::class}")

}
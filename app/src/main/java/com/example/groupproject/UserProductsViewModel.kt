package com.example.groupproject

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserProductsViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService: ApiService = Retrofit.Builder()
        .baseUrl("https://your-api-base-url.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    // LiveData для списка продуктов
    private val _userProducts = MutableLiveData<List<UserProduct>>()
    val userProducts: LiveData<List<UserProduct>> get() = _userProducts

    // LiveData для загрузки состояния
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Метод для получения списка товаров
    fun fetchUserProducts(userId: Int) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getUserProducts(userId)
                withContext(Dispatchers.Main) {
                    _userProducts.value = response
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("UserProductsViewModel", "Error fetching products: ${e.localizedMessage}")
                _isLoading.value = false
            }
        }
    }

    // Метод для обновления товара
    fun updateProductQuantity(productId: Int, newQuantity: Int, userId: Int) {
        val updatedProduct = UserProduct(
            id = productId,
            quantity = newQuantity,
            user = userId,
            product = productId // Идентификатор продукта
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.updateProductQuantity(productId, updatedProduct)
                if (response.isSuccessful) {
                    fetchUserProducts(userId) // Перезапрашиваем товары
                } else {
                    Log.e("UserProductsViewModel", "Failed to update product: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("UserProductsViewModel", "Error: ${e.localizedMessage}")
            }
        }
    }
}

package com.example.groupproject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SharedViewModel : ViewModel() {
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> get() = _cart

    private val _userProducts = MutableStateFlow<List<UserProduct>>(emptyList())
    val userProducts: StateFlow<List<UserProduct>> get() = _userProducts

    // Загрузка товаров пользователя
    fun fetchUserProducts(userId: Int) {
        viewModelScope.launch {
            try {
                val products = RetrofitInstance.api.getUserProducts(userId)
                _userProducts.value = products
            } catch (e: Exception) {
                println("Ошибка при загрузке товаров пользователя: ${e.message}")
            }
        }
    }

    // Добавление товара в корзину
    fun addToCart(userId: Int, productId: Int, productName: String, quantity: Int) {
        viewModelScope.launch {
            try {
                // Обновляем локальное состояние корзины
                _cart.value = _cart.value + CartItem(userId, productId, productName, quantity)

                // Имитация изменения данных пользователя
                val updatedProducts = _userProducts.value.map {
                    if (it.product == productId) {
                        it.copy(quantity = it.quantity + quantity)
                    } else it
                }
                _userProducts.value = updatedProducts
            } catch (e: Exception) {
                println("Ошибка добавления в корзину: ${e.message}")
            }
        }
    }
}

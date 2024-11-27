import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class CookieManager : CookieJar {
    private val cookieStore: MutableMap<HttpUrl, List<Cookie>> = mutableMapOf()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url] = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url] ?: emptyList()
    }
}

fun getCsrfTokenAndSessionId(): Pair<String, String>? {
    // URL для логина
    val loginUrl = "https://mobileee.pythonanywhere.com/api/login/"

    // Создаем JSON тело запроса
    val jsonInputString = """
        {
            "username": "user1",
            "password": "123"
        }
    """

    // Создаем клиент с CookieManager
    val cookieManager = CookieManager()
    val client = OkHttpClient.Builder()
        .cookieJar(cookieManager)
        .build()

    // Создаем POST запрос с логином
    val requestBody = jsonInputString
        .toRequestBody("application/json; charset=utf-8".toMediaType())

    val loginRequest = Request.Builder()
        .url(loginUrl)
        .post(requestBody)
        .build()

    client.newCall(loginRequest).execute().use { response ->
        if (response.isSuccessful) {
            println("Login successful!")

            // Получаем cookies после логина
            val cookies = cookieManager.loadForRequest(loginUrl.toHttpUrl())
            println("Cookies: $cookies")

            // Извлекаем csrftoken и sessionid
            val csrftoken = cookies.find { it.name == "csrftoken" }?.value
            val sessionid = cookies.find { it.name == "sessionid" }?.value

            return if (csrftoken != null && sessionid != null) {
                Pair(csrftoken, sessionid)
            } else {
                println("Ошибка: Не удалось получить CSRF токен или session id")
                null
            }

        } else {
            println("Login failed with code: ${response.code}")
            return null
        }
    }
}

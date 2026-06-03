package com.example.unifiedapp.ui.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.util.Log
import com.example.unifiedapp.remote.AuthApi

object ApiClient {

    const val AUTH_URL = "http://203.110.243.202:8000/"
    const val BASE_URL = "http://203.110.243.202:8000/"

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("API_DEBUG", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val detailInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        Log.d("API_REQUEST", "========== REQUEST ==========")
        Log.d("API_REQUEST", "URL: ${request.url}")
        Log.d("API_REQUEST", "Method: ${request.method}")
        Log.d("API_REQUEST", "Headers: ${request.headers}")
        request.body?.let {
            val buffer = okio.Buffer()
            it.writeTo(buffer)
            Log.d("API_REQUEST", "Body: ${buffer.readUtf8()}")
        }
        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val endTime = System.currentTimeMillis()
        Log.d("API_RESPONSE", "========== RESPONSE ==========")
        Log.d("API_RESPONSE", "Duration: ${endTime - startTime}ms")
        Log.d("API_RESPONSE", "Code: ${response.code}")
        Log.d("API_RESPONSE", "Message: ${response.message}")
        val responseBody = response.peekBody(Long.MAX_VALUE)
        Log.d("API_RESPONSE", "Body: ${responseBody.string()}")
        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(detailInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val emailApi: AuthApi by lazy { authApi }

    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(AUTH_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    fun testServerConnectivity(): Boolean {
        return try {
            val url = java.net.URL(AUTH_URL + "health")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.connect()
            val responseCode = connection.responseCode
            Log.d("API_DEBUG", "Server connectivity test: $responseCode")
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e("API_DEBUG", "Server connectivity failed: ${e.message}")
            false
        }
    }
}
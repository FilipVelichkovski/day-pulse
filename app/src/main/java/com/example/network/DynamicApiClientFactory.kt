package com.example.network

import com.example.model.ApiErrorResponse
import com.example.util.Formatters
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object DynamicApiClientFactory {

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val baseOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun create(rawBaseUrl: String, bearerToken: String): ActivityTrackerApi? {
        val normalizedUrl = Formatters.normalizeBaseUrl(rawBaseUrl) ?: return null
        val cleanToken = bearerToken.trim()

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val path = original.url.encodedPath
            val builder = original.newBuilder()
                .header("Accept", "application/json")
            // /api/v1/health requires no auth per server spec
            if (cleanToken.isNotEmpty() && !path.endsWith("/api/v1/health")) {
                builder.header("Authorization", "Bearer $cleanToken")
            }
            chain.proceed(builder.build())
        }

        val client = baseOkHttpClient.newBuilder()
            .addInterceptor(authInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ActivityTrackerApi::class.java)
    }

    fun parseHttpError(exception: HttpException): String {
        val code = exception.code()
        val rawBody = try {
            exception.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        val parsed = rawBody?.let { body ->
            try {
                moshi.adapter(ApiErrorResponse::class.java).fromJson(body)?.error
            } catch (_: Exception) {
                null
            }
        }
        return when (code) {
            401 -> "Unauthorized (401): ${parsed ?: "Invalid or missing Bearer token"}"
            400 -> "Bad Request (400): ${parsed ?: "Malformed request"}"
            else -> "HTTP $code: ${parsed ?: exception.message()}"
        }
    }
}

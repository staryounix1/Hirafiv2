package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClientProvider {

    private var cachedUrl: String? = null
    private var cachedService: SupabaseApiService? = null

    fun getApiServiceOrNull(): SupabaseApiService? {
        if (!SupabaseConfig.isConfigured) return null
        val url = SupabaseConfig.baseUrlWithSlash
        if (cachedService != null && cachedUrl == url) {
            return cachedService
        }
        // Never log request URLs or auth-related traffic in release builds.
        // BASIC logging is useful during local development but can expose identifiers
        // and private resource paths in production logcat.
        val okHttpBuilder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
        if (com.example.BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            okHttpBuilder.addInterceptor(logging)
        }
        val okHttp = okHttpBuilder.build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val service = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApiService::class.java)

        cachedUrl = url
        cachedService = service
        return service
    }
}

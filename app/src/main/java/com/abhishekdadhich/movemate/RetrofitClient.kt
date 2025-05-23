package com.abhishekdadhich.movemate

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // We'll add the API key interceptor here in the next step if we want a global one,
        // or pass it per call as defined in the ApiService interface.
        // For now, the ApiService expects it as a @Header parameter.
        .build()

    val instance: TfNSWApiService by lazy {
        Retrofit.Builder()
            .baseUrl(TfNSWApiService.BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TfNSWApiService::class.java)
    }
}
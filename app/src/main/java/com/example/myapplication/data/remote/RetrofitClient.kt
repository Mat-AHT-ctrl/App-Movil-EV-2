package com.example.myapplication.data.remote

import com.example.myapplication.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP unico de la app.
 *
 * La URL NO esta hardcodeada: viene de BuildConfig.BASE_URL, definida en
 * app/build.gradle.kts. Para apuntar a otro backend:
 *
 *   .\gradlew.bat :app:assembleDebug -PBACKEND_URL=http://10.0.2.2:8080/
 *
 * (10.0.2.2 es el localhost de tu PC visto desde el emulador Android)
 */
object RetrofitClient {

    /** Timeouts largos a proposito: el plan free de Render duerme el servicio. */
    private const val TIMEOUT_SEGUNDOS = 60L

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .callTimeout(TIMEOUT_SEGUNDOS + 30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /** Segundo cliente: API externa de feriados (dominio distinto al backend). */
    val feriadosApi: FeriadosApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://date.nager.at/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FeriadosApiService::class.java)
    }
}

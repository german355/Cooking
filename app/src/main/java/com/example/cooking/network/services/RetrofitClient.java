package com.example.cooking.network.services;

import android.util.Log;
import com.example.cooking.network.api.ApiService;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Класс для настройки Retrofit клиента
 */
public class RetrofitClient {
    private static final String BASE_URL = "http://89.35.130.107";
    private static final String TAG = "RetrofitClient";
    private static Retrofit retrofit = null;
    private static Retrofit ltrRetrofit = null;

    /**
     * Получить настроенный Retrofit клиент
     * 
     * @return Retrofit экземпляр
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            // Используем HttpClientManager для создания OkHttpClient с обработкой ошибок
            OkHttpClient client = HttpClientManager.getClient();

            // Проверяем, что URL заканчивается на слеш
            String baseUrl = BASE_URL;
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }

            // Создаем Retrofit с настроенным клиентом
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Log.d(TAG, "Создан Retrofit клиент с улучшенной обработкой сетевых ошибок");
        }
        return retrofit;
    }

    /**
     * Получить Retrofit клиент для LTR API
     * 
     * @param baseUrl Базовый URL для LTR API
     * @return Retrofit экземпляр для LTR API
     */
    public static Retrofit getLtrClient(String baseUrl) {
        // Проверяем, что URL заканчивается на слеш
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        // Используем HttpClientManager для создания OkHttpClient с обработкой ошибок
        OkHttpClient client = HttpClientManager.getClient();

        // Создаем новый Retrofit для LTR API
        ltrRetrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Log.d(TAG, "Создан Retrofit клиент для LTR API с URL: " + baseUrl);
        return ltrRetrofit;
    }

    /**
     * Получить API сервис
     * 
     * @return ApiService экземпляр
     */
    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }

    /**
     * Сброс Retrofit клиента для повторной инициализации
     * Может быть полезно при изменении настроек авторизации или сети
     */
    public static void resetClient() {
        retrofit = null;
        ltrRetrofit = null;
        Log.d(TAG, "Retrofit клиенты сброшены");
    }
}
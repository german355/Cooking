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
    private static final String BASE_URL = "http://r1.veroid.network:10009/";
    private static final String TAG = "RetrofitClient";
    private static Retrofit retrofit = null;

    /**
     * Получить настроенный Retrofit клиент
     * @return Retrofit экземпляр
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            // Используем HttpClientManager для создания OkHttpClient с обработкой ошибок
            OkHttpClient client = HttpClientManager.getClient();

            // Создаем Retrofit с настроенным клиентом
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Log.d(TAG, "Создан Retrofit клиент с улучшенной обработкой сетевых ошибок");
        }
        return retrofit;
    }

    /**
     * Получить API сервис
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
        Log.d(TAG, "Retrofit клиент сброшен");
    }
}
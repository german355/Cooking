package com.example.cooking.ServerWorker;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Класс для настройки Retrofit клиента
 */
public class RetrofitClient {
    private static final String BASE_URL = "http://g3.veroid.network:19029/";
    private static final String TAG = "RetrofitClient";
    private static Retrofit retrofit = null;
    
    /**
     * Получить настроенный Retrofit клиент
     * @return Retrofit экземпляр
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            // Настраиваем логирование запросов
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
                Log.d(TAG, message));
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // Настраиваем клиент с таймаутами и логированием
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .addInterceptor(loggingInterceptor)
                    .build();
            
            // Создаем Retrofit с настроенным клиентом
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
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
} 
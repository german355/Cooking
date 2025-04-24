package com.example.cooking.network.services;

import android.util.Log;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Менеджер для создания и настройки HTTP клиента с улучшенной обработкой ошибок
 */
public class HttpClientManager {
    private static final String TAG = "HttpClientManager";
    private static OkHttpClient client = null;
    
    // Увеличенные таймауты для предотвращения преждевременного обрыва соединения
    private static final int CONNECT_TIMEOUT = 30;
    private static final int READ_TIMEOUT = 30;
    private static final int WRITE_TIMEOUT = 30;
    
    /**
     * Получить настроенный OkHttpClient с улучшенной обработкой ошибок
     * @return OkHttpClient экземпляр
     */
    public static OkHttpClient getClient() {
        if (client == null) {
            // Создаем перехватчик для логирования
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
                Log.d(TAG, message));
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // Создаем перехватчик для повторных попыток при ошибках
            Interceptor retryInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    
                    // Максимальное количество попыток
                    int maxRetries = 3;
                    int retryCount = 0;
                    Response response = null;
                    
                    IOException lastException = null;
                    while (retryCount < maxRetries) {
                        try {
                            if (response != null) {
                                response.close();
                            }
                            
                            // Пытаемся выполнить запрос
                            response = chain.proceed(request);
                            
                            // Если успешно - возвращаем ответ
                            if (response.isSuccessful()) {
                                return response;
                            }
                            
                            // Если получили ошибку сервера, закрываем ответ и пробуем еще раз
                            if (response.code() >= 500) {
                                response.close();
                                retryCount++;
                                Log.w(TAG, "Повторная попытка запроса после ошибки сервера: " + response.code());
                                // Добавляем паузу между попытками
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    throw new IOException("Прервано ожидание", e);
                                }
                            } else {
                                // Если это не ошибка сервера, просто возвращаем ответ
                                return response;
                            }
                        } catch (IOException e) {
                            lastException = e;
                            retryCount++;
                            Log.w(TAG, "Повторная попытка запроса после сетевой ошибки: " + e.getMessage());
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new IOException("Прервано ожидание", ie);
                            }
                        }
                    }
                    
                    // Если все попытки не удались, выбрасываем последнее исключение
                    if (lastException != null) {
                        throw lastException;
                    }
                    
                    // Или возвращаем последний ответ
                    return response;
                }
            };
            
            // Настраиваем клиент с таймаутами и перехватчиками
            client = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .connectionPool(new ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(retryInterceptor)
                    .build();
            
            Log.d(TAG, "Создан HTTP клиент с улучшенной обработкой ошибок, отключенным Keep-Alive и HTTP/1.1");
        }
        
        return client;
    }
    
    /**
     * Сбросить HTTP клиент для повторной инициализации
     */
    public static void resetClient() {
        client = null;
        Log.d(TAG, "HTTP клиент сброшен");
    }
} 
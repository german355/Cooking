package com.example.cooking.network.services;

import android.util.Log;
import com.example.cooking.auth.UserLoginRequest;
import com.example.cooking.auth.UserRegisterRequest;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.network.api.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Сервис для работы с пользователем
 */
public class UserService {
    private static final String TAG = "UserService";
    private final ApiService apiService;
    
    public interface UserCallback {
        void onSuccess(ApiResponse response);
        void onFailure(String errorMessage);
    }
    
    public UserService() {
        this.apiService = RetrofitClient.getApiService();
    }
    
    /**
     * Регистрация пользователя после успешной регистрации в Firebase
     * @param email Email пользователя
     * @param name Имя пользователя
     * @param firebaseId ID пользователя в Firebase
     * @param callback Callback для обработки результата
     */
    public void registerFirebaseUser(String email, String name, String firebaseId, UserCallback callback) {
        Log.d(TAG, "Registering Firebase user: email=" + email + ", name=" + name + ", firebaseId=" + firebaseId);
        
        UserRegisterRequest request = new UserRegisterRequest(email, name, firebaseId);
        Call<ApiResponse> call = apiService.registerUser(request);
        
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    Log.d(TAG, "Register success: " + apiResponse.isSuccess());
                    
                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse);
                    } else {
                        callback.onFailure(apiResponse.getMessage());
                    }
                } else {
                    Log.e(TAG, "Register error code: " + response.code());
                    callback.onFailure("Ошибка сервера: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Register network error", t);
                callback.onFailure("Ошибка сети: " + t.getMessage());
            }
        });
    }
    
    /**
     * Вход пользователя после успешного входа в Firebase
     * @param email Email пользователя
     * @param firebaseId ID пользователя в Firebase
     * @param callback Callback для обработки результата
     */
    public void loginFirebaseUser(String email, String firebaseId, UserCallback callback) {
        Log.d(TAG, "Login Firebase user: email=" + email + ", firebaseId=" + firebaseId);
        
        UserLoginRequest request = new UserLoginRequest(email, firebaseId);
        Call<ApiResponse> call = apiService.loginUser(request);
        
        // Добавляем механизм повторных попыток на случай ошибки "unexpected end of stream"
        final int[] retryCount = {0};
        final int maxRetries = 3;
        
        Callback<ApiResponse> loginCallback = new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    Log.d(TAG, "Login success: " + apiResponse.isSuccess());
                    
                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse);
                    } else {
                        callback.onFailure(apiResponse.getMessage());
                    }
                } else {
                    Log.e(TAG, "Login error code: " + response.code());
                    
                    // Если мы получили ошибку 401, возможно нужно сбросить клиент и повторить попытку
                    if (response.code() == 401 && retryCount[0] < maxRetries) {
                        retryCount[0]++;
                        Log.d(TAG, "Повторная попытка входа после ошибки авторизации: " + retryCount[0]);
                        RetrofitClient.resetClient();
                        apiService.loginUser(request).enqueue(this);
                    } else {
                        callback.onFailure("Ошибка сервера: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Login network error", t);
                
                // Проверяем, является ли ошибка "unexpected end of stream" и повторяем попытку
                if (t.getMessage() != null && 
                    (t.getMessage().contains("unexpected end of stream") ||
                     t.getMessage().contains("timeout") ||
                     t.getMessage().contains("Connection reset")) && 
                    retryCount[0] < maxRetries) {
                    
                    retryCount[0]++;
                    Log.d(TAG, "Повторная попытка входа после ошибки сети: " + retryCount[0]);
                    
                    // Небольшая задержка перед повторной попыткой
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Прервано ожидание перед повторной попыткой", e);
                    }
                    
                    RetrofitClient.resetClient();
                    apiService.loginUser(request).enqueue(this);
                } else {
                    callback.onFailure("Ошибка сети: " + t.getMessage());
                }
            }
        };
        
        call.enqueue(loginCallback);
    }
} 
package com.example.cooking.ServerWorker;

import android.util.Log;
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
        
        call.enqueue(new Callback<ApiResponse>() {
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
                    callback.onFailure("Ошибка сервера: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Login network error", t);
                callback.onFailure("Ошибка сети: " + t.getMessage());
            }
        });
    }
} 
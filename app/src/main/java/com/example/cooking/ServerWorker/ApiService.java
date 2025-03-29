package com.example.cooking.ServerWorker;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Интерфейс API для взаимодействия с сервером
 */
public interface ApiService {
    
    /**
     * Регистрация пользователя
     * @param request запрос с данными регистрации
     * @return ответ сервера
     */
    @POST("register")
    Call<ApiResponse> registerUser(@Body UserRegisterRequest request);
    
    /**
     * Вход пользователя
     * @param request запрос с данными для входа
     * @return ответ сервера
     */
    @POST("login")
    Call<ApiResponse> loginUser(@Body UserLoginRequest request);
} 
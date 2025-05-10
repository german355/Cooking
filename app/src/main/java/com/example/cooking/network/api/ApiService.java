package com.example.cooking.network.api;

import com.example.cooking.auth.UserLoginRequest;
import com.example.cooking.auth.UserRegisterRequest;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.data.models.PasswordResetRequest;
import com.example.cooking.data.models.PasswordResetResponse;
import com.example.cooking.network.responses.RecipesResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Интерфейс API для взаимодействия с сервером
 */
public interface ApiService {
    
    /**
     * Регистрация пользователя
     * @param request запрос с данными регистрации
     * @return ответ сервера
     */
    @POST("auth/register")
    Call<ApiResponse> registerUser(@Body UserRegisterRequest request);
    
    /**
     * Вход пользователя
     * @param request запрос с данными для входа
     * @return ответ сервера
     */
    @POST("auth/login")
    Call<ApiResponse> loginUser(@Body UserLoginRequest request);

    /**
     * Получает список лайкнутых рецептов пользователя.
     * @param userId ID пользователя
     * @return Call объект с ответом типа RecipesResponse
     */
    @GET("recipes/liked")
    Call<RecipesResponse> getLikedRecipes(@Query("userId") String userId);

    /**
     * Простой поиск рецептов по строке.
     * @param query строка поиска
     * @return Call объект с ответом типа RecipesResponse
     */
    @GET("recipes/search-simple")
    Call<RecipesResponse> searchRecipesSimple(@Query("q") String query);

    @POST("auth/password-reset-request")
    Call<PasswordResetResponse> requestPasswordReset(@Body PasswordResetRequest request);
} 
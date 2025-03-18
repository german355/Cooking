package com.example.cooking.ServerWorker;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Интерфейс Retrofit для работы с API лайкнутых рецептов
 */
public interface LikedRecipesApi {
    
    /**
     * Метод для получения списка лайкнутых рецептов пользователя
     * @param userId ID пользователя
     * @return Call объект с ответом сервера
     */
    @GET("likedrecipes")
    Call<RecipesResponse> getLikedRecipes(@Query("userId") String userId);
    
    /**
     * Альтернативный метод для получения лайкнутых рецептов в виде строки
     * Используется как запасной вариант, когда возникают проблемы с десериализацией JSON
     * @param userId ID пользователя
     * @return Call объект с ответом сервера в виде строки
     */
    @GET("likedrecipes")
    Call<String> getLikedRecipesAsString(@Query("userId") String userId);
} 
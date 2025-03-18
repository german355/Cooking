package com.example.cooking.ServerWorker;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Интерфейс Retrofit для работы с API рецептов
 */
public interface RecipeApi {
    
    /**
     * Метод для получения списка всех рецептов
     * @return Call объект с ответом сервера
     */
    @GET("recipes")
    Call<RecipesResponse> getRecipes();
    
    /**
     * Альтернативный метод для получения рецептов в виде строки
     * Используется как запасной вариант, когда возникают проблемы с десериализацией JSON
     * @return Call объект с ответом сервера в виде строки
     */
    @GET("recipes")
    Call<String> getRecipesAsString();
} 
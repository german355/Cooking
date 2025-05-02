package com.example.cooking.network.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Интерфейс для взаимодействия с API рецептов через Retrofit
 */
public interface RecipeApiService {
    
    /**
     * Метод для добавления или редактирования рецепта
     * Если recipeId == null, создается новый рецепт
     * Если recipeId != null, редактируется существующий рецепт
     */
    @Multipart
    @POST("/recipes/add")
    Call<ResponseBody> addOrUpdateRecipe(
            @Part("title") RequestBody title,
            @Part("ingredients") RequestBody ingredients,
            @Part("instructions") RequestBody instructions,
            @Part("userId") RequestBody userId,
            @Part("recipeId") RequestBody recipeId,
            @Part MultipartBody.Part photo
    );
    
    /**
     * Метод для добавления или редактирования рецепта без фото
     */
    @Multipart
    @POST("/recipes/add")
    Call<ResponseBody> addOrUpdateRecipeWithoutPhoto(
            @Part("title") RequestBody title,
            @Part("ingredients") RequestBody ingredients,
            @Part("instructions") RequestBody instructions,
            @Part("userId") RequestBody userId,
            @Part("recipeId") RequestBody recipeId
    );
} 
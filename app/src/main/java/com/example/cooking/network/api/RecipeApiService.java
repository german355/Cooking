package com.example.cooking.network.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Header;

/**
 * Интерфейс для взаимодействия с API рецептов через Retrofit
 */
public interface RecipeApiService {
    
    /**
     * Метод для ДОБАВЛЕНИЯ нового рецепта с фото
     */
    @Multipart
    @POST("/recipes/add")
    Call<ResponseBody> addRecipe(
            @Part("title") RequestBody title,
            @Part("ingredients") RequestBody ingredients,
            @Part("instructions") RequestBody instructions,
            @Part("userId") RequestBody userId,
            @Part MultipartBody.Part photo
    );
    
    /**
     * Метод для ДОБАВЛЕНИЯ нового рецепта без фото
     */
    @Multipart
    @POST("/recipes/add")
    Call<ResponseBody> addRecipeWithoutPhoto(
            @Part("title") RequestBody title,
            @Part("ingredients") RequestBody ingredients,
            @Part("instructions") RequestBody instructions,
            @Part("userId") RequestBody userId
    );

    /**
     * Метод для ОБНОВЛЕНИЯ существующего рецепта
     */
    @Multipart
    @PUT("/recipes/update/{recipeId}")
    Call<ResponseBody> updateRecipe(
            @Path("recipeId") int recipeId,
            @Header("X-User-ID") String userIdHeader,
            @Header("X-User-Permission") String permissionHeader,
            @Part("title") RequestBody title,
            @Part("ingredients") RequestBody ingredients,
            @Part("instructions") RequestBody instructions,
            @Part MultipartBody.Part photo
    );
} 
package com.example.cooking.data.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.Recipe.Step;
import com.example.cooking.config.ServerConfig;
import com.example.cooking.network.api.LikedRecipesApi;
import com.example.cooking.network.responses.RecipesResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Упрощенный репозиторий для управления данными лайкнутых рецептов.
 */
public class LikedRecipesRepository {
    private static final String TAG = "LikedRecipesRepository";
    private static final String LIKED_RECIPES_CACHE_KEY = "cached_liked_recipes";
    private static final String LAST_UPDATE_TIME_KEY = "liked_recipes_last_update_time";
    private static final long CACHE_EXPIRATION_TIME = 30 * 60 * 100; // 3 min
    private static final String PREF_NAME = "liked_recipe_cache";
    private static final String API_URL = ServerConfig.BASE_API_URL;
    
    private final Context context;
    private final LikedRecipesApi likedRecipesApi;
    private static final Gson gson = new Gson();
    
    public interface LikedRecipesCallback {
        void onRecipesLoaded(List<Recipe> recipes);
        void onDataNotAvailable(String error);
    }
    
    public LikedRecipesRepository(Context context) {
        this.context = context;
        
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        
        Gson gsonConverter = new GsonBuilder()
                .setLenient()
                .create();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL + "/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gsonConverter))
                .build();
        
        likedRecipesApi = retrofit.create(LikedRecipesApi.class);
    }
    
    /**
     * Получает лайкнутые рецепты, сначала проверяя кэш, затем загружая с сервера.
     */
    public void getLikedRecipes(String userId, final LikedRecipesCallback callback) {
        Result<List<Recipe>> cachedResult = loadFromCache(userId);
        if (cachedResult.isSuccess() && !isCacheExpired(userId)) {
            callback.onRecipesLoaded(((Result.Success<List<Recipe>>) cachedResult).getData());
            return;
        }
        
        Call<RecipesResponse> call = likedRecipesApi.getLikedRecipes(userId);
        call.enqueue(new Callback<RecipesResponse>() {
            @Override
            public void onResponse(Call<RecipesResponse> call, Response<RecipesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RecipesResponse recipesResponse = response.body();
                    if (recipesResponse.isSuccess() && recipesResponse.getRecipes() != null) {
                        List<Recipe> recipes = recipesResponse.getRecipes();
                        saveToCache(userId, recipes);
                        callback.onRecipesLoaded(recipes);
                    } else {
                        if (cachedResult.isSuccess()) {
                           callback.onRecipesLoaded(((Result.Success<List<Recipe>>) cachedResult).getData());
                        } else {
                           callback.onDataNotAvailable("Ошибка в ответе сервера: " + recipesResponse.getMessage());
                        }
                    }
                } else {
                    if (cachedResult.isSuccess()) {
                           callback.onRecipesLoaded(((Result.Success<List<Recipe>>) cachedResult).getData());
                     } else {
                        callback.onDataNotAvailable("Ошибка HTTP: " + response.code());
                     }
                }
            }
            
            @Override
            public void onFailure(Call<RecipesResponse> call, Throwable t) {
                if (cachedResult.isSuccess()) {
                    callback.onRecipesLoaded(((Result.Success<List<Recipe>>) cachedResult).getData());
                 } else {
                    callback.onDataNotAvailable("Ошибка сети: " + t.getMessage());
                 }
            }
        });
    }
    
    /**
     * Сохраняет список лайкнутых рецептов в кэш.
     */
    private void saveToCache(String userId, List<Recipe> recipes) {
        try {
            JSONArray recipesArray = new JSONArray();
            for (Recipe recipe : recipes) {
                JSONObject recipeJson = new JSONObject();
                recipeJson.put("id", recipe.getId());
                recipeJson.put("title", recipe.getTitle());
                recipeJson.put("ingredients", gson.toJson(recipe.getIngredients()));
                recipeJson.put("instructions", gson.toJson(recipe.getSteps()));
                recipeJson.put("created_at", recipe.getCreated_at());
                recipeJson.put("userId", recipe.getUserId());
                recipeJson.put("photo", recipe.getPhoto_url());
                recipeJson.put("isLiked", recipe.isLiked());
                recipesArray.put(recipeJson);
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(LIKED_RECIPES_CACHE_KEY + "_" + userId, recipesArray.toString());
            editor.putLong(LAST_UPDATE_TIME_KEY + "_" + userId, System.currentTimeMillis());
            editor.apply();
            Log.d(TAG, "Сохранено в кэш лайкнутых рецептов: " + recipes.size());
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при кэшировании рецептов", e);
        }
    }
    
    /**
     * Загружает лайкнутые рецепты из кэша.
     */
    private Result<List<Recipe>> loadFromCache(String userId) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String cachedRecipes = prefs.getString(LIKED_RECIPES_CACHE_KEY + "_" + userId, null);
            if (cachedRecipes != null && !cachedRecipes.isEmpty()) {
                JSONArray recipesArray = new JSONArray(cachedRecipes);
                List<Recipe> recipes = new ArrayList<>();
                Type ingredientListType = new TypeToken<ArrayList<Ingredient>>() {}.getType();
                Type stepListType = new TypeToken<ArrayList<Step>>() {}.getType();

                for (int i = 0; i < recipesArray.length(); i++) {
                    JSONObject recipeJson = recipesArray.getJSONObject(i);
                    Recipe recipe = new Recipe();
                    recipe.setId(recipeJson.optInt("id", 0));
                    recipe.setTitle(recipeJson.optString("title", ""));

                    String ingredientsString = recipeJson.optString("ingredients", "");
                    String stepsString = recipeJson.optString("instructions", "");

                    List<Ingredient> ingredients = gson.fromJson(ingredientsString, ingredientListType);
                    List<Step> steps = gson.fromJson(stepsString, stepListType);

                    recipe.setIngredients(ingredients == null ? new ArrayList<>() : new ArrayList<>(ingredients));
                    recipe.setSteps(steps == null ? new ArrayList<>() : new ArrayList<>(steps));

                    recipe.setPhoto_url(recipeJson.optString("photo", ""));
                    recipe.setCreated_at(recipeJson.optString("created_at", ""));
                    recipe.setUserId(recipeJson.optString("userId", ""));
                    recipe.setLiked(recipeJson.optBoolean("isLiked", true));
                    recipes.add(recipe);
                }
                 Log.d(TAG, "Загружено из кэша лайкнутых рецептов: " + recipes.size());
                return new Result.Success<>(recipes);
            } else {
                 Log.d(TAG, "Кэш лайкнутых рецептов пуст или отсутствует");
                return new Result.Error<>("Кэш пуст");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при чтении кэша", e);
            return new Result.Error<>("Ошибка при чтении кэша: " + e.getMessage());
        }
    }
    
    /**
     * Проверяет, истек ли срок действия кэша для указанного пользователя.
     */
    private boolean isCacheExpired(String userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong(LAST_UPDATE_TIME_KEY + "_" + userId, 0);
        boolean expired = System.currentTimeMillis() - lastUpdateTime > CACHE_EXPIRATION_TIME;
        Log.d(TAG, "Проверка истечения кэша: " + (expired ? "Истек" : "Актуален"));
        return expired;
    }
    
    /**
     * Очищает кэш избранных рецептов для указанного пользователя
     */
    public void clearCache(String userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(LIKED_RECIPES_CACHE_KEY + "_" + userId);
        editor.remove(LAST_UPDATE_TIME_KEY + "_" + userId);
        editor.apply();
        Log.d(TAG, "Кэш избранных рецептов очищен для пользователя: " + userId);
    }
    
    /**
     * Обновляет статус лайка для рецепта
     * @param recipeId ID рецепта
     * @param userId ID пользователя
     * @param isLiked новый статус лайка
     */
    public void updateLikeStatus(int recipeId, String userId, boolean isLiked) {
        try {
            Log.d(TAG, "Обновление статуса лайка (заглушка): recipeId=" + recipeId + ", userId=" + userId + ", isLiked=" + isLiked);
            clearCache(userId);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении статуса лайка", e);
        }
    }
    
    /**
     * Класс результата операции.
     */
    abstract static class Result<T> {
        abstract boolean isSuccess();
        
        static class Success<T> extends Result<T> {
            private final T data;
            
            Success(T data) {
                this.data = data;
            }
            
            T getData() {
                return data;
            }
            
            @Override
            boolean isSuccess() {
                return true;
            }
        }
        
        static class Error<T> extends Result<T> {
            private final String errorMessage;
            
            Error(String errorMessage) {
                this.errorMessage = errorMessage;
            }
            
            String getErrorMessage() {
                return errorMessage;
            }
            
            @Override
            boolean isSuccess() {
                return false;
            }
        }
    }
} 
package com.example.cooking.data.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.config.ServerConfig;
import com.example.cooking.network.api.LikedRecipesApi;
import com.example.cooking.network.responses.RecipesResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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
        
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL + "/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        
        likedRecipesApi = retrofit.create(LikedRecipesApi.class);
    }
    
    /**
     * Получает лайкнутые рецепты, сначала проверяя кэш, затем загружая с сервера.
     */
    public void getLikedRecipes(String userId, final LikedRecipesCallback callback) {
        // Проверяем кэш

        
        // Загружаем с сервера
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
                        callback.onDataNotAvailable("Ошибка в ответе сервера");
                    }
                } else {
                    callback.onDataNotAvailable("Ошибка HTTP: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<RecipesResponse> call, Throwable t) {
                callback.onDataNotAvailable("Ошибка сети: " + t.getMessage());
                Result<List<Recipe>> cachedResult = loadFromCache(userId);
                if (cachedResult.isSuccess() && !isCacheExpired(userId)) {
                    callback.onRecipesLoaded(((Result.Success<List<Recipe>>) cachedResult).getData());
                    return;
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
                recipeJson.put("ingredients", recipe.getIngredients());
                recipeJson.put("instructions", recipe.getInstructions());
                recipeJson.put("created_at", recipe.getCreated_at());
                recipeJson.put("userId", recipe.getUserId());
                recipeJson.put("photo", recipe.getPhoto_url());
                recipesArray.put(recipeJson);
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(LIKED_RECIPES_CACHE_KEY + "_" + userId, recipesArray.toString());
            editor.putLong(LAST_UPDATE_TIME_KEY + "_" + userId, System.currentTimeMillis());
            editor.apply();
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
                for (int i = 0; i < recipesArray.length(); i++) {
                    JSONObject recipeJson = recipesArray.getJSONObject(i);
                    Recipe recipe = new Recipe();
                    recipe.setId(recipeJson.optInt("id", 0));
                    recipe.setTitle(recipeJson.optString("title", ""));
                    recipe.setIngredients(recipeJson.optString("ingredients", ""));
                    recipe.setInstructions(recipeJson.optString("instructions", ""));
                    recipe.setPhoto_url(recipeJson.optString("photo", ""));
                    recipe.setCreated_at(recipeJson.optString("created_at", ""));
                    recipe.setUserId(recipeJson.optString("userId", ""));
                    recipes.add(recipe);
                }
                return new Result.Success<>(recipes);
            } else {
                return new Result.Error<>("Кэш пуст");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при чтении кэша", e);
            return new Result.Error<>("Ошибка при чтении кэша");
        }
    }
    
    /**
     * Проверяет, истек ли срок действия кэша для указанного пользователя.
     */
    private boolean isCacheExpired(String userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong(LAST_UPDATE_TIME_KEY + "_" + userId, 0);
        return System.currentTimeMillis() - lastUpdateTime > CACHE_EXPIRATION_TIME;
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
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("recipeId", recipeId);
            jsonObject.put("userId", userId);
            
            Log.d(TAG, "Отправка запроса на обновление статуса лайка: recipeId=" + recipeId + ", userId=" + userId + ", isLiked=" + isLiked);
            
            // Очищаем кэш, чтобы обеспечить актуальность данных при следующей загрузке
            clearCache(userId);
            
            // Здесь может быть вызов API для обновления статуса лайка на сервере
            // Это заглушка, поскольку в текущей реализации этот метод вызывается из других мест
            Log.d(TAG, "Статус лайка обновлен: " + recipeId + ", новый статус: " + isLiked);
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
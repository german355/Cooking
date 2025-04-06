package com.example.cooking.utils;

import android.content.Context;
import android.util.Log;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.database.AppDatabase;
import com.example.cooking.data.database.RecipeDao;
import com.example.cooking.data.database.RecipeEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Сервис для поиска рецептов
 */
public class RecipeSearchService {
    
    private static final String TAG = "RecipeSearchService";
    private final RecipeDao recipeDao;
    private final Executor executor;
    
    public interface SearchCallback {
        void onSearchResults(List<Recipe> recipes);
        void onSearchError(String error);
    }
    
    public RecipeSearchService(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        recipeDao = database.recipeDao();
        executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Выполняет поиск рецептов по заголовку
     * @param query поисковый запрос
     * @param callback колбэк для возврата результатов
     */
    public void searchRecipes(String query, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onSearchResults(new ArrayList<>());
            return;
        }
        
        executor.execute(() -> {
            try {
                Log.d(TAG, "Выполняется поиск по запросу: " + query);
                List<RecipeEntity> entities = recipeDao.searchRecipesByTitle(query);
                Log.d(TAG, "Найдено рецептов: " + entities.size());
                
                // Конвертируем Entity в модели Recipe
                List<Recipe> recipes = new ArrayList<>();
                for (RecipeEntity entity : entities) {
                    recipes.add(entity.toRecipe());
                }
                
                // Отправляем результаты через колбэк
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onSearchResults(recipes));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка поиска: " + e.getMessage(), e);
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onSearchError(e.getMessage()));
            }
        });
    }
}
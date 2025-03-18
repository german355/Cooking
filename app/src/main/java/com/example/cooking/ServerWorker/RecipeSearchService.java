package com.example.cooking.ServerWorker;

import android.os.AsyncTask;
import android.util.Log;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ServerWorker.RecipeRepository.Result;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для поиска рецептов.
 * Отделен от RecipeRepository для лучшего разделения ответственности.
 */
public class RecipeSearchService {
    private static final String TAG = "RecipeSearchService";
    private final RecipeRepository recipeRepository;

    public interface SearchCallback {
        void onSearchResults(List<Recipe> recipes);

        void onSearchError(String error);
    }

    public RecipeSearchService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    /**
     * Выполняет поиск рецептов по названию.
     * @param query Поисковый запрос
     * @param callback Callback для возврата результатов
     */
    public void searchByTitle(String query, SearchCallback callback) {
        new TitleSearchTask(recipeRepository, query, callback).execute();
    }

    /**
     * Выполняет расширенный поиск рецептов по названию и ингредиентам.
     * @param query Поисковый запрос
     * @param callback Callback для возврата результатов
     */
    public void searchByTitleAndIngredients(String query, SearchCallback callback) {
        new AdvancedSearchTask(recipeRepository, query, callback).execute();
    }

    /**
     * Статический класс для поиска по названию без утечек памяти
     */
    private static class TitleSearchTask extends AsyncTask<Void, Void, List<Recipe>> {
        private final WeakReference<SearchCallback> callbackRef;
        private final RecipeRepository repository;
        private final String query;
        private String errorMessage = null;

        TitleSearchTask(RecipeRepository repository, String query, SearchCallback callback) {
            this.repository = repository;
            this.query = query;
            this.callbackRef = new WeakReference<>(callback);
        }

        @Override
        protected List<Recipe> doInBackground(Void... voids) {
            Log.d(TAG, "Поиск рецептов по названию: " + query);
            
            // Синхронно получаем рецепты из кэша
            Result<List<Recipe>> result = repository.loadFromCache();
            
            if (!result.isSuccess()) {
                RecipeRepository.Result.Error<List<Recipe>> error = 
                    (RecipeRepository.Result.Error<List<Recipe>>) result;
                errorMessage = error.getErrorMessage();
                Log.e(TAG, "Ошибка при загрузке рецептов из кэша: " + errorMessage);
                return null;
            }
            
            RecipeRepository.Result.Success<List<Recipe>> success = 
                (RecipeRepository.Result.Success<List<Recipe>>) result;
            List<Recipe> recipes = success.getData();
            
            // Если запрос пустой, возвращаем все рецепты
            if (query == null || query.trim().isEmpty()) {
                return recipes;
            }

            // Фильтруем рецепты по названию
            List<Recipe> filteredRecipes = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase().trim();

            for (Recipe recipe : recipes) {
                if (recipe.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                    filteredRecipes.add(recipe);
                }
            }

            Log.d(TAG, "Найдено рецептов: " + filteredRecipes.size());
            return filteredRecipes;
        }

        @Override
        protected void onPostExecute(List<Recipe> recipes) {
            SearchCallback callback = callbackRef.get();
            if (callback != null) {
                if (recipes != null) {
                    callback.onSearchResults(recipes);
                } else {
                    callback.onSearchError(errorMessage);
                }
            }
        }
    }


    /**
     * Статический класс для расширенного поиска без утечек памяти
     */
    private static class AdvancedSearchTask extends AsyncTask<Void, Void, List<Recipe>> {
        private final WeakReference<SearchCallback> callbackRef;
        private final RecipeRepository repository;
        private final String query;
        private String errorMessage = null;

        AdvancedSearchTask(RecipeRepository repository, String query, SearchCallback callback) {
            this.repository = repository;
            this.query = query;
            this.callbackRef = new WeakReference<>(callback);
        }

        @Override
        protected List<Recipe> doInBackground(Void... voids) {
            Log.d(TAG, "Поиск рецептов по названию и ингредиентам: " + query);
            
            // Синхронно получаем рецепты из кэша
            Result<List<Recipe>> result = repository.loadFromCache();
            
            if (!result.isSuccess()) {
                RecipeRepository.Result.Error<List<Recipe>> error = 
                    (RecipeRepository.Result.Error<List<Recipe>>) result;
                errorMessage = error.getErrorMessage();
                Log.e(TAG, "Ошибка при загрузке рецептов из кэша: " + errorMessage);
                return null;
            }
            
            RecipeRepository.Result.Success<List<Recipe>> success = 
                (RecipeRepository.Result.Success<List<Recipe>>) result;
            List<Recipe> recipes = success.getData();
            
            // Если запрос пустой, возвращаем все рецепты
            if (query == null || query.trim().isEmpty()) {
                return recipes;
            }

            // Фильтруем рецепты по названию и ингредиентам
            List<Recipe> filteredRecipes = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase().trim();

            for (Recipe recipe : recipes) {
                if (recipe.getTitle().toLowerCase().contains(lowerCaseQuery) || 
                    recipe.getIngredients().toLowerCase().contains(lowerCaseQuery)) {
                    filteredRecipes.add(recipe);
                }
            }

            Log.d(TAG, "Найдено рецептов: " + filteredRecipes.size());
            return filteredRecipes;
        }

        @Override
        protected void onPostExecute(List<Recipe> recipes) {
            SearchCallback callback = callbackRef.get();
            if (callback != null) {
                if (recipes != null) {
                    callback.onSearchResults(recipes);
                } else {
                    callback.onSearchError(errorMessage);
                }
            }
        }
    }

}
package com.example.cooking.ServerWorker;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;
import com.example.cooking.Recipe.Recipe;

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
            final List<Recipe>[] result = new List[1];
            final String[] error = new String[1];

            // Получаем рецепты из кэша
            repository.getRecipesFromCache(new RecipeRepository.RecipesCallback() {
                @Override
                public void onRecipesLoaded(List<Recipe> recipes) {
                    result[0] = recipes;
                }

                @Override
                public void onDataNotAvailable(String errorMsg) {
                    error[0] = errorMsg;
                }
            });

            // Проверяем, получили ли мы рецепты
            if (result[0] == null) {
                errorMessage = error[0] != null ? error[0] : "Не удалось получить рецепты из кэша";
                return null;
            }

            // Если запрос пустой, возвращаем все рецепты
            if (query == null || query.trim().isEmpty()) {
                return result[0];
            }

            // Фильтруем рецепты по названию
            List<Recipe> filteredRecipes = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase().trim();

            for (Recipe recipe : result[0]) {
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
            Log.d(TAG, "Расширенный поиск рецептов: " + query);
            final List<Recipe>[] result = new List[1];
            final String[] error = new String[1];

            // Получаем рецепты из кэша
            repository.getRecipesFromCache(new RecipeRepository.RecipesCallback() {
                @Override
                public void onRecipesLoaded(List<Recipe> recipes) {
                    result[0] = recipes;
                }

                @Override
                public void onDataNotAvailable(String errorMsg) {
                    error[0] = errorMsg;
                }
            });

            // Проверяем, получили ли мы рецепты
            if (result[0] == null) {
                errorMessage = error[0] != null ? error[0] : "Не удалось получить рецепты из кэша";
                return null;
            }

            // Если запрос пустой, возвращаем все рецепты
            if (query == null || query.trim().isEmpty()) {
                return result[0];
            }

            // Фильтруем рецепты по названию и ингредиентам
            List<Recipe> filteredRecipes = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase().trim();

            for (Recipe recipe : result[0]) {
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
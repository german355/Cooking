package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.utils.MySharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel для FavoritesFragment
 * Управляет данными избранных рецептов
 */
public class FavoritesViewModel extends AndroidViewModel {
    
    private static final String TAG = "FavoritesViewModel";
    private final LikedRecipesRepository likedRecipesRepository;
    private final ExecutorService executor;
    private final MySharedPreferences preferences;
    
    // LiveData для хранения состояния UI
    private final MutableLiveData<List<Recipe>> likedRecipes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private String userId;
    private List<Recipe> originalLikedRecipes = new ArrayList<>();
    
    public FavoritesViewModel(@NonNull Application application) {
        super(application);
        likedRecipesRepository = new LikedRecipesRepository(application);
        preferences = new MySharedPreferences(application);
        executor = Executors.newSingleThreadExecutor();
        userId = preferences.getString("userId", "0");
    }
    
    /**
     * @return LiveData со списком избранных рецептов
     */
    public LiveData<List<Recipe>> getLikedRecipes() {
        return likedRecipes;
    }
    
    /**
     * @return LiveData с состоянием загрузки
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    /**
     * @return LiveData с сообщением об ошибке
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * @return LiveData с состоянием обновления
     */
    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }
    
    /**
     * Проверяет авторизацию пользователя
     * @return true если пользователь авторизован
     */
    public boolean isUserLoggedIn() {
        return userId != null && !userId.equals("0");
    }
    
    /**
     * Загружает список избранных рецептов
     */
    public void loadLikedRecipes() {
        if (!isUserLoggedIn()) {
            errorMessage.setValue("Для просмотра избранных рецептов необходимо войти в аккаунт");
            likedRecipes.setValue(new ArrayList<>());
            originalLikedRecipes = new ArrayList<>();
            return;
        }
        
        isLoading.setValue(true);
        
        likedRecipesRepository.getLikedRecipes(userId, new LikedRecipesRepository.LikedRecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                processFetchedRecipes(recipes);
                isLoading.setValue(false);
            }
            
            @Override
            public void onDataNotAvailable(String error) {
                errorMessage.setValue("Не удалось загрузить лайкнутые рецепты: " + error);
                likedRecipes.setValue(new ArrayList<>());
                originalLikedRecipes = new ArrayList<>();
                isLoading.setValue(false);
                Log.e(TAG, "Ошибка при загрузке лайкнутых рецептов: " + error);
            }
        });
    }
    
    /**
     * Обновляет список избранных рецептов
     */
    public void refreshLikedRecipes() {
        if (!isUserLoggedIn()) {
            errorMessage.setValue("Для просмотра избранных рецептов необходимо войти в аккаунт");
            isRefreshing.setValue(false);
            return;
        }
        
        isRefreshing.setValue(true);
        likedRecipesRepository.clearCache(userId);
        
        likedRecipesRepository.getLikedRecipes(userId, new LikedRecipesRepository.LikedRecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                processFetchedRecipes(recipes);
                isRefreshing.setValue(false);
            }
            
            @Override
            public void onDataNotAvailable(String error) {
                errorMessage.setValue("Ошибка обновления списка: " + error);
                isRefreshing.setValue(false);
                Log.e(TAG, "Ошибка при обновлении лайкнутых рецептов: " + error);
            }
        });
    }
    
    /**
     * Выполняет поиск по локальному списку рецептов
     */
    public void performSearch(String query) {
        if (originalLikedRecipes == null || originalLikedRecipes.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Выполняется поиск в избранном: " + query);
        
        if (query == null || query.trim().isEmpty()) {
            likedRecipes.setValue(new ArrayList<>(originalLikedRecipes));
            errorMessage.setValue(null);
            return;
        }
        
        List<Recipe> filteredRecipes = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase().trim();
        
        for (Recipe recipe : originalLikedRecipes) {
            boolean titleMatch = recipe.getTitle() != null && recipe.getTitle().toLowerCase().contains(lowerCaseQuery);
            boolean ingredientMatch = false;
            
            if (recipe.getIngredients() != null) {
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient.getName() != null && ingredient.getName().toLowerCase().contains(lowerCaseQuery)) {
                        ingredientMatch = true;
                        break;
                    }
                }
            }
            
            if (titleMatch || ingredientMatch) {
                filteredRecipes.add(recipe);
            }
        }
        
        likedRecipes.setValue(filteredRecipes);
        
        if (filteredRecipes.isEmpty()) {
            errorMessage.setValue("По запросу \"" + query + "\" ничего не найдено");
        } else {
            errorMessage.setValue(null);
        }
    }
    
    /**
     * Обновляет состояние лайка для рецепта и отправляет изменения на сервер
     */
    public void toggleLikeStatus(Recipe recipe, boolean isLiked) {
        if (!isUserLoggedIn()) {
            errorMessage.setValue("Для управления избранными рецептами необходимо войти в аккаунт");
            return;
        }
        
        List<Recipe> currentList = originalLikedRecipes;
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        
        List<Recipe> updatedList = new ArrayList<>(currentList);
        
        if (!isLiked) {
            updatedList.removeIf(r -> r.getId() == recipe.getId());
            originalLikedRecipes = new ArrayList<>(updatedList);
        } else {
            recipe.setLiked(true);
            boolean found = false;
            for (Recipe r : updatedList) {
                if (r.getId() == recipe.getId()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                updatedList.add(recipe);
                originalLikedRecipes = new ArrayList<>(updatedList);
            }
        }
        
        likedRecipes.setValue(updatedList);
        
        executeIfActive(() -> {
            try {
                likedRecipesRepository.updateLikeStatus(recipe.getId(), userId, isLiked);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обновлении статуса лайка на сервере", e);
            }
        });
    }
    
    /**
     * Обновляет статус лайка рецепта
     * @param recipe рецепт
     * @param isLiked новый статус лайка
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked) {
        toggleLikeStatus(recipe, isLiked);
    }
    
    /**
     * Обрабатывает загруженные с сервера рецепты
     */
    private void processFetchedRecipes(List<Recipe> recipes) {
        if (recipes == null) {
            recipes = new ArrayList<>();
        }
        
        for (Recipe recipe : recipes) {
            recipe.setLiked(true);
        }
        
        originalLikedRecipes = new ArrayList<>(recipes);
        likedRecipes.setValue(new ArrayList<>(recipes));
        errorMessage.setValue(null);
        Log.d(TAG, "Обработано и установлено " + recipes.size() + " избранных рецептов");
    }
    
    /**
     * Обновляет список избранных рецептов (например, после выхода из аккаунта)
     */
    public void updateLikedRecipes(List<Recipe> recipes) {
        if (recipes == null) {
            recipes = new ArrayList<>();
        }
        processFetchedRecipes(recipes);
    }
    
    /**
     * Очищает ресурсы при уничтожении ViewModel
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
        Log.d(TAG, "FavoritesViewModel cleared");
    }
    
    /**
     * Метод, выполняющий операцию в executor с проверкой его состояния
     * @param task задача для выполнения
     */
    private void executeIfActive(Runnable task) {
        if (!executor.isShutdown() && !executor.isTerminated()) {
            try {
                executor.execute(task);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при выполнении задачи в Executor", e);
            }
        }
    }
    
    /**
     * Возвращает ID текущего пользователя
     * @return ID пользователя
     */
    public String getUserId() {
        return userId;
    }
} 
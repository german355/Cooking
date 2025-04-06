package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.RecipeLocalRepository;
import com.example.cooking.data.repositories.RecipeRemoteRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel для HomeFragment
 */
public class HomeViewModel extends AndroidViewModel {
    
    private static final String TAG = "HomeViewModel";
    private final RecipeLocalRepository localRepository;
    private final RecipeRemoteRepository remoteRepository;
    private final ExecutorService executor;
    
    // LiveData для состояния загрузки и ошибок
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    public HomeViewModel(@NonNull Application application) {
        super(application);
        localRepository = new RecipeLocalRepository(application);
        remoteRepository = new RecipeRemoteRepository(application);
        executor = Executors.newFixedThreadPool(2);
    }
    
    /**
     * Получить LiveData со списком рецептов из локального хранилища
     */
    public LiveData<List<Recipe>> getRecipes() {
        return localRepository.getAllRecipes();
    }
    
    /**
     * Получить LiveData с состоянием обновления данных
     */
    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }
    
    /**
     * Получить LiveData с сообщением об ошибке
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Загрузить рецепты с сервера и обновить локальную базу данных.
     * Эта операция будет запущена в фоне.
     */
    public void refreshRecipes() {
        isRefreshing.setValue(true);
        
        remoteRepository.getRecipes(new RecipeRemoteRepository.RecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                // Сохраняем рецепты в локальное хранилище
                executeIfActive(() -> {
                    try {
                        // Используем синхронную операцию очистки и вставки для гарантии целостности данных
                        localRepository.clearAllSync();
                        Log.d(TAG, "Локальная база данных очищена перед обновлением");
                        
                        // Затем вставляем актуальные рецепты с сервера
                        localRepository.insertAll(recipes);
                        Log.d(TAG, "Рецепты успешно сохранены в локальное хранилище: " + recipes.size());
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка сохранения рецептов в локальное хранилище", e);
                    } finally {
                        isRefreshing.postValue(false);
                    }
                });
            }
            
            @Override
            public void onDataNotAvailable(String error) {
                Log.e(TAG, "Ошибка загрузки рецептов с сервера: " + error);
                errorMessage.postValue(error);
                isRefreshing.postValue(false);
            }
        });
    }
    
    /**
     * Обновить состояние лайка для рецепта
     * @param recipe рецепт
     * @param isLiked новое состояние лайка
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked) {
        recipe.setLiked(isLiked);
        
        // Обновляем рецепт в локальной базе
        executeIfActive(() -> {
            localRepository.updateLikeStatus(recipe.getId(), isLiked);
        });
        
        // Отправляем обновление на сервер
        remoteRepository.updateLikeStatus(recipe, isLiked);
    }
    
    /**
     * Метод, выполняющий операцию в executor с проверкой его состояния
     * @param task задача для выполнения
     */
    private void executeIfActive(Runnable task) {
        if (!executor.isShutdown()) {
            executor.execute(task);
        } else {
            Log.w(TAG, "Executor уже завершен, пропускаем задачу");
        }
    }
    
    /**
     * Очистить ресурсы
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }

    /**
     * Обновляет статус "лайка" для рецепта в локальной базе данных
     * @param recipeId ID рецепта
     * @param isLiked статус лайка (true - лайкнут, false - не лайкнут)
     */
    public void updateLocalLikeStatus(int recipeId, boolean isLiked) {
        if (executor.isShutdown()) {
            Log.e(TAG, "Executor уже завершен, невозможно выполнить операцию");
            return;
        }
        
        executor.execute(() -> {
            try {
                Log.d(TAG, "Обновляем статус лайка в локальной базе данных для рецепта " + recipeId + " на " + isLiked);
                // Получаем рецепт из локальной базы данных
                Recipe recipe = localRepository.getRecipeById(recipeId);
                
                if (recipe != null) {
                    // Обновляем статус лайка
                    recipe.setLiked(isLiked);
                    // Сохраняем изменения в базе данных
                    localRepository.update(recipe);
                    Log.d(TAG, "Статус лайка успешно обновлен в базе данных");
                } else {
                    Log.d(TAG, "Рецепт с ID " + recipeId + " не найден в локальной базе данных");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обновлении статуса лайка в локальной базе данных: " + e.getMessage(), e);
            }
        });
    }
} 
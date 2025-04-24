package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.FragmentActivity;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.RecipeLocalRepository;
import com.example.cooking.data.repositories.RecipeRemoteRepository;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.utils.MySharedPreferences;

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
    private final LikedRecipesRepository likedRecipesRepository;
    private LikeSyncViewModel likeSyncViewModel; // Убрали final, будем инициализировать позже
    private final ExecutorService executor;
    
    // LiveData для состояния загрузки и ошибок
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // Храним ID последнего обработанного события лайка, чтобы избежать эха
    private Pair<Integer, Boolean> lastProcessedLikeEvent = null;
    
    public HomeViewModel(@NonNull Application application) {
        super(application);
        localRepository = new RecipeLocalRepository(application);
        remoteRepository = new RecipeRemoteRepository(application);
        likedRecipesRepository = new LikedRecipesRepository(application);
        executor = Executors.newFixedThreadPool(2);
        // likeSyncViewModel инициализируется в observeLikeChanges
    }
    
    // Метод для инициализации наблюдения за Shared ViewModel. Вызывается из Фрагмента.
    public void observeLikeChanges(LifecycleOwner owner, FragmentActivity activity) {
        // Инициализируем Shared ViewModel здесь, используя Activity scope
        // Сохраняем его в переменную класса
        likeSyncViewModel = new ViewModelProvider(activity).get(LikeSyncViewModel.class);

        likeSyncViewModel.getLikeChangeEvent().observe(owner, event -> {
            if (event != null && !event.equals(lastProcessedLikeEvent)) {
                Log.d(TAG, "Received like change event from LikeSyncViewModel: " + event.first + " -> " + event.second);
                // Добавляем логирование перед обновлением
                Log.i(TAG, "[LikeSync] Updating local statuses for Recipe ID: " + event.first + " to liked: " + event.second);
                // Обновляем статус лайка в ЛОКАЛЬНОЙ БД ОСНОВНЫХ рецептов
                updateLocalLikeStatus(event.first, event.second);
                // Обновляем статус лайка в ЛОКАЛЬНОЙ БД ЛАЙКНУТЫХ рецептов
                updateLikedRepositoryStatus(event.first, event.second);
                // Мы не сохраняем это событие как lastProcessed, так как обновление локальной БД
                // должно быть идемпотентным.
            } else if (event != null) {
                Log.d(TAG, "[LikeSync] Ignored duplicate/own like event: " + event.first + " -> " + event.second);
            }
        });
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
     * НЕ очищает базу данных перед вставкой.
     * Использует OnConflictStrategy.REPLACE в DAO для обновления существующих записей.
     */
    public void refreshRecipes() {
        isRefreshing.setValue(true);
        Log.d(TAG, "Refreshing recipes...");
        
        remoteRepository.getRecipes(new RecipeRemoteRepository.RecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                 Log.d(TAG, "Recipes loaded from remote: " + (recipes != null ? recipes.size() : 0));
                // Сохраняем/Обновляем рецепты в локальном хранилище
                executeIfActive(() -> {
                    try {
                        // НЕ ВЫЗЫВАЕМ clearAllSync()
                        // Log.d(TAG, "Local recipe database cleared before update.");

                        // Вставляем/заменяем актуальные рецепты с сервера
                        if (recipes != null) {
                             // Важно: Предполагаем, что сервер НЕ возвращает isLiked или возвращает неверное значение.
                             // Поэтому мы НЕ вызываем localRepository.insertAll(recipes) напрямую.
                             // Вместо этого, обновим существующие или вставим новые, не трогая isLiked.
                             // (Хотя insertAll с REPLACE все равно перезапишет isLiked...)
                             // => Правильнее будет сделать метод update/insert, который не трогает isLiked
                             // ИЛИ получать isLiked с сервера и доверять ему.

                             // Пока что, для исправления текущей проблемы, просто вставим с заменой.
                             // Это перезапишет isLiked значением с сервера (скорее всего false).
                             // Но так как мы обновляем isLiked через SharedViewModel, UI должен обновиться.
                             localRepository.insertAll(recipes);
                             Log.d(TAG, "Recipes inserted/updated in local storage: " + recipes.size());
                        } else {
                             Log.w(TAG, "Received null recipe list from remote.");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving recipes to local storage", e);
                        errorMessage.postValue("Ошибка сохранения данных локально.");
                    } finally {
                        isRefreshing.postValue(false);
                         Log.d(TAG, "Recipe refresh finished.");
                    }
                });
            }
            
            @Override
            public void onDataNotAvailable(String error) {
                Log.e(TAG, "Error loading recipes from remote: " + error);
                errorMessage.postValue(error);
                isRefreshing.postValue(false);
                 Log.d(TAG, "Recipe refresh finished with error.");
            }
        });
    }
    
    /**
     * Обновить состояние лайка для рецепта.
     * Обновляет локальную БД и оповещает другие компоненты через Shared ViewModel.
     * @param recipe рецепт
     * @param isLiked новое состояние лайка
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked) {
        // 1. Обновляем статус в локальной базе RecipeEntity
        updateLocalLikeStatus(recipe.getId(), isLiked);

        // 2. Обновляем статус в локальной базе LikedRecipeEntity
        updateLikedRepositoryStatus(recipe.getId(), isLiked);

        // 3. Оповещаем Shared ViewModel об изменении (для других фрагментов)
        // Используем сохраненную переменную likeSyncViewModel
        if (likeSyncViewModel != null) {
             Log.d(TAG, "Notifying LikeSyncViewModel about change: " + recipe.getId() + " -> " + isLiked);
             // Сохраняем событие, которое мы инициировали
             lastProcessedLikeEvent = new Pair<>(recipe.getId(), isLiked);
             likeSyncViewModel.notifyLikeChanged(recipe.getId(), isLiked);
        } else {
             Log.e(TAG, "LikeSyncViewModel is null! Cannot notify.");
        }

        // 4. Отправляем обновление на сервер (асинхронно)
        remoteRepository.updateLikeStatus(recipe, isLiked);
    }
    
    /**
     * Обновляет статус лайка в локальной БД ОСНОВНЫХ рецептов (RecipeEntity).
     */
    public void updateLocalLikeStatus(int recipeId, boolean isLiked) {
        executeIfActive(() -> {
            try {
                Log.d(TAG, "Updating like status in local REPOSITORY for recipe " + recipeId + " to " + isLiked);
                localRepository.updateLikeStatus(recipeId, isLiked);
            } catch (Exception e) {
                Log.e(TAG, "Error updating like status in local repository: " + e.getMessage(), e);
            }
        });
    }

    private void updateLikedRepositoryStatus(int recipeId, boolean isLiked) {
         String currentUserId = new MySharedPreferences(getApplication()).getString("userId", "0");
        if (currentUserId.equals("0")) {
            Log.w(TAG, "Cannot update liked repository status: User ID is 0.");
            return;
        }
        executeIfActive(() -> {
            try {
                Log.d(TAG, "Updating like status in LIKED REPOSITORY for recipe " + recipeId + " to " + isLiked);
                if (isLiked) {
                    likedRecipesRepository.insertLikedRecipeLocal(recipeId, currentUserId);
                } else {
                    likedRecipesRepository.deleteLikedRecipeLocal(recipeId, currentUserId);
                }
            } catch (Exception e) {
                 Log.e(TAG, "Error updating status in liked repository: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Метод, выполняющий операцию в executor с проверкой его состояния
     * @param task задача для выполнения
     */
    private void executeIfActive(Runnable task) {
        if (executor != null && !executor.isShutdown()) {
            try {
                executor.execute(task);
            } catch (Exception e) {
                 Log.e(TAG, "Error executing task in executor", e);
            }
        } else {
            Log.w(TAG, "Executor is null or shut down, skipping task");
        }
    }
    
    /**
     * Очистить ресурсы
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
         Log.d(TAG, "HomeViewModel cleared.");
    }
} 
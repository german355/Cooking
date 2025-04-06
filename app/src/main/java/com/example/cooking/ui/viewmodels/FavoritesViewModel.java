package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
            return;
        }
        
        isLoading.setValue(true);
        
        likedRecipesRepository.getLikedRecipes(userId, new LikedRecipesRepository.LikedRecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                // Обрабатываем загруженные рецепты
                processFetchedRecipes(recipes);
                isLoading.setValue(false);
            }
            
            @Override
            public void onDataNotAvailable(String error) {
                errorMessage.setValue("Не удалось загрузить лайкнутые рецепты: " + error);
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
            return;
        }
        
        isRefreshing.setValue(true);
        
        likedRecipesRepository.getLikedRecipes(userId, new LikedRecipesRepository.LikedRecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                // Обрабатываем загруженные рецепты
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
        if (likedRecipes.getValue() == null || likedRecipes.getValue().isEmpty()) {
            errorMessage.setValue("Список лайкнутых рецептов пуст");
            return;
        }
        
        Log.d(TAG, "Выполняется поиск в избранном: " + query);
        
        if (query.isEmpty()) {
            return; // Не делаем поиск для пустого запроса
        }
        
        List<Recipe> filteredRecipes = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();
        
        for (Recipe recipe : likedRecipes.getValue()) {
            // Проверяем совпадение в названии или ингредиентах
            if (recipe.getTitle().toLowerCase().contains(lowerCaseQuery) || 
                recipe.getIngredients().toLowerCase().contains(lowerCaseQuery)) {
                filteredRecipes.add(recipe);
            }
        }
        
        // Обновляем LiveData с отфильтрованными рецептами
        likedRecipes.setValue(filteredRecipes);
        
        if (filteredRecipes.isEmpty()) {
            errorMessage.setValue("По запросу \"" + query + "\" ничего не найдено");
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
        
        // Создаем новый список на основе текущего
        List<Recipe> currentList = likedRecipes.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        
        List<Recipe> updatedList = new ArrayList<>(currentList);
        
        // Если удаляем из избранного, удаляем рецепт из списка
        if (!isLiked) {
            updatedList.removeIf(r -> r.getId() == recipe.getId());
        }
        
        // Обновляем LiveData со списком избранных рецептов
        likedRecipes.setValue(updatedList);
        
        // Отправляем запрос на сервер в фоновом потоке
        executeIfActive(() -> {
            try {
                // Здесь логика отправки запроса на сервер через LikedRecipesRepository
                likedRecipesRepository.updateLikeStatus(recipe.getId(), userId, isLiked);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обновлении статуса лайка", e);
            }
        });
    }
    
    /**
     * Обновляет статус лайка рецепта
     * @param recipe рецепт
     * @param isLiked новый статус лайка
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked) {
        // Переиспользуем метод toggleLikeStatus для обновления лайка
        toggleLikeStatus(recipe, isLiked);
    }
    
    /**
     * Обрабатывает загруженные с сервера рецепты (удаляет дубликаты, проставляет статусы)
     */
    private void processFetchedRecipes(List<Recipe> recipes) {
        List<Recipe> processedRecipes = new ArrayList<>();
        
        for (Recipe recipe : recipes) {
            recipe.setLiked(true);
            
            // Проверяем, есть ли уже этот рецепт в списке по ID
            boolean isDuplicate = false;
            for (Recipe existing : processedRecipes) {
                if (existing.getId() == recipe.getId()) {
                    isDuplicate = true;
                    break;
                }
            }
            
            if (!isDuplicate) {
                processedRecipes.add(recipe);
            } else {
                Log.w(TAG, "Найден дубликат рецепта с id: " + recipe.getId() + ", пропускаем");
            }
        }
        
        Log.d(TAG, "Обработано " + processedRecipes.size() + " избранных рецептов");
        likedRecipes.setValue(processedRecipes);
    }
    
    /**
     * Обновляет список избранных рецептов
     * @param recipes новый список рецептов
     */
    public void updateLikedRecipes(List<Recipe> recipes) {
        // Убеждаемся, что обновление происходит в UI потоке
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            likedRecipes.setValue(recipes);
        } else {
            likedRecipes.postValue(recipes);
        }
        Log.d(TAG, "LiveData likedRecipes обновлен с " + (recipes != null ? recipes.size() : 0) + " рецептами");
    }
    
    /**
     * Очищает ресурсы при уничтожении ViewModel
     */
    @Override
    protected void onCleared() {
        executor.shutdown();
        super.onCleared();
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
     * Возвращает ID текущего пользователя
     * @return ID пользователя
     */
    public String getUserId() {
        return preferences.getUser().getUserId();
    }
} 
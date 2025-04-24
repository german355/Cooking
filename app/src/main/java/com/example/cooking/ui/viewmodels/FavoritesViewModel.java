package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModelProvider;

import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.data.repositories.RecipeLocalRepository;
import com.example.cooking.data.repositories.RecipeRemoteRepository;
import com.example.cooking.utils.MySharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ViewModel для FavoritesFragment
 * Управляет данными избранных рецептов
 */
public class FavoritesViewModel extends AndroidViewModel {
    
    private static final String TAG = "FavoritesViewModel";
    private final LikedRecipesRepository likedRecipesRepository;
    private final RecipeLocalRepository recipeLocalRepository;
    private LikeSyncViewModel likeSyncViewModel;
    private final ExecutorService executor;
    private final MySharedPreferences preferences;
    private final RecipeRemoteRepository remoteRepository;


    // LiveData для состояния UI (загрузка, ошибки, поиск)
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentSearchQuery = new MutableLiveData<>(""); // Для фильтрации
    
    // Храним ID последнего обработанного события лайка, чтобы избежать эха
    private Pair<Integer, Boolean> lastProcessedLikeEvent = null;
    
    private String userId;
    private LiveData<List<Recipe>> repositoryLikedRecipes; // LiveData из репозитория
    
    // Периодическая синхронизация (оставлена без изменений)
    private Runnable syncRunnable;
    private android.os.Handler syncHandler;
    
    public FavoritesViewModel(@NonNull Application application) {
        super(application);
        likedRecipesRepository = new LikedRecipesRepository(application);
        recipeLocalRepository = new RecipeLocalRepository(application);
        remoteRepository = new RecipeRemoteRepository(application);
        preferences = new MySharedPreferences(application);
        executor = Executors.newSingleThreadExecutor();
        userId = preferences.getString("userId", "0");
        syncHandler = new android.os.Handler();
        
        // Инициализируем LiveData из репозитория
        if (isUserLoggedIn()) {
            repositoryLikedRecipes = likedRecipesRepository.getLikedRecipes(userId);
        } else {
            // Если пользователь не вошел, создаем пустой LiveData
            MutableLiveData<List<Recipe>> emptyData = new MutableLiveData<>();
            emptyData.setValue(new ArrayList<>());
            repositoryLikedRecipes = emptyData;
        }
        // likeSyncViewModel инициализируется в observeLikeChanges
        startPeriodicSync();
    }
    
    // Метод для инициализации наблюдения за Shared ViewModel. Вызывается из Фрагмента.
    public void observeLikeChanges(LifecycleOwner owner, FragmentActivity activity) {
        // Инициализируем и сохраняем Shared ViewModel
        likeSyncViewModel = new ViewModelProvider(activity).get(LikeSyncViewModel.class);

        likeSyncViewModel.getLikeChangeEvent().observe(owner, event -> {
            if (event != null && !event.equals(lastProcessedLikeEvent)) {
                Log.d(TAG, "Received like change event from LikeSyncViewModel: " + event.first + " -> " + event.second);
                // Обновляем статус лайка в ЛОКАЛЬНОЙ БД ОСНОВНЫХ рецептов
                updateLocalRecipeLikeStatus(event.first, event.second);
                // Обновляем статус лайка в ЛОКАЛЬНОЙ БД ЛАЙКНУТЫХ рецептов
                // (это вызовет обновление repositoryLikedRecipes, если статус изменился)
                likedRecipesRepository.updateLikeStatusLocal(event.first, userId, event.second);
            }
        });
    }
    
    /**
     * Возвращает LiveData со списком избранных рецептов (фильтрованным по поиску).
     * Использует Transformations.switchMap для реагирования на изменения поискового запроса.
     */
    public LiveData<List<Recipe>> getFilteredLikedRecipes() {
        // Если пользователь не авторизован, всегда возвращаем пустой список
        if (!isUserLoggedIn()) {
             MutableLiveData<List<Recipe>> emptyData = new MutableLiveData<>();
             emptyData.setValue(new ArrayList<>());
             errorMessage.setValue("Для просмотра избранных рецептов необходимо войти в аккаунт");
             return emptyData;
        }
        errorMessage.setValue(null); // Сброс ошибки, если пользователь вошел
        
        return Transformations.switchMap(currentSearchQuery, query -> 
            Transformations.map(repositoryLikedRecipes, recipes -> {
                if (query == null || query.trim().isEmpty()) {
                    Log.d(TAG, "Фильтрация неактивна, возвращаем все лайкнутые: " + (recipes != null ? recipes.size() : 0));
                    return recipes != null ? recipes : new ArrayList<>();
                }
                List<Recipe> filteredList = new ArrayList<>();
                if (recipes != null) {
                     String lowerCaseQuery = query.toLowerCase().trim();
                    for (Recipe recipe : recipes) {
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
                             filteredList.add(recipe);
                         }
                    }
                }
                Log.d(TAG, "Фильтрация по '" + query + "', найдено: " + filteredList.size());
                 if (filteredList.isEmpty() && !query.trim().isEmpty()) {
                     errorMessage.postValue("По запросу \"" + query + "\" ничего не найдено");
                 } else {
                     errorMessage.postValue(null);
                 }
                return filteredList;
            })
        );
    }
    
    /**
     * @return LiveData с состоянием загрузки
     */
    public LiveData<Boolean> getIsLoading() {
        // Логику isLoading нужно будет пересмотреть, т.к. загрузка теперь управляется LiveData репозитория
        // Можно установить isLoading в true при инициализации и false, когда repositoryLikedRecipes выдаст первые данные
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
        // Аналогично isLoading, isRefreshing требует пересмотра
        // Можно установить true при вызове refresh и false после получения данных
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
     * Загружает/инициализирует список избранных рецептов (если еще не сделано).
     * Фактически, теперь это просто проверка авторизации, т.к. LiveData инициализируется в конструкторе.
     */
    public void loadLikedRecipes() {
        if (!isUserLoggedIn()) {
            errorMessage.setValue("Для просмотра избранных рецептов необходимо войти в аккаунт");
            // LiveData уже инициализирован пустым списком в конструкторе
        } else {
             errorMessage.setValue(null); // Сбрасываем ошибку, если пользователь авторизован
             // Можно инициировать принудительную синхронизацию, если нужно
             // likedRecipesRepository.syncLikedRecipesFromServerIfNeeded(userId); // Метод приватный в репозитории
             // Или просто позволить репозиторию самому синхронизироваться
             Log.d(TAG, "loadLikedRecipes: LiveData уже инициализировано.");
        }
        // isLoading.setValue(true); // Устанавливать isLoading лучше при наблюдении
    }
    
    /**
     * Обновляет список избранных рецептов (принудительно).
     * Теперь просто триггерит синхронизацию в репозитории.
     */
    public void refreshLikedRecipes() {
        if (!isUserLoggedIn()) {
            errorMessage.setValue("Для просмотра избранных рецептов необходимо войти в аккаунт");
            isRefreshing.setValue(false);
            return;
        }
        isRefreshing.setValue(true);
        // TODO: Добавить публичный метод в LikedRecipesRepository для принудительной синхронизации
        // Например: likedRecipesRepository.forceSyncLikedRecipes(userId);
        // Пока что просто логируем, т.к. репозиторий синхронизируется сам.
         Log.d(TAG, "refreshLikedRecipes: Запрос на обновление (реальная синхронизация зависит от репозитория)");
        // Устанавливать isRefreshing в false лучше, когда LiveData обновится
        // Это можно сделать через наблюдение в ViewModel или во Fragment
         // Временно убираем установку false здесь:
         // isRefreshing.setValue(false); 
    }
    
    /**
     * Устанавливает поисковый запрос.
     */
    public void performSearch(String query) {
        currentSearchQuery.setValue(query != null ? query : "");
    }
    
    /**
     * Обновляет состояние лайка для рецепта.
     * Обновляет ОБЕ локальные БД (LikedRecipeEntity и RecipeEntity) и оповещает SharedViewModel.
     * UI обновится автоматически через LiveData.
     */
    public void toggleLikeStatus(Recipe recipe, boolean isLiked) {
        if (!isUserLoggedIn()) {
            errorMessage.setValue("Для управления избранными рецептами необходимо войти в аккаунт");
            return;
        }
        
        // 1. Обновляем статус в ЛОКАЛЬНОЙ БД ЛАЙКНУТЫХ рецептов
        executeIfActive(() -> {
            Log.d(TAG, "Toggle like (LikedRepo): recipeId=" + recipe.getId() + ", userId=" + userId + ", isLiked=" + isLiked);
            likedRecipesRepository.updateLikeStatusLocal(recipe.getId(), userId, isLiked);
        });

        // 2. Обновляем статус в ЛОКАЛЬНОЙ БД ОСНОВНЫХ рецептов
        updateLocalRecipeLikeStatus(recipe.getId(), isLiked);

        updateLikedRepositoryStatus(recipe.getId(), isLiked);

        // 3. Оповещаем Shared ViewModel об изменении
        // Используем сохраненную переменную likeSyncViewModel
        if (likeSyncViewModel != null) {
            Log.d(TAG, "Notifying LikeSyncViewModel about change: " + recipe.getId() + " -> " + isLiked);
            // Сохраняем событие, которое мы инициировали
            lastProcessedLikeEvent = new Pair<>(recipe.getId(), isLiked);
            likeSyncViewModel.notifyLikeChanged(recipe.getId(), isLiked);
        } else {
            Log.e(TAG, "LikeSyncViewModel is null! Cannot notify.");
        }

        remoteRepository.updateLikeStatus(recipe, isLiked);

    }

    /**
     * Обновляет статус лайка в локальной БД ОСНОВНЫХ рецептов (RecipeEntity).
     */
    private void updateLocalRecipeLikeStatus(int recipeId, boolean isLiked) {
        executeIfActive(() -> {
             try {
                 Log.d(TAG, "Updating like status in local MAIN REPOSITORY for recipe " + recipeId + " to " + isLiked);
                 recipeLocalRepository.updateLikeStatus(recipeId, isLiked);
             } catch (Exception e) {
                 Log.e(TAG, "Error updating like status in local main repository: " + e.getMessage(), e);
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
     * Обновляет статус лайка рецепта (просто вызывает toggleLikeStatus).
     * @param recipe рецепт
     * @param isLiked новый статус лайка
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked) {
        toggleLikeStatus(recipe, isLiked);
    }
    
    /**
     * Обновляет список избранных рецептов (например, после выхода из аккаунта).
     * Теперь просто переинициализирует LiveData репозитория.
     */
    public void updateLikedRecipesOnLogout() {
         Log.d(TAG, "Выход пользователя, очистка LiveData избранного.");
         userId = "0"; // Сбрасываем userId
         MutableLiveData<List<Recipe>> emptyData = new MutableLiveData<>();
         emptyData.setValue(new ArrayList<>());
         repositoryLikedRecipes = emptyData; // Устанавливаем пустой LiveData
         currentSearchQuery.setValue(""); // Сбрасываем поиск
         errorMessage.setValue("Для просмотра избранных рецептов необходимо войти в аккаунт");
    }
    
    // TODO: Нужен метод для обновления при входе пользователя (пересоздать repositoryLikedRecipes с новым userId)
    public void updateUser(String newUserId) {
        Log.d(TAG, "Вход пользователя: " + newUserId);
        userId = newUserId;
        if (isUserLoggedIn()) {
            repositoryLikedRecipes = likedRecipesRepository.getLikedRecipes(userId);
            errorMessage.setValue(null);
        } else {
            updateLikedRecipesOnLogout();
        }
    }
    
    /**
     * Запускает периодическую синхронизацию лайкнутых рецептов с сервером
     */
    private void startPeriodicSync() {
        // Логика периодической синхронизации может потребовать пересмотра
        // Возможно, ее лучше делать напрямую в репозитории или через WorkManager
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                if (isUserLoggedIn()) {
                    Log.d(TAG, "Периодическая синхронизация избранного...");
                    // TODO: Вызвать метод принудительной синхронизации репозитория
                    // likedRecipesRepository.forceSyncLikedRecipes(userId);
                }
                syncHandler.postDelayed(this, 60000 * 5); // 5 минут
            }
        };
        syncHandler.postDelayed(syncRunnable, 60000 * 5); // Запуск через 5 минут
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }
        executor.shutdownNow();
        Log.d(TAG, "FavoritesViewModel cleared");
    }
    
    /**
     * Метод, выполняющий операцию в executor с проверкой его состояния
     * @param task задача для выполнения
     */
    private void executeIfActive(Runnable task) {
        if (executor != null && !executor.isShutdown() && !executor.isTerminated()) {
            try {
                executor.execute(task);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при выполнении задачи в Executor", e);
            }
        } else {
             Log.w(TAG, "Executor is null or shut down, skipping task");
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
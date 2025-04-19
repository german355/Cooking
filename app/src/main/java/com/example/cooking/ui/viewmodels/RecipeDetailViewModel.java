package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.database.RecipeEntity;
import com.example.cooking.data.repositories.RecipeRepository;
import com.example.cooking.network.services.RecipeDeleter;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.data.repositories.RecipeLocalRepository;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ViewModel для RecipeDetailActivity
 * Управляет данными и логикой экрана детального просмотра рецепта
 */
public class RecipeDetailViewModel extends AndroidViewModel {

    private static final String TAG = "RecipeDetailViewModel";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ExecutorService executor;
    private final RecipeDeleter recipeDeleter;
    private final RecipeLocalRepository localRepository;
    private final RecipeRepository recipeRepository;
    private final MySharedPreferences preferences;

    // LiveData для хранения состояний
    private final MutableLiveData<Recipe> recipe = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLiked = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isDeleting = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasEditPermission = new MutableLiveData<>(false);

    public RecipeDetailViewModel(@NonNull Application application) {
        super(application);
        client = new OkHttpClient();
        executor = Executors.newSingleThreadExecutor();
        recipeDeleter = new RecipeDeleter(application);
        localRepository = new RecipeLocalRepository(application);
        recipeRepository = new RecipeRepository(application);
        preferences = new MySharedPreferences(application);
    }

    /**
     * Загружает данные рецепта по ID из кэша репозитория.
     * @param recipeId ID рецепта для загрузки.
     */
    public void loadRecipe(int recipeId) {
        if (recipeId == -1) {
            errorMessage.postValue("Неверный ID рецепта.");
            return;
        }
        Log.d(TAG, "Загрузка рецепта с ID: " + recipeId + " из кэша репозитория");
        executeIfActive(() -> {
            RecipeRepository.Result<List<Recipe>> cachedResult = recipeRepository.loadFromCache();
            boolean needServer = false;
            List<Recipe> cachedRecipes = null;
            if (cachedResult.isSuccess()) {
                cachedRecipes = ((RecipeRepository.Result.Success<List<Recipe>>) cachedResult).getData();
                Recipe foundRecipe = null;
                for (Recipe r : cachedRecipes) {
                    if (r.getId() == recipeId) {
                        foundRecipe = r;
                        break;
                    }
                }
                if (foundRecipe != null) {
                    recipe.postValue(foundRecipe);
                    isLiked.postValue(foundRecipe.isLiked());
                    checkEditPermission(foundRecipe.getUserId());
                    Log.d(TAG, "Рецепт ID " + recipeId + " найден в кэше: " + foundRecipe.getTitle());
                    return;
                } else {
                    Log.e(TAG, "Рецепт с ID " + recipeId + " не найден в загруженном кэше. Пробуем загрузить с сервера.");
                    needServer = true;
                }
            } else {
                String error = ((RecipeRepository.Result.Error<List<Recipe>>) cachedResult).getErrorMessage();
                Log.e(TAG, "Ошибка загрузки рецептов из кэша: " + error + ". Пробуем загрузить с сервера.");
                needServer = true;
            }
            if (needServer) {
                recipeRepository.loadRecipeFromServer(recipeId, new RecipeRepository.RecipeCallback() {
                    @Override
                    public void onRecipeLoaded(Recipe loadedRecipe) {
                        recipe.postValue(loadedRecipe);
                        isLiked.postValue(loadedRecipe.isLiked());
                        checkEditPermission(loadedRecipe.getUserId());
                        // Сохраняем в локальное хранилище для будущего использования
                        localRepository.insertAll(java.util.Collections.singletonList(loadedRecipe));
                        Log.d(TAG, "Рецепт ID " + recipeId + " успешно загружен с сервера и сохранён локально.");
                    }
                    @Override
                    public void onDataNotAvailable(String error) {
                        errorMessage.postValue("Не удалось загрузить рецепт с сервера: " + error);
                        Log.e(TAG, "Не удалось загрузить рецепт с сервера: " + error);
                    }
                });
            }
        });
    }

    /**
     * Проверяет, имеет ли пользователь права на редактирование рецепта
     */
    private void checkEditPermission(String recipeUserId) {
        String currentUserId = preferences.getString("userId", "0");
        int permission = preferences.getInt("permission", 1);
        
        // Пользователь может редактировать, если он автор или администратор
        boolean canEdit = (recipeUserId != null && recipeUserId.equals(currentUserId)) || permission == 2;
        hasEditPermission.postValue(canEdit);
    }

    /**
     * Возвращает LiveData с информацией о рецепте
     */
    public LiveData<Recipe> getRecipe() {
        return recipe;
    }

    /**
     * Возвращает LiveData с состоянием лайка
     */
    public LiveData<Boolean> isLiked() {
        return isLiked;
    }

    /**
     * Возвращает LiveData с состоянием процесса удаления
     */
    public LiveData<Boolean> isDeleting() {
        return isDeleting;
    }

    /**
     * Возвращает LiveData с результатом удаления
     */
    public LiveData<Boolean> isDeleteSuccess() {
        return deleteSuccess;
    }

    /**
     * Возвращает LiveData с текстом ошибки
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Возвращает LiveData с информацией о наличии прав на редактирование
     */
    public LiveData<Boolean> hasEditPermission() {
        return hasEditPermission;
    }
    
    /**
     * Изменяет состояние "лайка" для рецепта
     */
    public void toggleLike() {
        Recipe currentRecipe = recipe.getValue();
        if (currentRecipe == null) {
            errorMessage.postValue("Данные рецепта еще не загружены.");
            return;
        }
        
        String userId = preferences.getString("userId", "0");
        if (userId.equals("0")) {
            errorMessage.postValue("Для добавления в избранное необходимо войти в аккаунт");
            return;
        }
        
        boolean currentLikeState = isLiked.getValue() != null ? isLiked.getValue() : false;
        boolean newLikeState = !currentLikeState;
        isLiked.postValue(newLikeState);
        currentRecipe.setLiked(newLikeState);
        
        sendLikeRequest(userId, currentRecipe.getId(), newLikeState);
        
        executeIfActive(() -> {
            localRepository.updateLikeStatus(currentRecipe.getId(), newLikeState);
            Log.d(TAG, "Статус лайка для ID " + currentRecipe.getId() + " обновлен в локальной БД на " + newLikeState);
        });
    }
    
    /**
     * Отправляет запрос на сервер для изменения статуса "лайк"
     */
    private void sendLikeRequest(String userId, int recipeId, boolean isLiked) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("recipeId", recipeId);
            jsonObject.put("userId", userId);
            
            String jsonBody = jsonObject.toString();
            Log.d(TAG, "Отправка запроса лайка: " + jsonBody);
            
            RequestBody body = RequestBody.create(jsonBody, JSON);
            
            Request request = new Request.Builder()
                    .url(com.example.cooking.config.ServerConfig.getFullUrl("/like"))
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Ошибка сети при изменении лайка", e);
                    errorMessage.postValue("Ошибка сети: " + e.getMessage());
                }
                
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Успешный ответ от сервера на изменение лайка");
                    } else {
                        Log.e(TAG, "Ошибка сервера при изменении лайка: " + response.code() + " " + response.message());
                        String responseBody = response.body() != null ? response.body().string() : "No body";
                        Log.e(TAG, "Тело ответа: " + responseBody);
                        errorMessage.postValue("Ошибка сервера: " + response.code());
                    }
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании запроса на изменение лайка", e);
            errorMessage.postValue("Ошибка приложения: " + e.getMessage());
        }
    }
    
    /**
     * Выполняет удаление рецепта
     * @param recipeId ID удаляемого рецепта
     */
    public void deleteRecipe(int recipeId) {
        Recipe currentRecipe = recipe.getValue();
        if (currentRecipe == null || currentRecipe.getId() != recipeId) {
            Log.e(TAG, "Невозможно удалить: текущий рецепт не совпадает с ID " + recipeId);
            errorMessage.postValue("Ошибка удаления: Несоответствие данных.");
            return;
        }
        
        String currentUserId = preferences.getString("userId", "0");
        int permission = preferences.getInt("permission", 1);
        
        boolean canDelete = (currentRecipe.getUserId() != null && currentRecipe.getUserId().equals(currentUserId)) || permission == 2;
        if (!canDelete) {
            errorMessage.postValue("У вас нет прав на удаление этого рецепта.");
            return;
        }
        
        isDeleting.postValue(true);
        
        recipeDeleter.deleteRecipe(recipeId, currentUserId, permission,
                new RecipeDeleter.DeleteRecipeCallback() {
            @Override
            public void onDeleteSuccess() {
                Log.d(TAG, "Рецепт с ID " + recipeId + " успешно удален.");
                recipe.postValue(null);
                isDeleting.postValue(false);
                deleteSuccess.postValue(true);
                // Удаляем рецепт из локальной базы данных
                localRepository.deleteRecipe(recipeId);
            }
            @Override
            public void onDeleteFailure(String error) {
                Log.e(TAG, "Ошибка удаления рецепта ID " + recipeId + ": " + error);
                errorMessage.postValue("Ошибка удаления: " + error);
                isDeleting.postValue(false);
                deleteSuccess.postValue(false);
            }
        });
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }

    /**
     * Выполняет задачу в фоновом потоке, если ViewModel активна.
     */
    private void executeIfActive(Runnable task) {
        if (!executor.isShutdown()) {
            executor.execute(task);
        }
    }
}
package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.RecipeLocalRepository;
import com.example.cooking.data.repositories.RecipeRepository;
import com.example.cooking.network.services.RecipeDeleter;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.ui.fragments.FavoritesFragment;

import org.json.JSONObject;

import java.io.IOException;
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
        preferences = new MySharedPreferences(application);
    }
    
    /**
     * Инициализирует ViewModel с данными рецепта
     */
    public void initWithRecipe(int recipeId, String title, String ingredients, String instructions, 
                             String createdAt, String userId, String photoUrl, boolean isLiked) {
        Recipe recipeData = new Recipe();
        recipeData.setId(recipeId);
        recipeData.setTitle(title);
        recipeData.setIngredients(ingredients);
        recipeData.setInstructions(instructions);
        recipeData.setCreated_at(createdAt);
        recipeData.setUserId(userId);
        recipeData.setPhoto_url(photoUrl);
        recipeData.setLiked(isLiked);
        
        this.recipe.setValue(recipeData);
        this.isLiked.setValue(isLiked);
        
        // Проверяем права на редактирование
        checkEditPermission(userId);
    }
    
    /**
     * Проверяет, имеет ли пользователь права на редактирование рецепта
     */
    private void checkEditPermission(String recipeUserId) {
        String currentUserId = preferences.getString("userId", "0");
        int permission = preferences.getInt("permission", 1);
        
        // Пользователь может редактировать, если он автор или администратор
        boolean canEdit = (recipeUserId != null && recipeUserId.equals(currentUserId)) || permission == 2;
        hasEditPermission.setValue(canEdit);
    }
    
    /**
     * @return LiveData с объектом рецепта
     */
    public LiveData<Recipe> getRecipe() {
        return recipe;
    }
    
    /**
     * @return LiveData с состоянием "лайкнутости" рецепта
     */
    public LiveData<Boolean> getIsLiked() {
        return isLiked;
    }
    
    /**
     * @return LiveData с состоянием процесса удаления
     */
    public LiveData<Boolean> getIsDeleting() {
        return isDeleting;
    }
    
    /**
     * @return LiveData с успешностью удаления
     */
    public LiveData<Boolean> getDeleteSuccess() {
        return deleteSuccess;
    }
    
    /**
     * @return LiveData с сообщением об ошибке
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * @return LiveData с наличием прав на редактирование
     */
    public LiveData<Boolean> getHasEditPermission() {
        return hasEditPermission;
    }
    
    /**
     * Изменяет состояние "лайка" для рецепта
     */
    public void toggleLike() {
        if (recipe.getValue() == null) {
            return;
        }
        
        String userId = preferences.getString("userId", "0");
        if (userId.equals("0")) {
            errorMessage.setValue("Для добавления в избранное необходимо войти в аккаунт");
            return;
        }
        
        boolean newLikeState = !isLiked.getValue();
        isLiked.setValue(newLikeState);
        
        // Обновляем рецепт
        Recipe currentRecipe = recipe.getValue();
        currentRecipe.setLiked(newLikeState);
        recipe.setValue(currentRecipe);
        
        // Отправляем запрос на сервер
        sendLikeRequest(userId, currentRecipe.getId(), newLikeState);
        
        // Обновляем локальную базу данных
        executeIfActive(() -> {
            localRepository.updateLikeStatus(currentRecipe.getId(), newLikeState);
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
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Ошибка сети при изменении лайка", e);
                    errorMessage.postValue("Ошибка сети: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Успешный ответ от сервера на изменение лайка");
                    } else {
                        Log.e(TAG, "Ошибка сервера при изменении лайка: " + response.code());
                        errorMessage.postValue("Ошибка сервера: " + response.code());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании запроса на изменение лайка", e);
            errorMessage.setValue("Ошибка приложения: " + e.getMessage());
        }
    }
    
    /**
     * Выполняет удаление рецепта
     */
    public void deleteRecipe() {
        if (recipe.getValue() == null) {
            return;
        }
        
        Recipe currentRecipe = recipe.getValue();
        String userId = preferences.getString("userId", "0");
        int permission = preferences.getInt("permission", 1);
        
        isDeleting.setValue(true);
        
        // Сначала уведомляем фрагменты об удалении
        notifyRecipeDeleted(currentRecipe);
        
        recipeDeleter.deleteRecipe(currentRecipe.getId(), currentRecipe.getUserId(), permission, 
                new RecipeDeleter.DeleteRecipeCallback() {
            @Override
            public void onDeleteSuccess() {
                isDeleting.setValue(false);
                deleteSuccess.setValue(true);
            }

            @Override
            public void onDeleteFailure(String error) {
                isDeleting.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }
    
    /**
     * Уведомляет фрагменты об удалении рецепта
     */
    private void notifyRecipeDeleted(Recipe deletedRecipe) {
        try {
            Log.d(TAG, "Уведомление фрагментов об удалении рецепта: " + deletedRecipe.getId());
            
            // Обновляем локальную базу данных
            executeIfActive(() -> {
                try {
                    // Удаляем рецепт из локальной базы данных
                    localRepository.deleteRecipe(deletedRecipe.getId());
                    Log.d(TAG, "Рецепт удален из локальной базы данных: " + deletedRecipe.getId());
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при удалении рецепта из локальной базы данных", e);
                }
            });
            
            // Если рецепт был в избранном, удаляем его и оттуда
            if (deletedRecipe.isLiked()) {
                FavoritesFragment.removeLikedRecipe(deletedRecipe.getId());
                Log.d(TAG, "Рецепт удален из FavoritesFragment: " + deletedRecipe.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при уведомлении фрагментов об удалении рецепта", e);
        }
    }
    
    /**
     * Обновляет данные рецепта после редактирования
     */
    public void updateRecipeData(String newTitle, String newIngredients, String newInstructions, String photoUrl) {
        if (recipe.getValue() == null) {
            return;
        }
        
        Recipe currentRecipe = recipe.getValue();
        currentRecipe.setTitle(newTitle);
        currentRecipe.setIngredients(newIngredients);
        currentRecipe.setInstructions(newInstructions);
        if (photoUrl != null && !photoUrl.isEmpty()) {
            currentRecipe.setPhoto_url(photoUrl);
        }
        
        recipe.setValue(currentRecipe);
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
} 
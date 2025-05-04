package com.example.cooking.network.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Step;
import com.example.cooking.data.repositories.RecipeRepository;
import com.example.cooking.network.api.RecipeApiService;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import androidx.annotation.NonNull;

/**
 * Менеджер для работы с API рецептов через Retrofit
 * Используется для добавления и редактирования рецептов
 */
public class RecipeManager {
    private static final String TAG = "RecipeManager";
    private static final String API_URL = "http://89.35.130.107";
    
    // Увеличенные таймауты для стабильности
    private static final int CONNECT_TIMEOUT = 30; // 30 секунд
    private static final int READ_TIMEOUT = 60; // 60 секунд
    private static final int WRITE_TIMEOUT = 60; // 60 секунд
    private static final int MAX_RETRY_ATTEMPTS = 3; // Максимальное количество повторных попыток
    
    private final RecipeApiService apiService;
    private final Context context;
    private static final Gson gson = new Gson();

    public void updateRecipe(Integer currentRecipeId, String currentTitle, List<Ingredient> currentIngredients, List<Step> currentSteps, byte[] bytes, String userId, int permission, RecipeSaveCallback recipeSaveCallback) {
    }

    /**
     * Интерфейс для обратного вызова результата добавления/редактирования рецепта
     */
    public interface RecipeSaveCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }
    
    public RecipeManager(Context context) {
        this.context = context;
        
        // Создаем OkHttpClient с увеличенными таймаутами
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true) // Включаем автоматические повторные попытки
                .build();
        
        // Создаем Retrofit с настроенным OkHttpClient
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        // Создаем API сервис
        apiService = retrofit.create(RecipeApiService.class);
    }
    
    /**
     * Проверяет наличие интернет-соединения
     * @return true, если есть подключение к интернету
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    /**
     * Добавляет или редактирует рецепт
     * @param title Название рецепта
     * @param ingredients Список ингредиентов
     * @param steps Список шагов
     * @param userId ID пользователя
     * @param imageBytes Изображение рецепта (может быть null)
     * @param callback Обратный вызов результата
     */
    public void saveRecipe(String title, List<Ingredient> ingredients, List<Step> steps,
                          String userId, byte[] imageBytes,
                          RecipeSaveCallback callback) {
        


        // Проверяем наличие интернет-соединения
        if (!isNetworkAvailable()) {
            Log.w(TAG, "RecipeManager.saveRecipe: Сеть недоступна (проверка внутри менеджера)");
            callback.onFailure("Отсутствует подключение к интернету. Пожалуйста, проверьте подключение и попробуйте снова.");
            return;
        }
        Log.d(TAG, "RecipeManager.saveRecipe: Сеть доступна (проверка внутри менеджера)");
        
        // Сериализуем списки в JSON
        String ingredientsJson = gson.toJson(ingredients);
        String stepsJson = gson.toJson(steps);
        
        Log.d(TAG, "Сохранение рецепта:" +
                " title: " + title +
                ", ingredients: " + ingredientsJson +
                ", steps: " + stepsJson +
                ", userId: " + userId +
                ", imageBytes: " + (imageBytes != null ? imageBytes.length + " байт" : "нет"));
        
        try {
            // Создаем RequestBody для текстовых полей (включая JSON-строки)
            RequestBody titleBody = RequestBody.create(title, MediaType.parse("text/plain"));
            RequestBody ingredientsBody = RequestBody.create(ingredientsJson, MediaType.parse("application/json"));
            RequestBody stepsBody = RequestBody.create(stepsJson, MediaType.parse("application/json"));
            RequestBody userIdBody = RequestBody.create(userId, MediaType.parse("text/plain"));
            
            Call<ResponseBody> call;
            
            // Если есть изображение, используем метод с фото
            if (imageBytes != null && imageBytes.length > 0) {
                String fileName = "recipe_" + System.currentTimeMillis() + ".jpg";
                RequestBody requestFile = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
                MultipartBody.Part photoPart = MultipartBody.Part.createFormData("photo", fileName, requestFile);
                
                call = apiService.addRecipe(
                        titleBody, ingredientsBody, stepsBody, userIdBody, photoPart
                );
                Log.d(TAG, "RecipeManager.saveRecipe: Отправка запроса в API...");
                Log.d(TAG, "Отправка запроса с изображением (размер: " + imageBytes.length + " байт)");
            } else {
                // Без изображения
                // Для нового рецепта без картинки используем метод без фото
                Log.d(TAG, "Создание нового рецепта без изображения");
                call = apiService.addRecipeWithoutPhoto(
                        titleBody, ingredientsBody, stepsBody, userIdBody
                );
            }
            
            // Выполняем запрос с поддержкой повторных попыток
            executeWithRetry(call, callback, 0);
            
        } catch (Exception e) {
            // Обрабатываем любые исключения при подготовке запроса
            Log.e(TAG, "Ошибка при подготовке запроса", e);
            callback.onFailure("Ошибка при подготовке запроса: " + e.getMessage());
        }
    }

    public void updateRecipe(String title, List<Ingredient> ingredients, List<Step> steps,
                             String userId, @NonNull Integer recipeId, byte[] imageBytes,
                             int permission, RecipeSaveCallback callback) {

        Log.d(TAG, "RecipeManager.updateRecipe: Метод вызван. Recipe ID: " + recipeId + ", Permission: " + permission + ", User ID: " + userId);

        if (!isNetworkAvailable()) {
            Log.w(TAG, "RecipeManager.updateRecipe: Сеть недоступна");
            callback.onFailure("Отсутствует подключение к интернету. Пожалуйста, проверьте подключение и попробуйте снова.");
            return;
        }
        Log.d(TAG, "RecipeManager.updateRecipe: Сеть доступна");

        String ingredientsJson = gson.toJson(ingredients);
        String stepsJson = gson.toJson(steps);

        Log.d(TAG, "Обновление рецепта:" +
                " title: " + title +
                ", ingredients: " + ingredientsJson +
                ", steps: " + stepsJson +
                ", userId: " + userId +
                ", recipeId: " + recipeId +
                ", permission: " + permission +
                ", imageBytes: " + (imageBytes != null ? imageBytes.length + " байт" : "нет"));

        try {
            RequestBody titleBody = RequestBody.create(title, MediaType.parse("text/plain"));
            RequestBody ingredientsBody = RequestBody.create(ingredientsJson, MediaType.parse("application/json"));
            RequestBody stepsBody = RequestBody.create(stepsJson, MediaType.parse("application/json"));

            MultipartBody.Part photoPart;
            if (imageBytes != null && imageBytes.length > 0) {
                String fileName = "recipe_" + System.currentTimeMillis() + ".jpg";
                RequestBody requestFile = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
                photoPart = MultipartBody.Part.createFormData("photo", fileName, requestFile);
                Log.d(TAG, "RecipeManager.updateRecipe: Подготовка запроса API (с новым фото)...");
            } else {
                photoPart = MultipartBody.Part.createFormData("photo", "", RequestBody.create(new byte[0], MediaType.parse("image/jpeg")));
                Log.d(TAG, "RecipeManager.updateRecipe: Подготовка запроса API (без нового фото)...");
            }

            String userIdHeader = String.valueOf(userId);
            String permissionHeader = String.valueOf(permission);

            Log.d(TAG, "RecipeManager.updateRecipe: Вызов apiService.updateRecipe (с заголовками)...");
            Call<ResponseBody> call = apiService.updateRecipe(
                    recipeId,         // ID в Path
                    userIdHeader,     // User ID в заголовке
                    permissionHeader, // Permission в заголовке
                    titleBody,        // Данные в теле
                    ingredientsBody,  // Данные в теле
                    stepsBody,        // Данные в теле
                    photoPart         // Фото (новое или пустое) в теле
            );

            executeWithRetry(call, callback, 0);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при подготовке запроса на обновление", e);
            callback.onFailure("Ошибка при подготовке запроса: " + e.getMessage());
        }
    }
    
    /**
     * Выполняет запрос с поддержкой повторных попыток при ошибках
     * @param call Запрос Retrofit
     * @param callback Обратный вызов результата
     * @param retryCount Текущее количество попыток
     */
    private void executeWithRetry(Call<ResponseBody> call, RecipeSaveCallback callback, int retryCount) {
        call.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        // Получаем тело ответа
                        String responseStr = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "Ответ сервера: " + responseStr);
                        
                        if (responseStr.isEmpty()) {
                            Log.e(TAG, "Получен пустой ответ от сервера");
                            if (retryCount < MAX_RETRY_ATTEMPTS) {
                                // Повторяем попытку при пустом ответе
                                Log.d(TAG, "Повторная попытка #" + (retryCount + 1));
                                executeWithRetry(call, callback, retryCount + 1);
                            } else {
                                callback.onFailure("Ошибка сервера: пустой ответ после " + MAX_RETRY_ATTEMPTS + " попыток");
                            }
                            return;
                        }
                        
                        // Парсим JSON
                        JSONObject jsonResponse = new JSONObject(responseStr);
                        boolean success = jsonResponse.optBoolean("success", false);
                        String message = jsonResponse.optString("message", "");
                        
                        if (success) {
                            // Очищаем кэш рецептов, чтобы при следующем запросе получить свежие данные
                            RecipeRepository repository = new RecipeRepository(context);
                            repository.clearCache();
                            
                            // Возвращаем успех
                            callback.onSuccess(message.isEmpty() ? "Рецепт успешно сохранен" : message);
                        } else {
                            // Возвращаем ошибку
                            callback.onFailure(message.isEmpty() ? "Ошибка при сохранении рецепта" : message);
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Ошибка при обработке ответа", e);
                        
                        if (retryCount < MAX_RETRY_ATTEMPTS) {
                            // Повторяем попытку при ошибке обработки ответа
                            Log.d(TAG, "Повторная попытка #" + (retryCount + 1) + " после ошибки обработки ответа");
                            executeWithRetry(call, callback, retryCount + 1);
                        } else {
                            callback.onFailure("Ошибка при обработке ответа: " + e.getMessage());
                        }
                    }
                } else {
                    try {
                        // Пытаемся получить сообщение об ошибке
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : null;
                        Log.e(TAG, "Ошибка HTTP " + response.code() + ": " + errorBody);
                        
                        // Специальная обработка для кода 403 (Forbidden)
                        if (response.code() == 403) {
                            callback.onFailure("У вас нет прав на редактирование этого рецепта. Только автор рецепта или администратор могут вносить изменения.");
                            return;
                        }
                        
                        // Проверяем, можно ли повторить запрос
                        if (retryCount < MAX_RETRY_ATTEMPTS && (response.code() >= 500 || response.code() == 429)) {
                            // Повторяем попытку при серверных ошибках (5xx) или слишком частых запросах (429)
                            Log.d(TAG, "Повторная попытка #" + (retryCount + 1) + " после HTTP ошибки " + response.code());
                            executeWithRetry(call, callback, retryCount + 1);
                        } else {
                            callback.onFailure("Ошибка сервера: " + response.code() + 
                                    (errorBody != null && !errorBody.isEmpty() ? " - " + errorBody : ""));
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Ошибка при чтении errorBody", e);
                        
                        if (retryCount < MAX_RETRY_ATTEMPTS) {
                            // Повторяем попытку при ошибке чтения errorBody
                            Log.d(TAG, "Повторная попытка #" + (retryCount + 1) + " после ошибки чтения errorBody");
                            executeWithRetry(call, callback, retryCount + 1);
                        } else {
                            callback.onFailure("Ошибка сервера: " + response.code());
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Ошибка сети", t);
                
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    // Делаем небольшую паузу перед повторной попыткой
                    try {
                        Thread.sleep(1000 * (retryCount + 1)); // Увеличиваем время ожидания с каждой попыткой
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Прерывание потока при ожидании повторной попытки", e);
                    }
                    
                    // Повторяем попытку при сетевой ошибке
                    Log.d(TAG, "Повторная попытка #" + (retryCount + 1) + " после сетевой ошибки: " + t.getMessage());
                    executeWithRetry(call, callback, retryCount + 1);
                } else {
                    callback.onFailure("Ошибка сети после " + MAX_RETRY_ATTEMPTS + " попыток: " + t.getMessage());
                }
            }
        });
    }
} 
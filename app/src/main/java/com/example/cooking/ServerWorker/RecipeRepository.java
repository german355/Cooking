package com.example.cooking.ServerWorker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import com.example.cooking.MySharedPreferences;
import com.example.cooking.Recipe.Recipe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;

/**
 * Репозиторий для управления данными рецептов.
 * Обеспечивает получение рецептов с сервера и их кэширование.
 */
public class RecipeRepository {
    private static final String TAG = "RecipeRepository";
    private static final String RECIPES_CACHE_KEY = "cached_recipes";
    private static final String LAST_UPDATE_TIME_KEY = "recipes_last_update_time";
    // 4 минуты в миллисекундах
    private static final long CACHE_EXPIRATION_TIME = (60 * 60 * 1000) / 15;
    
    // Константы для имени файла настроек
    private static final String PREF_NAME = "recipe_cache";
    
    private final Context context;
    private final OkHttpClient client;
    
    // URL сервера
    private static final String API_URL = "http://g3.veroid.network:19029";
    
    public interface RecipesCallback {
        void onRecipesLoaded(List<Recipe> recipes);
        void onDataNotAvailable(String error);
    }
    
    public RecipeRepository(Context context) {
        this.context = context;
        
        // Настраиваем OkHttpClient с более надежными параметрами
        this.client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)  // Включаем автоматические повторные попытки
                .connectTimeout(20, TimeUnit.SECONDS)  // Увеличиваем таймаут соединения
                .readTimeout(20, TimeUnit.SECONDS)     // Увеличиваем таймаут чтения
                .writeTimeout(20, TimeUnit.SECONDS)    // Увеличиваем таймаут записи
                .connectionPool(new ConnectionPool(0, 1, TimeUnit.MINUTES))  // Настройка пула соединений
                .build();
    }
    
    /**
     * Получает рецепты, сначала проверяя кэш, затем загружая с сервера.
     */
    @SuppressLint("StaticFieldLeak") //надо исправить
    public void getRecipes(RecipesCallback callback) {
        new AsyncTask<Void, Void, Result<List<Recipe>>>() {
            @Override
            protected Result<List<Recipe>> doInBackground(Void... voids) {
                // Сначала проверяем кэш
                Result<List<Recipe>> cachedResult = loadFromCache();
                if (cachedResult.isSuccess() && !isCacheExpired() || Notification.reLoad()) {
                    Log.d(TAG, "Используем данные из кэша");
                    return cachedResult;
                }
                
                // Если кэш отсутствует или просрочен, загружаем с сервера
                Response response = null;
                try {
                    Log.d(TAG, "Запрашиваем рецепты с сервера: " + API_URL + "/recipes");
                    Request request = new Request.Builder()
                            .url(API_URL + "/recipes")
                            .header("Connection", "close")
                            .build();
                    
                    response = client.newCall(request).execute();
                    
                    // Проверка ответа для безопасности
                    if (response.body() != null) {
                        try {
                            // Безопасно читаем тело ответа
                            String responseBody = response.body().string();
                            Log.d(TAG, "Ответ сервера размером: " + responseBody.length() + " байт");
                            
                            if (response.isSuccessful() && responseBody.length() > 0) {
                                List<Recipe> recipes = parseRecipesJson(responseBody);
                                Log.d(TAG, "Загружено рецептов с сервера: " + recipes.size());
                                
                                // Кэшируем рецепты только если список не пустой
                                if (!recipes.isEmpty()) {
                                    saveToCache(recipes);
                                }
                                return new Result.Success<>(recipes);
                            } else {
                                Log.e(TAG, "Ошибка HTTP: " + response.code() + " " + response.message());
                                
                                // Пробуем использовать кэш при неудачном ответе
                                if (cachedResult.isSuccess()) {
                                    Log.d(TAG, "Используем кэшированные данные из-за ошибки HTTP");
                                    return cachedResult;
                                }
                                
                                return new Result.Error<>("Ошибка сервера: " + response.code());
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Ошибка при чтении тела ответа", e);
                            
                            // Возвращаем кэш если есть проблемы с чтением ответа
                            if (cachedResult.isSuccess()) {
                                Log.d(TAG, "Используем кэшированные данные из-за ошибки чтения");
                                return cachedResult;
                            }
                            
                            return new Result.Error<>("Ошибка чтения ответа: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Получен пустой ответ");
                        
                        // Пробуем использовать кэш при пустом ответе
                        if (cachedResult.isSuccess()) {
                            Log.d(TAG, "Используем кэшированные данные из-за пустого ответа");
                            return cachedResult;
                        }
                        
                        return new Result.Error<>("Пустой ответ от сервера");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Сетевая ошибка при загрузке рецептов", e);
                    
                    // При сетевой ошибке используем кэш, если он есть
                    if (cachedResult.isSuccess()) {
                        Log.d(TAG, "Используем кэшированные данные из-за сетевой ошибки");
                        return cachedResult;
                    }
                    
                    return new Result.Error<>("Сетевая ошибка: " + e.getMessage());
                } finally {
                    // Всегда закрываем ответ
                    if (response != null) {
                        response.close();
                    }
                }
            }
            
            @Override
            protected void onPostExecute(Result<List<Recipe>> result) {
                if (result.isSuccess()) {
                    Result.Success<List<Recipe>> success = (Result.Success<List<Recipe>>) result;
                    callback.onRecipesLoaded(success.getData());
                } else {
                    Result.Error<List<Recipe>> error = (Result.Error<List<Recipe>>) result;
                    callback.onDataNotAvailable(error.getErrorMessage());
                }
            }
        }.execute();
    }
    
    /**
     * Парсит JSON-ответ сервера и создает список рецептов.
     */
    private List<Recipe> parseRecipesJson(String jsonData) {
        List<Recipe> recipes = new ArrayList<>();
        
        try {
            Log.d(TAG, "Начинаем парсинг ответа: " + jsonData.substring(0, Math.min(500, jsonData.length())));
            JSONObject jsonObject = new JSONObject(jsonData);
            
            // Проверяем, успешный ли ответ
            boolean success = jsonObject.optBoolean("success", false);
            Log.d(TAG, "Успешный ответ: " + success);
            
            if (success) {
                // Получаем количество рецептов, если доступно
                int count = jsonObject.optInt("count", -1);
                Log.d(TAG, "Количество рецептов в ответе: " + count);
                
                // Проверяем, есть ли поле "recipes"
                if (jsonObject.has("recipes")) {
                    JSONArray recipesArray = jsonObject.getJSONArray("recipes");
                    Log.d(TAG, "Найдено рецептов в JSON: " + recipesArray.length());
                    
                    for (int i = 0; i < recipesArray.length(); i++) {
                        JSONObject recipeJson = recipesArray.getJSONObject(i);
                        Log.d(TAG, "Обрабатываем рецепт [" + i + "]: " + recipeJson.toString());
                        
                        Recipe recipe = new Recipe();
                        recipe.setId(recipeJson.optInt("id", 0));
                        String userId = recipeJson.optString("userId", recipeJson.optString("user_id", "99"));
                        recipe.setUserId(userId);
                        recipe.setTitle(recipeJson.optString("title", "Без названия"));
                        recipe.setIngredients(recipeJson.optString("ingredients", ""));
                        recipe.setInstructions(recipeJson.optString("instructions", ""));
                        recipe.setCreated_at(recipeJson.optString("created_at", ""));
                        recipe.setPhoto_url(recipeJson.optString("photo", ""));
                        
                        Log.d(TAG, "Рецепт [" + i + "] обработан: " + recipe.getId() + " - " + recipe.getTitle());
                        recipes.add(recipe);
                    }
                } else {
                    Log.e(TAG, "В ответе нет поля 'recipes'");
                }
            } else {
                // Если success=false, пытаемся получить сообщение об ошибке
                String message = jsonObject.optString("message", "Неизвестная ошибка");
                Log.e(TAG, "Ошибка в ответе сервера: " + message);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка парсинга JSON: " + e.getMessage(), e);
            Log.e(TAG, "Некорректный JSON: " + jsonData);
        }
        
        Log.d(TAG, "Итого обработано рецептов: " + recipes.size());
        return recipes;
    }
    
    /**
     * Сохраняет список рецептов в кэш.
     */
    private void saveToCache(List<Recipe> recipes) {
        try {
            JSONArray recipesArray = new JSONArray();

            for (Recipe recipe : recipes) {
                JSONObject recipeJson = new JSONObject();
                recipeJson.put("id", recipe.getId());
                recipeJson.put("title", recipe.getTitle());
                recipeJson.put("ingredients", recipe.getIngredients());
                recipeJson.put("instructions", recipe.getInstructions());
                recipeJson.put("created_at", recipe.getCreated_at());
                recipeJson.put("userId", recipe.getUserId());
                recipeJson.put("photo", recipe.getPhoto_url());

                recipesArray.put(recipeJson);
            }
            
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(RECIPES_CACHE_KEY, recipesArray.toString());
            editor.putLong(LAST_UPDATE_TIME_KEY, System.currentTimeMillis());
            boolean success = editor.commit(); // Используем commit() вместо apply() для синхронного сохранения
            
            if (success) {
                Log.d(TAG, "Recipes cached successfully: " + recipes.size());
            } else {
                Log.e(TAG, "Failed to save recipes to cache");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error caching recipes: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error when caching recipes: " + e.getMessage(), e);
        }
    }
    
    /**
     * Загружает рецепты из кэша.
     */
    private Result<List<Recipe>> loadFromCache() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String cachedRecipes = prefs.getString(RECIPES_CACHE_KEY, null);
            
            if (cachedRecipes != null && !cachedRecipes.isEmpty()) {
                JSONArray recipesArray = new JSONArray(cachedRecipes);
                List<Recipe> recipes = new ArrayList<>();
                
                for (int i = 0; i < recipesArray.length(); i++) {
                    JSONObject recipeJson = recipesArray.getJSONObject(i);
                    
                    Recipe recipe = new Recipe();
                    
                    // Установка ID (преобразуем String в int)
                    try {
                        recipe.setId(Integer.parseInt(recipeJson.optString("id", "0")));
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Ошибка при парсинге ID рецепта: " + e.getMessage());
                        recipe.setId(0); // Устанавливаем значение по умолчанию
                    }
                    
                    recipe.setTitle(recipeJson.optString("title", ""));
                    recipe.setIngredients(recipeJson.optString("ingredients", ""));
                    recipe.setInstructions(recipeJson.optString("instructions", ""));
                    recipe.setPhoto_url(recipeJson.optString("photo", ""));
                    recipe.setCreated_at(recipeJson.optString("created_at", ""));
                    recipe.setUserId(recipeJson.optString("userId", ""));
                    
                    recipes.add(recipe);
                }
                
                Log.d(TAG, "Loaded " + recipes.size() + " recipes from cache");
                return new Result.Success<>(recipes);
            } else {
                Log.d(TAG, "Cache is empty or null");
                return new Result.Error<>("Кэш пуст");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON from cache: " + e.getMessage(), e);
            return new Result.Error<>("Ошибка при чтении кэша: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error when loading from cache: " + e.getMessage(), e);
            return new Result.Error<>("Неожиданная ошибка при чтении кэша: " + e.getMessage());
        }
    }
    
    /**
     * Проверяет, истек ли срок действия кэша.
     */
    private boolean isCacheExpired() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            long lastUpdateTime = prefs.getLong(LAST_UPDATE_TIME_KEY, 0);
            boolean expired = System.currentTimeMillis() - lastUpdateTime > CACHE_EXPIRATION_TIME;
            
            Log.d(TAG, "Cache expired: " + expired + 
                    " (last update: " + lastUpdateTime + 
                    ", current time: " + System.currentTimeMillis() + 
                    ", diff: " + (System.currentTimeMillis() - lastUpdateTime) + 
                    ", expiration time: " + CACHE_EXPIRATION_TIME + ")");
            
            return expired;
        } catch (Exception e) {
            Log.e(TAG, "Error checking cache expiration: " + e.getMessage(), e);
            return true; // в случае ошибки считаем, что кэш устарел
        }
    }
    
    /**
     * Принудительно загружает рецепты с сервера, игнорируя кэш.
     */
    @SuppressLint("StaticFieldLeak")
    public void refreshRecipes(RecipesCallback callback) {
        // Очищаем кэш перед загрузкой
        clearCache();
        
        new AsyncTask<Void, Void, Result<List<Recipe>>>() {
            @Override
            protected Result<List<Recipe>> doInBackground(Void... voids) {
                try {
                    Log.d(TAG, "Принудительно обновляем рецепты с сервера");
                    Request request = new Request.Builder()
                            .url(API_URL + "/recipes")
                            .header("Connection", "close")
                            .build();
                    
                    Response response = null;
                    try {
                        response = client.newCall(request).execute();
                        
                        // Проверка ответа
                        if (response != null && response.body() != null) {
                            try {
                                // Сохраняем все содержимое тела в строку с безопасной обработкой
                                String responseBody = response.body().string();
                                Log.d(TAG, "Получен ответ от сервера размером: " + responseBody.length() + " байт");
                                
                                if (response.isSuccessful() && responseBody.length() > 0) {
                                    List<Recipe> recipes = parseRecipesJson(responseBody);
                                    Log.d(TAG, "Загружено рецептов с сервера: " + recipes.size());
                                    
                                    // Кэшируем рецепты только если получили не пустой список
                                    if (!recipes.isEmpty()) {
                                        saveToCache(recipes);
                                    }
                                    return new Result.Success<>(recipes);
                                } else {
                                    Log.e(TAG, "Ошибка HTTP: " + response.code() + " " + response.message());
                                    return new Result.Error<>("Ошибка сервера: " + response.code());
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Ошибка при чтении тела ответа", e);
                                
                                // Пытаемся использовать кэш при проблемах с телом ответа
                                Result<List<Recipe>> cachedResult = loadFromCache();
                                if (cachedResult.isSuccess()) {
                                    Log.d(TAG, "Использую кэшированные данные из-за ошибки чтения ответа");
                                    return cachedResult;
                                }
                                
                                return new Result.Error<>("Ошибка чтения ответа: " + e.getMessage());
                            }
                        } else {
                            Log.e(TAG, "Получен пустой ответ");
                            
                            // Пытаемся использовать кэш при отсутствии ответа
                            Result<List<Recipe>> cachedResult = loadFromCache();
                            if (cachedResult.isSuccess()) {
                                Log.d(TAG, "Использую кэшированные данные из-за пустого ответа");
                                return cachedResult;
                            }
                            
                            return new Result.Error<>("Пустой ответ от сервера");
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Сетевая ошибка при загрузке рецептов", e);
                        
                        // Проверяем, есть ли кэшированные данные
                        Result<List<Recipe>> cachedResult = loadFromCache();
                        if (cachedResult.isSuccess()) {
                            Log.d(TAG, "Использую кэшированные данные из-за ошибки сети");
                            return cachedResult;
                        }
                        
                        return new Result.Error<>("Сетевая ошибка: " + e.getMessage());
                    } finally {
                        // Всегда закрываем ответ для освобождения ресурсов
                        if (response != null) {
                            response.close();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Непредвиденная ошибка", e);
                    return new Result.Error<>("Непредвиденная ошибка: " + e.getMessage());
                }
            }
            
            @Override
            protected void onPostExecute(Result<List<Recipe>> result) {
                if (result.isSuccess()) {
                    Result.Success<List<Recipe>> success = (Result.Success<List<Recipe>>) result;
                    callback.onRecipesLoaded(success.getData());
                } else {
                    Result.Error<List<Recipe>> error = (Result.Error<List<Recipe>>) result;
                    callback.onDataNotAvailable(error.getErrorMessage());
                }
            }
        }.execute();
    } // пока не используется но надо правильно придумать сценарий
    
    /**
     * Очищает кэш рецептов.
     */
    public void clearCache() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(RECIPES_CACHE_KEY);
            editor.remove(LAST_UPDATE_TIME_KEY);
            boolean success = editor.commit();
            
            if (success) {
                Log.d(TAG, "Cache cleared successfully");
            } else {
                Log.e(TAG, "Failed to clear cache");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache: " + e.getMessage(), e);
        }
    }
    
    /**
     * Получает рецепты только из кэша.
     */
    @SuppressLint("StaticFieldLeak")
    public void getRecipesFromCache(RecipesCallback callback) {
        new AsyncTask<Void, Void, Result<List<Recipe>>>() {
            @Override
            protected Result<List<Recipe>> doInBackground(Void... voids) {
                Log.d(TAG, "Загружаем рецепты только из кэша");
                try {
                    Result<List<Recipe>> result = loadFromCache();
                    if (result.isSuccess()) {
                        Result.Success<List<Recipe>> success = (Result.Success<List<Recipe>>) result;
                        Log.d(TAG, "Успешно загружено из кэша: " + success.getData().size() + " рецептов");
                    } else {
                        Result.Error<List<Recipe>> error = (Result.Error<List<Recipe>>) result;
                        Log.e(TAG, "Ошибка загрузки из кэша: " + error.getErrorMessage());
                    }
                    return result;
                } catch (Exception e) {
                    Log.e(TAG, "Неожиданная ошибка при доступе к кэшу: " + e.getMessage(), e);
                    return new Result.Error<>("Неожиданная ошибка при доступе к кэшу: " + e.getMessage());
                }
            }
            
            @Override
            protected void onPostExecute(Result<List<Recipe>> result) {
                try {
                    if (result.isSuccess()) {
                        Result.Success<List<Recipe>> success = (Result.Success<List<Recipe>>) result;
                        List<Recipe> recipes = success.getData();
                        Log.d(TAG, "onPostExecute: возвращаем " + recipes.size() + " рецептов из кэша");
                        callback.onRecipesLoaded(recipes);
                    } else {
                        Result.Error<List<Recipe>> error = (Result.Error<List<Recipe>>) result;
                        String errorMessage = error.getErrorMessage();
                        Log.e(TAG, "onPostExecute: ошибка загрузки из кэша: " + errorMessage);
                        callback.onDataNotAvailable(errorMessage);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onPostExecute: неожиданная ошибка: " + e.getMessage(), e);
                    callback.onDataNotAvailable("Неожиданная ошибка: " + e.getMessage());
                }
            }
        }.execute();
    }
    
    /**
     * Получает рецепты только с сервера без обращения к кэшу.
     */
    @SuppressLint("StaticFieldLeak")
    public void getRecipesFromServer(RecipesCallback callback) {
        new AsyncTask<Void, Void, Result<List<Recipe>>>() {
            @Override
            protected Result<List<Recipe>> doInBackground(Void... voids) {
                Response response = null;
                try {
                    Log.d(TAG, "Загружаем рецепты только с сервера");
                    Request request = new Request.Builder()
                            .url(API_URL + "/recipes")
                            .header("Connection", "close") // Отключаем повторное использование соединения
                            .build();
                    
                    response = client.newCall(request).execute();
                    
                    if (response != null && response.body() != null) {
                        try {
                            String responseBody = response.body().string();
                            Log.d(TAG, "Получен ответ от сервера размером: " + responseBody.length() + " байт");
                            
                            if (response.isSuccessful() && responseBody.length() > 0) {
                                List<Recipe> recipes = parseRecipesJson(responseBody);
                                Log.d(TAG, "Загружено рецептов с сервера: " + recipes.size());
                                
                                // Кэшируем рецепты только если список не пустой
                                if (!recipes.isEmpty()) {
                                    saveToCache(recipes);
                                }
                                return new Result.Success<>(recipes);
                            } else {
                                Log.e(TAG, "Ошибка HTTP: " + response.code() + " " + response.message());
                                return new Result.Error<>("Ошибка сервера: " + response.code());
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Ошибка при чтении тела ответа", e);
                            return new Result.Error<>("Ошибка чтения ответа: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Получен пустой ответ");
                        return new Result.Error<>("Пустой ответ от сервера");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Сетевая ошибка при загрузке рецептов", e);
                    return new Result.Error<>("Сетевая ошибка: " + e.getMessage());
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }
            }
            
            @Override
            protected void onPostExecute(Result<List<Recipe>> result) {
                if (result.isSuccess()) {
                    Result.Success<List<Recipe>> success = (Result.Success<List<Recipe>>) result;
                    callback.onRecipesLoaded(success.getData());
                } else {
                    Result.Error<List<Recipe>> error = (Result.Error<List<Recipe>>) result;
                    callback.onDataNotAvailable(error.getErrorMessage());
                }
            }
        }.execute();
    }
    
    /**
     * Обновляет рецепты в фоновом режиме, без блокировки UI.
     * Вызывает callback только при успешной загрузке.
     */
    @SuppressLint("StaticFieldLeak")
    public void refreshRecipesInBackground(RecipesCallback callback) {
        new AsyncTask<Void, Void, Result<List<Recipe>>>() {
            @Override
            protected Result<List<Recipe>> doInBackground(Void... voids) {
                Response response = null;
                // Создаем отдельный клиент с отключенным пулом соединений
                OkHttpClient refreshClient = new OkHttpClient.Builder()
                        .connectionPool(new ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
                        .retryOnConnectionFailure(true)
                        .build();
                try {
                    Log.d(TAG, "Фоновое обновление рецептов");
                    Request request = new Request.Builder()
                            .url(API_URL + "/recipes")
                            .header("Connection", "close") // гарантируем закрытие соединения после запроса
                            .build();

                    response = refreshClient.newCall(request).execute();

                    if (response != null && response.body() != null) {
                        try {
                            String responseBody = response.body().string();

                            if (response.isSuccessful() && responseBody.length() > 0) {
                                List<Recipe> recipes = parseRecipesJson(responseBody);
                                Log.d(TAG, "Фоново загружено рецептов: " + recipes.size());

                                if (!recipes.isEmpty()) {
                                    saveToCache(recipes);
                                    return new Result.Success<>(recipes);
                                }
                            } else {
                                Log.e(TAG, "Ошибка HTTP: " + response.code() + " " + response.message());
                                return new Result.Error<>("Ошибка сервера: " + response.code());
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Ошибка при чтении тела ответа", e);
                            return new Result.Error<>("Ошибка чтения ответа: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Получен пустой ответ");
                        return new Result.Error<>("Пустой ответ от сервера");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Сетевая ошибка при загрузке рецептов", e);
                    return new Result.Error<>("Сетевая ошибка: " + e.getMessage());
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }
                return new Result.Error<>("Ошибка");
            }

            @Override
            protected void onPostExecute(Result<List<Recipe>> result) {
                if (result.isSuccess()) {
                    Result.Success<List<Recipe>> success = (Result.Success<List<Recipe>>) result;
                    callback.onRecipesLoaded(success.getData());
                } else {
                    Result.Error<List<Recipe>> error = (Result.Error<List<Recipe>>) result;
                    callback.onDataNotAvailable(error.getErrorMessage());
                }
            }
        }.execute();
    }
    
    /**
     * Получает рецепты из кэша синхронно.
     * @return Result с рецептами или сообщением об ошибке
     */
    public Result<List<Recipe>> getRecipesFromCacheSync() {
        Log.d(TAG, "Синхронная загрузка рецептов из кэша");
        try {
            return loadFromCache();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при синхронной загрузке из кэша: " + e.getMessage(), e);
            return new Result.Error<>("Ошибка при загрузке из кэша: " + e.getMessage());
        }
    }
    
    /**
     * Класс результата операции.
     */
    abstract static class Result<T> {
        abstract boolean isSuccess();
        
        static class Success<T> extends Result<T> {
            private final T data;
            
            Success(T data) {
                this.data = data;
            }
            
            T getData() {
                return data;
            }
            
            @Override
            boolean isSuccess() {
                return true;
            }
        }
        
        static class Error<T> extends Result<T> {
            private final String errorMessage;
            
            Error(String errorMessage) {
                this.errorMessage = errorMessage;
            }
            
            String getErrorMessage() {
                return errorMessage;
            }
            
            @Override
            boolean isSuccess() {
                return false;
            }
        }
    }
} 
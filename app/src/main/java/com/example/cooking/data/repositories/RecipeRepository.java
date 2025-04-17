package com.example.cooking.data.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.Recipe.Step;
import com.example.cooking.config.ServerConfig;
import com.example.cooking.network.api.RecipeApi;
import com.example.cooking.network.responses.RecipesResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Репозиторий для управления данными рецептов с использованием всех преимуществ Retrofit.
 */
public class RecipeRepository {
    private static final String TAG = "RecipeRepository";
    private static final String API_URL = ServerConfig.BASE_API_URL;
    
    // Настройки кэша
    private static final long CACHE_SIZE = 10 * 1024 * 1024; // 10 МБ
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final int MAX_AGE = 60 * 4; // 4 минуты для онлайн кэша
    private static final int MAX_STALE = 60 * 60 * 24 * 7; // 7 дней для оффлайн кэша
    
    // Настройки кэша в SharedPreferences
    private static final String RECIPES_CACHE_KEY = "cached_recipes";
    private static final String LAST_UPDATE_TIME_KEY = "recipes_last_update_time";
    private static final String PREF_NAME = "recipe_cache";
    private static final long CACHE_EXPIRATION_TIME = (60 * 60 * 1000) / 15; // 4 минуты
    
    private final Context context;
    private final RecipeApi recipeApi;
    
    private static final Gson gson = new Gson(); // Экземпляр Gson
    
    public interface RecipesCallback {
        void onRecipesLoaded(List<Recipe> recipes);
        void onDataNotAvailable(String error);
    }
    
    public RecipeRepository(Context context) {
        this.context = context;
        
        // Создаем HTTP кэш
        File cacheDir = new File(context.getCacheDir(), "http-cache");
        Cache cache = new Cache(cacheDir, CACHE_SIZE);
        
        // логирование для отладки
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        //интерцептор для добавления заголовков кэширования
        Interceptor cacheInterceptor = chain -> {
            Request request = chain.request();
            
            // Всегда сначала пробуем загрузить свежие данные с сервера
            if (isNetworkAvailable()) {
                // Запрос к серверу с указанием не использовать кэш
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_NETWORK)
                        .build();
                
                Log.d(TAG, "Загрузка данных с сервера");
            } else {
                // Если сети нет, пробуем использовать кэш
                CacheControl cacheControl = new CacheControl.Builder()
                        .maxStale(MAX_STALE, TimeUnit.SECONDS)
                        .build();
                
                request = request.newBuilder()
                        .cacheControl(cacheControl)
                        .build();
                
                Log.d(TAG, "Нет сети, используем оффлайн кэш");
            }
            
            Response response = chain.proceed(request);
            
            // Кэшируем ответ для будущего использования в оффлайн режиме
            return response.newBuilder()
                    .removeHeader("Pragma")
                    .header(CACHE_CONTROL_HEADER, "public, max-age=" + MAX_AGE)
                    .build();
        };
        
        //  интерцептор для перехвата всех запросов
        Interceptor requestInterceptor = chain -> {
            Request original = chain.request();
            
            // Добавляем общие заголовки к запросу
            Request request = original.newBuilder()
                    .header("Accept", "application/json")
                    .method(original.method(), original.body())
                    .build();
            
            return chain.proceed(request);
        };
        
        // Настраиваем OkHttpClient с кэшем и интерцепторами
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(requestInterceptor)
                .addNetworkInterceptor(cacheInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        // Настраиваем Gson для более безопасного парсинга JSON
        Gson gsonConverter = new GsonBuilder()
                .setLenient()
                .create();
        
        //  Retrofit с настроенным клиентом и конвертером
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL + "/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gsonConverter))
                .build();
        
        //  имплементацию API
        recipeApi = retrofit.create(RecipeApi.class);
    }
    
    /**
     * Получает рецепты с сервера.
     */
    public void getRecipes(final RecipesCallback callback) {
        // Если нет сети, сразу пробуем загрузить из кэша
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Нет подключения к интернету, пробуем загрузить из кэша");
            Result<List<Recipe>> cachedResult = loadFromCache();
            if (cachedResult.isSuccess()) {
                List<Recipe> recipes = ((Result.Success<List<Recipe>>) cachedResult).getData();
                Log.d(TAG, "Загружено из кэша рецептов: " + recipes.size());
                callback.onRecipesLoaded(recipes);
                return;
            } else {
                Log.e(TAG, "Кэш недоступен: " + ((Result.Error<List<Recipe>>) cachedResult).getErrorMessage());
                callback.onDataNotAvailable("Нет подключения к интернету и нет данных в кэше");
                return;
            }
        }

        // Вызываем API асинхронно, используя enqueue
        Call<RecipesResponse> call = recipeApi.getRecipes();
        call.enqueue(new Callback<RecipesResponse>() {
            @Override
            public void onResponse(Call<RecipesResponse> call, retrofit2.Response<RecipesResponse> response) {
                if (response.isSuccessful()) {
                    RecipesResponse recipesResponse = response.body();
                    if (recipesResponse != null && recipesResponse.isSuccess() && recipesResponse.getRecipes() != null) {
                        List<Recipe> recipes = recipesResponse.getRecipes();
                        Log.d(TAG, "Загружено с сервера рецептов: " + recipes.size());
                        // Сохраняем в кэш для RecipeSearchService и для оффлайн режима
                        saveToCache(recipes);
                        callback.onRecipesLoaded(recipes);
                    } else {
                        String errorMsg = response.body() != null 
                                ? "Ошибка в ответе сервера: " + recipesResponse.getMessage()
                                : "Пустой ответ от сервера";
                        Log.e(TAG, errorMsg);
                        // Пробуем загрузить из кэша при ошибке сервера
                        tryLoadFromCache(errorMsg, callback);
                    }
                } else {
                    String errorBody = null;
                    try {
                        errorBody = response.errorBody() != null ? response.errorBody().string() : null;
                    } catch (IOException e) {
                        Log.e(TAG, "Ошибка при чтении errorBody", e);
                    }
                    
                    String errorMsg = "Ошибка HTTP " + response.code();
                    if (errorBody != null && !errorBody.isEmpty()) {
                        errorMsg += ": " + errorBody;
                    }
                    
                    Log.e(TAG, errorMsg);
                    // Пробуем загрузить из кэша при ошибке HTTP
                    tryLoadFromCache(errorMsg, callback);
                }
            }
            
            @Override
            public void onFailure(Call<RecipesResponse> call, Throwable t) {
                Log.e(TAG, "Ошибка сети: " + t.getMessage(), t);
                
                String errorMsg;
                if (!isNetworkAvailable()) {
                    errorMsg = "Нет подключения к интернету";
                } else {
                    errorMsg = "Ошибка сети: " + t.getMessage();
                }
                
                Log.e(TAG, errorMsg);
                // Пробуем загрузить из кэша при ошибке сети
                tryLoadFromCache(errorMsg, callback);
            }
        });
    }
    
    /**
     * Вспомогательный метод для загрузки данных из кэша при ошибках сети или сервера
     */
    private void tryLoadFromCache(String errorMsg, RecipesCallback callback) {
        Result<List<Recipe>> cachedResult = loadFromCache();
        if (cachedResult.isSuccess()) {
            List<Recipe> recipes = ((Result.Success<List<Recipe>>) cachedResult).getData();
            Log.d(TAG, "Загружены данные из кэша после ошибки сети: " + recipes.size() + " рецептов");
            callback.onRecipesLoaded(recipes);
        } else {
            // Если и кэш недоступен, возвращаем ошибку
            callback.onDataNotAvailable(errorMsg + ". Кэш также недоступен.");
        }
    }
    
    /**
     * Очищает кэш рецептов. Используется при добавлении/удалении/лайке рецептов.
     */
    public void clearCache() {
        try {
            // Очищаем SharedPreferences кэш
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(RECIPES_CACHE_KEY);
            editor.remove(LAST_UPDATE_TIME_KEY);
            editor.apply();
            
            // Очищаем HTTP кэш
            try {
                File cacheDir = new File(context.getCacheDir(), "http-cache");
                deleteDir(cacheDir);
                Log.d(TAG, "HTTP кэш очищен");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при очистке HTTP кэша", e);
            }
            
            Log.d(TAG, "Кэш успешно очищен");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при очистке кэша: " + e.getMessage(), e);
        }
    }
    
    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir != null && dir.delete();
    }
    
    /**
     * Сохраняет список рецептов в кэш SharedPreferences.
     * Используется для RecipeSearchService.
     */
    private void saveToCache(List<Recipe> recipes) {
        try {
            JSONArray recipesArray = new JSONArray();
            for (Recipe recipe : recipes) {
                JSONObject recipeJson = new JSONObject();
                recipeJson.put("id", recipe.getId());
                recipeJson.put("title", recipe.getTitle());
                recipeJson.put("ingredients", gson.toJson(recipe.getIngredients()));
                recipeJson.put("instructions", gson.toJson(recipe.getSteps()));
                recipeJson.put("created_at", recipe.getCreated_at());
                recipeJson.put("userId", recipe.getUserId());
                recipeJson.put("photo", recipe.getPhoto_url());
                recipeJson.put("isLiked", recipe.isLiked());
                recipesArray.put(recipeJson);
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(RECIPES_CACHE_KEY, recipesArray.toString());
            editor.putLong(LAST_UPDATE_TIME_KEY, System.currentTimeMillis());
            editor.apply();
            Log.d(TAG, "Сохранено в SharedPreferences кэш рецептов: " + recipes.size());
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при кэшировании рецептов в SharedPreferences", e);
        }
    }
    
    /**
     * Загружает рецепты из кэша SharedPreferences.
     * Используется в RecipeSearchService для поиска.
     */
    public Result<List<Recipe>> loadFromCache() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String cachedRecipes = prefs.getString(RECIPES_CACHE_KEY, null);
            if (cachedRecipes != null && !cachedRecipes.isEmpty()) {
                JSONArray recipesArray = new JSONArray(cachedRecipes);
                List<Recipe> recipes = new ArrayList<>();
                // Определяем типы для Gson
                Type ingredientListType = new TypeToken<ArrayList<Ingredient>>() {}.getType();
                Type stepListType = new TypeToken<ArrayList<Step>>() {}.getType();

                for (int i = 0; i < recipesArray.length(); i++) {
                    JSONObject recipeJson = recipesArray.getJSONObject(i);
                    Recipe recipe = new Recipe();
                    recipe.setId(recipeJson.optInt("id", 0));
                    recipe.setTitle(recipeJson.optString("title", ""));

                    // Десериализуем JSON строки обратно в списки
                    String ingredientsString = recipeJson.optString("ingredients", "");
                    String stepsString = recipeJson.optString("instructions", "");

                    List<Ingredient> ingredients = gson.fromJson(ingredientsString, ingredientListType);
                    List<Step> steps = gson.fromJson(stepsString, stepListType);

                     // Проверка на null после десериализации
                    recipe.setIngredients(ingredients == null ? new ArrayList<>() : new ArrayList<>(ingredients));
                    recipe.setSteps(steps == null ? new ArrayList<>() : new ArrayList<>(steps));

                    recipe.setPhoto_url(recipeJson.optString("photo", ""));
                    recipe.setCreated_at(recipeJson.optString("created_at", ""));
                    recipe.setUserId(recipeJson.optString("userId", ""));
                    recipe.setLiked(recipeJson.optBoolean("isLiked", false));
                    recipes.add(recipe);
                }
                 Log.d(TAG, "Загружено из SharedPreferences кэша рецептов: " + recipes.size());
                return new Result.Success<>(recipes);
            } else {
                 Log.d(TAG, "SharedPreferences кэш рецептов пуст");
                return new Result.Error<>("Кэш пуст");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при чтении кэша из SharedPreferences", e);
            return new Result.Error<>("Ошибка при чтении кэша: " + e.getMessage());
        }
    }
    
    /**
     * Проверяет, истек ли срок действия кэша.
     */
    private boolean isCacheExpired() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong(LAST_UPDATE_TIME_KEY, 0);
        boolean expired = System.currentTimeMillis() - lastUpdateTime > CACHE_EXPIRATION_TIME;
        Log.d(TAG, "Проверка истечения SharedPreferences кэша: " + (expired ? "Истек" : "Актуален"));
        return expired;
    }
    
    /**
     * Проверяет доступность сети.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    /**
     * Класс результата операции, используемый в RecipeSearchService.
     */
    public abstract static class Result<T> {
        public abstract boolean isSuccess();
        
        public static class Success<T> extends Result<T> {
            private final T data;
            
            public Success(T data) {
                this.data = data;
            }
            
            public T getData() {
                return data;
            }
            
            @Override
            public boolean isSuccess() {
                return true;
            }
        }
        
        public static class Error<T> extends Result<T> {
            private final String errorMessage;
            
            public Error(String errorMessage) {
                this.errorMessage = errorMessage;
            }
            
            public String getErrorMessage() {
                return errorMessage;
            }
            
            @Override
            public boolean isSuccess() {
                return false;
            }
        }
    }
}
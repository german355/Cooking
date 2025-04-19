package com.example.cooking.data.repositories;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.config.ServerConfig;
import com.example.cooking.network.api.RecipeApi;
import com.example.cooking.network.responses.RecipesResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.cooking.network.interceptors.AuthInterceptor;
import com.example.cooking.utils.MySharedPreferences;

/**
 * Репозиторий для работы с удаленным API рецептов
 */
public class RecipeRemoteRepository {

    private static final String TAG = "RecipeRemoteRepository";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // Настройки кэша
    private static final long CACHE_SIZE = 10 * 1024 * 1024; // 10 МБ
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final int MAX_AGE = 60 * 4; // 4 минуты для онлайн кэша
    private static final int MAX_STALE = 60 * 60 * 24 * 7; // 7 дней для оффлайн кэша

    private final Context context;
    private final RecipeApi recipeApi;
    private final OkHttpClient httpClient;
    private final MySharedPreferences preferences;

    public interface RecipesCallback {
        void onRecipesLoaded(List<Recipe> recipes);

        void onDataNotAvailable(String error);
    }

    public RecipeRemoteRepository(Context context) {
        this.context = context;
        this.preferences = new MySharedPreferences(context);

        // Создаем HTTP кэш
        Cache cache = new Cache(new java.io.File(context.getCacheDir(), "http-cache"), CACHE_SIZE);

        // Логирование для отладки
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Интерцептор для добавления заголовков кэширования
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

        httpClient = new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(new AuthInterceptor())
                .addInterceptor(request -> {
                    Request original = request.request();
                    Request newRequest = original.newBuilder()
                            .header("Accept", "application/json")
                            .method(original.method(), original.body())
                            .build();
                    return request.proceed(newRequest);
                })
                .addNetworkInterceptor(cacheInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // Настраиваем Gson для более безопасного парсинга JSON
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ServerConfig.BASE_API_URL + "/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        recipeApi = retrofit.create(RecipeApi.class);
    }

    /**
     * Получить рецепты с сервера
     * 
     * @param callback callback для возврата результата
     */
    public void getRecipes(final RecipesCallback callback) {
        // Проверяем доступность сети
        if (!isNetworkAvailable()) {
            callback.onDataNotAvailable("Нет подключения к интернету");
            return;
        }

        // Получаем наш внутренний userId из SharedPreferences
        String userId = preferences.getString("userId", null);
        if (userId == null || userId.isEmpty() || userId.equals("0")) { // Проверяем, что userId есть
            Log.e(TAG, "Внутренний userId не найден в SharedPreferences. Пользователь не авторизован?");
            // Возможно, стоит перенаправить на логин или показать другое сообщение
            callback.onDataNotAvailable("Ошибка: Пользователь не авторизован (внутренний ID не найден).");
            return;
        }
        Log.d(TAG, "Отправляем запрос getRecipes с внутренним userId: " + userId);

        // Вызываем API асинхронно, передавая userId
        Call<RecipesResponse> call = recipeApi.getRecipes(userId);
        call.enqueue(new Callback<RecipesResponse>() {
            @Override
            public void onResponse(Call<RecipesResponse> call, retrofit2.Response<RecipesResponse> response) {
                if (response.isSuccessful()) {
                    RecipesResponse recipesResponse = response.body();
                    if (recipesResponse != null && recipesResponse.isSuccess()
                            && recipesResponse.getRecipes() != null) {
                        List<Recipe> recipes = recipesResponse.getRecipes();
                        Log.d(TAG, "Загружено с сервера рецептов: " + recipes.size());
                        callback.onRecipesLoaded(recipes);
                    } else {
                        String errorMsg = response.body() != null
                                ? "Ошибка в ответе сервера: " + recipesResponse.getMessage()
                                : "Пустой ответ от сервера";
                        Log.e(TAG, errorMsg);
                        callback.onDataNotAvailable(errorMsg);
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
                    callback.onDataNotAvailable(errorMsg);
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
                callback.onDataNotAvailable(errorMsg);
            }
        });
    }

    /**
     * Обновляет статус лайка рецепта на сервере
     * 
     * @param recipe  рецепт, статус лайка которого нужно обновить
     * @param isLiked новое состояние лайка
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked) {
        if (!isNetworkAvailable()) {
            Log.e(TAG, "Нет подключения к интернету для обновления статуса лайка");
            return;
        }

        try {
            // Создаем JSON для запроса
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", recipe.getUserId());
            jsonObject.put("recipeId", recipe.getId());

            // Используем только эндпоинт ENDPOINT_LIKE для любых операций с лайками
            String url = ServerConfig.getFullUrl(ServerConfig.ENDPOINT_LIKE);
            RequestBody body = RequestBody.create(jsonObject.toString(), JSON);

            // Создаем и выполняем запрос
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e(TAG, "Ошибка при обновлении статуса лайка: " + e.getMessage(), e);
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Статус лайка успешно обновлен на сервере: recipeId=" + recipe.getId() + ", isLiked="
                                + isLiked);
                    } else {
                        Log.e(TAG,
                                "Ошибка при обновлении статуса лайка: " + response.code() + " " + response.message());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании запроса для обновления статуса лайка", e);
        }
    }

    /**
     * Проверка доступности сети
     * 
     * @return true, если сеть доступна
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
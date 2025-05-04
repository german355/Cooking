package com.example.cooking.ltr.network;

import androidx.annotation.NonNull;

import com.example.cooking.ltr.config.LTRServerConfig;
import com.example.cooking.ltr.models.Recipe;
import com.example.cooking.ltr.network.api.LTRApiService;
import com.example.cooking.ltr.network.callbacks.SearchRequestCallback;
import com.example.cooking.ltr.network.callbacks.SimilarRecipesCallback;
import com.example.cooking.ltr.network.models.ClickEventRequest;
import com.example.cooking.ltr.network.models.FavoriteSyncRequest;
import com.example.cooking.ltr.network.models.SearchResponse;
import com.example.cooking.ltr.network.models.SimilarRecipesResponse;
import com.example.cooking.network.services.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Клиент для взаимодействия с LTR API сервера
 */
public class LTRApiClient {
    // Базовый URL по умолчанию берем из конфигурации
    private static final String DEFAULT_BASE_URL = LTRServerConfig.BASE_URL;

    private String baseUrl = DEFAULT_BASE_URL;
    private String apiKey;
    private LTRApiService apiService;
    private Retrofit retrofit;

    /**
     * Установка базового URL для API
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Установка API-ключа для авторизации
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Инициализация клиента API
     */
    public void initialize() {
        // Используем общий Retrofit клиент с URL для LTR
        retrofit = RetrofitClient.getLtrClient(baseUrl);

        // Создаем API сервис
        apiService = retrofit.create(LTRApiService.class);
    }

    /**
     * Запрос персонализированных результатов поиска
     */
    public void requestPersonalizedResults(String query, boolean usePersonalization,
            final SearchRequestCallback callback) {
        // Создаем параметры запроса
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("q", query);
        queryParams.put("use_personalization", usePersonalization);
        queryParams.put("page", 1);
        queryParams.put("per_page", 20);

        // Выполняем запрос к API
        Call<SearchResponse> call = apiService.search(queryParams, getAuthHeader());
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call,
                    @NonNull Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getResults());
                } else {
                    callback.onError("Ошибка при получении результатов: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                callback.onError("Ошибка сети: " + t.getMessage());
            }
        });
    }

    /**
     * Запрос похожих рецептов
     */
    public void requestSimilarRecipes(long recipeId, final SimilarRecipesCallback callback) {
        // Создаем параметры запроса
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("type", "similar");
        queryParams.put("recipe_id", recipeId);
        queryParams.put("count", 5);

        // Выполняем запрос к API
        Call<SimilarRecipesResponse> call = apiService.getSimilarRecipes(queryParams, getAuthHeader());
        call.enqueue(new Callback<SimilarRecipesResponse>() {
            @Override
            public void onResponse(@NonNull Call<SimilarRecipesResponse> call,
                    @NonNull Response<SimilarRecipesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getRecommendations());
                } else {
                    callback.onError("Ошибка при получении похожих рецептов: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<SimilarRecipesResponse> call, @NonNull Throwable t) {
                callback.onError("Ошибка сети: " + t.getMessage());
            }
        });
    }

    /**
     * Отправка данных о клике на результат поиска
     */
    public void sendClickEvent(String query, long recipeId, int position) {
        // Создаем объект запроса
        ClickEventRequest request = new ClickEventRequest();
        request.setRecipeId(recipeId);
        request.setQuery(query);
        request.setPosition(position);
        request.setTimestamp(System.currentTimeMillis());

        // Выполняем запрос к API
        Call<Void> call = apiService.sendClickEvent(request, getAuthHeader());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                // Здесь можно добавить логирование успешной отправки события
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // Здесь можно добавить логирование неудачи и повторить попытку позже
            }
        });
    }

    /**
     * Синхронизация избранных рецептов с сервером
     */
    public void syncFavoritesWithServer(List<Recipe> favorites) {
        // Создаем объект запроса
        FavoriteSyncRequest request = new FavoriteSyncRequest();

        List<FavoriteSyncRequest.FavoriteItem> items = new ArrayList<>();
        for (Recipe recipe : favorites) {
            FavoriteSyncRequest.FavoriteItem item = new FavoriteSyncRequest.FavoriteItem();
            item.setRecipeId(recipe.getId());
            item.setAddedAt(recipe.getFavoriteTimestamp());
            items.add(item);
        }

        request.setFavorites(items);
        request.setLastSyncTimestamp(System.currentTimeMillis());

        // Выполняем запрос к API
        Call<Void> call = apiService.syncFavorites(request, getAuthHeader());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                // Обработка успешной синхронизации
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // Обработка ошибки синхронизации
            }
        });
    }

    /**
     * Получение заголовка авторизации
     */
    private String getAuthHeader() {
        return "Bearer " + apiKey;
    }
}
package com.example.cooking.ltr;

import android.content.Context;
import androidx.annotation.NonNull;

import com.example.cooking.ltr.cache.LTRCacheManager;
import com.example.cooking.ltr.logger.LTRDataCollector;
import com.example.cooking.ltr.models.Recipe;
import com.example.cooking.ltr.models.SearchResult;
import com.example.cooking.ltr.network.LTRApiClient;
import com.example.cooking.ltr.network.callbacks.SearchRequestCallback;
import com.example.cooking.ltr.network.callbacks.SimilarRecipesCallback;

import java.util.List;

/**
 * Основной класс клиентского SDK для взаимодействия с сервером LTR.
 * Выступает в роли фасада для всех LTR-взаимодействий в приложении.
 */
public class LTRClient {
    private static LTRClient instance;
    private final Context context;
    private final LTRDataCollector dataCollector;
    private final LTRApiClient apiClient;
    private final LTRCacheManager cacheManager;

    private String serverUrl = "http://api.cooking-server.com/v2/";
    private String apiKey;
    private boolean isPersonalizationEnabled = true;

    /**
     * Получение единственного экземпляра LTRClient (Singleton)
     */
    public static synchronized LTRClient getInstance(Context context) {
        if (instance == null) {
            instance = new LTRClient(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Приватный конструктор, используется только внутри getInstance
     */
    private LTRClient(Context context) {
        this.context = context;
        this.dataCollector = new LTRDataCollector(context);
        this.apiClient = new LTRApiClient();
        this.cacheManager = new LTRCacheManager(context);
    }

    /**
     * Получение доступа к коллектору данных
     */
    public LTRDataCollector getDataCollector() {
        return dataCollector;
    }

    /**
     * Установка URL сервера LTR
     */
    public LTRClient setServerUrl(String serverUrl) {
        // Убедимся, что URL заканчивается на "/"
        if (!serverUrl.endsWith("/")) {
            serverUrl = serverUrl + "/";
        }
        this.serverUrl = serverUrl;
        apiClient.setBaseUrl(serverUrl);
        return this;
    }

    /**
     * Установка API-ключа для авторизации на сервере
     */
    public LTRClient setApiKey(String apiKey) {
        this.apiKey = apiKey;
        apiClient.setApiKey(apiKey);
        return this;
    }

    /**
     * Включение/отключение персонализации
     */
    public LTRClient enablePersonalization(boolean enabled) {
        this.isPersonalizationEnabled = enabled;
        return this;
    }

    /**
     * Инициализация клиента LTR
     */
    public void initialize() {
        apiClient.initialize();
        dataCollector.initialize();

        // Отправка отложенных данных, если есть
        dataCollector.sendQueuedEvents();
    }

    /**
     * Освобождение ресурсов при завершении работы с клиентом
     * Должно вызываться в onDestroy() активити или в onCleared() ViewModel
     */
    public void shutdown() {
        dataCollector.shutdown();
    }

    /**
     * Сбор данных о клике на результат поиска
     */
    public void collectClickEvent(SearchResult result, int position) {
        dataCollector.collectClickEvent(result, position);
    }

    /**
     * Сбор данных о продолжительности просмотра рецепта
     */
    public void collectViewDuration(Recipe recipe, long durationMs) {
        dataCollector.collectViewDuration(recipe, durationMs);
    }

    /**
     * Сбор данных о сохранении рецепта
     */
    public void collectSaveAction(Recipe recipe) {
        dataCollector.collectSaveAction(recipe);
    }

    /**
     * Сбор данных о добавлении/удалении из избранного
     */
    public void collectFavoriteAction(Recipe recipe, boolean isFavorite) {
        dataCollector.collectFavoriteAction(recipe, isFavorite);
    }

    /**
     * Запрос персонализированных результатов поиска с сервера
     */
    public void requestPersonalizedResults(String query, SearchRequestCallback callback) {
        apiClient.requestPersonalizedResults(query, isPersonalizationEnabled, callback);
    }

    /**
     * Запрос похожих рецептов с сервера
     */
    public void requestSimilarRecipes(Recipe recipe, SimilarRecipesCallback callback) {
        apiClient.requestSimilarRecipes(recipe.getId(), callback);
    }

    /**
     * Синхронизация избранных рецептов с сервером
     */
    public void syncFavoritesWithServer(List<Recipe> favorites) {
        apiClient.syncFavoritesWithServer(favorites);
    }

    /**
     * Очистка кеша результатов
     */
    public void clearResultsCache() {
        cacheManager.clearCache();
    }
}
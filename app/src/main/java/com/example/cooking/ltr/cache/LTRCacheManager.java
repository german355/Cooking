package com.example.cooking.ltr.cache;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.room.Room;

import com.example.cooking.ltr.models.Recipe;
import com.example.cooking.ltr.models.SearchResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Класс для кеширования результатов поиска и рекомендаций,
 * полученных с сервера, для офлайн-доступа.
 */
public class LTRCacheManager {
    private static final String PREFS_NAME = "ltr_cache_prefs";
    private static final String LAST_CACHE_UPDATE_KEY = "last_cache_update";
    private static final long CACHE_EXPIRATION_TIME = TimeUnit.DAYS.toMillis(1); // 1 день

    private final Context context;
    private final SharedPreferences preferences;
    private final LTRCacheDatabase database;

    /**
     * Конструктор
     */
    public LTRCacheManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Инициализация базы данных Room для кеширования результатов
        this.database = Room.databaseBuilder(context,
                LTRCacheDatabase.class, "ltr_cache.db")
                .fallbackToDestructiveMigration()
                .build();
    }

    /**
     * Сохранение результатов поиска в кеш
     */
    public void cacheSearchResults(String query, List<SearchResult> results) {
        if (query == null || results == null || results.isEmpty())
            return;

        try {
            // Асинхронное сохранение результатов в базу данных
            new Thread(() -> {
                // Сначала удаляем старые результаты для этого запроса
                database.searchResultDao().deleteByQuery(query);

                // Затем сохраняем новые результаты
                for (SearchResult result : results) {
                    result.setQuery(query); // Привязываем результат к запросу
                    result.setCacheTimestamp(System.currentTimeMillis());
                    database.searchResultDao().insert(result);
                }

                // Обновляем время последнего обновления кеша
                updateLastCacheTime();
            }).start();
        } catch (Exception e) {
            // Логгирование ошибки
        }
    }

    /**
     * Сохранение рекомендаций в кеш
     */
    public void cacheRecommendations(String recommendationType, List<Recipe> recommendations) {
        if (recommendationType == null || recommendations == null || recommendations.isEmpty())
            return;

        try {
            // Асинхронное сохранение рекомендаций в базу данных
            new Thread(() -> {
                // Сначала удаляем старые рекомендации этого типа
                database.recommendationDao().deleteByType(recommendationType);

                // Затем сохраняем новые рекомендации
                for (Recipe recipe : recommendations) {
                    RecommendationEntity entity = new RecommendationEntity();
                    entity.setRecipeId(recipe.getId());
                    entity.setRecommendationType(recommendationType);
                    entity.setCacheTimestamp(System.currentTimeMillis());
                    entity.setRecipeData(recipe.toJson()); // Сериализуем рецепт в JSON

                    database.recommendationDao().insert(entity);
                }

                // Обновляем время последнего обновления кеша
                updateLastCacheTime();
            }).start();
        } catch (Exception e) {
            // Логгирование ошибки
        }
    }

    /**
     * Получение кешированных результатов поиска
     */
    public List<SearchResult> getCachedSearchResults(String query) {
        if (query == null)
            return null;

        try {
            // Возвращаем кешированные результаты из базы данных
            return database.searchResultDao().getByQuery(query);
        } catch (Exception e) {
            // Логгирование ошибки
            return null;
        }
    }

    /**
     * Получение кешированных рекомендаций
     */
    public List<Recipe> getCachedRecommendations(String recommendationType) {
        if (recommendationType == null)
            return null;

        try {
            // Получаем кешированные рекомендации из базы данных и преобразуем их обратно в
            // объекты Recipe
            List<RecommendationEntity> entities = database.recommendationDao().getByType(recommendationType);
            return RecommendationConverter.toRecipeList(entities);
        } catch (Exception e) {
            // Логгирование ошибки
            return null;
        }
    }

    /**
     * Очистка всего кеша
     */
    public void clearCache() {
        try {
            // Асинхронная очистка всех таблиц
            new Thread(() -> {
                database.searchResultDao().deleteAll();
                database.recommendationDao().deleteAll();

                // Сбрасываем время последнего обновления кеша
                resetLastCacheTime();
            }).start();
        } catch (Exception e) {
            // Логгирование ошибки
        }
    }

    /**
     * Очистка устаревших данных кеша
     */
    public void clearExpiredCache() {
        try {
            // Асинхронная очистка устаревших данных
            new Thread(() -> {
                long expireTime = System.currentTimeMillis() - CACHE_EXPIRATION_TIME;

                database.searchResultDao().deleteOlderThan(expireTime);
                database.recommendationDao().deleteOlderThan(expireTime);
            }).start();
        } catch (Exception e) {
            // Логгирование ошибки
        }
    }

    /**
     * Проверка свежести кеша
     */
    public boolean isCacheFresh() {
        long lastUpdate = preferences.getLong(LAST_CACHE_UPDATE_KEY, 0);
        return (System.currentTimeMillis() - lastUpdate) < CACHE_EXPIRATION_TIME;
    }

    /**
     * Обновление времени последнего обновления кеша
     */
    private void updateLastCacheTime() {
        preferences.edit()
                .putLong(LAST_CACHE_UPDATE_KEY, System.currentTimeMillis())
                .apply();
    }

    /**
     * Сброс времени последнего обновления кеша
     */
    private void resetLastCacheTime() {
        preferences.edit()
                .remove(LAST_CACHE_UPDATE_KEY)
                .apply();
    }
}
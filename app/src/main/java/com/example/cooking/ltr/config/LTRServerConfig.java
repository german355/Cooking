package com.example.cooking.ltr.config;

/**
 * Конфигурация для LTR-системы
 * Содержит все необходимые URL и endpoints для взаимодействия с LTR API сервера
 */
public class LTRServerConfig {

    /**
     * Базовый URL для LTR API сервера
     * Используем тот же хост, что и основное API приложения, но с другим
     * контекстным путем
     */
    public static final String BASE_URL = "http://r1.veroid.network:10009/ltr/v2/";

    /**
     * API ключ для авторизации на сервере (должен быть заменен на реальный)
     */
    public static final String API_KEY = "PLACEHOLDER_API_KEY";

    /* Эндпоинты для получения данных */

    /**
     * Эндпоинт для получения персонализированных результатов поиска
     */
    public static final String ENDPOINT_SEARCH = "/search";

    /**
     * Эндпоинт для получения пользовательских предпочтений
     */
    public static final String ENDPOINT_USER_PREFERENCES = "/user/preferences";

    /**
     * Эндпоинт для получения рекомендаций
     */
    public static final String ENDPOINT_RECOMMENDATIONS = "/recommendations";

    /* Эндпоинты для отправки данных */

    /**
     * Эндпоинт для отправки данных о клике на результат поиска
     */
    public static final String ENDPOINT_SEARCH_CLICK = "/search/click";

    /**
     * Эндпоинт для отправки обратной связи о релевантности
     */
    public static final String ENDPOINT_SEARCH_FEEDBACK = "/search/feedback";

    /**
     * Эндпоинт для синхронизации избранных рецептов
     */
    public static final String ENDPOINT_FAVORITES_SYNC = "/user/favorites/sync";

    /**
     * Эндпоинт для действий с избранным (добавление/удаление)
     */
    public static final String ENDPOINT_FAVORITE_ACTION = "/user/favorites";

    /**
     * Получить полный URL для указанного endpoint
     * 
     * @param endpoint эндпоинт LTR API
     * @return полный URL
     */
    public static String getFullUrl(String endpoint) {
        return BASE_URL + endpoint;
    }
}
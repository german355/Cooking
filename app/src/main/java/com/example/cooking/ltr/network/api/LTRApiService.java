package com.example.cooking.ltr.network.api;

import com.example.cooking.ltr.config.LTRServerConfig;
import com.example.cooking.ltr.network.models.ClickEventRequest;
import com.example.cooking.ltr.network.models.FavoriteActionRequest;
import com.example.cooking.ltr.network.models.FavoriteSyncRequest;
import com.example.cooking.ltr.network.models.FeedbackRequest;
import com.example.cooking.ltr.network.models.SearchResponse;
import com.example.cooking.ltr.network.models.SimilarRecipesResponse;
import com.example.cooking.ltr.network.models.UserPreferencesResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.QueryMap;

/**
 * Интерфейс для взаимодействия с API сервера Learning to Rank
 */
public interface LTRApiService {

        /**
         * Получение персонализированных результатов поиска
         * 
         * GET /search
         */
        @GET(LTRServerConfig.ENDPOINT_SEARCH)
        Call<SearchResponse> search(
                        @QueryMap Map<String, Object> options,
                        @Header("Authorization") String authorization);

        /**
         * Отправка данных о клике на результат поиска
         * 
         * POST /search/click
         */
        @POST(LTRServerConfig.ENDPOINT_SEARCH_CLICK)
        Call<Void> sendClickEvent(
                        @Body ClickEventRequest clickEvent,
                        @Header("Authorization") String authorization);

        /**
         * Отправка обратной связи о релевантности
         * 
         * POST /search/feedback
         */
        @POST(LTRServerConfig.ENDPOINT_SEARCH_FEEDBACK)
        Call<Void> sendFeedback(
                        @Body FeedbackRequest feedback,
                        @Header("Authorization") String authorization);

        /**
         * Получение персональных настроек пользователя
         * 
         * GET /user/preferences
         */
        @GET(LTRServerConfig.ENDPOINT_USER_PREFERENCES)
        Call<UserPreferencesResponse> getUserPreferences(
                        @Header("Authorization") String authorization);

        /**
         * Обновление пользовательских предпочтений
         * 
         * PUT /user/preferences
         */
        @PUT(LTRServerConfig.ENDPOINT_USER_PREFERENCES)
        Call<Void> updateUserPreferences(
                        @Body Map<String, Object> preferences,
                        @Header("Authorization") String authorization);

        /**
         * Синхронизация избранных рецептов с сервером
         * 
         * POST /user/favorites/sync
         */
        @POST(LTRServerConfig.ENDPOINT_FAVORITES_SYNC)
        Call<Void> syncFavorites(
                        @Body FavoriteSyncRequest request,
                        @Header("Authorization") String authorization);

        /**
         * Добавление/удаление рецепта из избранного
         * 
         * POST /user/favorites
         */
        @POST(LTRServerConfig.ENDPOINT_FAVORITE_ACTION)
        Call<Void> favoriteAction(
                        @Body FavoriteActionRequest request,
                        @Header("Authorization") String authorization);

        /**
         * Получение рекомендованных рецептов
         * 
         * GET /recommendations
         */
        @GET(LTRServerConfig.ENDPOINT_RECOMMENDATIONS)
        Call<SimilarRecipesResponse> getSimilarRecipes(
                        @QueryMap Map<String, Object> options,
                        @Header("Authorization") String authorization);
}
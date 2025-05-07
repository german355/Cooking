package com.example.cooking.utils;

import com.example.cooking.Recipe.Recipe;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.api.SearchApi;
import com.example.cooking.network.responses.RecipesResponse;
import com.example.cooking.network.responses.SearchResponse;
import com.example.cooking.network.services.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Collections;
import com.example.cooking.utils.MySharedPreferences;

public class RecipeSearchService {
    
    public interface SearchCallback {
        void onSearchResults(List<Recipe> recipes);
        void onSearchError(String error);
    }
    
    private final Context context;
    private final ApiService apiService;
    private final SearchApi searchApi;
    
    public RecipeSearchService(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService();
        this.searchApi = RetrofitClient.getClient().create(SearchApi.class);
    }
    
    /**
     * Заглушка для метода поиска: пока возвращает пустой список
     */
    public void searchRecipes(String query, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onSearchResults(Collections.emptyList());
            return;
        }
        MySharedPreferences preferences = new MySharedPreferences(context);
        boolean smartSearchEnabled = preferences.getBoolean("smart_search_enabled", false);
        android.util.Log.d("RecipeSearchService", "Smart search enabled from prefs: " + smartSearchEnabled);
        if (smartSearchEnabled) {
            String userId = preferences.getString("userId", "0");
            int page = 1;
            int perPage = 20;
            Call<SearchResponse> smartCall = searchApi.searchRecipes(query.trim(), userId, page, perPage);
            smartCall.enqueue(new Callback<SearchResponse>() {
                @Override
                public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        callback.onSearchResults(response.body().getData().getResults());
                    } else {
                        String message = response.body() != null ? "Статус: " + response.body().getStatus() : "Ошибка HTTP " + (response != null ? response.code() : "");
                        callback.onSearchError(message);
                    }
                }

                @Override
                public void onFailure(Call<SearchResponse> call, Throwable t) {
                    callback.onSearchError(t.getMessage() != null ? t.getMessage() : "Ошибка сети при умном поиске");
                }
            });
        } else {
            Call<RecipesResponse> call = apiService.searchRecipesSimple(query.trim());
            call.enqueue(new Callback<RecipesResponse>() {
                @Override
                public void onResponse(Call<RecipesResponse> call, Response<RecipesResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        callback.onSearchResults(response.body().getRecipes());
                    } else {
                        String message = response.body() != null ? response.body().getMessage() : "Ошибка HTTP " + response.code();
                        callback.onSearchError(message);
                    }
                }

                @Override
                public void onFailure(Call<RecipesResponse> call, Throwable t) {
                    callback.onSearchError(t.getMessage() != null ? t.getMessage() : "Ошибка сети при простом поиске");
                }
            });
        }
    }
}
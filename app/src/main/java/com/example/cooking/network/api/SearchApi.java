package com.example.cooking.network.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import com.example.cooking.network.responses.SearchResponse;

public interface SearchApi {
    @GET("search/")
    Call<SearchResponse> searchRecipes(
        @Query("q")        String query,
        @Query("user_id")  String userId,
        @Query("page")     int page,
        @Query("per_page") int perPage
    );
}
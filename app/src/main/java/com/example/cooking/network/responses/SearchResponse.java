package com.example.cooking.network.responses;

import com.example.cooking.Recipe.Recipe;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SearchResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private Data data;

    public String getStatus() {
        return status;
    }

    public Data getData() {
        return data;
    }

    /** Вложенный класс для объекта data */
    public static class Data {
        @SerializedName("results")
        private List<Recipe> results;
        
        public List<Recipe> getResults() {
            return results;
        }
    }
}

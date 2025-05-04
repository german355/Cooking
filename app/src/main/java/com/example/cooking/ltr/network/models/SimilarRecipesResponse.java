package com.example.cooking.ltr.network.models;

import com.example.cooking.ltr.models.Recipe;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Модель ответа сервера на запрос похожих рецептов
 */
public class SimilarRecipesResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("recommendation_type")
    private String recommendationType;

    @SerializedName("source_recipe_id")
    private long sourceRecipeId;

    @SerializedName("recommendations")
    private List<Recipe> recommendations;

    /**
     * Геттеры и сеттеры
     */

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(String recommendationType) {
        this.recommendationType = recommendationType;
    }

    public long getSourceRecipeId() {
        return sourceRecipeId;
    }

    public void setSourceRecipeId(long sourceRecipeId) {
        this.sourceRecipeId = sourceRecipeId;
    }

    public List<Recipe> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Recipe> recommendations) {
        this.recommendations = recommendations;
    }
}
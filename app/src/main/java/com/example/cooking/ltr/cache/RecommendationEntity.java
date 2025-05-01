package com.example.cooking.ltr.cache;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity-класс для хранения кешированных рекомендаций в базе данных Room
 */
@Entity(tableName = "recommendations")
public class RecommendationEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "recipe_id")
    private long recipeId;

    @ColumnInfo(name = "recommendation_type")
    @NonNull
    private String recommendationType = "";

    @ColumnInfo(name = "cache_timestamp")
    private long cacheTimestamp;

    @ColumnInfo(name = "recipe_data")
    @NonNull
    private String recipeData = ""; // JSON-строка с данными рецепта

    /**
     * Геттеры и сеттеры
     */

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(long recipeId) {
        this.recipeId = recipeId;
    }

    @NonNull
    public String getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(@NonNull String recommendationType) {
        this.recommendationType = recommendationType;
    }

    public long getCacheTimestamp() {
        return cacheTimestamp;
    }

    public void setCacheTimestamp(long cacheTimestamp) {
        this.cacheTimestamp = cacheTimestamp;
    }

    @NonNull
    public String getRecipeData() {
        return recipeData;
    }

    public void setRecipeData(@NonNull String recipeData) {
        this.recipeData = recipeData;
    }
}
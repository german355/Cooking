package com.example.cooking.ltr.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Модель запроса для отправки данных о добавлении/удалении рецепта из
 * избранного
 */
public class FavoriteActionRequest {

    @SerializedName("recipe_id")
    private long recipeId;

    @SerializedName("is_favorite")
    private boolean isFavorite;

    @SerializedName("timestamp")
    private long timestamp;

    /**
     * Геттеры и сеттеры
     */

    public long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(long recipeId) {
        this.recipeId = recipeId;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
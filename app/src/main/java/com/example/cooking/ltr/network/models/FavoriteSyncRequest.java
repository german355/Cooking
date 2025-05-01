package com.example.cooking.ltr.network.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Модель запроса для синхронизации избранных рецептов с сервером
 */
public class FavoriteSyncRequest {

    @SerializedName("favorites")
    private List<FavoriteItem> favorites;

    @SerializedName("last_sync_timestamp")
    private long lastSyncTimestamp;

    /**
     * Геттеры и сеттеры
     */

    public List<FavoriteItem> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<FavoriteItem> favorites) {
        this.favorites = favorites;
    }

    public long getLastSyncTimestamp() {
        return lastSyncTimestamp;
    }

    public void setLastSyncTimestamp(long lastSyncTimestamp) {
        this.lastSyncTimestamp = lastSyncTimestamp;
    }

    /**
     * Вложенный класс для элемента избранного
     */
    public static class FavoriteItem {

        @SerializedName("recipe_id")
        private long recipeId;

        @SerializedName("added_at")
        private long addedAt;

        /**
         * Геттеры и сеттеры
         */

        public long getRecipeId() {
            return recipeId;
        }

        public void setRecipeId(long recipeId) {
            this.recipeId = recipeId;
        }

        public long getAddedAt() {
            return addedAt;
        }

        public void setAddedAt(long addedAt) {
            this.addedAt = addedAt;
        }
    }
}
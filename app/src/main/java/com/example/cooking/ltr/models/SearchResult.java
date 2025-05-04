package com.example.cooking.ltr.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

/**
 * Модель данных результата поиска для использования в LTR-системе
 * Используется также как Entity для Room при кешировании результатов
 */
@Entity(tableName = "search_results")
public class SearchResult {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "recipe_id")
    @SerializedName("recipe_id")
    private long recipeId;

    @ColumnInfo(name = "title")
    @SerializedName("title")
    @NonNull
    private String title = "";

    @ColumnInfo(name = "description")
    @SerializedName("description")
    private String description;

    @ColumnInfo(name = "image_url")
    @SerializedName("image_url")
    private String imageUrl;

    @ColumnInfo(name = "rating")
    @SerializedName("rating")
    private float rating;

    @ColumnInfo(name = "cooking_time")
    @SerializedName("cooking_time")
    private int cookingTime;

    @ColumnInfo(name = "position")
    private int position;

    @ColumnInfo(name = "query")
    private String query;

    @ColumnInfo(name = "server_score")
    @SerializedName("server_score")
    private float serverScore;

    @ColumnInfo(name = "personalization_reason")
    @SerializedName("personalization_reason")
    private String personalizationReason;

    @ColumnInfo(name = "cache_timestamp")
    private long cacheTimestamp;

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
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    public void setCookingTime(int cookingTime) {
        this.cookingTime = cookingTime;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public float getServerScore() {
        return serverScore;
    }

    public void setServerScore(float serverScore) {
        this.serverScore = serverScore;
    }

    public String getPersonalizationReason() {
        return personalizationReason;
    }

    public void setPersonalizationReason(String personalizationReason) {
        this.personalizationReason = personalizationReason;
    }

    public long getCacheTimestamp() {
        return cacheTimestamp;
    }

    public void setCacheTimestamp(long cacheTimestamp) {
        this.cacheTimestamp = cacheTimestamp;
    }
}
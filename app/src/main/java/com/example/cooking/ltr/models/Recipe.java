package com.example.cooking.ltr.models;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Модель данных рецепта для использования в LTR-системе
 */
public class Recipe {

    @SerializedName("recipe_id")
    private long id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("rating")
    private float rating;

    @SerializedName("votes_count")
    private int votesCount;

    @SerializedName("cooking_time")
    private int cookingTime;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("ingredients_count")
    private int ingredientsCount;

    @SerializedName("is_favorite")
    private boolean isFavorite;

    @SerializedName("category")
    private Category category;

    @SerializedName("server_score")
    private float serverScore;

    @SerializedName("personalization_reason")
    private String personalizationReason;

    // Поле для отслеживания времени добавления в избранное
    private long favoriteTimestamp;

    /**
     * Сериализация в JSON
     */
    public String toJson() {
        return new Gson().toJson(this);
    }

    /**
     * Геттеры и сеттеры
     */

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getVotesCount() {
        return votesCount;
    }

    public void setVotesCount(int votesCount) {
        this.votesCount = votesCount;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    public void setCookingTime(int cookingTime) {
        this.cookingTime = cookingTime;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getIngredientsCount() {
        return ingredientsCount;
    }

    public void setIngredientsCount(int ingredientsCount) {
        this.ingredientsCount = ingredientsCount;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
        if (favorite) {
            favoriteTimestamp = System.currentTimeMillis();
        }
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
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

    public long getFavoriteTimestamp() {
        return favoriteTimestamp;
    }

    public void setFavoriteTimestamp(long favoriteTimestamp) {
        this.favoriteTimestamp = favoriteTimestamp;
    }

    /**
     * Вложенный класс для категории рецепта
     */
    public static class Category {
        @SerializedName("id")
        private int id;

        @SerializedName("name")
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
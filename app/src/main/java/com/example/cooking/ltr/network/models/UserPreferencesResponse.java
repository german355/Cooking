package com.example.cooking.ltr.network.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Модель ответа сервера на запрос пользовательских предпочтений
 */
public class UserPreferencesResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("preferences")
    private Preferences preferences;

    /**
     * Геттеры и сеттеры
     */

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    /**
     * Вложенный класс для представления предпочтений пользователя
     */
    public static class Preferences {

        @SerializedName("favorite_categories")
        private List<Integer> favoriteCategories;

        @SerializedName("frequent_ingredients")
        private List<String> frequentIngredients;

        @SerializedName("average_cooking_time")
        private int averageCookingTime;

        @SerializedName("dietary_preferences")
        private List<String> dietaryPreferences;

        @SerializedName("taste_profile")
        private Map<String, Float> tasteProfile;

        @SerializedName("search_history")
        private SearchHistory searchHistory;

        @SerializedName("personalization_score")
        private float personalizationScore;

        public List<Integer> getFavoriteCategories() {
            return favoriteCategories;
        }

        public void setFavoriteCategories(List<Integer> favoriteCategories) {
            this.favoriteCategories = favoriteCategories;
        }

        public List<String> getFrequentIngredients() {
            return frequentIngredients;
        }

        public void setFrequentIngredients(List<String> frequentIngredients) {
            this.frequentIngredients = frequentIngredients;
        }

        public int getAverageCookingTime() {
            return averageCookingTime;
        }

        public void setAverageCookingTime(int averageCookingTime) {
            this.averageCookingTime = averageCookingTime;
        }

        public List<String> getDietaryPreferences() {
            return dietaryPreferences;
        }

        public void setDietaryPreferences(List<String> dietaryPreferences) {
            this.dietaryPreferences = dietaryPreferences;
        }

        public Map<String, Float> getTasteProfile() {
            return tasteProfile;
        }

        public void setTasteProfile(Map<String, Float> tasteProfile) {
            this.tasteProfile = tasteProfile;
        }

        public SearchHistory getSearchHistory() {
            return searchHistory;
        }

        public void setSearchHistory(SearchHistory searchHistory) {
            this.searchHistory = searchHistory;
        }

        public float getPersonalizationScore() {
            return personalizationScore;
        }

        public void setPersonalizationScore(float personalizationScore) {
            this.personalizationScore = personalizationScore;
        }
    }

    /**
     * Вложенный класс для представления истории поиска
     */
    public static class SearchHistory {

        @SerializedName("recent_queries")
        private List<String> recentQueries;

        @SerializedName("frequent_terms")
        private List<String> frequentTerms;

        public List<String> getRecentQueries() {
            return recentQueries;
        }

        public void setRecentQueries(List<String> recentQueries) {
            this.recentQueries = recentQueries;
        }

        public List<String> getFrequentTerms() {
            return frequentTerms;
        }

        public void setFrequentTerms(List<String> frequentTerms) {
            this.frequentTerms = frequentTerms;
        }
    }
}
package com.example.cooking.ltr.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Модель запроса для отправки обратной связи о релевантности результата
 */
public class FeedbackRequest {

    @SerializedName("recipe_id")
    private long recipeId;

    @SerializedName("query")
    private String query;

    @SerializedName("relevance_score")
    private int relevanceScore;

    @SerializedName("feedback_type")
    private String feedbackType;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("comments")
    private String comments;

    /**
     * Геттеры и сеттеры
     */

    public long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(long recipeId) {
        this.recipeId = recipeId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(int relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public String getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
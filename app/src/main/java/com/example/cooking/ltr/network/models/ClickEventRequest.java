package com.example.cooking.ltr.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Модель запроса для отправки данных о клике на результат поиска
 */
public class ClickEventRequest {

    @SerializedName("recipe_id")
    private long recipeId;

    @SerializedName("query")
    private String query;

    @SerializedName("position")
    private int position;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("session_id")
    private String sessionId;

    @SerializedName("device_info")
    private String deviceInfo;

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

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
}
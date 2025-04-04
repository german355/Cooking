package com.example.cooking.network.responses;

import com.example.cooking.Recipe.Recipe;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Класс для представления ответа от сервера с рецептами
 */
public class RecipesResponse {
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("recipes")
    private List<Recipe> recipes;
    
    @SerializedName("count")
    private int count;
    
    @SerializedName("message")
    private String message;
    
    public boolean isSuccess() {
        return success;
    }
    
    public List<Recipe> getRecipes() {
        return recipes;
    }
    
    public int getCount() {
        return count;
    }
    
    public String getMessage() {
        return message;
    }
} 
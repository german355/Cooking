package com.example.cooking.data.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.cooking.Recipe.Recipe;

@Entity(tableName = "liked_recipes")
public class LikedRecipeEntity {
    @PrimaryKey
    @NonNull
    private int recipeId;
    @NonNull
    private String userId;

    public LikedRecipeEntity(int recipeId, @NonNull String userId) {
        this.recipeId = recipeId;
        this.userId = userId;
    }

    public int getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public Recipe toRecipe() {
        Recipe recipe = new Recipe();
        recipe.setId(this.recipeId);
        recipe.setUserId(this.userId);
        recipe.setLiked(true);
        return recipe;
    }
}
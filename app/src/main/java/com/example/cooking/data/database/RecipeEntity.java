package com.example.cooking.data.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.cooking.Recipe.Recipe;

/**
 * Entity класс для хранения рецептов в Room Database
 */
@Entity(tableName = "recipes")
public class RecipeEntity {
    @PrimaryKey
    private int id;
    private String title;
    private String ingredients;
    private String instructions;
    private String created_at;
    private String userId;
    private String photo_url;
    private boolean isLiked;

    // Конструкторы
    public RecipeEntity() {
    }

    // Конвертирует модель Recipe в RecipeEntity
    public RecipeEntity(Recipe recipe) {
        this.id = recipe.getId();
        this.title = recipe.getTitle();
        this.ingredients = recipe.getIngredients();
        this.instructions = recipe.getInstructions();
        this.created_at = recipe.getCreated_at();
        this.userId = recipe.getUserId();
        this.photo_url = recipe.getPhoto_url();
        this.isLiked = recipe.isLiked();
    }

    // Конвертирует RecipeEntity в Recipe
    public Recipe toRecipe() {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setTitle(title);
        recipe.setIngredients(ingredients);
        recipe.setInstructions(instructions);
        recipe.setCreated_at(created_at);
        recipe.setUserId(userId);
        recipe.setPhoto_url(photo_url);
        recipe.setLiked(isLiked);
        return recipe;
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPhoto_url() {
        return photo_url;
    }

    public void setPhoto_url(String photo_url) {
        this.photo_url = photo_url;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    @NonNull
    @Override
    public String toString() {
        return "RecipeEntity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", ingredients='" + ingredients + '\'' +
                ", instructions='" + instructions + '\'' +
                ", created_at='" + created_at + '\'' +
                ", userId='" + userId + '\'' +
                ", photo_url='" + photo_url + '\'' +
                ", isLiked=" + isLiked +
                '}';
    }
} 
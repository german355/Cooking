package com.example.cooking.Recipe;

/**
 * Класс, представляющий рецепт.
 * Содержит всю информацию о рецепте, необходимую для отображения
 */
public class Recipe {
    private int id;
    private String title;
    private String ingredients;
    private String instructions;
    private String created_at;
    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Конструктор по умолчанию
    public Recipe() {
        // Пустой конструктор для создания объекта через сеттеры
    }
    
    // Конструктор для тестовых данных
    
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
    
    public String getCreated_at() {return created_at;}
    
    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", ingredients='" + ingredients + '\'' +
                ", instructions='" + instructions + '\'' +
                ", created_at='" + created_at + '\'' +
                '}';
    }
}
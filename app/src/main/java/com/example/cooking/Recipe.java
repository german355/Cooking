package com.example.cooking;

/**
 * Модель данных рецепта.
 * Содержит всю информацию о конкретном рецепте.
 */
public class Recipe {
    private String title;      // Название рецепта
    private String Creator;    // Автор рецепта
    private String Instructor; // Инструкция по приготовлению
    private String food;       // Список ингредиентов

    /**
     * Конструктор для создания нового рецепта
     * @param title название рецепта
     */
    public Recipe(String title /*String Creator*/) {
        //this.Creator = Creator;
        this.title = title;
    }

    // Геттеры для получения данных рецепта
    public String getTitle() {
        return title;
    }

    public String getCreator() {
        return Creator;
    }

    public String getInstructor() {
        return Instructor;
    }

    public String getFood() {
        return food;
    }
}
package com.example.cooking.ltr.cache;

import com.example.cooking.ltr.models.Recipe;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Утилитный класс для конвертации между Entity и моделями рекомендаций
 */
public class RecommendationConverter {

    private static final Gson gson = new Gson();

    /**
     * Преобразует JSON-строку в объект Recipe
     */
    public static Recipe fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            return gson.fromJson(json, Recipe.class);
        } catch (Exception e) {
            // Логгирование ошибки
            return null;
        }
    }

    /**
     * Преобразует список сущностей рекомендаций в список Recipe
     */
    public static List<Recipe> toRecipeList(List<RecommendationEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        List<Recipe> recipes = new ArrayList<>(entities.size());

        for (RecommendationEntity entity : entities) {
            Recipe recipe = fromJson(entity.getRecipeData());
            if (recipe != null) {
                recipes.add(recipe);
            }
        }

        return recipes;
    }
}
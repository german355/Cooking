package com.example.cooking.data.database.converters;

import android.util.Log;
import androidx.room.TypeConverter;
import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Step;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Type Converters для Room для преобразования списков Ingredient и Step в JSON и обратно.
 */
public class DataConverters {
    private static Gson gson = new Gson();

    // --- Ingredient List Converters ---

    @TypeConverter
    public static String fromIngredientList(List<Ingredient> ingredients) {
        if (ingredients == null) {
            return null;
        }
        return gson.toJson(ingredients);
    }

    @TypeConverter
    public static List<Ingredient> toIngredientList(String ingredientsString) {
        if (ingredientsString == null || ingredientsString.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            Type listType = new TypeToken<ArrayList<Ingredient>>() {}.getType();
            List<Ingredient> list = gson.fromJson(ingredientsString, listType);
            if (list == null) {
                return Collections.emptyList();
            }
            
            // Логирование для отладки
            Log.d("DataConverters", "Преобразовано ингредиентов: " + list.size() + ", JSON: " + ingredientsString);
            
            return list;
        } catch (Exception e) {
            Log.e("DataConverters", "Ошибка преобразования JSON ингредиентов: " + ingredientsString + ", " + e.getMessage());
            
            // Обработка ошибки: если данные не являются массивом
            try {
                // Попробуем проверить, является ли это числом (например, id)
                Integer.parseInt(ingredientsString);
                // Если это число, вернем пустой список
                return Collections.emptyList();
            } catch (NumberFormatException nfe) {
                // Если это не число, попробуем другие варианты обработки
                // Например, это может быть одиночный объект, а не массив
                try {
                    // Проверяем, является ли это одиночным объектом
                    Ingredient singleIngredient = gson.fromJson(ingredientsString, Ingredient.class);
                    if (singleIngredient != null) {
                        return Collections.singletonList(singleIngredient);
                    }
                } catch (Exception ex) {
                    // Игнорируем и возвращаем пустой список
                }
            }
            // В случае любой ошибки, возвращаем пустой список
            return Collections.emptyList();
        }
    }

    // --- Step List Converters ---

    @TypeConverter
    public static String fromStepList(List<Step> steps) {
        if (steps == null) {
            return null;
        }
        return gson.toJson(steps);
    }

    @TypeConverter
    public static List<Step> toStepList(String stepsString) {
        if (stepsString == null || stepsString.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            Type listType = new TypeToken<ArrayList<Step>>() {}.getType();
            List<Step> list = gson.fromJson(stepsString, listType);
            if (list == null) {
                return Collections.emptyList();
            }
            
            // Проверяем и корректируем номера шагов
            for (int i = 0; i < list.size(); i++) {
                Step step = list.get(i);
                if (step.getNumber() <= 0) {
                    step.setNumber(i + 1);
                }
            }
            
            // Логирование для отладки
            Log.d("DataConverters", "Преобразовано шагов: " + list.size() + ", JSON: " + stepsString);
            
            return list;
        } catch (Exception e) {
            Log.e("DataConverters", "Ошибка преобразования JSON шагов: " + stepsString + ", " + e.getMessage());
            
            // Аналогичная обработка ошибок, как для ингредиентов
            try {
                Integer.parseInt(stepsString);
                return Collections.emptyList();
            } catch (NumberFormatException nfe) {
                try {
                    Step singleStep = gson.fromJson(stepsString, Step.class);
                    if (singleStep != null) {
                        return Collections.singletonList(singleStep);
                    }
                } catch (Exception ex) {
                    // Игнорируем и возвращаем пустой список
                }
            }
            return Collections.emptyList();
        }
    }
} 
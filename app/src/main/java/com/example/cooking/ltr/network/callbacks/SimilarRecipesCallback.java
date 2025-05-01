package com.example.cooking.ltr.network.callbacks;

import com.example.cooking.ltr.models.Recipe;

import java.util.List;

/**
 * Коллбэк для получения похожих рецептов с сервера
 */
public interface SimilarRecipesCallback {

    /**
     * Вызывается при успешном получении похожих рецептов
     * 
     * @param recipes список похожих рецептов
     */
    void onSuccess(List<Recipe> recipes);

    /**
     * Вызывается при ошибке получения рецептов
     * 
     * @param errorMessage сообщение об ошибке
     */
    void onError(String errorMessage);
}
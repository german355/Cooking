package com.example.cooking.ltr.network.callbacks;

import com.example.cooking.ltr.models.SearchResult;

import java.util.List;

/**
 * Коллбэк для получения результатов поиска с сервера
 */
public interface SearchRequestCallback {

    /**
     * Вызывается при успешном получении результатов поиска
     * 
     * @param results список результатов поиска
     */
    void onSuccess(List<SearchResult> results);

    /**
     * Вызывается при ошибке получения результатов
     * 
     * @param errorMessage сообщение об ошибке
     */
    void onError(String errorMessage);
}
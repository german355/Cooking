package com.example.cooking.ltr.cache;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cooking.ltr.models.SearchResult;

import java.util.List;

/**
 * DAO для работы с кешированными результатами поиска
 */
@Dao
public interface SearchResultDao {

    /**
     * Вставка результата поиска в кеш
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SearchResult result);

    /**
     * Получение кешированных результатов для заданного запроса
     */
    @Query("SELECT * FROM search_results WHERE query = :query ORDER BY position ASC")
    List<SearchResult> getByQuery(String query);

    /**
     * Удаление всех результатов для заданного запроса
     */
    @Query("DELETE FROM search_results WHERE query = :query")
    void deleteByQuery(String query);

    /**
     * Удаление устаревших результатов (старше указанного timestamp)
     */
    @Query("DELETE FROM search_results WHERE cache_timestamp < :timestamp")
    void deleteOlderThan(long timestamp);

    /**
     * Удаление всех кешированных результатов
     */
    @Query("DELETE FROM search_results")
    void deleteAll();
}
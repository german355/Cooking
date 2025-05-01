package com.example.cooking.ltr.cache;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO для работы с кешированными рекомендациями
 */
@Dao
public interface RecommendationDao {

    /**
     * Вставка рекомендации в кеш
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RecommendationEntity recommendation);

    /**
     * Получение кешированных рекомендаций по типу
     */
    @Query("SELECT * FROM recommendations WHERE recommendation_type = :type")
    List<RecommendationEntity> getByType(String type);

    /**
     * Удаление всех рекомендаций указанного типа
     */
    @Query("DELETE FROM recommendations WHERE recommendation_type = :type")
    void deleteByType(String type);

    /**
     * Удаление устаревших рекомендаций (старше указанного timestamp)
     */
    @Query("DELETE FROM recommendations WHERE cache_timestamp < :timestamp")
    void deleteOlderThan(long timestamp);

    /**
     * Удаление всех кешированных рекомендаций
     */
    @Query("DELETE FROM recommendations")
    void deleteAll();
}
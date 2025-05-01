package com.example.cooking.ltr.cache;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.cooking.ltr.models.SearchResult;

/**
 * База данных Room для кеширования результатов поиска и рекомендаций
 */
@Database(entities = { SearchResult.class, RecommendationEntity.class }, version = 1, exportSchema = false)
public abstract class LTRCacheDatabase extends RoomDatabase {

    /**
     * DAO для работы с результатами поиска
     */
    public abstract SearchResultDao searchResultDao();

    /**
     * DAO для работы с рекомендациями
     */
    public abstract RecommendationDao recommendationDao();
}
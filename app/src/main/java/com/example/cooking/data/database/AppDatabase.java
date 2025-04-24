package com.example.cooking.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Класс базы данных приложения
 */
@Database(entities = {RecipeEntity.class, LikedRecipeEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "recipes_database";
    private static volatile AppDatabase INSTANCE;
    
    /**
     * Получить DAO для работы с рецептами
     * @return RecipeDao
     */
    public abstract RecipeDao recipeDao();
    
    /**
     * Получить DAO для работы с рецептами
     * @return RecipeDao
     */
    public abstract LikedRecipeDao likedRecipeDao();
    
    /**
     * Получить инстанс базы данных (Singleton pattern)
     * @param context контекст приложения
     * @return инстанс базы данных
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            .fallbackToDestructiveMigration() // Если схема базы изменилась, пересоздать её
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
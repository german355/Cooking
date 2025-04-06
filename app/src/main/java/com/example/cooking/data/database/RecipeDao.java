package com.example.cooking.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

/**
 * DAO интерфейс для работы с рецептами в базе данных
 */
@Dao
public interface RecipeDao {
    
    /**
     * Получить все рецепты
     * @return LiveData список всех рецептов
     */
    @Query("SELECT * FROM recipes")
    LiveData<List<RecipeEntity>> getAllRecipes();
    
    /**
     * Получить все рецепты (без LiveData)
     * @return список всех рецептов
     */
    @Query("SELECT * FROM recipes")
    List<RecipeEntity> getAllRecipesList();
    
    /**
     * Получить рецепт по ID
     * @param id идентификатор рецепта
     * @return рецепт
     */
    @Query("SELECT * FROM recipes WHERE id = :id")
    RecipeEntity getRecipeById(int id);
    
    /**
     * Вставить новые рецепты, заменить существующие при конфликте
     * @param recipes список рецептов для вставки
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RecipeEntity> recipes);
    
    /**
     * Вставить один рецепт, заменить существующий при конфликте
     * @param recipe рецепт для вставки
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RecipeEntity recipe);
    
    /**
     * Обновить рецепт
     * @param recipe рецепт для обновления
     */
    @Update
    void update(RecipeEntity recipe);
    
    /**
     * Удалить все рецепты
     */
    @Query("DELETE FROM recipes")
    void deleteAll();
    
    /**
     * Удалить рецепт по ID
     * @param id идентификатор рецепта для удаления
     */
    @Query("DELETE FROM recipes WHERE id = :id")
    void deleteById(int id);
    
    /**
     * Получить все лайкнутые рецепты
     * @return LiveData список лайкнутых рецептов
     */
    @Query("SELECT * FROM recipes WHERE isLiked = 1")
    LiveData<List<RecipeEntity>> getLikedRecipes();
    
    /**
     * Поиск рецептов по заголовку
     * @param query поисковой запрос
     * @return список найденных рецептов
     */
    @Query("SELECT * FROM recipes WHERE title LIKE '%' || :query || '%'")
    List<RecipeEntity> searchRecipesByTitle(String query);
    
    /**
     * Обновить состояние лайка рецепта
     * @param recipeId идентификатор рецепта
     * @param isLiked новое состояние лайка
     */
    @Query("UPDATE recipes SET isLiked = :isLiked WHERE id = :recipeId")
    void updateLikeStatus(int recipeId, boolean isLiked);
    
    /**
     * Удалить рецепт
     * @param recipe рецепт для удаления
     */
    @Delete
    void delete(RecipeEntity recipe);
} 
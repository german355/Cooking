package com.example.cooking.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LikedRecipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LikedRecipeEntity likedRecipe);

    @Delete
    void delete(LikedRecipeEntity likedRecipe);

    @Query("DELETE FROM liked_recipes WHERE recipeId = :recipeId AND userId = :userId")
    void deleteById(int recipeId, String userId);

    @Query("DELETE FROM liked_recipes WHERE userId = :userId")
    void deleteAllForUser(String userId);

    @Query("SELECT * FROM liked_recipes WHERE userId = :userId")
    LiveData<List<LikedRecipeEntity>> getLikedRecipesForUser(String userId);

    @Query("SELECT EXISTS(SELECT 1 FROM liked_recipes WHERE recipeId = :recipeId AND userId = :userId)")
    boolean isRecipeLiked(int recipeId, String userId);

    @Query("SELECT recipeId FROM liked_recipes WHERE userId = :userId")
    List<Integer> getLikedRecipeIdsSync(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LikedRecipeEntity> likedRecipes);
}
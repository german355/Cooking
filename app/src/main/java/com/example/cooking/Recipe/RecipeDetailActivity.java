package com.example.cooking.Recipe;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.cooking.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.ServerWorker.RecipeDeleter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Активность для отображения подробной информации о рецепте.
 * Показывает полное описание, ингредиенты и инструкцию.
 */
public class RecipeDetailActivity extends AppCompatActivity {
    // Константы для передачи данных между активностями
    public static final String EXTRA_RECIPE_TITLE = "recipe_title";
    public static final String EXTRA_RECIPE_CREATOR = "recipe_creator";
    public static final String EXTRA_RECIPE_INSTRUCTOR = "recipe_instructor";
    public static final String EXTRA_RECIPE_FOOD = "recipe_food";
    private static final String TAG = "RecipeDatail";

    /**
     * Инициализирует активность и заполняет её данными о рецепте
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        // Настраиваем toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Настраиваем обработчик нажатия на кнопку "назад"
        toolbar.setNavigationOnClickListener(v -> finish());

        // Получаем данные о рецепте из Intent
        int recipeId = getIntent().getIntExtra("recipe_id", -1);
        String title = getIntent().getStringExtra("recipe_title");
        String ingredients = getIntent().getStringExtra("recipe_ingredients");
        String instructions = getIntent().getStringExtra("recipe_instructions");
        String created_at = getIntent().getStringExtra("Created_at");
        String userId = getIntent().getStringExtra("userId");

        // Настраиваем заголовок Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        // Находим и настраиваем FAB
        FloatingActionButton fab = findViewById(R.id.delete_button);
        MySharedPreferences user = new MySharedPreferences(this);
        
        // Добавляем логирование для отладки
        String currentUserId = user.getString("userId", "99");
        Log.d("RecipeDetail","Recipe userId: " + userId);
        Log.d("RecipeDetail","Recipe currentId: " + currentUserId);
        
        // Проверяем, принадлежит ли рецепт текущему пользователю
        if (userId != null && userId.equals(currentUserId)) {
            fab.setVisibility(View.VISIBLE);
            Log.d("RecipeDetail","Fab иден");
        } else {
            fab.setVisibility(View.GONE);
            Log.d("RecipeDetail","Fab спрятан");
        }
        
        // Настраиваем обработчик нажатия на кнопку удаления
        fab.setOnClickListener(view -> {
            RecipeDeleter deleter = new RecipeDeleter();
            deleter.deleteRecipe(recipeId, userId, new RecipeDeleter.DeleteRecipeCallback() {
                @Override
                public void onDeleteSuccess() {

                    Log.d("DeleteRecipe", "Рецепт удалён");
                    finish();
                }

                @Override
                public void onDeleteFailure(String error) {
                    // Обработка ошибки удаления рецепта
                    Log.e("DeleteRecipe", "Ошибка удаления рецепта:  " + error);
                }
            });
        });

        // Находим TextView для названия, ингредиентов и инструкций
        TextView titleTextView = findViewById(R.id.recipe_title);
        TextView ingredientsTextView = findViewById(R.id.recipe_ingredients);
        TextView instructionsTextView = findViewById(R.id.recipe_instructions);

        // Устанавливаем текст
        titleTextView.setText(title);
        ingredientsTextView.setText(ingredients);
        instructionsTextView.setText(instructions);

    }



}
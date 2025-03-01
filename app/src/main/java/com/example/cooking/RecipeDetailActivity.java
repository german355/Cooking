package com.example.cooking;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

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

    /**
     * Инициализирует активность и заполняет её данными о рецепте
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        String title = getIntent().getStringExtra(EXTRA_RECIPE_TITLE);
        String creator = getIntent().getStringExtra(EXTRA_RECIPE_CREATOR);
        String instructor = getIntent().getStringExtra(EXTRA_RECIPE_INSTRUCTOR);
        String food = getIntent().getStringExtra(EXTRA_RECIPE_FOOD);

        TextView titleTextView = findViewById(R.id.detail_title);
        TextView creatorTextView = findViewById(R.id.detail_creator);
        TextView instructorTextView = findViewById(R.id.detail_instructor);
        TextView foodTextView = findViewById(R.id.detail_food);
        ImageView imageView = findViewById(R.id.detail_image);

        titleTextView.setText(title);
        creatorTextView.setText("Автор: " + (creator != null ? creator : "Не указан"));
        instructorTextView.setText("Инструкция: " + (instructor != null ? instructor : "Не указана"));
        foodTextView.setText("Ингредиенты: " + (food != null ? food : "Не указаны"));
        imageView.setImageResource(R.drawable.ic_food_placeholder);
    }
} 
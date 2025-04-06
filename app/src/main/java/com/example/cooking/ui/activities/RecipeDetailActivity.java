package com.example.cooking.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.network.services.RecipeDeleter;
import com.example.cooking.ui.fragments.FavoritesFragment;
import com.example.cooking.ui.viewmodels.RecipeDetailViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;

import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.util.concurrent.TimeUnit;

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
    public static final String EXTRA_RECIPE_PHOTO_URL = "photo_url";
    private static final String TAG = "RecipeDetailActivity";
    private static final int EDIT_RECIPE_REQUEST = 1001;
    
    // UI-компоненты
    private FloatingActionButton fabLike;
    private TextView titleTextView;
    private TextView ingredientsTextView;
    private TextView instructionsTextView;
    private ShapeableImageView recipeImageView;
    private TextView createdAtTextView;
    
    // Данные рецепта
    private int recipeId;
    private String userId;
    
    // ViewModel
    private RecipeDetailViewModel viewModel;
    
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
        recipeId = getIntent().getIntExtra("recipe_id", -1);
        String title = getIntent().getStringExtra("recipe_title");
        String ingredients = getIntent().getStringExtra("recipe_ingredients");
        String instructions = getIntent().getStringExtra("recipe_instructions");
        String createdAt = getIntent().getStringExtra("Created_at");
        userId = getIntent().getStringExtra("userId");
        String photoUrl = getIntent().getStringExtra("photo_url");
        boolean isLiked = getIntent().getBooleanExtra("isLiked", false);

        // Проверяем, что ID рецепта валидный
        if (recipeId == -1) {
            Log.e(TAG, "onCreate: Неверный ID рецепта");
            Toast.makeText(this, "Ошибка загрузки рецепта", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Инициализируем UI-компоненты
        titleTextView = findViewById(R.id.recipe_title);
        ingredientsTextView = findViewById(R.id.recipe_ingredients);
        instructionsTextView = findViewById(R.id.recipe_instructions);
        recipeImageView = findViewById(R.id.recipe_image);
        createdAtTextView = findViewById(R.id.recipe_date);
        createdAtTextView.setText("Создано: " + createdAt);
        fabLike = findViewById(R.id.like_button);

        // Настраиваем заголовок Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
        
        // Инициализируем и настраиваем ViewModel
        viewModel = new ViewModelProvider(this).get(RecipeDetailViewModel.class);
        viewModel.initWithRecipe(recipeId, title, ingredients, instructions, createdAt, userId, photoUrl, isLiked);
        
        // Настраиваем наблюдателей LiveData
        setupObservers();
        
        // Настраиваем обработчики событий
        setupEventListeners();
    }
    
    /**
     * Настраивает обработчики событий
     */
    private void setupEventListeners() {
        // Настраиваем клик по кнопке "лайк"
        fabLike.setOnClickListener(v -> viewModel.toggleLike());
    }
    
    /**
     * Настраивает наблюдение за данными из ViewModel
     */
    private void setupObservers() {
        // Наблюдаем за данными рецепта
        viewModel.getRecipe().observe(this, recipe -> {
            updateUI(
                recipe.getTitle(),
                recipe.getIngredients(),
                recipe.getInstructions(),
                recipe.getCreated_at(),
                recipe.getPhoto_url(),
                recipe.isLiked()
            );
        });
        
        // Наблюдаем за статусом лайка
        viewModel.getIsLiked().observe(this, isLiked -> updateLikeButtonState(isLiked));
        
        // Наблюдаем за статусом удаления
        viewModel.getDeleteSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Рецепт удалён", Toast.LENGTH_SHORT).show();
                
                // Устанавливаем результат RESULT_OK для обновления списка рецептов
                Intent resultIntent = new Intent();
                resultIntent.putExtra("recipe_deleted", true);
                setResult(RESULT_OK, resultIntent);
                
                finish();
            }
        });
        
        // Наблюдаем за статусом загрузки при удалении
        viewModel.getIsDeleting().observe(this, isDeleting -> {
            if (isDeleting) {
                // Можно показать прогресс, если нужно
            }
        });
        
        // Наблюдаем за сообщениями об ошибках
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                // Проверяем, содержит ли сообщение информацию о недостатке прав
                if (error.contains("нет прав на удаление")) {
                    // Показываем диалоговое окно с объяснением
                    new AlertDialog.Builder(this)
                        .setTitle("Недостаточно прав")
                        .setMessage(error)
                        .setPositiveButton("Понятно", (dialog, which) -> dialog.dismiss())
                        .setCancelable(true)
                        .show();
                } else {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        // Наблюдаем за наличием прав на редактирование
        viewModel.getHasEditPermission().observe(this, hasPermission -> {
            invalidateOptionsMenu(); // Перерисуем меню после изменения прав
        });
    }
    
    /**
     * Обновляет UI элементы с данными рецепта
     */
    private void updateUI(String title, String ingredients, String instructions, String createdAt, 
                         String photoUrl, boolean isLiked) {
        // Обновляем текстовые поля
        titleTextView.setText(title);
        ingredientsTextView.setText(ingredients);
        instructionsTextView.setText(instructions);
        createdAtTextView.setText("Создано: " + createdAt);
        
        // Настраиваем заголовок Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
        
        // Обновляем статус лайка
        updateLikeButtonState(isLiked);
        
        // Загружаем изображение
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.white_card_background)
                .error(R.drawable.white_card_background)
                .centerCrop()
                .into(recipeImageView);
        } else {
            recipeImageView.setImageResource(R.drawable.white_card_background);
        }
    }
    
    /**
     * Обновляет иконку кнопки лайка в зависимости от состояния
     */
    private void updateLikeButtonState(boolean isLiked) {
        if (isLiked) {
            fabLike.setImageResource(R.drawable.ic_favorite);
        } else {
            fabLike.setImageResource(R.drawable.ic_favorite_border);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipe_detail, menu);
        
        // Показываем кнопки редактирования/удаления только если пользователь имеет права
        Boolean hasEditPermission = viewModel.getHasEditPermission().getValue();
        menu.findItem(R.id.action_edit).setVisible(hasEditPermission != null && hasEditPermission);
        menu.findItem(R.id.action_delete).setVisible(hasEditPermission != null && hasEditPermission);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Recipe recipe = viewModel.getRecipe().getValue();
        if (recipe == null) {
            return super.onOptionsItemSelected(item);
        }
        
        int itemId = item.getItemId();
        
        if (itemId == R.id.action_share) {
            shareRecipe(recipe);
            return true;
        } else if (itemId == R.id.action_edit) {
            editRecipe(recipe);
            return true;
        } else if (itemId == R.id.action_delete) {
            showDeleteConfirmationDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Показывает диалог подтверждения удаления рецепта
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление рецепта")
                .setMessage("Вы уверены, что хотите удалить этот рецепт? Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    dialog.dismiss();
                    
                    // Получаем текущий рецепт перед удалением
                    Recipe currentRecipe = viewModel.getRecipe().getValue();
                    
                    // Настраиваем наблюдателя для результата удаления
                    viewModel.getDeleteSuccess().observe(this, success -> {
                        if (success) {
                            // Если удаление успешно, устанавливаем результат для обновления родительского экрана
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("recipe_deleted", true);
                            resultIntent.putExtra("recipe_id", currentRecipe != null ? currentRecipe.getId() : -1);
                            setResult(RESULT_OK, resultIntent);
                            
                            // Показываем уведомление и закрываем активность
                            Toast.makeText(this, "Рецепт успешно удален", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                    
                    // Запускаем процесс удаления
                    viewModel.deleteRecipe();
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .show();
    }
    
    /**
     * Делится рецептом через другие приложения
     */
    private void shareRecipe(Recipe recipe) {
        String shareText = recipe.getTitle() + "\n\n" +
                "Ингредиенты:\n" + recipe.getIngredients() + "\n\n" +
                "Инструкции:\n" + recipe.getInstructions();
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Рецепт: " + recipe.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        startActivity(Intent.createChooser(shareIntent, "Поделиться рецептом"));
    }
    
    /**
     * Открывает активность редактирования рецепта
     */
    private void editRecipe(Recipe recipe) {
        Intent intent = new Intent(this, EditRecipeActivity.class);
        intent.putExtra("recipe_id", recipe.getId());
        intent.putExtra("recipe_title", recipe.getTitle());
        intent.putExtra("recipe_ingredients", recipe.getIngredients());
        intent.putExtra("recipe_instructions", recipe.getInstructions());
        intent.putExtra("photo_url", recipe.getPhoto_url());
        
        startActivityForResult(intent, EDIT_RECIPE_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == EDIT_RECIPE_REQUEST && resultCode == RESULT_OK && data != null) {
            // Обновляем данные рецепта после редактирования
            String newTitle = data.getStringExtra("recipe_title");
            String newIngredients = data.getStringExtra("recipe_ingredients");
            String newInstructions = data.getStringExtra("recipe_instructions");
            String photoUrl = data.getStringExtra("photo_url");
            
            // Обновляем данные в ViewModel
            viewModel.updateRecipeData(newTitle, newIngredients, newInstructions, photoUrl);
            
            // Показываем уведомление об успешном обновлении
            Toast.makeText(this, "Рецепт обновлен", Toast.LENGTH_SHORT).show();
        }
    }
}
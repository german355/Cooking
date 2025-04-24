package com.example.cooking.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Step;
import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.adapters.StepAdapter;
import com.example.cooking.ui.adapters.IngredientViewAdapter;
import com.example.cooking.ui.viewmodels.RecipeDetailViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    public static final String EXTRA_SELECTED_RECIPE = "SELECTED_RECIPE"; // <-- Ключ для Parcelable
    private static final String TAG = "RecipeDetailActivity";
    private static final int EDIT_RECIPE_REQUEST = 1001;
    
    // UI-компоненты
    private FloatingActionButton fabLike;
    private TextView titleTextView;
    private ShapeableImageView recipeImageView;
    private TextView createdAtTextView;
    private Button decreasePortionButton;
    private Button increasePortionButton;
    private TextView portionCountTextView;
    private Button toListButton;
    private Button toCartButton;
    private RecyclerView stepsRecyclerView;
    private StepAdapter stepAdapter;
    private RecyclerView ingredientsRecyclerView;
    private IngredientViewAdapter ingredientAdapter;
    
    // Данные рецепта
    private Recipe currentRecipe; // Будем хранить весь объект
    private int recipeId;
    private String userId;
    private int currentPortionCount = 1;
    private List<Step> steps = new ArrayList<>();
    private List<Ingredient> ingredients = new ArrayList<>();
    
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
        currentRecipe = getIntent().getParcelableExtra(EXTRA_SELECTED_RECIPE); // Используем константу

        // Проверяем, что данные получены
        if (currentRecipe == null) {
            Log.e(TAG, "Объект Recipe не найден в Intent extras. Ключ: " + EXTRA_SELECTED_RECIPE);
            Toast.makeText(this, "Ошибка: Не удалось загрузить данные рецепта.", Toast.LENGTH_LONG).show();
            finish(); // Закрываем активность, если данных нет
            return;
        }

        // Извлекаем данные из объекта Recipe
        recipeId = currentRecipe.getId();
        userId = currentRecipe.getUserId();
        // Списки берем напрямую из объекта
        this.ingredients = currentRecipe.getIngredients() != null ? new ArrayList<>(currentRecipe.getIngredients()) : new ArrayList<>();
        this.steps = currentRecipe.getSteps() != null ? new ArrayList<>(currentRecipe.getSteps()) : new ArrayList<>();

        // Логируем полученные данные для отладки
        Log.d(TAG, "Получен рецепт: ID = " + recipeId + ", Название = " + currentRecipe.getTitle());
        Log.d(TAG, "Кол-во ингредиентов: " + this.ingredients.size());
        Log.d(TAG, "Кол-во шагов: " + this.steps.size());
        Log.d(TAG, "Дата создания: " + currentRecipe.getCreated_at());
        Log.d(TAG, "ID пользователя: " + userId);
        Log.d(TAG, "URL фото: " + currentRecipe.getPhoto_url());
        Log.d(TAG, "Лайкнут: " + currentRecipe.isLiked());
        
        // Инициализируем UI-компоненты
        titleTextView = findViewById(R.id.recipe_title);
        recipeImageView = findViewById(R.id.recipe_image);
        createdAtTextView = findViewById(R.id.recipe_date);
        fabLike = findViewById(R.id.like_button);
        decreasePortionButton = findViewById(R.id.decrease_portion);
        increasePortionButton = findViewById(R.id.increase_portion);
        portionCountTextView = findViewById(R.id.portion_count);
        stepsRecyclerView = findViewById(R.id.steps_recyclerview);
        ingredientsRecyclerView = findViewById(R.id.ingredients_recyclerview);
        
        // Настраиваем RecyclerView для шагов
        setupStepsRecyclerView();
        
        // Настраиваем RecyclerView для ингредиентов
        setupIngredientsRecyclerView();

        // Инициализируем и настраиваем ViewModel
        viewModel = new ViewModelProvider(this).get(RecipeDetailViewModel.class);
        // Вместо передачи всех полей, передаем только ID
        // ViewModel должен сам загрузить Recipe из репозитория по ID
        viewModel.loadRecipe(recipeId); 
        
        // Настраиваем наблюдателей LiveData
        setupObservers();
        
        // Настраиваем обработчики событий
        setupEventListeners();

        // Первичное отображение данных из Intent (пока ViewModel загружает)
        Log.d(TAG, "onCreate: Установка начального UI...");
        if (titleTextView != null && currentRecipe != null) {
            Log.d(TAG, "onCreate: Установка заголовка: " + currentRecipe.getTitle());
            titleTextView.setText(currentRecipe.getTitle());
        } else {
            Log.e(TAG, "onCreate: titleTextView is null или currentRecipe is null перед установкой заголовка");
        }
        createdAtTextView.setText(String.format(Locale.getDefault(), "Создано: %s", currentRecipe.getCreated_at()));
        updateLikeButton(currentRecipe.isLiked());
        if (recipeImageView != null && currentRecipe != null && currentRecipe.getPhoto_url() != null && !currentRecipe.getPhoto_url().isEmpty()) {
            Log.d(TAG, "onCreate: Загрузка изображения Glide: " + currentRecipe.getPhoto_url());
            Glide.with(this)
                 .load(currentRecipe.getPhoto_url())
                 .placeholder(R.drawable.placeholder_image)
                 .error(R.drawable.error_image)
                 .into(recipeImageView);
        } else {
            Log.w(TAG, "onCreate: Не удалось загрузить изображение Glide (ImageView null, Recipe null, URL null/пустой)");
             if(recipeImageView != null) recipeImageView.setImageResource(R.drawable.default_recipe_image); // Установим дефолтное изображение
        }
        updatePortionCount();
    }
    
    /**
     * Настраивает RecyclerView для шагов рецепта
     */
    private void setupStepsRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        stepsRecyclerView.setLayoutManager(layoutManager);
        // Создаем адаптер
        stepAdapter = new StepAdapter(this); 
        stepsRecyclerView.setAdapter(stepAdapter);
        // Сразу передаем шаги, полученные из Intent
        stepAdapter.submitList(this.steps); 
        Log.d(TAG, "setupStepsRecyclerView: Передано шагов в адаптер: " + this.steps.size()); // Добавим лог
    }
    
    /**
     * Настраивает RecyclerView для ингредиентов
     */
    private void setupIngredientsRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        ingredientsRecyclerView.setLayoutManager(layoutManager);
        // Инициализируем адаптер списком, полученным из Parcelable
        ingredientAdapter = new IngredientViewAdapter(this, this.ingredients);
        ingredientsRecyclerView.setAdapter(ingredientAdapter);
    }
    
    /**
     * Настраивает обработчики событий
     */
    private void setupEventListeners() {
        // Настраиваем клик по кнопке "лайк"
        fabLike.setOnClickListener(v -> viewModel.toggleLike());
        
        // Настраиваем кнопки изменения порции
        decreasePortionButton.setOnClickListener(v -> {
            if (currentPortionCount > 1) {
                currentPortionCount--;
                updatePortionCount();
            }
        });
        
        increasePortionButton.setOnClickListener(v -> {
            currentPortionCount++;
            updatePortionCount();
        });
            }
    
    /**
     * Обновляет отображение количества порций
     */
    private void updatePortionCount() {
        portionCountTextView.setText(String.valueOf(currentPortionCount));
        // Убедимся, что адаптер не null перед вызовом
        if (ingredientAdapter != null) { 
            ingredientAdapter.updatePortionCount(currentPortionCount);
        }
    }
    
    /**
     * Настраивает наблюдение за данными из ViewModel
     */
    private void setupObservers() {
        // Наблюдаем за данными рецепта из ViewModel
        viewModel.getRecipe().observe(this, recipeFromVm -> {
            if (recipeFromVm != null) {
                Log.d(TAG, "Получен обновленный рецепт из ViewModel. Шагов: " + (recipeFromVm.getSteps() != null ? recipeFromVm.getSteps().size() : "null"));
                currentRecipe = recipeFromVm; // Обновляем текущий рецепт
                updateUI(currentRecipe); // Обновляем весь UI свежими данными
            } else {
                Log.w(TAG, "ViewModel вернул null Recipe объект.");
                // Можно показать сообщение об ошибке или использовать данные из Intent
                // если они были успешно получены ранее
                if (currentRecipe != null) { 
                    updateUI(currentRecipe); // Показываем то, что пришло в Intent
                } else {
                     Toast.makeText(this, "Не удалось загрузить детали рецепта.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Наблюдаем за состоянием лайка
        viewModel.isLiked().observe(this, this::updateLikeButton);
        
        // Наблюдаем за сообщениями об ошибках
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
        
        // Наблюдаем за статусом удаления
        viewModel.isDeleteSuccess().observe(this, isSuccess -> {
            if (isSuccess) {
                Toast.makeText(this, "Рецепт успешно удален", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
        
        // Наблюдаем за разрешением на редактирование
        viewModel.hasEditPermission().observe(this, hasPermission -> {
            invalidateOptionsMenu(); // Обновляем меню
        });
    }
    
    /**
     * Обновляет UI элементы данными из рецепта (полученного из ViewModel)
     */
    private void updateUI(Recipe recipe) {
        if (recipe == null) {
            Log.w(TAG, "updateUI вызван с null Recipe объектом.");
            return;
        }
        Log.d(TAG, "updateUI: Обновление UI для рецепта: " + recipe.getTitle());
        
        if (titleTextView != null) {
             Log.d(TAG, "updateUI: Установка заголовка: " + recipe.getTitle());
            titleTextView.setText(recipe.getTitle());
        } else {
             Log.e(TAG, "updateUI: titleTextView is null");
        }
        createdAtTextView.setText(String.format(Locale.getDefault(), "Создано: %s", recipe.getCreated_at()));
        updateLikeButton(recipe.isLiked());

        // Обновляем адаптеры новыми данными
        this.ingredients = recipe.getIngredients() != null ? new ArrayList<>(recipe.getIngredients()) : new ArrayList<>();
        if (ingredientAdapter != null) {
            ingredientAdapter.updateIngredients(this.ingredients);
            ingredientAdapter.updatePortionCount(currentPortionCount); 
        } else {
             Log.e(TAG, "updateUI: ingredientAdapter is null");
        }
        
        this.steps = recipe.getSteps() != null ? new ArrayList<>(recipe.getSteps()) : new ArrayList<>();
        if (stepAdapter != null) {
             stepAdapter.submitList(this.steps);
        } else {
             Log.e(TAG, "updateUI: stepAdapter is null");
        }

        if (recipeImageView != null && recipe.getPhoto_url() != null && !recipe.getPhoto_url().isEmpty()) {
             Log.d(TAG, "updateUI: Загрузка изображения Glide: " + recipe.getPhoto_url());
             Glide.with(this)
                  .load(recipe.getPhoto_url())
                  .placeholder(R.drawable.placeholder_image)
                  .error(R.drawable.error_image)
                  .into(recipeImageView);
        } else {
             Log.w(TAG, "updateUI: Не удалось загрузить изображение Glide (ImageView null, URL null/пустой)");
             if(recipeImageView != null) recipeImageView.setImageResource(R.drawable.default_recipe_image); // Установим дефолтное изображение
        }
        invalidateOptionsMenu(); 
    }
    
    /**
     * Обновляет состояние кнопки лайка
     */
    private void updateLikeButton(boolean isLiked) {
        if (isLiked) {
            fabLike.setImageResource(R.drawable.ic_favorite); // Закрашенный лайк
        } else {
            fabLike.setImageResource(R.drawable.ic_favorite_border); // Пустой лайк
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipe_detail, menu);
        MenuItem editItem = menu.findItem(R.id.action_edit);
        MenuItem deleteItem = menu.findItem(R.id.action_delete);

        // Показываем или скрываем кнопки в зависимости от разрешений
        Boolean hasPermission = viewModel.hasEditPermission().getValue();
        if (editItem != null) editItem.setVisible(hasPermission != null && hasPermission);
        if (deleteItem != null) deleteItem.setVisible(hasPermission != null && hasPermission);

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
                .setTitle("Удалить рецепт?")
                .setMessage("Вы уверены, что хотите удалить этот рецепт? Это действие необратимо.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    Recipe recipe = viewModel.getRecipe().getValue();
                    if (recipe != null) {
                        viewModel.deleteRecipe(recipe.getId());
                    } else {
                        Toast.makeText(this, "Ошибка: Не удалось получить ID рецепта для удаления.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    
    /**
     * Делится рецептом через другие приложения
     */
    private void shareRecipe(Recipe recipe) {
        if (recipe == null) return;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareBody = "Посмотри рецепт: " + recipe.getTitle() + "\nПодробнее: [ссылка на приложение или веб-версию]"; // TODO: Добавить ссылку
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Рецепт: " + recipe.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);

        startActivity(Intent.createChooser(shareIntent, "Поделиться рецептом через"));
    }
    
    /**
     * Открывает активность редактирования рецепта
     */
    private void editRecipe(Recipe recipe) {
        if (recipe == null) return;
        Intent intent = new Intent(this, EditRecipeActivity.class);
        intent.putExtra(EditRecipeActivity.EXTRA_EDIT_RECIPE, recipe);
        startActivityForResult(intent, EDIT_RECIPE_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == EDIT_RECIPE_REQUEST && resultCode == RESULT_OK) {
            // Рецепт был изменен, нужно перезагрузить данные
            Toast.makeText(this, "Рецепт обновлен", Toast.LENGTH_SHORT).show();
            if (viewModel != null) {
                viewModel.loadRecipe(recipeId); // Заставляем ViewModel перезагрузить данные
            }
        }
    }
}
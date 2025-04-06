package com.example.cooking.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.cooking.R;
import com.example.cooking.ui.viewmodels.AddRecipeViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Активность для добавления нового рецепта
 * Использует AddRecipeViewModel для управления бизнес-логикой
 */
public class AddRecipeActivity extends AppCompatActivity {
    private static final String TAG = "AddRecipeActivity";
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_PICK_IMAGE = 1002;
    
    // UI компоненты
    private TextInputEditText titleEditText;
    private TextInputEditText ingredientsEditText;
    private TextInputEditText instructionsEditText;
    private Button saveButton;
    private ProgressBar progressBar;
    private ImageView recipeImageView;
    private TextInputLayout titleInputLayout;
    private TextInputLayout ingredientsInputLayout;
    private TextInputLayout instructionsInputLayout;
    private TextView textImageView;
    
    // ViewModel
    private AddRecipeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Activity создается");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        Log.d(TAG, "onCreate: Layout установлен - activity_add_recipe");

        // Настраиваем toolbar
        setupToolbar();

        // Инициализируем ViewModel
        viewModel = new ViewModelProvider(this).get(AddRecipeViewModel.class);
        
        // Инициализируем UI компоненты
        initViews();
        
        // Настраиваем наблюдателей LiveData
        setupObservers();
        
        // Настраиваем обработчики событий
        setupEventListeners();
    }
    
    /**
     * Настраивает toolbar
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Добавить рецепт");
        Log.d(TAG, "setupToolbar: Toolbar настроен, заголовок установлен");
    }
    
    /**
     * Инициализирует UI компоненты
     */
    private void initViews() {
        titleEditText = findViewById(R.id.recipe_title);
        ingredientsEditText = findViewById(R.id.recipe_ingredients);
        instructionsEditText = findViewById(R.id.recipe_instructions);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);
        recipeImageView = findViewById(R.id.recipe_image);
        titleInputLayout = findViewById(R.id.recipe_title_layout);
        ingredientsInputLayout = findViewById(R.id.recipe_ingredients_layout);
        instructionsInputLayout = findViewById(R.id.recipe_instructions_layout);
        textImageView = findViewById(R.id.textImage);
        
        recipeImageView.setImageResource(R.drawable.select_recipe_view);
        Log.d(TAG, "initViews: Все UI элементы инициализированы");
    }
    
    /**
     * Настраивает наблюдателей LiveData из ViewModel
     */
    private void setupObservers() {
        // Наблюдаем за статусом загрузки
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            saveButton.setEnabled(!isLoading);
            if (isLoading) {
                saveButton.setText("Сохранение...");
            } else {
                saveButton.setText("Сохранить");
            }
        });
        
        // Наблюдаем за сообщениями об ошибках
        viewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
        
        // Наблюдаем за успешным сохранением
        viewModel.getSaveSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Рецепт успешно сохранен", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }
        });
        
        // Наблюдаем за ошибками валидации
        viewModel.getTitleError().observe(this, error -> {
            if (error != null) {
                titleInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
                titleInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
                titleInputLayout.setError(error);
            } else {
                titleInputLayout.setError(null);
            }
        });
        
        viewModel.getIngredientsError().observe(this, error -> {
            if (error != null) {
                ingredientsInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
                ingredientsInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
                ingredientsInputLayout.setError(error);
            } else {
                ingredientsInputLayout.setError(null);
            }
        });
        
        viewModel.getInstructionsError().observe(this, error -> {
            if (error != null) {
                instructionsInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
                instructionsInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
                instructionsInputLayout.setError(error);
            } else {
                instructionsInputLayout.setError(null);
            }
        });
        
        viewModel.getImageError().observe(this, error -> {
            if (error != null) {
                textImageView.setTextColor(Color.RED);
                textImageView.setText(error);
            } else {
                textImageView.setTextColor(Color.BLACK);
                textImageView.setText("Выберите изображение");
            }
        });
    }
    
    /**
     * Настраивает обработчики событий для UI компонентов
     */
    private void setupEventListeners() {
        // Обработчик изменения текста для названия рецепта
        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setTitle(s.toString());
            }
        });
        
        // Обработчик изменения текста для ингредиентов
        ingredientsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setIngredients(s.toString());
            }
        });
        
        // Обработчик изменения текста для инструкций
        instructionsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setInstructions(s.toString());
            }
        });
        
        // Обработчик клика по изображению
        recipeImageView.setOnClickListener(view -> {
            Log.d(TAG, "onClick: Нажата кнопка выбора изображения");
            checkStoragePermissionAndPickImage();
        });
        
        // Обработчик клика по кнопке "Сохранить"
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "onClick: Нажата кнопка 'Сохранить'");
            viewModel.saveRecipe();
        });
    }
    
    /**
     * Проверяет разрешение на доступ к хранилищу и открывает галерею
     */
    private void checkStoragePermissionAndPickImage() {
        // Определяем, какое разрешение запрашивать в зависимости от версии Android
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+ (API 33+)
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // Запрашиваем разрешение
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_STORAGE_PERMISSION);
            Log.d(TAG, "checkStoragePermissionAndPickImage: Запрошено разрешение: " + permission);
            // Показываем пользователю, почему нужно разрешение
            Toast.makeText(this, "Для выбора фото необходимо предоставить разрешение на доступ к галерее", 
                    Toast.LENGTH_LONG).show();
        } else {
            // Если разрешение уже есть, открываем галерею
            openGallery();
        }
    }
    
    /**
     * Открывает галерею для выбора изображения
     */
    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
            Log.d(TAG, "openGallery: Галерея открыта");
        } catch (Exception e) {
            Log.e(TAG, "openGallery: Ошибка при открытии галереи", e);
            Toast.makeText(this, "Не удалось открыть галерею: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Разрешение получено, открываем галерею");
                openGallery();
            } else {
                Log.d(TAG, "onRequestPermissionsResult: Разрешение отклонено");
                Toast.makeText(this, "Для выбора изображения необходим доступ к хранилищу", 
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                Log.d(TAG, "onActivityResult: Изображение выбрано: " + selectedImageUri);
                
                // Отображаем выбранное изображение в UI
                recipeImageView.setImageURI(selectedImageUri);
                textImageView.setText("Изображение выбрано");
                
                // Обрабатываем изображение через ViewModel
                viewModel.processSelectedImage(selectedImageUri);
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Спрашиваем пользователя о выходе, если есть введенные данные
            if (!titleEditText.getText().toString().isEmpty() || 
                !ingredientsEditText.getText().toString().isEmpty() || 
                !instructionsEditText.getText().toString().isEmpty()) {
                    
                showExitConfirmDialog();
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Показывает диалог подтверждения выхода
     */
    private void showExitConfirmDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Отменить создание рецепта?")
            .setMessage("Введенные данные будут потеряны")
            .setPositiveButton("Да", (dialog, which) -> {
                dialog.dismiss();
                finish();
            })
            .setNegativeButton("Нет", (dialog, which) -> dialog.dismiss())
            .show();
    }
    
    @Override
    public void onBackPressed() {
        // Спрашиваем пользователя о выходе, если есть введенные данные
        if (!titleEditText.getText().toString().isEmpty() || 
            !ingredientsEditText.getText().toString().isEmpty() || 
            !instructionsEditText.getText().toString().isEmpty()) {
                
            showExitConfirmDialog();
        } else {
            super.onBackPressed();
        }
    }
}
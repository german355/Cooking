package com.example.cooking.ui.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.network.services.RecipeManager;
import com.example.cooking.ui.viewmodels.EditRecipeViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class EditRecipeActivity extends AppCompatActivity {
    private static final String TAG = "EditRecipeActivity";
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_PICK_IMAGE = 1002;
    
    private TextInputEditText titleEditText;
    private TextInputEditText ingredientsEditText;
    private TextInputEditText instructionsEditText;
    private Button saveButton;
    private ProgressBar progressBar;
    private ImageView recipeImageView;
    private TextView textImage;
    
    private EditRecipeViewModel viewModel;
    private int recipeId;
    private String photoUrl;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Activity создается");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe); // Используем тот же layout, что и для добавления
        
        // Инициализируем ViewModel
        viewModel = new ViewModelProvider(this).get(EditRecipeViewModel.class);
        
        // Настраиваем toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Редактировать рецепт");
        
        // Инициализируем представления
        titleEditText = findViewById(R.id.recipe_title);
        ingredientsEditText = findViewById(R.id.recipe_ingredients);
        instructionsEditText = findViewById(R.id.recipe_instructions);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);
        recipeImageView = findViewById(R.id.recipe_image);
        textImage = findViewById(R.id.textImage);
        
        // Получаем данные из Intent
        Intent intent = getIntent();
        recipeId = intent.getIntExtra("recipe_id", -1);
        String title = intent.getStringExtra("recipe_title");
        String ingredients = intent.getStringExtra("recipe_ingredients");
        String instructions = intent.getStringExtra("recipe_instructions");
        photoUrl = intent.getStringExtra("photo_url");
        
        // Проверяем, что получили ID рецепта
        if (recipeId == -1) {
            Toast.makeText(this, "Ошибка: ID рецепта не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Устанавливаем данные рецепта в ViewModel
        viewModel.setRecipeData(recipeId, title, ingredients, instructions, photoUrl);
        
        // Настраиваем наблюдателей LiveData
        setupObservers();
        
        // Настраиваем обработчики событий
        setupEventListeners();
        
        // Настраиваем кнопку сохранения
        saveButton.setText("Сохранить изменения"); // Меняем текст кнопки для редактирования
    }
    
    /**
     * Настраивает наблюдателей LiveData для обновления UI
     */
    private void setupObservers() {
        // Наблюдатель для заголовка
        viewModel.getTitle().observe(this, title -> {
            if (!titleEditText.getText().toString().equals(title)) {
                titleEditText.setText(title);
            }
        });
        
        // Наблюдатель для ингредиентов
        viewModel.getIngredients().observe(this, ingredients -> {
            if (!ingredientsEditText.getText().toString().equals(ingredients)) {
                ingredientsEditText.setText(ingredients);
            }
        });
        
        // Наблюдатель для инструкций
        viewModel.getInstructions().observe(this, instructions -> {
            if (!instructionsEditText.getText().toString().equals(instructions)) {
                instructionsEditText.setText(instructions);
            }
        });
        
        // Наблюдатель для URL фото
        viewModel.getPhotoUrl().observe(this, url -> {
            photoUrl = url;
            if (url != null && !url.isEmpty()) {
                // Загружаем изображение в ImageView
                Glide.with(this)
                    .load(url)
                    .into(recipeImageView);
                textImage.setText("Изображение загружено");
            }
        });
        
        // Наблюдатель для байтов изображения
        viewModel.getImageBytes().observe(this, bytes -> {
            if (bytes != null && bytes.length > 0) {
                // Отображаем изображение из байтов
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                recipeImageView.setImageBitmap(bitmap);
                textImage.setText("Изображение загружено");
            }
        });
        
        // Наблюдатель для состояния загрузки
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            saveButton.setEnabled(!isLoading);
            if (isLoading) {
                saveButton.setText("Сохранение...");
            } else {
                saveButton.setText("Сохранить изменения");
            }
        });
        
        // Наблюдатель для сообщений об ошибках
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                // Проверяем, содержит ли сообщение об ошибке информацию о недостатке прав (ошибка 403)
                if (errorMessage.contains("нет прав на редактирование")) {
                    // Показываем диалоговое окно с объяснением
                    new AlertDialog.Builder(EditRecipeActivity.this)
                        .setTitle("Недостаточно прав")
                        .setMessage(errorMessage)
                        .setPositiveButton("Понятно", (dialog, which) -> {
                            dialog.dismiss();
                            // Закрываем активность после закрытия диалога
                            finish();
                        })
                        .setCancelable(false)
                        .show();
                } else {
                    // Для других ошибок показываем стандартный Toast
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        // Наблюдатель для сообщений об успехе
        viewModel.getSuccessMessage().observe(this, successMessage -> {
            if (successMessage != null && !successMessage.isEmpty()) {
                Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                
                // Возвращаем результат в RecipeDetailActivity и закрываем
                Intent resultIntent = new Intent();
                resultIntent.putExtra("recipe_title", viewModel.getTitle().getValue());
                resultIntent.putExtra("recipe_ingredients", viewModel.getIngredients().getValue());
                resultIntent.putExtra("recipe_instructions", viewModel.getInstructions().getValue());
                resultIntent.putExtra("photo_url", photoUrl); // Возвращаем URL изображения
                setResult(RESULT_OK, resultIntent);
                
                finish();
            }
        });
    }
    
    /**
     * Настраивает обработчики событий UI
     */
    private void setupEventListeners() {
        // Обработчик изменения заголовка
        titleEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                viewModel.setTitle(titleEditText.getText().toString());
            }
        });
        
        // Обработчик изменения ингредиентов
        ingredientsEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                viewModel.setIngredients(ingredientsEditText.getText().toString());
            }
        });
        
        // Обработчик изменения инструкций
        instructionsEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                viewModel.setInstructions(instructionsEditText.getText().toString());
            }
        });
        
        // Настраиваем кнопку выбора изображения
        recipeImageView.setOnClickListener(v -> {
            checkStoragePermissionAndPickImage();
        });
        
        // Настраиваем кнопку сохранения
        saveButton.setOnClickListener(v -> {
            // Обновляем данные в ViewModel перед сохранением
            viewModel.setTitle(titleEditText.getText().toString());
            viewModel.setIngredients(ingredientsEditText.getText().toString());
            viewModel.setInstructions(instructionsEditText.getText().toString());
            
            // Запускаем процесс обновления рецепта
            viewModel.updateRecipe();
        });
    }
    
    /**
     * Проверяет разрешение на доступ к хранилищу и открывает галерею
     */
    private void checkStoragePermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                    REQUEST_STORAGE_PERMISSION);
        } else {
            openGallery();
        }
    }
    
    /**
     * Открывает галерею для выбора изображения
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Необходимо разрешение для выбора изображения", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                // Передаем Uri выбранного изображения в ViewModel для обработки
                viewModel.processSelectedImage(selectedImageUri);
                textImage.setText("Изображение выбрано");
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Спрашиваем пользователя, хочет ли он отменить редактирование
            new AlertDialog.Builder(this)
                .setTitle("Отменить редактирование?")
                .setMessage("Все несохраненные изменения будут потеряны.")
                .setPositiveButton("Да", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setNegativeButton("Нет", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 

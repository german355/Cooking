package com.example.cooking.ServerWorker;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.cooking.MySharedPreferences;
import com.example.cooking.R;
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
    private Button selectImageButton;
    private TextView textImage;
    
    private int recipeId;
    private String photoUrl;
    private Uri selectedImageUri;
    private byte[] imageBytes;
    private MySharedPreferences user;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Activity создается");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe); // Используем тот же layout, что и для добавления
        
        // Настраиваем toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Редактировать рецепт");
        
        // Инициализируем SharedPreferences
        user = new MySharedPreferences(this);
        
        // Инициализируем представления
        titleEditText = findViewById(R.id.recipe_title);
        ingredientsEditText = findViewById(R.id.recipe_ingredients);
        instructionsEditText = findViewById(R.id.recipe_instructions);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);
        recipeImageView = findViewById(R.id.recipe_image);
        selectImageButton = findViewById(R.id.btn_select_image);
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
        
        // Заполняем поля данными рецепта
        titleEditText.setText(title);
        ingredientsEditText.setText(ingredients);
        instructionsEditText.setText(instructions);
        
        // Загружаем изображение, если оно есть
        if (photoUrl != null && !photoUrl.isEmpty()) {
            // Загружаем изображение в ImageView
            Glide.with(this)
                .load(photoUrl)
                .into(recipeImageView);
            textImage.setText("Изображение загружено");
            
            // Также загружаем изображение в байты для сохранения при обновлении
            loadImageFromUrl(photoUrl);
        }
        
        // Настраиваем кнопку выбора изображения
        selectImageButton.setOnClickListener(v -> {
            checkStoragePermissionAndPickImage();
        });
        
        // Настраиваем кнопку сохранения
        saveButton.setText("Сохранить изменения"); // Меняем текст кнопки для редактирования
        saveButton.setOnClickListener(v -> {
            String newTitle = titleEditText.getText().toString();
            String newIngredients = ingredientsEditText.getText().toString();
            String newInstructions = instructionsEditText.getText().toString();
            
            if (validateTitle(newTitle) && validateIngredients(newIngredients) && 
                    validateInstructions(newInstructions)) {
                updateRecipe();
            }
        });
    }
    
    /**
     * Обновляет рецепт через RecipeManager
     */
    private void updateRecipe() {
        Log.d(TAG, "updateRecipe: Начало процесса обновления рецепта");
        
        // Проверяем подключение к интернету
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Отсутствует подключение к интернету", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Получаем введенные пользователем данные
        String title = titleEditText.getText().toString().trim();
        String ingredients = ingredientsEditText.getText().toString().trim();
        String instructions = instructionsEditText.getText().toString().trim();
        String userId = user.getString("userId", "99");
        
        // Показываем индикатор загрузки и блокируем кнопку
        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);
        
        // Проверяем статус изображения
        if (imageBytes == null && photoUrl != null && !photoUrl.isEmpty()) {
            // Если изображение не загрузилось из URL, но URL существует
            Toast.makeText(this, "Подождите, изображение еще загружается", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            saveButton.setEnabled(true);
            return;
        }
        
        // Логируем наличие изображения
        if (imageBytes != null) {
            Log.d(TAG, "updateRecipe: Изображение готово к отправке, размер: " + imageBytes.length + " байт");
        } else {
            Log.d(TAG, "updateRecipe: Изображение не будет обновлено (imageBytes = null)");
        }
        
        // Создаем RecipeManager и обновляем рецепт
        RecipeManager recipeManager = new RecipeManager(this);
        recipeManager.saveRecipe(title, ingredients, instructions, userId, recipeId, imageBytes, 
                new RecipeManager.RecipeSaveCallback() {
            @Override
            public void onSuccess(String message) {
                progressBar.setVisibility(View.GONE);
                saveButton.setEnabled(true);
                Toast.makeText(EditRecipeActivity.this, message, Toast.LENGTH_LONG).show();
                
                // Возвращаем результат в RecipeDetailActivity и закрываем
                Intent resultIntent = new Intent();
                resultIntent.putExtra("recipe_title", title);
                resultIntent.putExtra("recipe_ingredients", ingredients);
                resultIntent.putExtra("recipe_instructions", instructions);
                resultIntent.putExtra("photo_url", photoUrl); // Возвращаем URL изображения
                setResult(RESULT_OK, resultIntent);
                
                finish();
            }
            
            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                saveButton.setEnabled(true);
                Toast.makeText(EditRecipeActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * Загружает изображение из URL и преобразует его в массив байтов
     */
    private void loadImageFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "loadImageFromUrl: URL пустой или null");
            return;
        }
        
        // Сначала загружаем изображение в ImageView через Glide для быстрого отображения
        Glide.with(this)
             .load(url)
             .into(recipeImageView);
        
        // Показываем прогресс загрузки
        progressBar.setVisibility(View.VISIBLE);
        textImage.setText("Загрузка изображения...");
        
        // Запускаем загрузку в отдельном потоке
        new Thread(() -> {
            try {
                // Загружаем изображение из URL
                java.net.URL imageUrl = new java.net.URL(url);
                Bitmap bitmap = BitmapFactory.decodeStream(imageUrl.openConnection().getInputStream());
                
                if (bitmap != null) {
                    // Сжимаем изображение до разумного размера
                    Bitmap resizedBitmap = resizeBitmap(bitmap, 800);
                    
                    // Преобразуем в массив байтов
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                    imageBytes = byteArrayOutputStream.toByteArray();
                    
                    // Обновляем UI в основном потоке
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        textImage.setText("Изображение загружено");
                        Log.d(TAG, "Изображение загружено из URL и сохранено в памяти");
                    });
                } else {
                    // Если не удалось загрузить изображение
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        textImage.setText("Не удалось загрузить изображение");
                        Log.e(TAG, "Не удалось загрузить изображение из URL");
                    });
                }
            } catch (Exception e) {
                // В случае ошибки
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    textImage.setText("Ошибка загрузки изображения");
                    Log.e(TAG, "Ошибка при загрузке изображения из URL: " + e.getMessage(), e);
                });
            }
        }).start();
    }
    
    // Проверка разрешения на доступ к хранилищу и выбор изображения
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
            // Показываем пользователю, почему нужно разрешение
            Toast.makeText(this, "Для выбора фото необходимо предоставить разрешение на доступ к галерее", 
                    Toast.LENGTH_LONG).show();
        } else {
            // Если разрешение уже есть, открываем галерею
            openGallery();
        }
    }
    
    // Открытие галереи для выбора изображения
    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } catch (Exception e) {
            Log.e(TAG, "openGallery: Ошибка при открытии галереи", e);
            Toast.makeText(this, "Не удалось открыть галерею: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    // Обработка результата запроса разрешения
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Если разрешение получено, открываем галерею
                openGallery();
            } else {
                Toast.makeText(this, "Для выбора изображения необходим доступ к хранилищу", 
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // Обработка результата выбора изображения
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                selectedImageUri = data.getData();
                // Отображаем выбранное изображение
                recipeImageView.setImageURI(selectedImageUri);
                
                // Преобразуем изображение в массив байтов для отправки
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                
                // Сжимаем изображение до разумного размера
                Bitmap resizedBitmap = resizeBitmap(bitmap, 800);
                
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                imageBytes = byteArrayOutputStream.toByteArray();
                
                if (inputStream != null) {
                    inputStream.close();
                }
                
                textImage.setText("Изображение выбрано");
                textImage.setTextColor(Color.BLACK);
                
                Toast.makeText(this, "Изображение выбрано", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "onActivityResult: Ошибка при обработке изображения", e);
                Toast.makeText(this, "Ошибка при загрузке изображения: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // Метод для изменения размера Bitmap
    private Bitmap resizeBitmap(Bitmap bitmap, int maxSide) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= maxSide && height <= maxSide) {
            return bitmap; // Нет необходимости менять размер
        }
        
        float ratio = (float) width / height;
        int newWidth, newHeight;
        
        if (width > height) {
            newWidth = maxSide;
            newHeight = (int) (maxSide / ratio);
        } else {
            newHeight = maxSide;
            newWidth = (int) (maxSide * ratio);
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Метод для проверки наличия интернет-соединения
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    private boolean validateTitle(String title) {
        if (TextUtils.isEmpty(title) || title.length() < 2) {
            TextInputLayout titleInputLayout = findViewById(R.id.recipe_title_layout);
            titleInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setError("Название рецепта не может быть пустым и короче 2 символов");
            return false;
        } else if (title.length() > 255) {
            TextInputLayout titleInputLayout = findViewById(R.id.recipe_title_layout);
            titleInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setError("Название рецепта не должно превышать 255 символов");
            return false;
        } else {
            TextInputLayout titleInputLayout = findViewById(R.id.recipe_title_layout);
            titleInputLayout.setError(null);
            return true;
        }
    }
    
    private boolean validateIngredients(String ingredients) {
        if (TextUtils.isEmpty(ingredients)) {
            TextInputLayout layout = findViewById(R.id.recipe_ingredients_layout);
            layout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            layout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            layout.setError("Список ингредиентов не может быть пустым");
            return false;
        } else if (ingredients.length() > 2000) {
            TextInputLayout layout = findViewById(R.id.recipe_ingredients_layout);
            layout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            layout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            layout.setError("Список ингредиентов не может содержать более 2000 символов");
            return false;
        } else {
            TextInputLayout layout = findViewById(R.id.recipe_ingredients_layout);
            layout.setError(null);
            return true;
        }
    }
    
    private boolean validateInstructions(String instructions) {
        if (TextUtils.isEmpty(instructions)) {
            TextInputLayout layout = findViewById(R.id.recipe_instructions_layout);
            layout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            layout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            layout.setError("Инструкция не может быть пустой");
            return false;
        } else if (instructions.length() > 2000) {
            TextInputLayout layout = findViewById(R.id.recipe_instructions_layout);
            layout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            layout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            layout.setError("Инструкция не может содержать более 2000 символов");
            return false;
        } else {
            TextInputLayout layout = findViewById(R.id.recipe_instructions_layout);
            layout.setError(null);
            return true;
        }
    }
} 
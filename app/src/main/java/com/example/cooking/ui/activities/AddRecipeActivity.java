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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;

import com.example.cooking.network.services.RecipeManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

import static com.example.cooking.R.drawable.select_recipe_view;


public class AddRecipeActivity extends AppCompatActivity {
    private static final String TAG = "AddRecipeActivity";
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_PICK_IMAGE = 1002;
    
    private TextInputEditText titleEditText;
    private MySharedPreferences user;
    private TextInputEditText ingredientsEditText;
    private TextInputEditText instructionsEditText;
    private Button saveButton;
    private ProgressBar progressBar;
    
    // Добавляем новые переменные для работы с изображениями
    private ImageView recipeImageView;
    private Button selectImageButton;
    private Uri selectedImageUri;
    private byte[] imageBytes;
    
    private static final String API_URL = "http://g3.veroid.network:19029";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Activity создается");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        Log.d(TAG, "onCreate: Layout установлен - activity_add_recipe");


        // Настраиваем toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Добавить рецепт");
        Log.d(TAG, "onCreate: Toolbar настроен, заголовок установлен");

        // Инициализируем представления
        titleEditText = findViewById(R.id.recipe_title);
        ingredientsEditText = findViewById(R.id.recipe_ingredients);
        instructionsEditText = findViewById(R.id.recipe_instructions);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);
        
        // Инициализируем новые компоненты для работы с изображениями
        recipeImageView = findViewById(R.id.recipe_image);
        recipeImageView.setImageResource(select_recipe_view);

        recipeImageView.setOnClickListener(view -> {
            Log.d(TAG, "onClick: Нажата кнопка выбора изображения");
            checkStoragePermissionAndPickImage();
        });

        
        Log.d(TAG, "onCreate: Все UI элементы инициализированы");

        // Инициализируем SharedPreferences
        user = new MySharedPreferences(this);
        Log.d(TAG, user.getString("userId", "99"));
        Log.d(TAG, "onCreate: SharedPreferences инициализирован");

        titleEditText.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                validateTitle(s.toString());
            }
        });

        // Настраиваем обработчик кнопки "Сохранить"
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleEditText.getText().toString();
                String ingredient = ingredientsEditText.getText().toString();
                String instructions = instructionsEditText.getText().toString();
                //Log.d(TAG, "onClick: Нажата кнопка 'Сохранить'");
                if (validateTitle(title) && validateIngredient(ingredient) && validateinstructions(instructions) && validatephoto(imageBytes)){
                    saveRecipe();
                }else{
                    validateIngredient(ingredient);
                    validateinstructions(instructions);
                    validatephoto(imageBytes);
                }
            }
        });
        Log.d(TAG, "onCreate: Обработчик кнопки 'Сохранить' настроен");

        Log.d(TAG, "onCreate: Обработчик кнопки выбора изображения настроен");
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
            Log.d(TAG, "checkStoragePermissionAndPickImage: Запрошено разрешение: " + permission);
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
            Log.d(TAG, "openGallery: Открыта галерея для выбора изображения");
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
                Log.d(TAG, "onRequestPermissionsResult: Разрешение получено, открываем галерею");
                Toast.makeText(this, "Разрешение получено, теперь вы можете выбрать фото", 
                        Toast.LENGTH_SHORT).show();
                openGallery();
            } else {
                Toast.makeText(this, "Для выбора изображения необходим доступ к хранилищу", 
                        Toast.LENGTH_SHORT).show();
                Log.w(TAG, "onRequestPermissionsResult: Разрешение не получено");
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
                
                Log.d(TAG, "onActivityResult: Изображение выбрано, размер: " + imageBytes.length + " байт");
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

    private void saveRecipe() {
        Log.d(TAG, "saveRecipe: Начало процесса сохранения рецепта");

        // Проверяем подключение к интернету
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Отсутствует подключение к интернету", Toast.LENGTH_LONG).show();
            Log.w(TAG, "saveRecipe: Нет подключения к интернету");
            return;
        }

        // Получаем введенные пользователем данные
        String title = titleEditText.getText().toString().trim();
        String ingredients = ingredientsEditText.getText().toString().trim();
        String instructions = instructionsEditText.getText().toString().trim();
        String userId = user.getString("userId", "99");

        Log.d(TAG, "saveRecipe: Получены данные - title: " + title +
                ", ingredients length: " + ingredients.length() +
                ", instructions length: " + instructions.length() +
                ", userId: " + userId);

        // Показываем индикатор загрузки и блокируем кнопку
        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);
        Log.d(TAG, "saveRecipe: Показан индикатор загрузки, кнопка заблокирована");

        // Создаем экземпляр RecipeManager и сохраняем рецепт
        RecipeManager recipeManager = new RecipeManager(this);
        recipeManager.saveRecipe(title, ingredients, instructions, userId, null, imageBytes, 
                new RecipeManager.RecipeSaveCallback() {
            @Override
            public void onSuccess(String message) {
                progressBar.setVisibility(View.GONE);
                saveButton.setEnabled(true);
                Toast.makeText(AddRecipeActivity.this, message, Toast.LENGTH_LONG).show();
                Log.d(TAG, "saveRecipe: Успешно - " + message);
                finish(); // Закрываем активность после успешного добавления
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                saveButton.setEnabled(true);
                Toast.makeText(AddRecipeActivity.this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "saveRecipe: Ошибка - " + error);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Log.d(TAG, "onOptionsItemSelected: Нажата кнопка 'Назад' в toolbar");
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
            // Получаем TextInputLayout для названия рецепта
            TextInputLayout titleInputLayout = findViewById(R.id.recipe_title_layout);
            // Устанавливаем цвет ошибки и текст
            titleInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setError("Название рецепта не может быть пустым и короче 2 символов");
            return false;
        } else if (title.length() > 255 ) {
            // Получаем TextInputLayout для названия рецепта
            TextInputLayout titleInputLayout = findViewById(R.id.recipe_title_layout);
            // Устанавливаем цвет ошибки и текст
            titleInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setError("Название рецепта не должно превышать 255 символов");
            return false;
        } else {
            // Сбрасываем сообщение об ошибке
            TextInputLayout titleInputLayout = findViewById(R.id.recipe_title_layout);
            titleInputLayout.setError(null);
            return true;
        }
    }

    private boolean validateIngredient(String ingredient) {
        if (TextUtils.isEmpty(ingredient)) {
            // Получаем TextInputLayout для названия рецепта
            TextInputLayout titleInputLayout = findViewById(R.id.recipe_ingredients_layout);
            // Устанавливаем цвет ошибки и текст
            titleInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setError("Список ингредиентов не может быть пустым");
            return false;
        } else if (ingredient.length() > 2000) {
            TextInputLayout titleInputLayout = findViewById(R.id.recipe_ingredients_layout);
            // Устанавливаем цвет ошибки и текст
            titleInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setError("Список ингредиентов не может содержать более 2000 символов");
            return false;
        } else {
            // Сбрасываем сообщение об ошибке
            TextInputLayout titleInputLayout = findViewById(R.id.recipe_ingredients_layout);
            titleInputLayout.setError(null);
            return true;
        }
    }

    private boolean validateinstructions(String instructions) {
        if (TextUtils.isEmpty(instructions)) {
            // Получаем TextInputLayout для названия рецепта
            TextInputLayout titleInputLayout = findViewById(R.id.recipe_instructions_layout);
            // Устанавливаем цвет ошибки и текст
            titleInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setError("Инструкция не может быть пустой");
            return false;
        } else if (instructions.length() > 2000) {
            TextInputLayout titleInputLayout = findViewById(R.id.recipe_instructions_layout);
            // Устанавливаем цвет ошибки и текст
            titleInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            titleInputLayout.setError("Инструкция не может содержать более 2000 символов");
            return false;
        } else {
            // Сбрасываем сообщение об ошибке
            TextInputLayout titleInputLayout = findViewById(R.id.recipe_instructions_layout);
            titleInputLayout.setError(null);
            return true;
        }
    }

    private boolean validatephoto(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            // Получаем TextView и меняем цвет текста на красный
            TextView textImageView = findViewById(R.id.textImage);
            if (textImageView != null) {
                textImageView.setTextColor(Color.RED);
                // Можно также изменить текст для большей наглядности

            }
            
            // Показываем Toast с сообщением
            Toast.makeText(this, "Пожалуйста, выберите изображение", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            // Возвращаем нормальный цвет текста
            TextView textImageView = findViewById(R.id.textImage);
            if (textImageView != null) {
                textImageView.setTextColor(Color.BLACK); // Стандартный цвет текста
                textImageView.setText("Выберите изображение");
            }
            return true;
        }
    }
}
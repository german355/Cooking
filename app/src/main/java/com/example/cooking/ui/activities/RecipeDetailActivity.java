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

import com.bumptech.glide.Glide;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.network.services.RecipeDeleter;
import com.example.cooking.ui.fragments.FavoritesFragment;
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
    private static final String TAG = "RecipeDatail";
    
    // Переменная состояния лайка, как поле класса
    private boolean isLiked;
    private FloatingActionButton fabLike;

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
        String photoUrl = getIntent().getStringExtra("photo_url");
        TextView creatTime = findViewById(R.id.recipe_date);
        creatTime.setText("Создано: " + created_at);

        // Настраиваем заголовок Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        // Находим и настраиваем FAB
        fabLike = findViewById(R.id.like_button);
        MySharedPreferences user = new MySharedPreferences(this);

        int permission = user.getInt("permission", 1);
        
        // Добавляем логирование для отладки
        String currentUserId = user.getString("userId", "99");
        Log.d("RecipeDetail","Recipe userId: " + userId);
        Log.d("RecipeDetail","Recipe currentId: " + currentUserId);
        Log.d("permission", String.valueOf(permission));
        
        // Проверяем, принадлежит ли рецепт текущему пользователю
        
        // Проверяем, лайкнут ли рецепт, на основе переданных данных
        isLiked = getIntent().getBooleanExtra("isLiked", false);
        // Если рецепт лайкнут, меняем иконку кнопки
        updateLikeButtonState();


        // Настраиваем обработчик нажатия на кнопку лайка
        fabLike.setOnClickListener(view -> {
            // Проверяем, авторизован ли пользователь
            if (currentUserId.equals("0")) {
                Toast.makeText(this, "Для добавления рецепта в избранное необходимо войти в аккаунт", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Переключаем визуальное состояние лайка
            isLiked = !isLiked;
            updateLikeButtonState();
            
            // Заменяем использование класса NewLike прямым API-запросом
            try {
                // Создаем JSON для запроса
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("userId", currentUserId);
                jsonObject.put("recipeId", recipeId);
                
                // Создаем тело запроса
                RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json; charset=utf-8"));
                
                // Создаем запрос к API
                Request request = new Request.Builder()
                        .url("http://r1.veroid.network:10009/like")
                        .post(body)
                        .build();
                
                // Создаем OkHttpClient с увеличенными таймаутами
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS) // 30 секунд на подключение
                        .readTimeout(30, TimeUnit.SECONDS) // 30 секунд на чтение
                        .writeTimeout(30, TimeUnit.SECONDS) // 30 секунд на запись
                        .retryOnConnectionFailure(true) // Автоматически повторять при ошибках соединения
                        .build();
                
                // Выполняем запрос асинхронно с повторными попытками
                executeWithRetry(client, request, 0, 3);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при создании запроса лайка", e);
                Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Откатываем состояние лайка, если произошла ошибка
                isLiked = !isLiked;
                updateLikeButtonState();
            }
        });

        // Находим TextView для названия, ингредиентов и инструкций
        TextView titleTextView = findViewById(R.id.recipe_title);
        TextView ingredientsTextView = findViewById(R.id.recipe_ingredients);
        TextView instructionsTextView = findViewById(R.id.recipe_instructions);
        ShapeableImageView recipeImageView = findViewById(R.id.recipe_image);

        // Устанавливаем текст
        titleTextView.setText(title);
        ingredientsTextView.setText(ingredients);
        instructionsTextView.setText(instructions);
        
        // Загружаем изображение рецепта, если оно доступно
        if (photoUrl != null && !photoUrl.isEmpty()) {
            // Загружаем с помощью Glide и применяем скругление углов
            Glide.with(this)
                 .load(photoUrl)
                 .placeholder(R.drawable.white_card_background)
                 .error(R.drawable.white_card_background)
                 .centerCrop()
                 .into(recipeImageView);
            Log.d(TAG, "Загрузка изображения: " + photoUrl);
        } else {
            // Если URL отсутствует, устанавливаем фоновое изображение
            recipeImageView.setImageResource(R.drawable.white_card_background);
            Log.d(TAG, "URL изображения отсутствует или пуст");
        }
    }
    
    /**
     * Выполняет запрос с повторными попытками при ошибках
     */
    private void executeWithRetry(OkHttpClient client, Request request, int retryCount, int maxRetries) {
        // Получаем recipeId из текущего Intent для использования в методе
        final int recipeId = getIntent().getIntExtra("recipe_id", -1);
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                Log.e(TAG, "Ошибка сети при попытке #" + (retryCount + 1), e);
                
                if (retryCount < maxRetries) {
                    // Делаем паузу перед повторной попыткой
                    try {
                        Thread.sleep(1000 * (retryCount + 1));
                    } catch (InterruptedException ie) {
                        Log.e(TAG, "Прерывание ожидания", ie);
                    }
                    
                    // Повторяем запрос
                    runOnUiThread(() -> {
                        Log.d(TAG, "Повторная попытка запроса #" + (retryCount + 1));
                        executeWithRetry(client, request, retryCount + 1, maxRetries);
                    });
                } else {
                    // Все попытки исчерпаны
                    runOnUiThread(() -> {
                        Toast.makeText(RecipeDetailActivity.this, 
                                "Ошибка сети: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Ошибка сети при лайке после " + maxRetries + " попыток", e);
                        // Откатываем состояние если ошибка
                        isLiked = !isLiked;
                        updateLikeButtonState();
                    });
                }
            }
            
            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                final boolean success = response.isSuccessful();
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(RecipeDetailActivity.this, 
                                "Статус лайка изменен", 
                                Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Лайк успешно изменен");
                        
                        // Если рецепт был лайкнут, добавляем его в FavoritesFragment
                        if (isLiked) {
                            Log.d(TAG, "Добавляем рецепт в FavoritesFragment");
                            // Создаем объект Recipe из данных Intent
                            Recipe recipe = new Recipe();
                            recipe.setId(getIntent().getIntExtra("recipe_id", -1));
                            recipe.setTitle(getIntent().getStringExtra("recipe_title"));
                            recipe.setIngredients(getIntent().getStringExtra("recipe_ingredients"));
                            recipe.setInstructions(getIntent().getStringExtra("recipe_instructions"));
                            recipe.setPhoto_url(getIntent().getStringExtra("photo_url"));
                            recipe.setCreated_at(getIntent().getStringExtra("Created_at"));
                            recipe.setUserId(getIntent().getStringExtra("userId"));
                            recipe.setLiked(true);
                            
                            // Добавляем в список избранного
                            FavoritesFragment.addLikedRecipe(recipe);
                        }
                        
                        // Установка результата для возврата в HomeFragment
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("recipe_id", recipeId);
                        resultIntent.putExtra("isLiked", isLiked);
                        setResult(RESULT_OK, resultIntent);
                    } else {
                        if (retryCount < maxRetries && (response.code() >= 500 || response.code() == 429)) {
                            // Повторяем попытку для серверных ошибок
                            Log.d(TAG, "Повторная попытка #" + (retryCount + 1) + " после HTTP ошибки " + response.code());
                            // Делаем паузу перед повторной попыткой
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                executeWithRetry(client, request, retryCount + 1, maxRetries);
                            }, 1000 * (retryCount + 1));
                        } else {
                            Toast.makeText(RecipeDetailActivity.this, 
                                    "Ошибка сервера: " + response.code(), 
                                    Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Ошибка сервера при лайке: " + response.code());
                            
                            // Откатываем состояние лайка, если произошла ошибка
                            isLiked = !isLiked;
                            updateLikeButtonState();
                        }
                    }
                });
            }
        });
    }
    
    /**
     * Обновляет состояние кнопки лайка в зависимости от isLiked
     */
    private void updateLikeButtonState() {
        // Изменяем иконку в зависимости от состояния isLiked
        if (isLiked) {
            fabLike.setImageResource(R.drawable.ic_favorite);
        } else {
            fabLike.setImageResource(R.drawable.ic_favorite_border);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipe_detail, menu);
        
        // Проверяем, может ли пользователь редактировать/удалять рецепт
        String recipeUserId = getIntent().getStringExtra("userId");
        MySharedPreferences user = new MySharedPreferences(this);
        String currentUserId = user.getString("userId", "0");
        int permission = user.getInt("permission", 1);
        
        // Скрываем меню, если пользователь не создатель рецепта и не администратор
        if (!(recipeUserId != null && recipeUserId.equals(currentUserId)) && permission != 2) {
            MenuItem editItem = menu.findItem(R.id.action_edit);
            MenuItem deleteItem = menu.findItem(R.id.action_delete);
            
            if (editItem != null) {
                editItem.setVisible(false);
            }
            
            if (deleteItem != null) {
                deleteItem.setVisible(false);
            }
            
            // Возможно также скрыть всё меню, если оно содержит только эти пункты
            MenuItem moreItem = menu.findItem(R.id.action_more);
            if (moreItem != null) {
                moreItem.setVisible(false);
            }
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_edit) {
            // Обработка нажатия на "Редактировать"
            Log.d(TAG, "onOptionsItemSelected: Нажата кнопка 'Редактировать'");
            
            // Получаем данные о рецепте из Intent
            int recipeId = getIntent().getIntExtra("recipe_id", -1);
            String userId = getIntent().getStringExtra("userId");
            String title = getIntent().getStringExtra("recipe_title");
            String ingredients = getIntent().getStringExtra("recipe_ingredients");
            String instructions = getIntent().getStringExtra("recipe_instructions");
            String photoUrl = getIntent().getStringExtra("photo_url");
            
            // Проверяем права пользователя на редактирование
            MySharedPreferences user = new MySharedPreferences(this);
            String currentUserId = user.getString("userId", "99");
            int permission = user.getInt("permission", 1);
            
            // Если пользователь не автор рецепта и не администратор, показываем сообщение
            if (!(userId != null && userId.equals(currentUserId)) && permission != 2) {
                new AlertDialog.Builder(this)
                    .setTitle("Недостаточно прав")
                    .setMessage("У вас нет прав на редактирование этого рецепта. Только автор рецепта или администратор могут вносить изменения.")
                    .setPositiveButton("Понятно", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
                return true;
            }
            
            // Запускаем активность редактирования рецепта с передачей данных
            Intent intent = new Intent(this, EditRecipeActivity.class);
            intent.putExtra("recipe_id", recipeId);
            intent.putExtra("recipe_title", title);
            intent.putExtra("recipe_ingredients", ingredients);
            intent.putExtra("recipe_instructions", instructions);
            intent.putExtra("photo_url", photoUrl);
            
            // Запускаем активность и ожидаем результат
            startActivityForResult(intent, 200);
            return true;
        } else if (id == R.id.action_delete) {
            // Обработка нажатия на "Удалить"
            Log.d(TAG, "onOptionsItemSelected: Нажата кнопка 'Удалить'");
            showDeleteConfirmationDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Показывает диалог подтверждения удаления рецепта
     */
    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Удаление рецепта");
        builder.setMessage("Вы уверены, что хотите удалить этот рецепт?");
        builder.setPositiveButton("Удалить", (dialog, which) -> {
            // Получаем данные о рецепте из Intent
            int recipeId = getIntent().getIntExtra("recipe_id", -1);
            String userId = getIntent().getStringExtra("userId");
            
            // Получаем permission пользователя
            MySharedPreferences user = new MySharedPreferences(this);
            int permission = user.getInt("permission", 1);
            
            // Используем RecipeDeleter для удаления рецепта
            RecipeDeleter deleter = new RecipeDeleter(this);
            deleter.deleteRecipe(recipeId, userId, permission, new RecipeDeleter.DeleteRecipeCallback() {
                @Override
                public void onDeleteSuccess() {
                    Log.d("DeleteRecipe", "Рецепт удалён");
                    Toast.makeText(RecipeDetailActivity.this, "Рецепт удалён", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onDeleteFailure(String error) {
                    // Обработка ошибки удаления рецепта
                    Log.e("DeleteRecipe", "Ошибка удаления рецепта: " + error);
                    
                    // Проверяем, содержит ли сообщение об ошибке информацию о недостатке прав (ошибка 403)
                    if (error.contains("нет прав на удаление")) {
                        // Показываем диалоговое окно с объяснением
                        new AlertDialog.Builder(RecipeDetailActivity.this)
                            .setTitle("Недостаточно прав")
                            .setMessage(error)
                            .setPositiveButton("Понятно", (dialog, which) -> dialog.dismiss())
                            .setCancelable(true)
                            .show();
                    } else {
                        Toast.makeText(RecipeDetailActivity.this, "Ошибка удаления рецепта: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    
    /**
     * Обрабатывает результат редактирования рецепта
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Обработка результата от EditRecipeActivity
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "onActivityResult: Получен результат от EditRecipeActivity");
            
            // Получаем обновленные данные
            String newTitle = data.getStringExtra("recipe_title");
            String newIngredients = data.getStringExtra("recipe_ingredients");
            String newInstructions = data.getStringExtra("recipe_instructions");
            String photoUrl = data.getStringExtra("photo_url");
            
            // Обновляем данные в текущем Intent
            getIntent().putExtra("recipe_title", newTitle);
            getIntent().putExtra("recipe_ingredients", newIngredients);
            getIntent().putExtra("recipe_instructions", newInstructions);
            
            // Обновляем заголовок Toolbar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(newTitle);
            }
            
            // Обновляем текстовые поля
            TextView titleTextView = findViewById(R.id.recipe_title);
            TextView ingredientsTextView = findViewById(R.id.recipe_ingredients);
            TextView instructionsTextView = findViewById(R.id.recipe_instructions);
            
            titleTextView.setText(newTitle);
            ingredientsTextView.setText(newIngredients);
            instructionsTextView.setText(newInstructions);
            
            // Обновляем изображение, если изменилось
            if (photoUrl != null && !photoUrl.isEmpty()) {
                ShapeableImageView recipeImageView = findViewById(R.id.recipe_image);
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.white_card_background)
                    .error(R.drawable.white_card_background)
                    .centerCrop()
                    .into(recipeImageView);
            }
            
            Toast.makeText(this, "Рецепт успешно обновлен", Toast.LENGTH_SHORT).show();
        }
    }
}
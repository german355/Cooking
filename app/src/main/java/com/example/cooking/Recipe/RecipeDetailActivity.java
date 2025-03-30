package com.example.cooking.Recipe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.cooking.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.ServerWorker.RecipeDeleter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;

import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

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
        int  recipeId = getIntent().getIntExtra("recipe_id", -1);
        String title = getIntent().getStringExtra("recipe_title");
        String ingredients = getIntent().getStringExtra("recipe_ingredients");
        String instructions = getIntent().getStringExtra("recipe_instructions");
        String created_at = getIntent().getStringExtra("Created_at");
        String userId = getIntent().getStringExtra("userId");
        String photoUrl = getIntent().getStringExtra("photo_url");

        // Настраиваем заголовок Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        // Находим и настраиваем FAB
        FloatingActionButton fabDelite = findViewById(R.id.delete_button);
        fabLike = findViewById(R.id.like_button);
        MySharedPreferences user = new MySharedPreferences(this);

        int permission = user.getInt("permission", 1);
        
        // Добавляем логирование для отладки
        String currentUserId = user.getString("userId", "99");
        Log.d("RecipeDetail","Recipe userId: " + userId);
        Log.d("RecipeDetail","Recipe currentId: " + currentUserId);
        Log.d("permission", String.valueOf(permission));
        
        // Проверяем, принадлежит ли рецепт текущему пользователю
        if ((userId != null && userId.equals(currentUserId) ) || permission == 2) {
            fabDelite.setVisibility(View.VISIBLE);
            Log.d("RecipeDetail","Fab иден");
        } else {
            fabDelite.setVisibility(View.GONE);
            Log.d("RecipeDetail","Fab спрятан");
        }
        
        // Проверяем, лайкнут ли рецепт, на основе переданных данных
        isLiked = getIntent().getBooleanExtra("isLiked", false);
        // Если рецепт лайкнут, меняем иконку кнопки
        updateLikeButtonState();
        
        // Настраиваем обработчик нажатия на кнопку удаления
        fabDelite.setOnClickListener(view -> {
            RecipeDeleter deleter = new RecipeDeleter(this);
            deleter.deleteRecipe(recipeId, userId, permission, new RecipeDeleter.DeleteRecipeCallback() {
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

        final int finalRecipeId = recipeId;
        fabLike.setOnClickListener(view -> {
            // Переключаем визуальное состояние лайка
            isLiked = !isLiked;
            updateLikeButtonState();
            
            // Заменяем использование класса NewLike прямым API-запросом
            try {
                // Создаем JSON для запроса
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("userId", currentUserId);
                jsonObject.put("recipeId", finalRecipeId);
                
                // Создаем тело запроса
                RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json; charset=utf-8"));
                
                // Создаем запрос к API
                Request request = new Request.Builder()
                        .url("http://g3.veroid.network:19029/like")
                        .post(body)
                        .build();
                
                // Создаем OkHttpClient
                OkHttpClient client = new OkHttpClient();
                
                // Выполняем запрос асинхронно
                client.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, java.io.IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(RecipeDetailActivity.this, 
                                    "Ошибка сети: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Ошибка сети при лайке", e);
                            // Откатываем состояние если ошибка
                            isLiked = !isLiked;
                            updateLikeButtonState();
                        });
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
                                
                                // Установка результата для возврата в HomeFragment
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("recipe_id", finalRecipeId);
                                resultIntent.putExtra("isLiked", isLiked);
                                setResult(RESULT_OK, resultIntent);
                            } else {
                                Toast.makeText(RecipeDetailActivity.this, 
                                        "Ошибка сервера: " + response.code(), 
                                        Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Ошибка сервера при лайке: " + response.code());
                                
                                // Откатываем состояние лайка, если произошла ошибка
                                isLiked = !isLiked;
                                updateLikeButtonState();
                            }
                        });
                    }
                });
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
     * Обновляет состояние кнопки лайка в зависимости от текущего значения isLiked
     */
    private void updateLikeButtonState() {
        if (isLiked) {
            fabLike.setImageResource(R.drawable.ic_favorite);
        } else {
            fabLike.setImageResource(R.drawable.ic_favorite_border);
        }
    }
}
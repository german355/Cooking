package com.example.cooking.ServerWorker;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.cooking.MySharedPreferences;
import com.example.cooking.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddRecipeActivity extends AppCompatActivity {
    private static final String TAG = "AddRecipeActivity";
    
    private EditText titleEditText;
    private MySharedPreferences user;
    private EditText ingredientsEditText;
    private EditText instructionsEditText;
    private Button saveButton;
    private ProgressBar progressBar;
    
    private static final String API_URL = "http://g3.veroid.network:19029";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

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
        Log.d(TAG, "onCreate: Все UI элементы инициализированы");
        
        // Инициализируем SharedPreferences
        user = new MySharedPreferences(this);
        Log.d(TAG, user.getString("userId", "99"));
        Log.d(TAG, "onCreate: SharedPreferences инициализирован");
        
        // Настраиваем обработчик кнопки "Сохранить"
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Нажата кнопка 'Сохранить'");
                saveRecipe();
            }
        });
        Log.d(TAG, "onCreate: Обработчик кнопки 'Сохранить' настроен");
    }
    
    private void saveRecipe() {
        Log.d(TAG, "saveRecipe: Начало процесса сохранения рецепта");
        
        // Получаем введенные пользователем данные
        String title = titleEditText.getText().toString().trim();
        String ingredients = ingredientsEditText.getText().toString().trim();
        String instructions = instructionsEditText.getText().toString().trim();
        String id = user.getString("userId", "99");


        Log.d(TAG, "saveRecipe: Получены данные - title: " + title + 
                ", ingredients length: " + ingredients.length() + 
                ", instructions length: " + instructions.length() + 
                ", userId: " + id);
        
        // Проверяем, что все поля заполнены
        if (title.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            Log.w(TAG, "saveRecipe: Не все поля заполнены - title: " + (title.isEmpty() ? "пусто" : "заполнено") + 
                    ", ingredients: " + (ingredients.isEmpty() ? "пусто" : "заполнено") + 
                    ", instructions: " + (instructions.isEmpty() ? "пусто" : "заполнено"));
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Показываем индикатор загрузки и блокируем кнопку
        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);
        Log.d(TAG, "saveRecipe: Показан индикатор загрузки, кнопка заблокирована");
        
        // Создаем и выполняем асинхронную задачу для отправки рецепта на сервер
        Log.d(TAG, "saveRecipe: Запуск асинхронной задачи SaveRecipeTask");
        new SaveRecipeTask(title, ingredients, instructions, id ).execute();
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
    
    // Асинхронная задача для отправки рецепта на сервер
    private class SaveRecipeTask extends AsyncTask<Void, Void, Boolean> {
        private final String title;
        private final String ingredients;
        private final String instructions;
        //private final String creat_at;
        private final String userId;
        private String errorMessage;
        
        SaveRecipeTask(String title, String ingredients, String instructions, /*String creatAt,*/ String userId) {
            this.title = title;
            this.ingredients = ingredients;
            this.instructions = instructions;
            //this.creat_at = creatAt;
            this.userId = userId;

            Log.d(TAG, "SaveRecipeTask: Создан экземпляр задачи с параметрами - title: " + title + 
                    ", ingredients length: " + ingredients.length() + 
                    ", instructions length: " + instructions.length()  );
                    //", userId: " + creatAt);
        }
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute: Начало выполнения асинхронной задачи");
        }
        
        @Override
        protected Boolean doInBackground(Void... voids) {
            Log.d(TAG, "doInBackground: Выполнение в фоновом потоке");
            try {
                // Создаем JSON с данными рецепта
                JSONObject recipeJson = new JSONObject();
                recipeJson.put("title", title);
                recipeJson.put("ingredients", ingredients);
                recipeJson.put("instructions", instructions);
                recipeJson.put("userId", userId );
                Log.d(TAG, "doInBackground: Создан JSON объект: " + recipeJson.toString());
                
                // Создаем HTTP-клиент и запрос
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(recipeJson.toString(), JSON);
                String url = API_URL + "/addrecipes";
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();
                Log.d(TAG, "doInBackground: Подготовлен HTTP запрос к URL: " + url);
                
                // Выполняем запрос
                Log.d(TAG, "doInBackground: Отправка HTTP запроса...");
                long startTime = System.currentTimeMillis();
                Response response = client.newCall(request).execute();
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "doInBackground: Получен ответ за " + (endTime - startTime) + " мс, код: " + response.code());
                
                String responseData = response.body().string();
                Log.d(TAG, "doInBackground: Тело ответа: " + responseData);
                
                // Обрабатываем ответ
                if (response.isSuccessful()) {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    boolean success = jsonResponse.optBoolean("success", false);
                    Log.d(TAG, "doInBackground: Успешный ответ, success = " + success);
                    return success;
                } else {
                    errorMessage = "Ошибка сервера: " + response.code();
                    Log.e(TAG, "doInBackground: " + errorMessage);
                    return false;
                }
            } catch (JSONException e) {
                Log.e(TAG, "doInBackground: JSON error", e);
                errorMessage = "Ошибка формата данных";
                return false;
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: Network error", e);
                errorMessage = "Ошибка сети";
                return false;
            } catch (Exception e) {
                Log.e(TAG, "doInBackground: Unknown error", e);
                errorMessage = "Неизвестная ошибка";
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            Log.d(TAG, "onPostExecute: Завершение асинхронной задачи, результат: " + success);
            
            // Скрываем индикатор загрузки и разблокируем кнопку
            progressBar.setVisibility(View.GONE);
            saveButton.setEnabled(true);
            Log.d(TAG, "onPostExecute: Индикатор загрузки скрыт, кнопка разблокирована");
            
            if (success) {
                // Очищаем кэш рецептов, чтобы при возврате на главный экран данные обновились
                RecipeRepository repository = new RecipeRepository(AddRecipeActivity.this);
                repository.clearCache();
                Log.d(TAG, "onPostExecute: Кэш рецептов очищен");
                
                // Показываем сообщение об успехе и закрываем активность
                Toast.makeText(AddRecipeActivity.this, "Рецепт успешно сохранен", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onPostExecute: Показано сообщение об успехе, закрытие активности");
                finish();
            } else {
                // Показываем сообщение об ошибке
                String message = errorMessage != null ? errorMessage : "Не удалось сохранить рецепт";
                Log.e(TAG, "onPostExecute: Показано сообщение об ошибке: " + message);
                Toast.makeText(AddRecipeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
package com.example.cooking;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cooking.ServerWorker.LoginTask;
import com.example.cooking.ServerWorker.Regist;

public class StartActivity extends AppCompatActivity implements LoginTask.LoginCallback {
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    MySharedPreferences id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.start__activity);
            
            // Инициализация MySharedPreferences до проверки auth
            id = new MySharedPreferences(this);
            
            // Проверка авторизации
            if (id.getBoolean("auth", false)){
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return; // Важно: выходим из метода, чтобы избежать дальнейшей инициализации
            }
            
            // Инициализация Views
            emailEditText = findViewById(R.id.emailEditText);
            passwordEditText = findViewById(R.id.passwordEditText);
            loginButton = findViewById(R.id.loginButton);
            registerTextView = findViewById(R.id.registerTextView);

            // Заполняем поля, если данные переданы
            prefillCredentialsIfProvided();

            // Обработчик нажатия на текст регистрации
            registerTextView.setOnClickListener(v -> {
                Intent intent = new Intent(StartActivity.this, Regist.class);
                startActivity(intent);
            });

            loginButton.setOnClickListener(v -> {
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Запускаем асинхронную задачу для входа
                new LoginTask(this).execute(email, password);
                
                // Показываем индикатор загрузки
                loginButton.setEnabled(false);
                loginButton.setText("Подождите...");
            });
        } catch (Exception e) {
            // Логируем ошибку для отладки
            Log.e("StartActivity", "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Ошибка запуска приложения", Toast.LENGTH_LONG).show();
        }
    }

    private void prefillCredentialsIfProvided() {
        // Проверяем, были ли переданы учетные данные с экрана регистрации
        Intent intent = getIntent();
        if (intent != null) {
            String email = intent.getStringExtra("email");
            String password = intent.getStringExtra("password");
            
            if (email != null && !email.isEmpty()) {
                emailEditText.setText(email);
            }
            
            if (password != null && !password.isEmpty()) {
                passwordEditText.setText(password);
            }
        }
    }

    @Override
    public void onLoginSuccess(String userId, String userName, int permission) {
        runOnUiThread(() -> {
            try {

                id.putString("userId", userId);
                id.putString("userName", userName);
                id.putBoolean("auth", true);
                id.putInt("permission", permission );

                // Переходим на главный экран
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Log.e("StartActivity", "Error starting MainActivity: " + e.getMessage());
                Toast.makeText(this, "Ошибка при переходе на главный экран", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLoginFailure(String error) {
        runOnUiThread(() -> {
            try {
                Toast.makeText(this, "Не верный логин или пароль", Toast.LENGTH_LONG).show();
                loginButton.setEnabled(true);
                loginButton.setText("Войти");
                Log.d("err", error);
            } catch (Exception e) {
                Log.e("StartActivity", "Error handling login failure: " + e.getMessage());
            }
        });
    }
}
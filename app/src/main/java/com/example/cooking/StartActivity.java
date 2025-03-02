package com.example.cooking;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity implements LoginTask.LoginCallback {
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start__activity);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);

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
    }

    @Override
    public void onLoginSuccess(String userId) {
        runOnUiThread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("CookingApp", MODE_PRIVATE);
                prefs.edit().putString("userId", userId).apply();

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
                Toast.makeText(this, "Ошибка входа: " + error, Toast.LENGTH_LONG).show();
                loginButton.setEnabled(true);
                loginButton.setText("Войти");
                Log.d("err", error);
            } catch (Exception e) {
                Log.e("StartActivity", "Error handling login failure: " + e.getMessage());
            }
        });
    }
}

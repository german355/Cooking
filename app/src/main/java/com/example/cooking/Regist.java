package com.example.cooking;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

public class Regist extends AppCompatActivity implements RegistrationTask.RegistrationCallback {
    private TextInputEditText Email;
    private TextInputEditText Pass;
    private TextInputEditText ConfirmPass;
    private Button regist;
    private TextView enter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Инициализация views
        Email = findViewById(R.id.emailEditTextRegist);
        Pass = findViewById(R.id.passwordEditText1);
        ConfirmPass = findViewById(R.id.passwordEditText2);
        regist = findViewById(R.id.registerButton);
        enter = findViewById(R.id.loginPromptTextView);

        // Настройка toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Обработчик нажатия на кнопку регистрации
        regist.setOnClickListener(v -> {
            String email = Email.getText().toString();
            String password = Pass.getText().toString();
            String confirmPassword = ConfirmPass.getText().toString();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
                return;
            }

            // Показываем индикатор загрузки
            regist.setEnabled(false);
            regist.setText("Подождите...");

            // Запускаем регистрацию
            new RegistrationTask(this).execute(email, password, confirmPassword);
        });

        // Обработчик нажатия на текст входа
        enter.setOnClickListener(v -> {
            Intent intent = new Intent(Regist.this, StartActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onRegistrationSuccess() {
        runOnUiThread(() -> {
            try {
                Intent intent = new Intent(Regist.this, StartActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Log.e("Regist", "Error regist");
                Toast.makeText(this, "Ошибка регистрации", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRegistrationFailure(String error) {
        runOnUiThread(() -> {
            try {
                Toast.makeText(this, "Ошибка регистрации: " + error, Toast.LENGTH_LONG).show();
                // Возвращаем кнопку в исходное состояние
                regist.setEnabled(true);
                regist.setText("Зарегистрироваться");
                Log.d("err", error);
            } catch (Exception e) {
                Log.e("Regist", "Error handling registration failure: " + e.getMessage());
            }
        });
    }
}

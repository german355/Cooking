package com.example.cooking.ServerWorker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cooking.R;
import com.example.cooking.StartActivity;
import com.google.android.material.textfield.TextInputEditText;

public class Regist extends AppCompatActivity implements RegistrationTask.RegistrationCallback {
    private TextInputEditText nameEditText;
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
        nameEditText = findViewById(R.id.NameEditText);
        Email = findViewById(R.id.emailEditText);
        Pass = findViewById(R.id.passwordEditText);
        ConfirmPass = findViewById(R.id.passwordEditText2);
        regist = findViewById(R.id.registerButton);
        enter = findViewById(R.id.loginPromptTextView);

        // Обработчик нажатия на кнопку регистрации
        regist.setOnClickListener(v -> {
            String name = nameEditText.getText().toString();
            String email = Email.getText().toString();
            String password = Pass.getText().toString();
            String confirmPassword = ConfirmPass.getText().toString();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
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
            new RegistrationTask(this).execute(email, password, name);
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
                // Показываем пользователю, что регистрация успешна
                Toast.makeText(this, "Регистрация успешна! Перенаправление на экран входа...", Toast.LENGTH_SHORT).show();
                
                // Создаем Intent и передаем учетные данные
                Intent intent = new Intent(Regist.this, StartActivity.class);
                intent.putExtra("email", Email.getText().toString());
                intent.putExtra("password", Pass.getText().toString());
                
                // Сразу переходим на экран входа
                startActivity(intent);
                finish();
                
            } catch (Exception e) {
                Log.e("Regist", "Error during successful registration completion: " + e.getMessage());
                Toast.makeText(this, "Регистрация успешна, но возникла ошибка при переходе на экран входа", Toast.LENGTH_SHORT).show();
                
                // Возвращаем кнопку в исходное состояние на случай, если что-то пошло не так
                regist.setEnabled(true);
                regist.setText("Зарегистрироваться");
            }
        });
    }

    @Override
    public void onRegistrationFailure(String error) {
        runOnUiThread(() -> {
            try {
                // Показываем более дружественное сообщение
                String friendlyMessage;
                if (error.contains("Пользователь с таким email уже существует")) {
                    friendlyMessage = "Этот логин уже используется. Попробуйте другой.";
                } else if (error.contains("соединение прервано")) {
                    friendlyMessage = "Проблема с подключением к серверу. Проверьте интернет-соединение.";
                } else {
                    friendlyMessage = "Не удалось зарегистрироваться. Попробуйте позже.";
                }
                
                Toast.makeText(this, friendlyMessage, Toast.LENGTH_LONG).show();
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

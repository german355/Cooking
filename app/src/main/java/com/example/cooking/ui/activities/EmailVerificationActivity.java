package com.example.cooking.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cooking.R;
import com.google.firebase.auth.FirebaseAuth;

public class EmailVerificationActivity extends AppCompatActivity {
    private static final String TAG = "EmailVerificationAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Обрабатываем deep link без layout

        Uri data = getIntent().getData();
        if (data != null) {
            String mode = data.getQueryParameter("mode");
            String oobCode = data.getQueryParameter("oobCode");
            if ("verifyEmail".equals(mode) && oobCode != null) {
                FirebaseAuth.getInstance().applyActionCode(oobCode)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Email успешно подтверждён", Toast.LENGTH_LONG).show();
                            } else {
                                String err = task.getException() != null ? task.getException().getMessage() : "";
                                Toast.makeText(this, "Ошибка подтверждения email: " + err, Toast.LENGTH_LONG).show();
                            }
                            // Переходим на главный экран
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                return;
            }
        }
        // Если нет параметров или они некорректны, возвращаемся на главный экран
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
} 
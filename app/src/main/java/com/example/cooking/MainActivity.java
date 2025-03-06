package com.example.cooking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.cooking.ServerWorker.AddRecipeActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.View;

/**
 * Главная активность приложения.
 * Отвечает за управление фрагментами и нижней навигацией.
 */
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private TextView topTitleTextView;
    private FloatingActionButton addButton;

    /**
     * Вызывается при создании активности.
     * Инициализирует интерфейс и настраивает навигацию.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Настройка нижней навигации
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String title = "";

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
                title = "Главная";
            } else if (itemId == R.id.nav_favorites) {
                selectedFragment = new FavoritesFragment();
                title = "Избранное";
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
                title = "Профиль";
            }

            // Обновляем заголовок в зависимости от выбранного фрагмента
            if (title != null && !title.isEmpty()) {
                topTitleTextView.setText(title);
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
                return true;
            }
            return false;
        });

        // Инициализируем TextView
        topTitleTextView = findViewById(R.id.topTitleTextView);
        
        // Инициализируем и настраиваем кнопку добавления
        addButton = findViewById(R.id.fab_add);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Здесь будет код для обработки нажатия на кнопку
                Toast.makeText(MainActivity.this, "Добавление нового рецепта", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
                startActivity(intent);
            }
        });

        // Установка начального фрагмента при запуске
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
            
            // Устанавливаем начальный заголовок
            topTitleTextView.setText("Главная");
        }
    }
}

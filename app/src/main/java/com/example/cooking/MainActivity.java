package com.example.cooking;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Главная активность приложения.
 * Отвечает за управление фрагментами и нижней навигацией.
 */
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

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
            int itemId = item.getItemId();
            
            // Выбор нужного фрагмента в зависимости от нажатой кнопки
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_favorites) {
                selectedFragment = new FavoritesFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            // Замена текущего фрагмента на выбранный
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            }
            return true;
        });

        // Установка начального фрагмента при запуске
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new HomeFragment())
            .commit();
    }
}

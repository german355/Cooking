package com.example.cooking.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.ui.fragments.ProfileFragment;
import com.example.cooking.ui.fragments.AuthFragment;
import com.example.cooking.ui.fragments.FavoritesFragment;
import com.example.cooking.ui.fragments.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.View;
import android.util.Log;
import com.example.cooking.FireBase.FirebaseAuthManager;

/**
 * Главная активность приложения.
 * Отвечает за управление фрагментами и нижней навигацией.
 */
public class MainActivity extends AppCompatActivity {
    public BottomNavigationView bottomNavigationView;
    private FloatingActionButton addButton;
    private Fragment currentFragment;
    private MySharedPreferences preferences;

    /**
     * Вызывается при создании активности.
     * Инициализирует интерфейс и настраивает навигацию.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addButton = findViewById(R.id.fab_add);
        preferences = new MySharedPreferences(this);

        // Инициализация Firebase Auth Manager и Google Sign In
        FirebaseAuthManager firebaseAuthManager = FirebaseAuthManager.getInstance();
        String webClientId = getString(R.string.default_web_client_id);
        firebaseAuthManager.initGoogleSignIn(this, webClientId);



        // Проверяем, чтобы избежать повторного добавления при повороте экрана


        // Настройка нижней навигации
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
                addButton.show();
            } else if (itemId == R.id.nav_favorites) {
                selectedFragment = new FavoritesFragment();
                addButton.hide();
            } else if (itemId == R.id.nav_profile)  {
                // Проверяем, авторизован ли пользователь
                if (isUserLoggedIn()) {
                    selectedFragment = new ProfileFragment();
                } else {
                    selectedFragment = new AuthFragment();
                }
                addButton.hide();
            }

            if (selectedFragment != null) {
                currentFragment = selectedFragment;
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Инициализируем и настраиваем кнопку добавления
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Проверяем, авторизован ли пользователь
                if (isUserLoggedIn()) {
                    // Если авторизован, переходим к добавлению рецепта
                    Toast.makeText(MainActivity.this, "Добавление нового рецепта", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
                    startActivity(intent);
                } else {
                    // Если не авторизован, показываем сообщение
                    Toast.makeText(MainActivity.this, "Для добавления рецептов необходимо войти в аккаунт", Toast.LENGTH_SHORT).show();
                    // Переключаемся на вкладку профиля для авторизации
                    bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                }
            }
        });

        if (savedInstanceState == null) {
            // Проверяем наличие флага для показа фрагмента авторизации
            boolean showAuthFragment = getIntent().getBooleanExtra("show_auth_fragment", false);
            
            if (showAuthFragment) {
                // Показываем фрагмент авторизации и скрываем кнопку добавления
                currentFragment = new AuthFragment();
                addButton.hide();
                // Устанавливаем выбранный пункт меню
                bottomNavigationView.setSelectedItemId(R.id.nav_profile);
            } else {
                // По умолчанию показываем домашний фрагмент
                currentFragment = new HomeFragment();
            }
            
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, currentFragment)
                    .commit();
        }
    }

    /**
     * Проверяет, авторизован ли пользователь
     * @return true если пользователь авторизован, false иначе
     */
    private boolean isUserLoggedIn() {
        String userId = preferences.getString("userId", "0");
        return !userId.equals("0");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipes, menu);
        
        // Комментируем обработку поиска через меню, так как поиск теперь делается через SearchView во фрагментах
        /*
        // Настройка поиска
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Если хотите делать поиск в реальном времени, раскомментируйте строку ниже
                // performSearch(newText);
                return true;
            }
        });
        */
        
        return true;
    }
    
    /**
     * Выполняет поиск, передавая запрос в текущий активный фрагмент
     */
    private void performSearch(String query) {
        if (currentFragment instanceof HomeFragment) {
            ((HomeFragment) currentFragment).performSearch(query);
        } else if (currentFragment instanceof FavoritesFragment) {
            ((FavoritesFragment) currentFragment).performSearch(query);
        }
    }

    /**
     * Метод для программного переключения на домашний фрагмент с рецептами
     */
    public void navigateToHomeFragment() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Если текущий фрагмент - профиль, проверяем статус авторизации
        if (currentFragment instanceof ProfileFragment && !isUserLoggedIn()) {
            // Если пользователь вышел из аккаунта, показываем AuthFragment
            currentFragment = new AuthFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, currentFragment)
                    .commit();
        } else if (currentFragment instanceof AuthFragment && isUserLoggedIn()) {
            // Если пользователь вошел в аккаунт, показываем ProfileFragment
            currentFragment = new ProfileFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, currentFragment)
                    .commit();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Для отладки
        Log.d("MainActivity", "onActivityResult: requestCode=" + requestCode + 
                ", resultCode=" + resultCode + ", data=" + (data != null ? "не null" : "null"));

        // Константа RC_SIGN_IN должна быть той же, что используется в FirebaseAuthManager
        final int RC_SIGN_IN = 9001;
        
        // Если это ответ от Google Sign In, логируем дополнительную информацию
        if (requestCode == RC_SIGN_IN) {
            Log.d("MainActivity", "Получен результат для Google Sign In, передаю его в фрагмент");
            
            // Логирование extras из intent для отладки
            if (data != null && data.getExtras() != null) {
                for (String key : data.getExtras().keySet()) {
                    Log.d("MainActivity", "Intent extra - key: " + key + 
                          ", value: " + String.valueOf(data.getExtras().get(key)));
                }
            }
        }
        
        // Передаем результат текущему фрагменту
        if (currentFragment != null) {
            currentFragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}

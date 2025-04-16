package com.example.cooking.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.NavOptions;

import com.example.cooking.R;
import com.example.cooking.ui.viewmodels.MainViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Главная активность приложения.
 * Отвечает за управление фрагментами и нижней навигацией.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ADD_RECIPE = 100;

    // UI компоненты
    public BottomNavigationView bottomNavigationView;
    private FloatingActionButton addButton;

    // Навигация
    private NavController navController;

    // Данные и состояние
    private MainViewModel viewModel;

    /**
     * Вызывается при создании активности.
     * Инициализирует интерфейс и настраивает навигацию.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        initViews();
        setupNavigation(savedInstanceState);
        setupEventHandlers();
        setupObservers();

        viewModel.initGoogleSignIn(getString(R.string.default_web_client_id));

        // Устанавливаем цвет статус-бара программно
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(R.color.md_theme_surfaceContainer));
        }
    }

    /**
     * Инициализирует UI компоненты
     */
    private void initViews() {
        // Инициализируем кнопку добавления рецепта
        addButton = findViewById(R.id.fab_add);

        // Инициализируем нижнюю навигацию
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    /**
     * Настраивает навигацию с использованием Navigation Component
     */
    private void setupNavigation(Bundle savedInstanceState) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Убираем стандартную привязку
            // NavigationUI.setupWithNavController(bottomNavigationView, navController);

            // Следим за изменениями пункта назначения для обновления UI (оставляем для
            // кнопки)
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                boolean showAddButton = id != R.id.nav_profile &&
                        id != R.id.destination_profile &&
                        id != R.id.destination_auth &&
                        id != R.id.destination_settings;
                viewModel.setShowAddButton(showAddButton);
            });

            // Добавляем умный ручной обработчик
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int destinationId = item.getItemId();

                // Опции навигации для сохранения состояния
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true) // Не перезапускать, если уже наверху стека
                        .setRestoreState(true) // Восстановить состояние при возврате
                        // Перейти к началу *основного* графа, не включая его, сохранив стек текущей
                        // вкладки
                        .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                        .build();

                try {
                    navController.navigate(destinationId, null, options);
                    return true; // Успешно перешли
                } catch (IllegalArgumentException e) {
                    // Если destinationId не найден в текущем графе (редко, но возможно)
                    Log.e(TAG, "Не удалось найти пункт назначения: " + item.getTitle(), e);
                    return false;
                }
            });

            // Устанавливаем начальный выбранный элемент (например, Главная)
            // Это может быть не нужно, если startDestination графа совпадает с элементом
            // меню
            if (savedInstanceState == null) { // Только при первом запуске
                bottomNavigationView.setSelectedItemId(navController.getGraph().getStartDestinationId());
            }
        }
    }

    /**
     * Настраивает обработчики событий
     */
    private void setupEventHandlers() {
        // Настраиваем кнопку добавления рецепта
        addButton.setOnClickListener(view -> {
            handleAddButtonClick();
        });
    }

    /**
     * Настраивает наблюдателей LiveData из ViewModel
     */
    private void setupObservers() {
        // Наблюдатель для видимости кнопки добавления
        viewModel.getShowAddButton().observe(this, show -> {
            if (show) {
                addButton.show();
            } else {
                addButton.hide();
            }
        });

        // Наблюдатель для состояния авторизации
        viewModel.getIsUserLoggedIn().observe(this, isLoggedIn -> {
            Log.d(TAG, "Состояние авторизации изменилось: " + isLoggedIn);

            // Если мы сейчас на экранах профиля и статус авторизации изменился
            if (navController.getCurrentDestination() != null) {
                int currentDestId = navController.getCurrentDestination().getId();

                if (currentDestId == R.id.nav_profile) {
                    // Перезагружаем текущий фрагмент профиля
                    navController.navigate(R.id.nav_profile);
                }
            }
        });

        // Наблюдатель для события выхода
        viewModel.getLogoutEvent().observe(this, ignored -> {
            Log.d(TAG, "Получено событие выхода");

            // При выходе перезагружаем экран профиля
            if (navController.getCurrentDestination() != null) {
                int currentDestId = navController.getCurrentDestination().getId();

                if (currentDestId == R.id.nav_profile ||
                        currentDestId == R.id.destination_profile ||
                        currentDestId == R.id.destination_settings) {
                    // Возвращаемся к корневому экрану профиля
                    navController.navigate(R.id.nav_profile);
                }
            }
        });
    }

    /**
     * Обрабатывает нажатие на кнопку добавления рецепта
     */
    private void handleAddButtonClick() {
        Log.d(TAG, "Нажата кнопка добавления рецепта");
        Intent intent = new Intent(this, AddRecipeActivity.class);
        startActivityForResult(intent, REQUEST_ADD_RECIPE);
    }

    /**
     * Управляет переходом к домашнему фрагменту
     */
    public void navigateToHomeFragment() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    /**
     * Управляет переходом к экрану авторизации
     */
    public void navigateToAuthScreen() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        if (navController.getCurrentDestination().getId() == R.id.nav_profile) {
            navController.navigate(R.id.action_sharedProfile_to_auth);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_RECIPE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Получен результат: Рецепт добавлен");
                Toast.makeText(this, "Рецепт успешно добавлен", Toast.LENGTH_SHORT).show();
                refreshHomeFragment();
            } else {
                Log.d(TAG, "Получен результат: Отмена добавления рецепта");
            }
        }
    }

    /**
     * Обновляет домашний фрагмент при возврате из AddRecipeActivity
     */
    private void refreshHomeFragment() {
        // Проверяем, находимся ли мы на домашнем фрагменте
        if (bottomNavigationView.getSelectedItemId() == R.id.nav_home) {
            // Если да, обновляем через Navigation Component
            int currentId = navController.getCurrentDestination().getId();
            if (currentId == R.id.nav_home) {
                // Перезагружаем текущий фрагмент
                navController.navigate(R.id.nav_home);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
}

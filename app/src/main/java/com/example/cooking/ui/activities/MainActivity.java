package com.example.cooking.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.cooking.R;
import com.example.cooking.ui.fragments.AuthBlockFragment;
import com.example.cooking.ui.fragments.AuthFragment;
import com.example.cooking.ui.fragments.FavoritesFragment;
import com.example.cooking.ui.fragments.HomeFragment;
import com.example.cooking.ui.fragments.ProfileFragment;
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
    
    // Фрагменты
    private HomeFragment homeFragment;
    private FavoritesFragment favoritesFragment;
    private ProfileFragment profileFragment;
    private AuthFragment authFragment;
    
    // Текущий активный фрагмент
    private Fragment activeFragment;
    
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
        
        // Инициализируем ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        // Инициализируем UI компоненты
        initViews();
        
        // Инициализируем фрагменты
        initFragments(savedInstanceState);
        
        // Настраиваем обработчики событий
        setupEventHandlers();
        
        // Настраиваем наблюдателей LiveData
        setupObservers();
        
        // Инициализируем Google Sign In
        viewModel.initGoogleSignIn(getString(R.string.default_web_client_id));
        
        // Загружаем начальный фрагмент, если это первый запуск
        if (savedInstanceState == null) {
            loadInitialFragment();
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
     * Инициализирует фрагменты
     */
    private void initFragments(Bundle savedInstanceState) {
        // Создаем фрагменты только если это первый запуск
        if (savedInstanceState == null) {
            Log.d(TAG, "Первый запуск, инициализация фрагментов");
            homeFragment = new HomeFragment();
            favoritesFragment = new FavoritesFragment();
            
            // НЕ создаем сразу ProfileFragment и AuthFragment
            // Они будут созданы при переходе на вкладку профиля
            
            // Устанавливаем связь между фрагментами для синхронизации
            FavoritesFragment.setHomeFragment(homeFragment);
            
            // Добавляем основные фрагменты в контейнер, но профильные НЕ добавляем сразу
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setReorderingAllowed(true); // Оптимизация для анимации
            
            // Добавляем только основные фрагменты в контейнер
            transaction.add(R.id.fragment_container, homeFragment, "home");
            transaction.add(R.id.fragment_container, favoritesFragment, "favorites").hide(favoritesFragment);
            
            // Проверяем состояние авторизации, но фрагменты профиля не добавляем
            boolean isLoggedIn = viewModel.isUserLoggedIn();
            Log.d(TAG, "Состояние авторизации при инициализации: " + isLoggedIn);
            
            // Применяем транзакцию
            try {
                transaction.commit();
                Log.d(TAG, "Транзакция добавления фрагментов выполнена успешно");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при добавлении фрагментов: " + e.getMessage(), e);
                transaction.commitAllowingStateLoss();
            }
            
            // Устанавливаем начальный активный фрагмент
            activeFragment = homeFragment;
            Log.d(TAG, "Установлен начальный активный фрагмент: HomeFragment");
        } else {
            // Восстанавливаем фрагменты по тегам
            Log.d(TAG, "Восстановление состояния фрагментов после пересоздания активности");
            FragmentManager fragmentManager = getSupportFragmentManager();
            homeFragment = (HomeFragment) fragmentManager.findFragmentByTag("home");
            favoritesFragment = (FavoritesFragment) fragmentManager.findFragmentByTag("favorites");
            profileFragment = (ProfileFragment) fragmentManager.findFragmentByTag("profile");
            authFragment = (AuthFragment) fragmentManager.findFragmentByTag("auth");
            
            // Устанавливаем связь между фрагментами снова, если она потерялась
            if (homeFragment != null) {
                 FavoritesFragment.setHomeFragment(homeFragment);
            }

            // Находим активный фрагмент
            activeFragment = null; // Сбрасываем на всякий случай
            if (homeFragment != null && homeFragment.isVisible()) {
                activeFragment = homeFragment;
                Log.d(TAG, "Восстановлен активный фрагмент: HomeFragment");
            } else if (favoritesFragment != null && favoritesFragment.isVisible()) {
                activeFragment = favoritesFragment;
                Log.d(TAG, "Восстановлен активный фрагмент: FavoritesFragment");
            } else if (profileFragment != null && profileFragment.isVisible()) {
                activeFragment = profileFragment;
                Log.d(TAG, "Восстановлен активный фрагмент: ProfileFragment");
            } else if (authFragment != null && authFragment.isVisible()) {
                activeFragment = authFragment;
                Log.d(TAG, "Восстановлен активный фрагмент: AuthFragment");
            }
            
            // Если не удалось найти видимый фрагмент, возможно, произошла ошибка 
            // или ни один фрагмент еще не стал видимым. Попробуем определить по ID.
             if (activeFragment == null) {
                 Log.w(TAG, "Не удалось найти видимый фрагмент при восстановлении, пытаемся определить по ID");
                 int selectedItemId = bottomNavigationView.getSelectedItemId();
                 if (selectedItemId == R.id.nav_home) {
                     activeFragment = homeFragment;
                     Log.d(TAG, "Установлен активный фрагмент по ID: HomeFragment");
                 } else if (selectedItemId == R.id.nav_favorites) {
                     activeFragment = favoritesFragment;
                     Log.d(TAG, "Установлен активный фрагмент по ID: FavoritesFragment");
                 } else if (selectedItemId == R.id.nav_profile) {
                     // При восстановлении нужно проверить реальное состояние авторизации
                     viewModel.checkAuthState(); // Убедимся, что состояние актуально
                     boolean isLoggedIn = viewModel.isUserLoggedIn();
                     Log.d(TAG, "Состояние авторизации при восстановлении: " + isLoggedIn);
                     
                     // Явно создаем фрагменты, если они не существуют
                     if (isLoggedIn) {
                         if (profileFragment == null) {
                             profileFragment = new ProfileFragment();
                         }
                         activeFragment = profileFragment;
                     } else {
                         if (authFragment == null) {
                             authFragment = new AuthFragment();
                         }
                         activeFragment = authFragment;
                     }
                     
                     Log.d(TAG, "Установлен активный фрагмент: " + (isLoggedIn ? "ProfileFragment" : "AuthFragment"));
                     
                     // Форсированно вызываем updateProfileFragment чтобы убедиться в правильности отображения
                     updateProfileFragment(isLoggedIn);
                 }
             }
             
             Log.d(TAG, "Активный фрагмент после восстановления: " + (activeFragment != null ? activeFragment.getClass().getSimpleName() : "null"));
        }
    }
    
    /**
     * Настраивает обработчики событий
     */
    private void setupEventHandlers() {
        // Настраиваем навигацию
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            // Переключаем фрагмент только если он не совпадает с текущим
            if (bottomNavigationView.getSelectedItemId() != itemId) {
                 viewModel.setSelectedNavigationItem(itemId);
                 switchFragment(itemId);
            }
            
            return true;
        });
        
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
        
        // Наблюдатель для выбранного пункта меню
        viewModel.getSelectedNavigationItem().observe(this, itemId -> {
            if (itemId != null && bottomNavigationView.getSelectedItemId() != itemId) {
                bottomNavigationView.setSelectedItemId(itemId);
            }
        });
        
        // Наблюдатель для состояния авторизации (реагирует на вход/проверку состояния)
        viewModel.getIsUserLoggedIn().observe(this, isLoggedIn -> {
            Log.d(TAG, "Состояние авторизации изменилось: " + isLoggedIn);
            // Если выбран профиль, обновляем его (показываем Profile или Auth)
            if (bottomNavigationView.getSelectedItemId() == R.id.nav_profile) {
                 Log.d(TAG, "Вкладка 'Профиль' активна, вызываем updateProfileFragment");
                updateProfileFragment(isLoggedIn);
            }
            // Нет необходимости обрабатывать выход здесь, т.к. есть logoutEvent
        });
        
        // Наблюдатель для события выхода
        viewModel.getLogoutEvent().observe(this, ignored -> {
            Log.d(TAG, "Получено событие выхода, вызываем updateProfileFragment(false)");
            
            // При выходе используем метод полного пересоздания фрагментов 
            // для предотвращения проблем с UI
            recreateProfileFragments(false);
        });
        
        // Наблюдатель для сообщений об ошибках
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Загружает начальный фрагмент
     */
    private void loadInitialFragment() {
        // Проверяем наличие флага для показа фрагмента авторизации
        boolean showAuthFragment = getIntent().getBooleanExtra("show_auth_fragment", false);
        
        if (showAuthFragment) {
            // Показываем фрагмент авторизации
            viewModel.setSelectedNavigationItem(R.id.nav_profile);
        } else {
            // По умолчанию показываем домашний фрагмент
            viewModel.setSelectedNavigationItem(R.id.nav_home);
        }
    }
    
    /**
     * Переключает фрагменты с сохранением их состояний
     */
    private void switchFragment(int itemId) {
        Fragment selectedFragment = null;
        String selectedTag = null;
        
        if (itemId == R.id.nav_home) {
            selectedFragment = homeFragment;
            selectedTag = "home";
        } else if (itemId == R.id.nav_favorites) {
            selectedFragment = favoritesFragment;
            selectedTag = "favorites";
        } else if (itemId == R.id.nav_profile) {
            // Выбираем ProfileFragment или AuthFragment в зависимости от состояния авторизации
            boolean isLoggedIn = viewModel.isUserLoggedIn();
            Log.d(TAG, "switchFragment на профиль, статус авторизации: " + isLoggedIn);
            
            // Вместо обычного переключения, используем специальный метод для вкладки профиля
            updateProfileFragment(isLoggedIn);
            // Здесь можно вернуться, так как updateProfileFragment уже сделал все необходимое
            return;
        }
        
        // Проверяем, что фрагменты не null и selectedFragment отличается от activeFragment
        if (selectedFragment != null && selectedFragment != activeFragment) {
            try {
                FragmentManager fm = getSupportFragmentManager();
                
                // Начинаем транзакцию
                FragmentTransaction transaction = fm.beginTransaction();
                
                // Скрываем все фрагменты в контейнере
                for (Fragment fragment : fm.getFragments()) {
                    if (fragment.getId() == R.id.fragment_container) {
                        transaction.hide(fragment);
                        Log.d(TAG, "Скрыт фрагмент: " + fragment.getTag());
                    }
                }
                
                // Если фрагмент уже добавлен - показываем его
                if (selectedFragment.isAdded()) {
                    transaction.show(selectedFragment);
                    Log.d(TAG, "Показан существующий фрагмент: " + selectedTag);
                } else {
                    // Иначе добавляем его в контейнер
                    transaction.add(R.id.fragment_container, selectedFragment, selectedTag);
                    Log.d(TAG, "Добавлен фрагмент: " + selectedTag);
                }
                
                // Выполняем транзакцию
                transaction.commitNow();
                
                // Обновляем активный фрагмент
                activeFragment = selectedFragment;
                Log.d(TAG, "Обновлен активный фрагмент: " + selectedTag);
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при переключении фрагментов: " + e.getMessage(), e);
                
                try {
                    // Запасной вариант транзакции с commitAllowingStateLoss
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    
                    if (activeFragment != null && activeFragment.isAdded()) {
                        transaction.hide(activeFragment);
                    }
                    
                    if (selectedFragment.isAdded()) {
                        transaction.show(selectedFragment);
                    } else {
                        transaction.add(R.id.fragment_container, selectedFragment, selectedTag);
                    }
                    
                    transaction.commitAllowingStateLoss();
                    activeFragment = selectedFragment;
                    Log.d(TAG, "Fallback: транзакция commitAllowingStateLoss завершена");
                } catch (Exception e2) {
                    Log.e(TAG, "Критическая ошибка при переключении фрагментов: " + e2.getMessage(), e2);
                }
            }
        } else if (selectedFragment == activeFragment) {
            Log.d(TAG, "Попытка переключиться на уже активный фрагмент: " + (selectedFragment != null ? selectedFragment.getClass().getSimpleName() : "null"));
        } else {
            Log.w(TAG, "Не удалось переключить фрагмент: selectedFragment=" + (selectedFragment != null ? selectedFragment.getClass().getSimpleName() : "null") + ", activeFragment=" + (activeFragment != null ? activeFragment.getClass().getSimpleName() : "null"));
        }
    }
    
    /**
     * Возвращает тег для фрагмента
     */
    private String getFragmentTag(Fragment fragment) {
        if (fragment == homeFragment) return "home";
        if (fragment == favoritesFragment) return "favorites";
        if (fragment == profileFragment) return "profile";
        if (fragment == authFragment) return "auth";
        return fragment.getClass().getSimpleName(); // Запасной вариант
    }
    
    /**
     * Обрабатывает нажатие на кнопку добавления рецепта
     */
    private void handleAddButtonClick() {
        // Проверяем, авторизован ли пользователь
        if (viewModel.isUserLoggedIn()) {
            // Если авторизован, переходим к добавлению рецепта
            Toast.makeText(this, "Добавление нового рецепта", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, AddRecipeActivity.class);
            startActivityForResult(intent, REQUEST_ADD_RECIPE);
        } else {
            // Если не авторизован, показываем сообщение
            Toast.makeText(this, "Для добавления рецептов необходимо войти в аккаунт", Toast.LENGTH_SHORT).show();
            // Переключаемся на вкладку профиля для авторизации
            viewModel.setSelectedNavigationItem(R.id.nav_profile);
        }
    }
    
    /**
     * Обновляет фрагмент профиля в зависимости от состояния авторизации
     * Показывает либо ProfileFragment, либо AuthFragment.
     */
    public void updateProfileFragment(boolean showProfile) {
        Log.d(TAG, "updateProfileFragment вызван. Показать профиль: " + showProfile);

        // Определяем, какой фрагмент будем показывать
        Fragment fragmentToShow;
        String tagToShow;
        
        // Создаем фрагменты, если они еще не созданы
        if (showProfile) {
            if (profileFragment == null) {
                profileFragment = new ProfileFragment();
                Log.d(TAG, "Создан новый ProfileFragment");
            }
            fragmentToShow = profileFragment;
            tagToShow = "profile";
        } else {
            if (authFragment == null) {
                authFragment = new AuthFragment();
                Log.d(TAG, "Создан новый AuthFragment");
            }
            fragmentToShow = authFragment;
            tagToShow = "auth";
        }

        // Если целевой фрагмент уже активен, ничего не делаем
        if (activeFragment == fragmentToShow) {
            Log.d(TAG, "Целевой фрагмент (" + tagToShow + ") уже активен.");
            return;
        }

        try {
            FragmentManager fm = getSupportFragmentManager();
            
            // Начинаем транзакцию
            FragmentTransaction transaction = fm.beginTransaction();
            
            // Скрываем все фрагменты в контейнере
            for (Fragment fragment : fm.getFragments()) {
                if (fragment.getId() == R.id.fragment_container) {
                    transaction.hide(fragment);
                    Log.d(TAG, "Скрыт фрагмент: " + fragment.getTag());
                }
            }
            
            // Если фрагмент уже добавлен - показываем его
            if (fragmentToShow.isAdded()) {
                transaction.show(fragmentToShow);
                Log.d(TAG, "Показан существующий фрагмент: " + tagToShow);
            } else {
                // Иначе добавляем его в контейнер
                transaction.add(R.id.fragment_container, fragmentToShow, tagToShow);
                Log.d(TAG, "Добавлен фрагмент: " + tagToShow);
            }
            
            // Выполняем транзакцию
            transaction.commitNow();
            
            // Обновляем активный фрагмент
            activeFragment = fragmentToShow;
            Log.d(TAG, "Обновлен активный фрагмент: " + tagToShow);
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении ProfileFragment: " + e.getMessage(), e);
            
            // Запасной вариант с новой транзакцией и commitAllowingStateLoss
            try {
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                
                // Удаляем все фрагменты профиля
                Fragment existingAuth = fm.findFragmentByTag("auth");
                Fragment existingProfile = fm.findFragmentByTag("profile");
                
                if (existingAuth != null) {
                    transaction.remove(existingAuth);
                    Log.d(TAG, "Удален существующий AuthFragment");
                }
                
                if (existingProfile != null) {
                    transaction.remove(existingProfile);
                    Log.d(TAG, "Удален существующий ProfileFragment");
                }
                
                // Добавляем нужный фрагмент заново
                transaction.add(R.id.fragment_container, fragmentToShow, tagToShow);
                Log.d(TAG, "Заново добавлен фрагмент: " + tagToShow);
                
                transaction.commitAllowingStateLoss();
                activeFragment = fragmentToShow;
                
            } catch (Exception e2) {
                Log.e(TAG, "Критическая ошибка при обновлении ProfileFragment: " + e2.getMessage(), e2);
            }
        }
    }
    
    /**
     * Полностью пересоздает фрагменты профиля в случае проблем с UI.
     * Метод может быть вызван для решения проблем с наложением UI элементов.
     * @param showProfileFragment true, если нужно показать профиль, false - для экрана авторизации
     */
    private void recreateProfileFragments(boolean showProfileFragment) {
        Log.d(TAG, "Полное пересоздание фрагментов профиля. Показать профиль: " + showProfileFragment);
        
        try {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();
            
            // Удаляем существующие фрагменты профиля и авторизации
            Fragment existingAuth = fm.findFragmentByTag("auth");
            Fragment existingProfile = fm.findFragmentByTag("profile");
            
            if (existingAuth != null) {
                transaction.remove(existingAuth);
                Log.d(TAG, "Удален существующий AuthFragment");
            }
            
            if (existingProfile != null) {
                transaction.remove(existingProfile);
                Log.d(TAG, "Удален существующий ProfileFragment");
            }
            
            // Сбрасываем ссылки на фрагменты
            authFragment = null;
            profileFragment = null;
            
            // Применяем транзакцию удаления
            transaction.commitNow();
            
            // Создаем новый запрашиваемый фрагмент
            Fragment fragmentToShow;
            String tagToShow;
            
            if (showProfileFragment) {
                profileFragment = new ProfileFragment();
                fragmentToShow = profileFragment;
                tagToShow = "profile";
            } else {
                authFragment = new AuthFragment();
                fragmentToShow = authFragment;
                tagToShow = "auth";
            }
            
            // Добавляем новый фрагмент в отдельной транзакции
            transaction = fm.beginTransaction();
            transaction.add(R.id.fragment_container, fragmentToShow, tagToShow);
            transaction.commitNow();
            
            activeFragment = fragmentToShow;
            Log.d(TAG, "Пересоздан и добавлен фрагмент: " + tagToShow);
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при пересоздании фрагментов профиля: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipes, menu);
        return true;
    }
    
    /**
     * Метод для программного переключения на домашний фрагмент с рецептами
     */
    public void navigateToHomeFragment() {
        viewModel.setSelectedNavigationItem(R.id.nav_home);
    }
    
    /**
     * Метод для программного переключения на экран авторизации.
     * Используется ProfileFragment после выхода.
     */
    public void navigateToAuthScreen() {
        Log.d(TAG, "navigateToAuthScreen() вызван (устарел, используется logoutEvent)");
        // Этот метод больше не нужен напрямую, т.к. логика в logoutEvent наблюдателе
        // Можно оставить для обратной совместимости или удалить
        // updateProfileFragment(false);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем состояние авторизации
        viewModel.checkAuthState();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Обрабатываем результат активности через ViewModel
        viewModel.handleActivityResult(requestCode, resultCode);
        
        // Обрабатываем возвращение из активности добавления рецепта
        if (requestCode == REQUEST_ADD_RECIPE) {
            Log.d(TAG, "onActivityResult: Вернулись из AddRecipeActivity, resultCode = " + resultCode);
            
            if (resultCode == RESULT_OK) {
                // Если рецепт был успешно добавлен, показываем сообщение
                Toast.makeText(this, "Рецепт успешно добавлен", Toast.LENGTH_SHORT).show();
                
                // Если текущий фрагмент - HomeFragment, обновляем список рецептов
                refreshHomeFragment();
            }
        }
    }
    
    /**
     * Обновляет HomeFragment
     */
    private void refreshHomeFragment() {
        if (homeFragment != null) {
            homeFragment.refreshRecipes();
        }
    }
}

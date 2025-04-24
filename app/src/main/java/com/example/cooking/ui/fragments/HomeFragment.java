package com.example.cooking.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.Intent;
import android.widget.Toast;

import com.example.cooking.ui.activities.MainActivity;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.adapters.RecipeListAdapter;
import com.example.cooking.ui.activities.AddRecipeActivity;
import com.example.cooking.utils.RecipeSearchService;
import com.example.cooking.ui.viewmodels.HomeViewModel;

import android.widget.SearchView;

/**
 * Фрагмент главного экрана.
 * Отображает сетку рецептов в виде карточек.
 */
public class HomeFragment extends Fragment implements RecipeListAdapter.OnRecipeLikeListener {
    private static final String TAG = "HomeFragment";
    
    private RecyclerView recyclerView;
    private RecipeListAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;
    private MySharedPreferences preferences;
    private String userId;
    private HomeViewModel viewModel;
    
    // Добавляем поля для автоматического обновления
    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;
    private static final long AUTO_REFRESH_INTERVAL = 30000; // 30 секунд
    private boolean autoRefreshEnabled = true;
    
    /**
     * Создает и настраивает представление фрагмента.
     * Инициализирует RecyclerView и загружает рецепты.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Получаем ID пользователя
        preferences = new MySharedPreferences(requireContext());
        userId = preferences.getString("userId", "0");
        
        // Инициализация ViewModel
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        
        // Инициализация views
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        
        // Инициализация и настройка SearchView
        SearchView searchView = view.findViewById(R.id.search_view_main);
        
        // Дополнительная настройка SearchView для обеспечения кликабельности
        searchView.setIconifiedByDefault(false);  // Поле всегда открыто
        searchView.setSubmitButtonEnabled(false); // Убираем кнопку подтверждения для чистоты интерфейса
        
        // Программно удаляем нижнюю линию из SearchView
        int searchPlateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(searchPlateId);
        if (searchPlate != null) {
            searchPlate.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            // Находим EditText внутри SearchView и убираем у него подчеркивание
            int searchSrcTextId = getResources().getIdentifier("android:id/search_src_text", null, null);
            android.widget.EditText searchEditText = searchView.findViewById(searchSrcTextId);
            if (searchEditText != null) {
                searchEditText.setBackground(null);
                searchEditText.setHintTextColor(getResources().getColor(R.color.md_theme_onSurfaceVariant, null));
                searchEditText.setTextColor(getResources().getColor(R.color.md_theme_onSurface, null));
            }
        }
        
        // Обеспечиваем кликабельность по всему полю
        searchView.setOnClickListener(v -> {
            searchView.setIconified(false);
            searchView.requestFocusFromTouch();
        });
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Опционально: поиск при вводе текста
                performSearch(newText);
                return true;
            }
        });
        
        // Настройка RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        // Инициализируем адаптер
        adapter = new RecipeListAdapter(this);
        recyclerView.setAdapter(adapter);
        
        // Настраиваем swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refreshRecipes());
        
        // Наблюдаем за данными из ViewModel
        observeViewModel();
        
        // Инициализируем наблюдение за Shared ViewModel
        if (getActivity() != null) {
            viewModel.observeLikeChanges(getViewLifecycleOwner(), getActivity());
        }
        
        // Загружаем данные при первом запуске, если это необходимо
        viewModel.loadInitialRecipesIfNeeded();
        
        // Инициализируем обработчик для автоматического обновления
        autoRefreshHandler = new Handler(Looper.getMainLooper());
        autoRefreshRunnable = () -> {
            if (autoRefreshEnabled) {
                Log.d(TAG, "Выполняется автоматическое обновление рецептов");
                viewModel.refreshRecipes();
                // Запланировать следующее обновление
                autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL);
            }
        };
        
        // Запускаем автоматическое обновление
        startAutoRefresh();
        
        return view;
    }
    
    /**
     * Настраиваем наблюдение за LiveData из ViewModel
     */
    private void observeViewModel() {
        // Наблюдаем за списком рецептов
        viewModel.getRecipes().observe(getViewLifecycleOwner(), recipes -> {
            if (recipes != null && !recipes.isEmpty()) {
                adapter.submitList(recipes);
                showEmptyView(false);
            } else {
                showEmptyView(true);
            }
        });
        
        // Наблюдаем за состоянием загрузки
        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), isRefreshing -> {
            swipeRefreshLayout.setRefreshing(isRefreshing);
            
            // Показываем прогресс бар только при первой загрузке, когда список пустой
            if (adapter.getItemCount() == 0) {
                progressBar.setVisibility(isRefreshing ? View.VISIBLE : View.GONE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });
        
        // Наблюдаем за сообщениями об ошибках
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showErrorMessage(error);
            }
        });
    }
    
    /**
     * Обработка нажатия на кнопку лайка
     */
    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        // Проверяем, авторизован ли пользователь
        if (userId.equals("0")) {
            // Показываем Toast-сообщение
            Toast.makeText(requireContext(), 
                "Войдите в систему, чтобы добавлять рецепты в избранное", 
                Toast.LENGTH_SHORT).show();
            
            // Для неавторизованного пользователя восстанавливаем исходное состояние чекбокса
            // Эта часть не нужна, так как мы изменили логику в адаптере
            return;
        }
        
        // Только для авторизованных пользователей показываем сообщение об успехе
        String message = isLiked ? 
            "Рецепт добавлен в избранное" : 
            "Рецепт удален из избранного";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        
        // Обновляем состояние в ViewModel (это обновит и локальную базу, и сервер)
        viewModel.updateLikeStatus(recipe, isLiked);

        // TODO: Реализовать обновление статуса лайка в FavoritesFragment через Shared ViewModel
        // Удаляем прямые вызовы статических методов FavoritesFragment
        /*
        if (isLiked) {
            Log.d(TAG, "Добавляем рецепт в FavoritesFragment: " + recipe.getId() + " - " + recipe.getTitle());
            FavoritesFragment.addLikedRecipe(recipe);
        } else {
            Log.d(TAG, "Удаляем рецепт из FavoritesFragment: " + recipe.getId());
            FavoritesFragment.removeLikedRecipe(recipe.getId());
        }
        */
    }
    
    /**
     * Показывает/скрывает сообщение о пустом списке
     */
    private void showEmptyView(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    /**
     * Настройка меню в Action Bar
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    /**
     * Обработка нажатий на пункты меню
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            // Проверяем, авторизован ли пользователь
            if (userId.equals("0")) {
                Toast.makeText(requireContext(), "Вы должны войти в систему, чтобы добавлять рецепты", Toast.LENGTH_SHORT).show();
                // Перенаправляем на экран авторизации
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                }
                return true;
            }
            
            // Запускаем активность для добавления рецепта
            Intent intent = new Intent(getActivity(), AddRecipeActivity.class);
            startActivityForResult(intent, 100);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Включаем меню в Action Bar
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    
    /**
     * Обновляем данные при возвращении к фрагменту
     */
    @Override
    public void onResume() {
        super.onResume();
        // При возвращении к фрагменту запускаем автоматическое обновление
        startAutoRefresh();
        
        // НЕ ЗАГРУЖАЕМ заново при возвращении
        /* Убираем этот блок
        // При возвращении проверяем актуальность данных
        if (adapter.getItemCount() == 0) {
            viewModel.refreshRecipes();
        }
        */
    }
    
    /**
     * Останавливает автоматическое обновление рецептов при уходе с фрагмента
     */
    @Override
    public void onPause() {
        super.onPause();
        // При уходе с фрагмента останавливаем автоматическое обновление
        stopAutoRefresh();
    }
    
    /**
     * Выполняет поиск рецептов
     */
    public void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Если запрос пустой, показываем все рецепты
            viewModel.getRecipes().removeObservers(getViewLifecycleOwner());
            viewModel.getRecipes().observe(getViewLifecycleOwner(), recipes -> {
                if (recipes != null) {
                    adapter.submitList(recipes);
                    showEmptyView(recipes.isEmpty());
                }
            });
        } else {
            // Иначе выполняем поиск
            RecipeSearchService searchService = new RecipeSearchService(requireContext());
            searchService.searchRecipes(query.trim(), new RecipeSearchService.SearchCallback() {
                @Override
                public void onSearchResults(List<Recipe> recipes) {
                    if (recipes != null) {
                        adapter.submitList(recipes);
                        showEmptyView(recipes.isEmpty());
                    } else {
                        showEmptyView(true);
                    }
                }
                
                @Override
                public void onSearchError(String error) {
                    showErrorMessage("Ошибка поиска: " + error);
                }
            });
        }
    }
    
    /**
     * Показывает сообщение об ошибке
     */
    private void showErrorMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Обрабатывает результат запуска активности
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Обработка результата от AddRecipeActivity
        if (requestCode == 100 && resultCode == getActivity().RESULT_OK) {
            // Рецепт был добавлен, обновляем список
            viewModel.refreshRecipes();
        }
        // Обработка результата от RecipeDetailActivity
        else if (requestCode == 200 && resultCode == getActivity().RESULT_OK) {
            // Проверяем, был ли рецепт удален
            if (data != null && data.getBooleanExtra("recipe_deleted", false)) {
                int deletedRecipeId = data.getIntExtra("recipe_id", -1);
                Log.d(TAG, "Получен результат от RecipeDetailActivity, рецепт был удален: " + deletedRecipeId);
                
                // Если ID рецепта указан, обновляем UI немедленно
                if (deletedRecipeId != -1) {
                    // Получаем текущий список и удаляем рецепт
                    List<Recipe> currentList = adapter.getCurrentList();
                    List<Recipe> updatedList = new ArrayList<>(currentList);
                    
                    // Удаляем рецепт с указанным ID
                    updatedList.removeIf(recipe -> recipe.getId() == deletedRecipeId);
                    
                    // Обновляем UI
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            adapter.submitList(updatedList);
                            Log.d(TAG, "Список рецептов обновлен после удаления: " + deletedRecipeId);
                            showEmptyView(updatedList.isEmpty());
                        });
                    }
                }
            } else {
                // Рецепт был отредактирован, обновляем список
                Log.d(TAG, "Получен результат от RecipeDetailActivity, обновляем список рецептов");
                viewModel.refreshRecipes();
            }
        }
    }
    
    /**
     * Обновляет список рецептов.
     * Метод добавлен для совместимости с обновленным MainActivity.
     */
    public void refreshRecipes() {
        refreshData();
    }
    
    /**
     * Обновляет данные во фрагменте, запрашивая свежий список рецептов.
     */
    public void refreshData() {
        // Запускаем обновление данных через ViewModel
        if (viewModel != null) {
            viewModel.refreshRecipes();
        }
    }
    
    /**
     * Запускает автоматическое обновление рецептов
     */
    private void startAutoRefresh() {
        if (autoRefreshEnabled) {
            Log.d(TAG, "Запущено автоматическое обновление рецептов");
            autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL);
        }
    }
    
    /**
     * Останавливает автоматическое обновление рецептов
     */
    private void stopAutoRefresh() {
        Log.d(TAG, "Остановлено автоматическое обновление рецептов");
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }
}
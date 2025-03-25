package com.example.cooking;

import android.os.Bundle;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.Intent;
import android.widget.Toast;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.Recipe.RecipeAdapter;
import com.example.cooking.ServerWorker.AddRecipeActivity;
import com.example.cooking.ServerWorker.RecipeRepository;
import com.example.cooking.ServerWorker.RecipeSearchService;

import android.widget.SearchView;

/**
 * Фрагмент главного экрана.
 * Отображает сетку рецептов в виде карточек.
 */
public class HomeFragment extends Fragment implements RecipeRepository.RecipesCallback {
    private static final String TAG = "HomeFragment";
    
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private RecipeRepository repository;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;

    /**
     * Создает и настраивает представление фрагмента.
     * Инициализирует RecyclerView и загружает рецепты.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Инициализация views
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        
        // Инициализация и настройка SearchView
        SearchView searchView = view.findViewById(R.id.search_view_main);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Опционально: поиск при вводе текста
                // performSearch(newText);
                return true;
            }
        });
        
        // Настройка RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        // Создаем пустой список рецептов
        List<Recipe> recipes = new ArrayList<>();
        
        // Инициализируем адаптер
        adapter = new RecipeAdapter(recipes);
        recyclerView.setAdapter(adapter);
        
        // Инициализируем репозиторий
        repository = new RecipeRepository(requireContext());
        
        // Настраиваем swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadRecipes);
        
        // Загружаем рецепты
        loadRecipes();
        
        return view;
    }
    
    /**
     * Загружает рецепты из репозитория.
     */
    private void loadRecipes() {
        Log.d(TAG, "Начинаем загрузку рецептов");
        showLoading(true);
        repository.getRecipes(this);
    }
    
    /**
     * Вызывается, когда рецепты успешно загружены.
     */
    @Override
    public void onRecipesLoaded(List<Recipe> recipes) {
        Log.d(TAG, "Рецепты загружены, количество: " + recipes.size());
        showLoading(false);
        
        if (recipes.isEmpty()) {
            // Показываем сообщение о пустом списке рецептов
            emptyView.setText("Нет доступных рецептов. Добавьте первый рецепт!");
            showEmptyView(true);
        } else {
            // Обновляем список рецептов
            adapter.updateRecipes(recipes);
            showEmptyView(false);
        }
    }
    
    /**
     * Вызывается при ошибке загрузки рецептов.
     */
    @Override
    public void onDataNotAvailable(String error) {
        Log.e(TAG, "Error loading recipes: " + error);
        showLoading(false);
        
        // Показываем сообщение об ошибке
        emptyView.setText("Ошибка загрузки рецептов: " + error + "\nПопробуйте позже или добавьте рецепт");
        showEmptyView(true);
        
        // Показываем Toast с ошибкой
        if (getContext() != null) {
            Toast.makeText(getContext(), "Ошибка: " + error, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Управляет отображением индикатора загрузки.
     */
    private void showLoading(boolean show) {
        if (swipeRefreshLayout.isRefreshing()) {
            if (!show) {
                swipeRefreshLayout.setRefreshing(false);
            }
        } else {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        
        if (show) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Управляет отображением сообщения о пустом списке.
     */
    private void showEmptyView(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        
        if (show && emptyView.getText().toString().isEmpty()) {
            emptyView.setText(R.string.no_recipes_available);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_recipes, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_recipe) {
            Intent intent = new Intent(getActivity(), AddRecipeActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            refreshFromServer();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Важно для добавления меню во фрагмент
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecipes();
    }

    /**
     * Выполняет поиск рецептов по заданному запросу
     * @param query текст запроса
     */
    public void performSearch(String query) {
        Log.d(TAG, "Выполняется поиск: " + query);
        showLoading(true);
        
        // Проверяем, что репозиторий инициализирован
        if (repository == null) {
            Log.e(TAG, "Ошибка: репозиторий не инициализирован");
            showErrorMessage("Внутренняя ошибка приложения. Пожалуйста, перезапустите.");
            return;
        }
        
        // Создаем сервис поиска
        RecipeSearchService searchService = new RecipeSearchService(repository);
        
        try {
            // Выполняем поиск по заголовку и ингредиентам
            searchService.searchByTitleAndIngredients(query, new RecipeSearchService.SearchCallback() {
                @Override
                public void onSearchResults(List<Recipe> recipes) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            showLoading(false);
                            if (recipes == null || recipes.isEmpty()) {
                                showEmptyView(true);
                                emptyView.setText(getString(R.string.no_search_results, query));
                            } else {
                                showEmptyView(false);
                                adapter.updateRecipes(recipes);
                                Log.d(TAG, "Найдено рецептов: " + recipes.size());
                            }
                        });
                    }
                }

                @Override
                public void onSearchError(String error) {
                    Log.e(TAG, "Ошибка поиска: " + error);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            showLoading(false);
                            showErrorMessage("Ошибка поиска: " + error);
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Неожиданная ошибка при поиске: " + e.getMessage(), e);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showErrorMessage("Неожиданная ошибка при поиске: " + e.getMessage());
                });
            }
        }
    }
    
    /**
     * Показывает сообщение об ошибке
     */
    private void showErrorMessage(String message) {
        showEmptyView(true);
        emptyView.setText(message);
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Принудительно обновляет данные с сервера, очищая кэш
     */
    private void refreshFromServer() {
        if (isAdded()) {
            Toast.makeText(getContext(), "Обновление данных с сервера...", Toast.LENGTH_SHORT).show();
        }
        
        Log.d(TAG, "Принудительное обновление с сервера");
        showLoading(true);
        
        repository.getRecipes(this);
    }
}
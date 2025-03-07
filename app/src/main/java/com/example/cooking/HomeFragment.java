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
        
        try {
            // Сначала пробуем загрузить данные без сети
            boolean useCache = true;
            loadRecipes(useCache);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке кэша: " + e.getMessage());
            showEmptyView(true);
        }
    }

    private void loadRecipes(boolean useCache) {
        Log.d(TAG, "Начинаем загрузку рецептов, использовать кэш: " + useCache);
        showLoading(true);
        
        if (useCache) {
            // Пробуем загрузить только из кэша
            repository.getRecipesFromCache(new RecipeRepository.RecipesCallback() {
                @Override
                public void onRecipesLoaded(List<Recipe> recipes) {
                    Log.d(TAG, "Рецепты загружены из кэша, количество: " + recipes.size());
                    showLoading(false);
                    
                    if (recipes.isEmpty()) {
                        // Если кэш пуст, пробуем загрузить с сервера
                        loadRecipes(false);
                    } else {
                        // Обновляем список рецептов из кэша
                        adapter.updateRecipes(recipes);
                        showEmptyView(false);
                        
                        // Фоново обновляем данные с сервера
                        repository.refreshRecipesInBackground(new RecipeRepository.RecipesCallback() {
                            @Override
                            public void onRecipesLoaded(List<Recipe> updatedRecipes) {
                                if (!updatedRecipes.isEmpty()) {
                                    adapter.updateRecipes(updatedRecipes);
                                }
                            }
                            
                            @Override
                            public void onDataNotAvailable(String error) {
                                // Игнорируем ошибки, т.к. уже показываем данные из кэша
                                Log.w(TAG, "Ошибка фонового обновления: " + error);
                            }
                        });
                    }
                }
                
                @Override
                public void onDataNotAvailable(String error) {
                    // Если кэш недоступен, пробуем загрузить с сервера
                    loadRecipes(false);
                }
            });
        } else {
            // Пробуем загрузить только с сервера
            repository.getRecipesFromServer(this);
        }
    }
} 
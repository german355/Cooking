package com.example.cooking;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.Recipe.RecipeAdapter;
import com.example.cooking.ServerWorker.LikedRecipesRepository;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Фрагмент для отображения избранных (лайкнутых) рецептов пользователя.
 */
public class FavoritesFragment extends Fragment {
    private static final String TAG = "FavoritesFragment";
    
    private LikedRecipesRepository likedRecipesRepository;
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private CircularProgressIndicator progressIndicator;
    private MySharedPreferences preferences;
    
    private String userId;
    private List<Recipe> allLikedRecipes = new ArrayList<>();
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        
        // Получаем ID пользователя из SharedPreferences
        preferences = new MySharedPreferences(requireContext());
        userId = preferences.getString("userId", "0");
        
        // Инициализация UI компонентов
        recyclerView = view.findViewById(R.id.recycler_view_favorites);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_favorites);
        emptyView = view.findViewById(R.id.empty_view_favorites);
        progressIndicator = view.findViewById(R.id.loading_view_favorites);

        List<Recipe> recipes = new ArrayList<>();
        
        // Инициализация адаптера и настройка RecyclerView
        adapter = new RecipeAdapter(recipes);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        // Инициализация и настройка SearchView
        SearchView searchView = view.findViewById(R.id.search_view_favorite);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Реализуем поиск при вводе текста
                if (newText.isEmpty()) {
                    // Если поле пустое, показываем все рецепты
                    adapter.updateRecipes(allLikedRecipes);
                } else {
                    // Иначе выполняем поиск
                    performSearch(newText);
                }
                return true;
            }
        });
        
        // Настройка SwipeRefreshLayout для обновления данных
        //swipeRefreshLayout.setOnRefreshListener(this::refreshLikedRecipes);
        
        // Инициализация репозитория
        likedRecipesRepository = new LikedRecipesRepository(requireContext());
        
        // Загрузка лайкнутых рецептов
        loadLikedRecipes();
        
        return view;
    }
    
    /**
     * Загружает лайкнутые рецепты из репозитория
     */
    private void loadLikedRecipes() {
        showLoading();
        
        likedRecipesRepository.getLikedRecipes(userId, new LikedRecipesRepository.LikedRecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                hideLoading();
                
                // Сохраняем полный список рецептов
                allLikedRecipes = new ArrayList<>(recipes);
                
                // Обновляем UI
                updateRecipesList(recipes);
                
                Log.d(TAG, "Загружено " + recipes.size() + " лайкнутых рецептов");
            }

            @Override
            public void onDataNotAvailable(String error) {
                hideLoading();
                showError("Не удалось загрузить лайкнутые рецепты: " + error);
                
                Log.e(TAG, "Ошибка при загрузке лайкнутых рецептов: " + error);
            }
        });
    }
    
    /**
     * Обновляет лайкнутые рецепты с сервера
     */
    /*private void refreshLikedRecipes() {
        swipeRefreshLayout.setRefreshing(true);
        
        likedRecipesRepository.refreshLikedRecipes(userId, new LikedRecipesRepository.LikedRecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                swipeRefreshLayout.setRefreshing(false);
                
                // Сохраняем полный список рецептов
                allLikedRecipes = new ArrayList<>(recipes);
                
                // Обновляем UI
                updateRecipesList(recipes);
                
                Toast.makeText(getContext(), "Список лайкнутых рецептов обновлен", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Обновлено " + recipes.size() + " лайкнутых рецептов");
            }

            @Override
            public void onDataNotAvailable(String error) {
                swipeRefreshLayout.setRefreshing(false);
                showError("Ошибка обновления списка: " + error);
                
                Log.e(TAG, "Ошибка при обновлении лайкнутых рецептов: " + error);
            }
        });
    }*/
    
    /**
     * Выполняет поиск рецептов по заданному запросу среди лайкнутых рецептов
     * @param query текст запроса
     */
    public void performSearch(String query) {
        Log.d(TAG, "Выполняется поиск в избранном: " + query);
        
        if (allLikedRecipes.isEmpty()) {
            showError("Список лайкнутых рецептов пуст");
            return;
        }
        
        // Поиск по локальному списку рецептов
        List<Recipe> filteredRecipes = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();
        
        for (Recipe recipe : allLikedRecipes) {
            // Проверяем совпадение в названии или ингредиентах
            if (recipe.getTitle().toLowerCase().contains(lowerCaseQuery) || 
                recipe.getIngredients().toLowerCase().contains(lowerCaseQuery)) {
                filteredRecipes.add(recipe);
            }
        }
        
        // Обновляем UI с отфильтрованным списком
        updateRecipesList(filteredRecipes);
        
        // Показываем сообщение о результатах поиска
        if (filteredRecipes.isEmpty()) {
            showError("По запросу \"" + query + "\" ничего не найдено");
        } else {
            Log.d(TAG, "Найдено " + filteredRecipes.size() + " рецептов по запросу: " + query);
        }
    }
    
    /**
     * Обновляет список рецептов в UI
     */
    private void updateRecipesList(List<Recipe> recipes) {
        if (recipes.isEmpty()) {
            showEmptyView();
        } else {
            hideEmptyView();
            adapter.updateRecipes(recipes);
        }
    }
    
    private void showEmptyView() {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
    
    private void hideEmptyView() {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
    
    private void showLoading() {
        progressIndicator.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }
    
    private void hideLoading() {
        progressIndicator.setVisibility(View.GONE);
    }
    
    private void showError(String message) {
        Toast.makeText(getContext(),"Ошибка сети", Toast.LENGTH_SHORT).show();
        emptyView.setText("Нет лайкнутых рецептов\n" + message);
        showEmptyView();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Обновляем данные при возвращении к фрагменту
        loadLikedRecipes();
    }
} 
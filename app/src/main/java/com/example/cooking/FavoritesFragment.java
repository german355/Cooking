package com.example.cooking;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
import com.example.cooking.fragments.EmptyFavoritesFragment;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;

/**
 * Фрагмент для отображения избранных (лайкнутых) рецептов пользователя.
 */
public class FavoritesFragment extends Fragment implements RecipeAdapter.OnRecipeLikeListener {
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
    
    private static final String API_URL = "http://g3.veroid.network:19029";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();
    
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
        adapter = new RecipeAdapter(recipes, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        // Инициализация и настройка SearchView
        SearchView searchView = view.findViewById(R.id.search_view_favorite);
        
        // Дополнительная настройка SearchView для обеспечения кликабельности
        searchView.setIconifiedByDefault(false);  // Поле всегда открыто
        searchView.setSubmitButtonEnabled(false); // Убираем кнопку подтверждения для чистоты интерфейса
        
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
        swipeRefreshLayout.setOnRefreshListener(this::refreshLikedRecipes);
        
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
                
                // Устанавливаем флаг liked для всех рецептов в списке лайкнутых
                for (Recipe recipe : allLikedRecipes) {
                    recipe.setLiked(true);
                }
                
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
    private void refreshLikedRecipes() {
        swipeRefreshLayout.setRefreshing(true);
        
        likedRecipesRepository.getLikedRecipes(userId, new LikedRecipesRepository.LikedRecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                swipeRefreshLayout.setRefreshing(false);
                
                // Сохраняем полный список рецептов
                allLikedRecipes = new ArrayList<>(recipes);
                
                // Устанавливаем флаг liked для всех рецептов в списке лайкнутых
                for (Recipe recipe : allLikedRecipes) {
                    recipe.setLiked(true);
                }
                
                // Обновляем UI
                updateRecipesList(recipes);
                
                // Проверяем, что контекст доступен перед показом Toast
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Список лайкнутых рецептов обновлен", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "Обновлено " + recipes.size() + " лайкнутых рецептов");
            }

            @Override
            public void onDataNotAvailable(String error) {
                swipeRefreshLayout.setRefreshing(false);
                showError("Ошибка обновления списка: " + error);
                
                Log.e(TAG, "Ошибка при обновлении лайкнутых рецептов: " + error);
            }
        });
    }
    
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
            showEmptyFavoritesFragment();
        } else {
            hideEmptyView();
            adapter.updateRecipes(recipes);
        }
    }
    
    /**
     * Показывает фрагмент пустого состояния избранного
     */
    private void showEmptyFavoritesFragment() {
        View rootView = getView();
        if (rootView != null) {
            // Скрываем основной контент
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            
            // Находим и делаем видимым контейнер для пустого состояния
            View emptyContainer = rootView.findViewById(R.id.empty_container_favorites);
            if (emptyContainer != null) {
                emptyContainer.setVisibility(View.VISIBLE);
            }
        } else {
            Log.w(TAG, "Не удалось показать пустой фрагмент: View == null");
        }
    }

    /**
     * Скрывает фрагмент пустого состояния избранного
     */
    private void hideEmptyView() {
        View rootView = getView();
        if (rootView != null) {
            View emptyContainer = rootView.findViewById(R.id.empty_container_favorites);
            if (emptyContainer != null) {
                emptyContainer.setVisibility(View.GONE);
            }
            emptyView.setVisibility(View.GONE); // Убеждаемся, что сообщение об ошибке скрыто
            recyclerView.setVisibility(View.VISIBLE); // Показываем RecyclerView
        } else {
            Log.w(TAG, "Не удалось скрыть пустой вид: View == null");
        }
    }
    
    /**
     * Показывает сообщение об ошибке
     */
    private void showError(String message) {
        View rootView = getView();
        if (rootView != null) {
            hideLoading(); // Скрываем индикатор загрузки
            hideEmptyView(); // Скрываем контейнер пустого состояния
            emptyView.setText(message);
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE); // Скрываем список
        } else {
            Log.w(TAG, "Не удалось показать ошибку: View == null");
            // Показываем Toast как запасной вариант, только если контекст доступен
            if (getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "Не удалось показать сообщение об ошибке: Context == null, сообщение: " + message);
            }
        }
    }
    
    /**
     * Показывает индикатор загрузки
     */
    private void showLoading() {
        View rootView = getView();
        if (rootView != null && progressIndicator != null && recyclerView != null && emptyView != null) {
            progressIndicator.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            View emptyContainer = rootView.findViewById(R.id.empty_container_favorites);
            if (emptyContainer != null) {
                emptyContainer.setVisibility(View.GONE);
            }
        } else {
            Log.w(TAG, "Не удалось показать индикатор загрузки: View или компоненты == null");
        }
    }
    
    /**
     * Скрывает индикатор загрузки
     */
    private void hideLoading() {
        View rootView = getView();
        if (rootView != null && progressIndicator != null) {
            progressIndicator.setVisibility(View.GONE);
            // Отображение RecyclerView или emptyView управляется отдельно
        } else {
            Log.w(TAG, "Не удалось скрыть индикатор загрузки: View или progressIndicator == null");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Загружаем список только если он пуст
        if (adapter.getItemCount() == 0) {
            loadLikedRecipes();
        }
    }

    /**
     * Обработка нажатия на кнопку лайка
     */
    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        Log.d(TAG, "Изменение лайка рецепта id:" + recipe.getId() + " на " + isLiked);
        
        if (!isLiked) {
            // Если рецепт был удален из избранного, удаляем его из списка
            int position = allLikedRecipes.indexOf(recipe);
            if (position != -1) {
                allLikedRecipes.remove(position);
                
                // Обновляем только удаленный элемент, не весь список
                adapter.notifyItemRemoved(position);
                
                // Если список пуст, показываем пустое состояние
                if (allLikedRecipes.isEmpty()) {
                    showEmptyFavoritesFragment();
                }
            }
            
            // Отправляем запрос на сервер без вызова полной перезагрузки
            toggleLikeOnServer(recipe, false);
        } else {
            // Если рецепт добавлен в избранное (что странно в данном фрагменте),
            // просто обновляем состояние на сервере
            toggleLikeOnServer(recipe, true);
        }
    }
    
    /**
     * Отправляет запрос на сервер для изменения статуса "лайк"
     */
    private void toggleLikeOnServer(Recipe recipe, boolean isLiked) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("recipeId", recipe.getId());
            jsonObject.put("userId", userId);
            
            String jsonBody = jsonObject.toString();
            Log.d(TAG, "Отправка запроса лайка: " + jsonBody);
            
            RequestBody body = RequestBody.create(jsonBody, JSON);
            
            Request request = new Request.Builder()
                    .url(API_URL + "/like")
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    Log.e(TAG, "Ошибка сети при изменении лайка", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Ошибка сети: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                
                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Успешный ответ от сервера на изменение лайка");
                    } else {
                        Log.e(TAG, "Ошибка сервера при изменении лайка: " + response.code());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании запроса на изменение лайка", e);
        }
    }
} 
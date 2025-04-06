package com.example.cooking.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cooking.ui.activities.MainActivity;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.adapters.RecipeAdapter;
import com.example.cooking.ui.viewmodels.FavoritesViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    
    // Статическая ссылка на текущий экземпляр фрагмента для взаимодействия между фрагментами
    private static FavoritesFragment currentInstance;
    
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private CircularProgressIndicator progressIndicator;
    
    private String userId;
    private List<Recipe> allLikedRecipes = new ArrayList<>();
    
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();
    
    // Добавление ViewModel
    private FavoritesViewModel viewModel;
    
    // Статический список лайкнутых рецептов для синхронизации между фрагментами
    private static List<Recipe> likedRecipes = new ArrayList<>();
    // Статическая ссылка на HomeFragment для обновления UI
    private static HomeFragment homeFragment;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Сохраняем ссылку на текущий экземпляр фрагмента
        currentInstance = this;
        
        // Получаем ID пользователя из SharedPreferences
        MySharedPreferences preferences = new MySharedPreferences(requireContext());
        userId = preferences.getString("userId", "0");
        
        // Проверяем, авторизован ли пользователь
        if (userId.equals("0")) {
            // Если пользователь не авторизован, показываем AuthBlockFragment
            View authBlockView = inflater.inflate(R.layout.fragment_auth_block, container, false);
            Button loginButton = authBlockView.findViewById(R.id.btn_login);
            
            // Устанавливаем обработчик нажатия на кнопку
            loginButton.setOnClickListener(v -> {
                // Переходим на экран авторизации
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                }
            });
            
            return authBlockView;
        }
        
        // Для авторизованных пользователей показываем обычный интерфейс
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        
        // Инициализация UI компонентов
        recyclerView = view.findViewById(R.id.recycler_view_favorites);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_favorites);
        emptyView = view.findViewById(R.id.empty_view_favorites);
        progressIndicator = view.findViewById(R.id.loading_view_favorites);

        // Инициализация адаптера и настройка RecyclerView
        adapter = new RecipeAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        // Инициализация ViewModel
        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        
        // Наблюдение за данными ViewModel
        observeViewModel();
        
        // Инициализация и настройка SearchView
        SearchView searchView = view.findViewById(R.id.search_view_favorite);
        
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
                viewModel.performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Реализуем поиск при вводе текста
                if (newText.isEmpty()) {
                    // Если поле пустое, загружаем заново все рецепты
                    viewModel.loadLikedRecipes();
                } else {
                    // Иначе выполняем поиск
                    viewModel.performSearch(newText);
                }
                return true;
            }
        });
        
        // Настройка SwipeRefreshLayout для обновления данных
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refreshLikedRecipes());
        
        // Загружаем данные
        viewModel.loadLikedRecipes();
        
        return view;
    }
    
    /**
     * Настраивает наблюдение за данными из ViewModel
     */
    private void observeViewModel() {
        // Наблюдаем за списком избранных рецептов
        viewModel.getLikedRecipes().observe(getViewLifecycleOwner(), recipes -> {
            Log.d(TAG, "Получено обновление LiveData: " + (recipes != null ? recipes.size() : 0) + " рецептов");
            if (recipes != null) {
                allLikedRecipes = new ArrayList<>(recipes); // Обновляем локальный список для совместимости
                updateRecipesList(recipes);
            } else {
                // Если список null, показываем пустой экран
                showEmptyFavoritesFragment();
            }
        });
        
        // Наблюдаем за состоянием загрузки
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showLoading();
            } else {
                hideLoading();
            }
        });
        
        // Наблюдаем за состоянием обновления
        viewModel.getIsRefreshing().observe(getViewLifecycleOwner(), isRefreshing -> {
            swipeRefreshLayout.setRefreshing(isRefreshing);
        });
        
        // Наблюдаем за сообщениями об ошибках
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showError(error);
            }
        });
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
                
                // Показываем фрагмент с пустым состоянием
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.empty_container_favorites, new EmptyFavoritesFragment())
                        .commit();
            } else {
                // Если контейнер не найден, используем стандартное пустое представление
                emptyView.setVisibility(View.VISIBLE);
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
                
                // Удаляем EmptyFavoritesFragment если он показан
                Fragment emptyFragment = getChildFragmentManager().findFragmentById(R.id.empty_container_favorites);
                if (emptyFragment != null) {
                    getChildFragmentManager().beginTransaction()
                        .remove(emptyFragment)
                        .commit();
                }
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
        
        // Обновляем список при возобновлении фрагмента
        if (!userId.equals("0")) {
            viewModel.refreshLikedRecipes();
        }
    }

    /**
     * Обработка нажатия на кнопку лайка
     */
    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        // Проверяем, авторизован ли пользователь
        if (viewModel.getUserId().equals("0")) {
            Toast.makeText(requireContext(), "Войдите в систему, чтобы добавлять рецепты в избранное", Toast.LENGTH_SHORT).show();
            // Перенаправляем на экран авторизации
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_profile);
            }
            return;
        }

        // Только для авторизованных пользователей показываем сообщение об успехе
        String message = isLiked ? 
            "Рецепт добавлен в избранное" : 
            "Рецепт удален из избранного";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

        // Обновляем состояние в ViewModel (это обновит и локальную базу, и сервер)
        viewModel.updateLikeStatus(recipe, isLiked);
        
        // Если лайк был снят, обновляем состояние в HomeFragment
        if (!isLiked) {
            Log.d(TAG, "Рецепт удален из избранного: " + recipe.getId());
            // Удаляем рецепт из статического списка
            removeLikedRecipe(recipe.getId());
            
            // Обновляем UI в HomeFragment
            if (homeFragment != null) {
                Log.d(TAG, "Обновляем состояние в HomeFragment");
                homeFragment.updateRecipeLikeStatus(recipe.getId(), false);
            }
        }
    }
    
    /**
     * Метод для добавления рецепта в список лайкнутых из других фрагментов
     * @param recipe Рецепт, который нужно добавить
     */
    public static void addLikedRecipe(Recipe recipe) {
        if (currentInstance == null || currentInstance.viewModel == null) {
            Log.d(TAG, "FavoritesFragment или его ViewModel не инициализирован");
            return;
        }

        // Получаем текущий список из ViewModel
        List<Recipe> currentRecipes = currentInstance.viewModel.getLikedRecipes().getValue();
        if (currentRecipes == null) {
            currentRecipes = new ArrayList<>();
        }

        // Проверяем, существует ли уже рецепт с таким ID
        boolean exists = false;
        for (Recipe r : currentRecipes) {
            if (r.getId() == recipe.getId()) {
                exists = true;
                break;
            }
        }

        // Если рецепт не найден, добавляем его
        if (!exists) {
            List<Recipe> updatedList = new ArrayList<>(currentRecipes);
            // Создаем копию и устанавливаем флаг лайка
            Recipe recipeCopy = copyRecipe(recipe);
            recipeCopy.setLiked(true);
            updatedList.add(recipeCopy);
            
            // Обновляем LiveData в ViewModel
            currentInstance.viewModel.updateLikedRecipes(updatedList);
            Log.d(TAG, "Рецепт добавлен в ViewModel: " + recipe.getId());
        } else {
            Log.d(TAG, "Рецепт уже существует в ViewModel: " + recipe.getId());
        }
    }
    
    /**
     * Метод для удаления рецепта из списка лайкнутых из других фрагментов
     * @param recipeId ID рецепта, который нужно удалить
     */
    public static void removeLikedRecipe(int recipeId) {
        if (currentInstance == null || currentInstance.viewModel == null) {
            Log.d(TAG, "FavoritesFragment или его ViewModel не инициализирован");
            return;
        }

        // Получаем текущий список из ViewModel
        List<Recipe> currentRecipes = currentInstance.viewModel.getLikedRecipes().getValue();
        if (currentRecipes == null || currentRecipes.isEmpty()) {
            Log.d(TAG, "Список избранных пуст или не инициализирован");
            return;
        }

        List<Recipe> updatedList = new ArrayList<>();
        boolean removed = false;
        for (Recipe r : currentRecipes) {
            if (r.getId() != recipeId) {
                updatedList.add(r);
            } else {
                removed = true;
            }
        }

        // Если рецепт был найден и удален, обновляем LiveData
        if (removed) {
            currentInstance.viewModel.updateLikedRecipes(updatedList);
            Log.d(TAG, "Рецепт удален из ViewModel: " + recipeId);
        } else {
            Log.d(TAG, "Рецепт не найден в ViewModel: " + recipeId);
        }
    }
    
    /**
     * Создает копию объекта Recipe
     */
    private static Recipe copyRecipe(Recipe original) {
        Recipe copy = new Recipe();
        copy.setId(original.getId());
        copy.setTitle(original.getTitle());
        copy.setIngredients(original.getIngredients());
        copy.setInstructions(original.getInstructions());
        copy.setPhoto_url(original.getPhoto_url());
        copy.setCreated_at(original.getCreated_at());
        copy.setUserId(original.getUserId());
        copy.setLiked(original.isLiked()); // Копируем состояние лайка
        return copy;
    }
    
    /**
     * Устанавливает ссылку на HomeFragment для синхронизации
     * @param fragment Экземпляр HomeFragment
     */
    public static void setHomeFragment(HomeFragment fragment) {
        homeFragment = fragment;
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
                    .url(com.example.cooking.config.ServerConfig.getFullUrl("/like"))
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

    /**
     * Обновить данные фрагмента
     */
    public void refreshData() {
        if (viewModel != null && !userId.equals("0")) {
            viewModel.refreshLikedRecipes();
        } else {
            Toast.makeText(requireContext(), "Для просмотра избранных рецептов необходимо войти в аккаунт", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Обрабатывает результат запуска активности
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Обработка результата от RecipeDetailActivity
        if (requestCode == 200 && resultCode == android.app.Activity.RESULT_OK) {
            // Проверяем, был ли рецепт удален
            if (data != null && data.getBooleanExtra("recipe_deleted", false)) {
                int deletedRecipeId = data.getIntExtra("recipe_id", -1);
                Log.d(TAG, "Получен результат от RecipeDetailActivity, рецепт был удален: " + deletedRecipeId);
                
                // Если ID рецепта указан, обновляем UI немедленно
                if (deletedRecipeId != -1) {
                    // Удаляем рецепт из списка избранных
                    removeLikedRecipe(deletedRecipeId);
                    
                    // Получаем текущий список и создаем новый без удаленного рецепта
                    List<Recipe> currentList = adapter.getRecipes();
                    List<Recipe> updatedList = new ArrayList<>();
                    
                    // Копируем все рецепты, кроме удаленного
                    for (Recipe recipe : currentList) {
                        if (recipe.getId() != deletedRecipeId) {
                            updatedList.add(recipe);
                        }
                    }
                    
                    // Обновляем UI
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            adapter.updateRecipes(updatedList);
                            Log.d(TAG, "Список избранных рецептов обновлен после удаления: " + deletedRecipeId);
                            
                            // Проверяем, не пустой ли список
                            if (updatedList.isEmpty()) {
                                showEmptyFavoritesFragment();
                            }
                        });
                    }
                }
            } else {
                // Рецепт был отредактирован или изменен статус лайка, обновляем список
                Log.d(TAG, "Получен результат от RecipeDetailActivity, обновляем список избранных рецептов");
                refreshData();
            }
        }
    }
}
package com.example.cooking.ui.fragments;

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

import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.adapters.RecipeAdapter;
import com.example.cooking.data.repositories.LikedRecipesRepository;
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
    
    // Статическая ссылка на текущий экземпляр фрагмента для взаимодействия между фрагментами
    private static FavoritesFragment currentInstance;
    
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
        // Сохраняем ссылку на текущий экземпляр фрагмента
        currentInstance = this;
        
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
        
        // Проверяем, авторизован ли пользователь
        if (userId.equals("0")) {
            // Если пользователь не авторизован, показываем соответствующее сообщение
            showError("Для просмотра избранных рецептов необходимо войти в аккаунт");
            swipeRefreshLayout.setEnabled(false); // Отключаем обновление свайпом
            return view;
        }
        
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
                
                // Очищаем предыдущие данные
                allLikedRecipes.clear();
                
                // Проверяем каждый рецепт перед добавлением, чтобы избежать дубликатов
                for (Recipe recipe : recipes) {
                    recipe.setLiked(true);
                    
                    // Проверяем, есть ли уже этот рецепт в списке по ID
                    boolean isDuplicate = false;
                    for (Recipe existing : allLikedRecipes) {
                        if (existing.getId() == recipe.getId()) {
                            isDuplicate = true;
                            break;
                        }
                    }
                    
                    if (!isDuplicate) {
                        allLikedRecipes.add(recipe);
                    } else {
                        Log.w(TAG, "Найден дубликат рецепта с id: " + recipe.getId() + ", пропускаем");
                    }
                }
                
                Log.d(TAG, "Загружено " + allLikedRecipes.size() + " лайкнутых рецептов");
                
                // Обновляем UI
                updateRecipesList(allLikedRecipes);
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
        // Проверяем, что репозиторий инициализирован
        if (likedRecipesRepository == null) {
            Log.e(TAG, "Невозможно обновить лайкнутые рецепты: likedRecipesRepository == null");
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            showError("Для просмотра избранных рецептов необходимо войти в аккаунт");
            return;
        }
        
        // Показываем индикатор обновления только если это явное обновление
        swipeRefreshLayout.setRefreshing(true);
        
        // Добавляем логирование для отладки
        Log.d(TAG, "Запрашиваем обновление списка лайкнутых рецептов для пользователя: " + userId);
        
        likedRecipesRepository.getLikedRecipes(userId, new LikedRecipesRepository.LikedRecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                // Скрываем индикатор обновления
                swipeRefreshLayout.setRefreshing(false);
                
                // Очищаем предыдущие данные полностью
                allLikedRecipes.clear();
                
                // Добавляем новые рецепты с проверкой на дубликаты
                for (Recipe recipe : recipes) {
                    recipe.setLiked(true);
                    
                    // Проверяем, есть ли уже этот рецепт в списке по ID
                    boolean isDuplicate = false;
                    for (Recipe existing : allLikedRecipes) {
                        if (existing.getId() == recipe.getId()) {
                            isDuplicate = true;
                            break;
                        }
                    }
                    
                    if (!isDuplicate) {
                        allLikedRecipes.add(recipe);
                    } else {
                        Log.w(TAG, "Найден дубликат рецепта с id: " + recipe.getId() + ", пропускаем");
                    }
                }
                
                Log.d(TAG, "Получено " + recipes.size() + " лайкнутых рецептов, после фильтрации: " + allLikedRecipes.size());
                
                // Обновляем UI с чистым списком
                updateRecipesList(allLikedRecipes);
                
                // Показываем Toast только при явном обновлении (когда пользователь тянет вниз)
                if (swipeRefreshLayout.isRefreshing()) {
                    // Проверяем, что контекст доступен перед показом Toast
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Список лайкнутых рецептов обновлен", Toast.LENGTH_SHORT).show();
                    }
                }
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
        
        // Проверяем, авторизован ли пользователь и инициализирован ли репозиторий
        if (!userId.equals("0") && likedRecipesRepository != null) {
            // Обновляем список только если пользователь авторизован
            refreshLikedRecipes();
        }
    }

    /**
     * Обработка нажатия на кнопку лайка
     */
    @Override
    public void onRecipeLike(Recipe recipe, boolean isLiked) {
        Log.d(TAG, "Изменение лайка рецепта id:" + recipe.getId() + " на " + isLiked);
        
        if (!isLiked) {
            // Отправляем запрос на сервер сначала
            toggleLikeOnServer(recipe, false);
            
            // Создаем новый список, исключая рецепт с удаленным лайком
            List<Recipe> updatedRecipes = new ArrayList<>();
            boolean recipeFound = false;
            
            for (Recipe r : allLikedRecipes) {
                if (r.getId() == recipe.getId()) {
                    recipeFound = true;
                    Log.d(TAG, "Рецепт с id " + recipe.getId() + " найден и будет удален из списка");
                } else {
                    updatedRecipes.add(r);
                }
            }
            
            if (!recipeFound) {
                Log.w(TAG, "Рецепт с id " + recipe.getId() + " не найден в списке allLikedRecipes!");
            }
            
            // Обновляем список и UI
            allLikedRecipes = updatedRecipes;
            Log.d(TAG, "Размер обновленного списка: " + updatedRecipes.size());
            adapter.updateRecipes(updatedRecipes);
            
            // Если список пуст, показываем пустое состояние
            if (allLikedRecipes.isEmpty()) {
                showEmptyFavoritesFragment();
            }
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

    /**
     * Метод для обновления списка лайкнутых рецептов из других фрагментов
     * @param recipe рецепт, который нужно добавить в список лайкнутых
     */
    public static void addLikedRecipe(Recipe recipe) {
        if (currentInstance == null) {
            Log.d(TAG, "Невозможно добавить рецепт в избранное: FavoritesFragment не инициализирован");
            return;
        }
        
        // Проверяем, авторизован ли пользователь
        if (currentInstance.userId.equals("0")) {
            Log.d(TAG, "Невозможно добавить рецепт в избранное: пользователь не авторизован");
            return;
        }
        
        // Проверяем, инициализирован ли репозиторий
        if (currentInstance.likedRecipesRepository == null) {
            Log.d(TAG, "Невозможно добавить рецепт в избранное: likedRecipesRepository не инициализирован");
            return;
        }
        
        Log.d(TAG, "Добавление рецепта в список лайкнутых: " + recipe.getId() + " - " + recipe.getTitle());
        
        // Создаем копию рецепта и устанавливаем флаг лайка
        Recipe recipeCopy = new Recipe();
        recipeCopy.setId(recipe.getId());
        recipeCopy.setTitle(recipe.getTitle());
        recipeCopy.setIngredients(recipe.getIngredients());
        recipeCopy.setInstructions(recipe.getInstructions());
        recipeCopy.setPhoto_url(recipe.getPhoto_url());
        recipeCopy.setCreated_at(recipe.getCreated_at());
        recipeCopy.setUserId(recipe.getUserId());
        recipeCopy.setLiked(true);
        
        // Проверяем, есть ли уже этот рецепт в списке
        boolean alreadyExists = false;
        for (Recipe existing : currentInstance.allLikedRecipes) {
            if (existing.getId() == recipe.getId()) {
                alreadyExists = true;
                break;
            }
        }
        
        // Если рецепта нет в списке, добавляем его
        if (!alreadyExists) {
            currentInstance.allLikedRecipes.add(recipeCopy);
            
            // Если фрагмент видим, обновляем UI
            if (currentInstance.isVisible()) {
                currentInstance.updateRecipesList(currentInstance.allLikedRecipes);
            }
            
            Log.d(TAG, "Рецепт успешно добавлен в список лайкнутых (размер: " + 
                    currentInstance.allLikedRecipes.size() + ")");
        } else {
            Log.d(TAG, "Рецепт уже есть в списке лайкнутых");
        }
    }
} 
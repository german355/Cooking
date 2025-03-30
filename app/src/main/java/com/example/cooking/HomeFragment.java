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
import com.example.cooking.ServerWorker.LikedRecipesRepository;
import com.example.cooking.ServerWorker.RecipeRepository;
import com.example.cooking.ServerWorker.RecipeSearchService;

import android.widget.SearchView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.json.JSONObject;
import okhttp3.Request;

/**
 * Фрагмент главного экрана.
 * Отображает сетку рецептов в виде карточек.
 */
public class HomeFragment extends Fragment implements RecipeRepository.RecipesCallback, RecipeAdapter.OnRecipeLikeListener {
    private static final String TAG = "HomeFragment";
    
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private RecipeRepository repository;
    private LikedRecipesRepository likedRecipesRepository;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;
    private MySharedPreferences preferences;
    private String userId;
    
    private static final String API_URL = "http://g3.veroid.network:19029";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

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
        
        // Обеспечиваем кликабельность по всему полю
        searchView.setOnClickListener(v -> {
            searchView.setIconified(false);
            searchView.requestFocusFromTouch();
        });
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //performSearch(query);
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
        
        // Создаем пустой список рецептов
        List<Recipe> recipes = new ArrayList<>();
        
        // Инициализируем адаптер с обработчиком лайков
        adapter = new RecipeAdapter(recipes, this);
        recyclerView.setAdapter(adapter);
        
        // Инициализируем репозитории
        repository = new RecipeRepository(requireContext());
        likedRecipesRepository = new LikedRecipesRepository(requireContext());
        
        // Настраиваем swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadRecipes);
        
        // Загружаем рецепты
        loadRecipes();
        
        return view;
    }
    
    /**
     * Загружает рецепты из репозитория и отмечает избранные
     */
    private void loadRecipes() {
        Log.d(TAG, "Начинаем загрузку рецептов");
        showLoading(true);
        
        // Загружаем избранные рецепты
        likedRecipesRepository.getLikedRecipes(userId, new LikedRecipesRepository.LikedRecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> likedRecipes) {
                // Создаем список ID избранных рецептов
                final List<Integer> likedIds = new ArrayList<>();
                for (Recipe recipe : likedRecipes) {
                    likedIds.add(recipe.getId());
                }
                
                // Теперь загружаем все рецепты и отмечаем избранные
                repository.getRecipes(new RecipeRepository.RecipesCallback() {
                    @Override
                    public void onRecipesLoaded(List<Recipe> allRecipes) {
                        // Отмечаем избранные рецепты
                        for (Recipe recipe : allRecipes) {
                            if (likedIds.contains(recipe.getId())) {
                                recipe.setLiked(true);
                            }
                        }
                        
                        // Обновляем UI с полным списком рецептов
                        HomeFragment.this.onRecipesLoaded(allRecipes);
                    }
                    
                    @Override
                    public void onDataNotAvailable(String error) {
                        HomeFragment.this.onDataNotAvailable(error);
                    }
                });
            }
            
            @Override
            public void onDataNotAvailable(String error) {
                // Если не удалось загрузить избранные, просто загружаем все рецепты
                Log.e(TAG, "Ошибка загрузки избранных: " + error);
                repository.getRecipes(HomeFragment.this);
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
            Toast.makeText(requireContext(), "Вы должны войти в систему, чтобы добавлять рецепты в избранное", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Мгновенно обновляем UI-состояние (без перезагрузки)
        recipe.setLiked(isLiked);
        
        // Обновляем только измененный элемент, а не весь список
        int position = -1;
        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (adapter.getRecipeAt(i).getId() == recipe.getId()) {
                position = i;
                break;
            }
        }
        
        if (position != -1) {
            adapter.notifyItemChanged(position);
        }
        
        try {
            // Создаем JSON для запроса
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", userId);
            jsonObject.put("recipeId", recipe.getId());
            
            // Отправляем запрос на сервер без вызова refresh
            toggleLikeOnServer(jsonObject.toString(), recipe, isLiked);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании JSON для запроса лайка", e);
            Toast.makeText(requireContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Отправляет запрос на сервер для добавления/удаления лайка
     */
    private void toggleLikeOnServer(String jsonBody, Recipe recipe, boolean isLiked) {
        // Для лайка не нужен полный индикатор загрузки - это быстрая операция
        // showLoading(true);
        
        try {
            // Создаем тело запроса
            RequestBody body = RequestBody.create(jsonBody, JSON);
            
            // Создаем запрос к API
            Request request = new Request.Builder()
                    .url(API_URL + "/like")
                    .post(body)
                    .build();
            
            // Выполняем запрос асинхронно
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        // showLoading(false);
                        Toast.makeText(requireContext(), 
                                "Ошибка сети: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, e.getMessage());
                        // Возвращаем состояние кнопки
                        recipe.setLiked(!isLiked);
                        adapter.notifyDataSetChanged();
                    });
                }
                
                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    final boolean success = response.isSuccessful();
                    requireActivity().runOnUiThread(() -> {
                        // showLoading(false);
                        if (success) {
                            // Сообщение в зависимости от статуса лайка
                            String message = isLiked ? 
                                    "Рецепт добавлен в избранное" : 
                                    "Рецепт удален из избранного";
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        } else {
                            // Если ошибка, возвращаем состояние кнопки
                            recipe.setLiked(!isLiked);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(requireContext(), 
                                    "Ошибка сервера: " + response.code(), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            // showLoading(false);
            Log.e(TAG, "Ошибка при отправке запроса лайка", e);
            Toast.makeText(requireContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // Возвращаем состояние кнопки
            recipe.setLiked(!isLiked);
            adapter.notifyDataSetChanged();
        }
    }
    
    /**
     * Легкий индикатор загрузки, не скрывающий основной контент.
     * Используется для операций, не требующих перезагрузки всего списка.
     */
    private void showLightLoading(boolean show) {
        if (swipeRefreshLayout.isRefreshing()) {
            if (!show) {
                swipeRefreshLayout.setRefreshing(false);
            }
        } else {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        // В отличие от showLoading, мы не скрываем recyclerView и emptyView
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
        // Добавляем проверку, нужно ли перезагружать рецепты
        // Загружаем рецепты только при первом открытии или после действий, требующих обновления
        if (adapter.getItemCount() == 0) {
            // Загружаем рецепты только если список пуст
            loadRecipes();
        }
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
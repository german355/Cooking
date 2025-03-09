package com.example.cooking;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ServerWorker.RecipeRepository;
import com.example.cooking.ServerWorker.RecipeSearchService;
import java.util.List;

/**
 * Фрагмент для отображения избранных рецептов.
 * (Пока содержит только заглушку)
 */
public class FavoritesFragment extends Fragment {
    private static final String TAG = "FavoritesFragment";
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        
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
                // Опционально: поиск при вводе текста
                // performSearch(newText);
                return true;
            }
        });
        
        return view;
    }
    
    /**
     * Выполняет поиск рецептов по заданному запросу
     * @param query текст запроса
     */
    public void performSearch(String query) {
        Log.d(TAG, "Выполняется поиск в избранном: " + query);
        
        // Заглушка, так как фрагмент еще не реализован полностью
        // Здесь должна быть логика поиска в избранных рецептах
        Toast.makeText(getContext(), "Поиск в избранном: " + query, Toast.LENGTH_SHORT).show();
        
        // Когда функциональность избранного будет реализована, 
        // здесь должен быть код поиска, похожий на HomeFragment:
        /*
        RecipeRepository repository = new RecipeRepository(getContext());
        RecipeSearchService searchService = new RecipeSearchService(repository);
        
        searchService.searchByTitleAndIngredients(query, new RecipeSearchService.SearchCallback() {
            @Override
            public void onSearchResults(List<Recipe> recipes) {
                // Обработка результатов поиска
            }

            @Override
            public void onSearchError(String error) {
                // Обработка ошибок
            }
        });
        */
    }
} 
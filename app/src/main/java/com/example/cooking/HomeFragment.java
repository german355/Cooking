package com.example.cooking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Фрагмент главного экрана.
 * Отображает сетку рецептов в виде карточек.
 */
public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;

    /**
     * Создает и настраивает представление фрагмента.
     * Инициализирует RecyclerView и заполняет его рецептами.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        
        List<Recipe> recipes = new ArrayList<>();
        // Добавляем больше тестовых данных для демонстрации сетки
        recipes.add(new Recipe("Борщ"));
        recipes.add(new Recipe("Пельмени"));
        recipes.add(new Recipe("Салат"));
        recipes.add(new Recipe("Суп"));
        recipes.add(new Recipe("Плов"));
        recipes.add(new Recipe("Котлеты"));
        recipes.add(new Recipe("Пицца"));
        recipes.add(new Recipe("Паста"));
        
        adapter = new RecipeAdapter(recipes);
        recyclerView.setAdapter(adapter);
        
        return view;
    }
} 
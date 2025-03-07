package com.example.cooking.Recipe;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cooking.R;

import java.util.List;

/**
 * Адаптер для отображения рецептов в RecyclerView.
 * Управляет отображением карточек рецептов и обработкой нажатий.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    private List<Recipe> recipes;

    public RecipeAdapter(List<Recipe> recipes) {
        this.recipes = recipes;
    }

    /**
     * Создает новый ViewHolder для карточки рецепта
     */
    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recipe_card, parent, false);
        return new RecipeViewHolder(view);
    }

    /**
     * Заполняет карточку данными конкретного рецептa
     *
     */
    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.titleTextView.setText(recipe.getTitle());
        
        // Устанавливаем обработчик нажатий на карточку
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), RecipeDetailActivity.class);
            intent.putExtra("recipe_id", recipe.getId());
            intent.putExtra("recipe_title", recipe.getTitle());
            intent.putExtra("recipe_ingredients", recipe.getIngredients());
            intent.putExtra("recipe_instructions", recipe.getInstructions());
            intent.putExtra("Created_at", recipe.getCreated_at());
            intent.putExtra("userId", recipe.getUserId());
            Log.d("Id", recipe.getUserId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        this.recipes.clear();
        this.recipes.addAll(newRecipes);
        notifyDataSetChanged();
    }

    /**
     * Внутренний класс для хранения ссылок на элементы карточки
     */
    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        ImageView imageView;
        CardView cardView;

        RecipeViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.recipe_title);
            imageView = itemView.findViewById(R.id.recipe_image);
            cardView = itemView.findViewById(R.id.recipe_card);
        }
    }
} 
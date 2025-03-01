package com.example.cooking;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
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
                .inflate(R.layout.recipe_card_item, parent, false);
        return new RecipeViewHolder(view);
    }

    /**
     * Заполняет карточку данными конкретного рецепта
     */
    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.titleTextView.setText(recipe.getTitle());
        holder.imageView.setImageResource(R.drawable.ic_food_placeholder);

        // Добавляем обработчик нажатия
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_TITLE, recipe.getTitle());
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_CREATOR, recipe.getCreator());
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_INSTRUCTOR, recipe.getInstructor());
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_FOOD, recipe.getFood());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    /**
     * Внутренний класс для хранения ссылок на элементы карточки
     */
    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView imageView;
        TextView titleTextView;

        RecipeViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            imageView = itemView.findViewById(R.id.recipe_image);
            titleTextView = itemView.findViewById(R.id.recipe_title);
        }
    }
} 
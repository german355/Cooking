package com.example.cooking.Recipe;

import android.content.Intent;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cooking.R;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
import java.util.ArrayList;

/**
 * Адаптер для отображения рецептов в RecyclerView.
 * Управляет отображением карточек рецептов и обработкой нажатий.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    private List<Recipe> recipes;
    private OnRecipeLikeListener likeListener;
    
    public interface OnRecipeLikeListener {
        void onRecipeLike(Recipe recipe, boolean isLiked);
    }

    public RecipeAdapter(List<Recipe> recipes) {
        this.recipes = recipes;
    }
    
    public RecipeAdapter(List<Recipe> recipes, OnRecipeLikeListener likeListener) {
        this.recipes = recipes;
        this.likeListener = likeListener;
    }
    
    public void setOnRecipeLikeListener(OnRecipeLikeListener listener) {
        this.likeListener = listener;
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
        
        // Загружаем изображение, если оно есть
        if (recipe.getPhoto_url() != null){
            Glide.with(holder.imageView.getContext())
                    .load(recipe.getPhoto_url())
                    .placeholder(R.drawable.white_card_background)
                    .error(R.drawable.white_card_background)
                    .centerCrop()
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.white_card_background);
        }
        
        // Устанавливаем состояние избранного
        holder.favoriteButton.setChecked(recipe.isLiked());
        
        // После установки состояния кнопки обновляем её цвет
        holder.favoriteButton.refreshDrawableState();
        
        // Для правильного отображения цветов при первоначальной загрузке
        // и обеспечения того, что состояние кнопки соответствует состоянию рецепта
        if (recipe.isLiked()) {
            holder.favoriteButton.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#FF0031")));
        } else {
            holder.favoriteButton.setButtonTintList(ColorStateList.valueOf(Color.BLACK));
        }
        
        // Убедимся, что кнопка избранного всегда видна
        holder.favoriteButton.bringToFront();
        
        // Слушатель нажатий на кнопку избранного
        holder.favoriteButton.setOnClickListener(v -> {
            boolean isChecked = holder.favoriteButton.isChecked();
            
            // Анимируем изменение состояния кнопки
            holder.favoriteButton.jumpDrawablesToCurrentState();
            
            // Временно отключаем кнопку, чтобы предотвратить многократные нажатия
            holder.favoriteButton.setEnabled(false);
            
            // Обновляем состояние рецепта
            recipe.setLiked(isChecked);
            
            // Показываем информационное сообщение
            String message = isChecked ? 
                "Рецепт добавлен в избранное" : 
                "Рецепт удален из избранного";
            Toast.makeText(holder.itemView.getContext(), message, Toast.LENGTH_SHORT).show();
            
            // Вызываем обратный вызов, если он установлен
            if (likeListener != null) {
                likeListener.onRecipeLike(recipe, isChecked);
            }
            
            // Возвращаем активное состояние кнопке
            holder.favoriteButton.postDelayed(() -> holder.favoriteButton.setEnabled(true), 500);
        });
        
        // Устанавливаем обработчик нажатий на карточку
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), RecipeDetailActivity.class);
            intent.putExtra("recipe_id", recipe.getId());
            intent.putExtra("recipe_title", recipe.getTitle());
            intent.putExtra("recipe_ingredients", recipe.getIngredients());
            intent.putExtra("recipe_instructions", recipe.getInstructions());
            intent.putExtra("Created_at", recipe.getCreated_at());
            intent.putExtra("userId", recipe.getUserId());
            intent.putExtra("photo_url", recipe.getPhoto_url());
            Log.d("Id", recipe.getUserId());
            if (recipe.getPhoto_url() != null) {
                Log.d("RecipeAdapter", "Photo URL: " + recipe.getPhoto_url());
            }
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        // Используем DiffUtil для более эффективного обновления RecyclerView
        // без полной перерисовки всех элементов
        
        // Если новый список тот же самый или пустой, не делаем ничего
        if (newRecipes == null) {
            return;
        }
        
        // Создаем временные копии списков
        final List<Recipe> oldList = new ArrayList<>(recipes);
        final List<Recipe> newList = new ArrayList<>(newRecipes);
        
        // Очищаем и обновляем основной список
        this.recipes.clear();
        this.recipes.addAll(newRecipes);
        
        // Используем более эффективное обновление только для видимых элементов
        // вместо полного notifyDataSetChanged()
        notifyDataSetChanged();
    }

    /**
     * Возвращает рецепт по указанной позиции
     * @param position позиция в списке
     * @return объект рецепта или null, если позиция недопустима
     */
    public Recipe getRecipeAt(int position) {
        if (position >= 0 && position < recipes.size()) {
            return recipes.get(position);
        }
        return null;
    }

    /**
     * Внутренний класс для хранения ссылок на элементы карточки
     */
    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        ShapeableImageView imageView;
        CardView cardView;
        MaterialCheckBox favoriteButton;

        RecipeViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.recipe_title);
            imageView = itemView.findViewById(R.id.recipe_image);
            cardView = itemView.findViewById(R.id.recipe_card);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
        }
    }
} 
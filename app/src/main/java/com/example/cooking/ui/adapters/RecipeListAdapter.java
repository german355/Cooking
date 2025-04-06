package com.example.cooking.ui.adapters;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cooking.R;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.ui.activities.RecipeDetailActivity;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * Адаптер для отображения рецептов в RecyclerView с использованием DiffUtil.
 * Обеспечивает плавное обновление списка рецептов.
 */
public class RecipeListAdapter extends ListAdapter<Recipe, RecipeListAdapter.RecipeViewHolder> {
    
    private static final String TAG = "RecipeListAdapter";
    private final OnRecipeLikeListener likeListener;

    // DiffUtil для эффективного обновления RecyclerView
    private static final DiffUtil.ItemCallback<Recipe> DIFF_CALLBACK = new DiffUtil.ItemCallback<Recipe>() {
        @Override
        public boolean areItemsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            // Проверяем, тот же ли это рецепт по ID
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            // Проверяем, изменилось ли содержимое рецепта
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                   oldItem.isLiked() == newItem.isLiked() &&
                   (oldItem.getPhoto_url() == null ? newItem.getPhoto_url() == null :
                    oldItem.getPhoto_url().equals(newItem.getPhoto_url()));
        }
    };

    // Интерфейс для обработки нажатий на кнопку "Нравится"
    public interface OnRecipeLikeListener {
        void onRecipeLike(Recipe recipe, boolean isLiked);
    }

    public RecipeListAdapter(OnRecipeLikeListener likeListener) {
        super(DIFF_CALLBACK);
        this.likeListener = likeListener;
    }

    /**
     * Получает рецепт по позиции
     */
    public Recipe getRecipeAt(int position) {
        return getItem(position);
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recipe_card, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = getItem(position);
        
        // Заполняем данные карточки
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
            
            // Вызываем метод обработки лайка, который проверит авторизацию
            if (likeListener != null) {
                // Сначала сбрасываем состояние чекбокса, чтобы избежать визуального изменения до проверки авторизации
                holder.favoriteButton.setChecked(!isChecked);
                
                // Временно отключаем кнопку для предотвращения повторных нажатий
                holder.favoriteButton.setEnabled(false);
                
                // Вызываем обратный вызов, где будет проверка авторизации
                // и только если действие успешно, изменяем визуальное состояние
                likeListener.onRecipeLike(recipe, isChecked);
                
                // Возвращаем активное состояние кнопке с задержкой
                holder.favoriteButton.postDelayed(() -> holder.favoriteButton.setEnabled(true), 500);
            }
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
            intent.putExtra("isLiked", recipe.isLiked());
            
            if (recipe.getPhoto_url() != null) {
                Log.d(TAG, "Photo URL: " + recipe.getPhoto_url());
            }
            
            // Запускаем активность с ожиданием результата
            ((AppCompatActivity) v.getContext()).startActivityForResult(intent, 200);
        });
    }

    /**
     * ViewHolder для карточки рецепта
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
package com.example.cooking.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cooking.R;
import com.example.cooking.Recipe.Ingredient;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения и редактирования списка ингредиентов в RecyclerView.
 */
public class EditIngredientsAdapter extends RecyclerView.Adapter<EditIngredientsAdapter.IngredientViewHolder> {

    private List<Ingredient> ingredients;
    private final Context context;
    private final IngredientInteractionListener listener;

    /**
     * Интерфейс для взаимодействия с элементами списка ингредиентов.
     */
    public interface IngredientInteractionListener {
        void onIngredientUpdated(int position, Ingredient ingredient);
        void onIngredientRemoved(int position);
    }

    public EditIngredientsAdapter(Context context, IngredientInteractionListener listener) {
        this.context = context;
        this.listener = listener;
        this.ingredients = new ArrayList<>();
    }

    /**
     * Обновляет список ингредиентов и уведомляет об изменении данных.
     */
    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = new ArrayList<>(ingredients);
        notifyDataSetChanged();
    }

    public List<Ingredient> getIngredients() {
        return new ArrayList<>(ingredients);
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient_edit, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        holder.bind(ingredients.get(position), position);
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    public class IngredientViewHolder extends RecyclerView.ViewHolder {
        private final TextInputEditText nameEditText;
        private final TextInputEditText countEditText;
        private final TextInputEditText typeEditText;
        private final ImageButton removeButton;
        
        private Ingredient currentIngredient;
        private int currentPosition;
        
        private TextWatcher nameWatcher;
        private TextWatcher countWatcher;
        private TextWatcher typeWatcher;

        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            nameEditText = itemView.findViewById(R.id.edit_ingredient_name);
            countEditText = itemView.findViewById(R.id.edit_ingredient_count);
            typeEditText = itemView.findViewById(R.id.edit_ingredient_type);
            removeButton = itemView.findViewById(R.id.button_remove_ingredient);
            
            removeButton.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onIngredientRemoved(getAdapterPosition());
                    ingredients.remove(getAdapterPosition());
                    notifyItemRemoved(getAdapterPosition());
                }
            });
        }

        /**
         * Привязывает данные ингредиента к элементам UI.
         */
        public void bind(Ingredient ingredient, int position) {
            currentIngredient = ingredient;
            currentPosition = position;
            
            // Удаляем предыдущие слушатели перед установкой текста
            removeTextWatchers();
            
            // Устанавливаем текущие значения
            nameEditText.setText(ingredient.getName());
            countEditText.setText(ingredient.getCount() > 0 ? String.valueOf(ingredient.getCount()) : "");
            typeEditText.setText(ingredient.getType());
            
            // Скрываем кнопку удаления только для первого ингредиента (индекс 0)
            // Остальные ингредиенты можно удалять, даже если они стали первыми после удаления предыдущих
            if (position == 0) {
                removeButton.setVisibility(View.GONE);
            } else {
                removeButton.setVisibility(View.VISIBLE);
            }
            
            // Добавляем слушатели изменений
            setupTextWatchers();
        }
        
        private void removeTextWatchers() {
            if (nameWatcher != null) nameEditText.removeTextChangedListener(nameWatcher);
            if (countWatcher != null) countEditText.removeTextChangedListener(countWatcher);
            if (typeWatcher != null) typeEditText.removeTextChangedListener(typeWatcher);
        }
        
        private void setupTextWatchers() {
            nameWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (currentIngredient != null) {
                        currentIngredient.setName(s.toString().trim());
                        listener.onIngredientUpdated(currentPosition, currentIngredient);
                    }
                }
            };
            
            countWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (currentIngredient != null) {
                        try {
                            int count = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
                            currentIngredient.setCount(count);
                            listener.onIngredientUpdated(currentPosition, currentIngredient);
                        } catch (NumberFormatException e) {
                            // Игнорируем неверный формат
                        }
                    }
                }
            };
            
            typeWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (currentIngredient != null) {
                        currentIngredient.setType(s.toString().trim());
                        listener.onIngredientUpdated(currentPosition, currentIngredient);
                    }
                }
            };
            
            nameEditText.addTextChangedListener(nameWatcher);
            countEditText.addTextChangedListener(countWatcher);
            typeEditText.addTextChangedListener(typeWatcher);
        }
    }
    
    // Вспомогательный класс для упрощения реализации TextWatcher
    private static abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
} 
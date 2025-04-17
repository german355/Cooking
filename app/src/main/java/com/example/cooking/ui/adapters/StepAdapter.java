package com.example.cooking.ui.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cooking.R;
import com.example.cooking.Recipe.Step;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.Objects;

/**
 * Адаптер для отображения ИЛИ редактирования шагов рецепта в RecyclerView.
 * Использует разные макеты и ViewHolder'ы в зависимости от наличия listener'а.
 */
public class StepAdapter extends ListAdapter<Step, RecyclerView.ViewHolder> {

    private final Context context;
    private final StepUpdateListener listener;

    // View types
    private static final int VIEW_TYPE_DISPLAY = 1;
    private static final int VIEW_TYPE_EDIT = 2;

    /**
     * Интерфейс для обновления шагов в активности (используется только в режиме редактирования).
     */
    public interface StepUpdateListener {
        void onStepUpdated(int position, Step step);
        void onStepRemoved(int position);
    }

    // Конструктор для РЕДАКТИРОВАНИЯ (с listener)
    public StepAdapter(Context context, @NonNull StepUpdateListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
    }

    // Конструктор для ОТОБРАЖЕНИЯ (без listener)
    public StepAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = null; // Явно указываем, что listener'а нет
    }

    @Override
    public int getItemViewType(int position) {
        // Возвращаем тип в зависимости от наличия listener'а
        return listener == null ? VIEW_TYPE_DISPLAY : VIEW_TYPE_EDIT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_EDIT) {
            View view = inflater.inflate(R.layout.item_step_edit, parent, false);
            return new StepEditViewHolder(view, listener); // Передаем listener
        } else { // VIEW_TYPE_DISPLAY
            View view = inflater.inflate(R.layout.item_step, parent, false);
            return new StepDisplayViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Step step = getItem(position);
        if (holder instanceof StepEditViewHolder) {
            ((StepEditViewHolder) holder).bind(step, position);
        } else if (holder instanceof StepDisplayViewHolder) {
            ((StepDisplayViewHolder) holder).bind(step, position + 1); // Display ViewHolder использует номер + 1
        }
    }

    // ViewHolder для ОТОБРАЖЕНИЯ шага рецепта
    static class StepDisplayViewHolder extends RecyclerView.ViewHolder {
        private final TextView stepNumberTextView;
        private final TextView stepInstructionTextView;
        private final ShapeableImageView stepImageView;
        private final LinearLayout buttonContainer;

        public StepDisplayViewHolder(@NonNull View itemView) {
            super(itemView);
            stepNumberTextView = itemView.findViewById(R.id.text_step_number);
            stepInstructionTextView = itemView.findViewById(R.id.edit_step_instruction);
            stepImageView = itemView.findViewById(R.id.step_image);
            buttonContainer = itemView.findViewById(R.id.button_container);
        }

        public void bind(Step step, int number) {
            if (step == null) {
                Log.e("StepDisplayViewHolder", "Получен null шаг");
                stepNumberTextView.setText("Шаг " + number);
                stepInstructionTextView.setText("Ошибка: описание шага отсутствует");
                stepImageView.setVisibility(View.GONE);
                return;
            }

            stepNumberTextView.setText("Шаг " + number);
            stepInstructionTextView.setText(step.getInstruction() != null ? step.getInstruction() : "Нет описания");

            String imageUrl = step.getUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                stepImageView.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(imageUrl).placeholder(R.drawable.placeholder_image).error(R.drawable.error_image).into(stepImageView);
            } else {
                stepImageView.setVisibility(View.GONE);
            }
            // Скрываем контейнер с кнопками в режиме отображения
            if (buttonContainer != null) {
                buttonContainer.setVisibility(View.GONE);
            }
        }
    }

    // ViewHolder для РЕДАКТИРОВАНИЯ шага рецепта
    class StepEditViewHolder extends RecyclerView.ViewHolder {
        private final TextView stepNumberTextView;
        private final EditText stepInstructionEditText;
        private final ImageButton removeButton;
        private final ShapeableImageView stepImageView;
        private final StepUpdateListener listener;
        private boolean isInitializing = true; // Флаг для TextWatcher

        public StepEditViewHolder(@NonNull View itemView, StepUpdateListener listener) {
            super(itemView);
            this.listener = listener;
            stepNumberTextView = itemView.findViewById(R.id.text_step_number);
            stepInstructionEditText = itemView.findViewById(R.id.edit_step_instruction);
            removeButton = itemView.findViewById(R.id.button_remove_step);
            stepImageView = itemView.findViewById(R.id.step_image);

            setupTextWatchers();
            setupRemoveButton();
        }

        void bind(Step step, int position) {
            isInitializing = true;

            stepNumberTextView.setText(String.format("Шаг %d", position + 1));
            stepInstructionEditText.setText(step.getInstruction());

            // Показываем кнопку удаления для всех шагов, кроме первого, если listener есть
            removeButton.setVisibility(position == 0 || listener == null ? View.GONE : View.VISIBLE);

            String imageUrl = step.getUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                stepImageView.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(imageUrl).placeholder(R.drawable.placeholder_image).error(R.drawable.error_image).into(stepImageView);
            } else {
                stepImageView.setVisibility(View.GONE);
            }

            isInitializing = false;
        }

        private void setupTextWatchers() {
            stepInstructionEditText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    // Проверяем isInitializing и listener
                    if (!isInitializing && listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        Step step = getItem(getAdapterPosition()); // Получаем элемент из ListAdapter
                        if (step != null) { // Доп. проверка на null
                            step.setInstruction(s.toString());
                            listener.onStepUpdated(getAdapterPosition(), step);
                        }
                    }
                }
            });
        }

        private void setupRemoveButton() {
            removeButton.setOnClickListener(v -> {
                // Проверяем listener
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onStepRemoved(getAdapterPosition());
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<Step> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Step>() {
                @Override
                public boolean areItemsTheSame(@NonNull Step oldItem, @NonNull Step newItem) {
                    return oldItem.getNumber() == newItem.getNumber();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Step oldItem, @NonNull Step newItem) {
                    return oldItem.getNumber() == newItem.getNumber() &&
                           Objects.equals(oldItem.getInstruction(), newItem.getInstruction()) &&
                           Objects.equals(oldItem.getUrl(), newItem.getUrl());
                }
            };
} 
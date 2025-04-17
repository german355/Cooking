package com.example.cooking.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cooking.R;
import com.example.cooking.Recipe.Step;

import java.util.List;

/**
 * Адаптер для отображения и редактирования списка шагов приготовления в RecyclerView.
 */
public class EditStepsAdapter extends RecyclerView.Adapter<EditStepsAdapter.StepViewHolder> {

    private List<Step> steps;
    private final StepInteractionListener listener;

    /**
     * Интерфейс для взаимодействия с элементами списка шагов.
     */
    public interface StepInteractionListener {
        void onStepChanged(int position, Step step);
        void onStepRemove(int position);
    }

    public EditStepsAdapter(List<Step> steps, StepInteractionListener listener) {
        this.steps = steps;
        this.listener = listener;
    }

    /**
     * Обновляет список шагов и уведомляет об изменении данных.
     */
    public void updateSteps(List<Step> newSteps) {
        this.steps = newSteps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_step_edit, parent, false);
        return new StepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        holder.bind(steps.get(position), position);
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    class StepViewHolder extends RecyclerView.ViewHolder {
        private final TextView stepNumberTextView;
        private final EditText stepInstructionEditText;
        private final ImageButton removeButton;
        private boolean isInitializing = true;

        StepViewHolder(@NonNull View itemView) {
            super(itemView);
            stepNumberTextView = itemView.findViewById(R.id.text_step_number);
            stepInstructionEditText = itemView.findViewById(R.id.edit_step_instruction);
            removeButton = itemView.findViewById(R.id.button_remove_step);

            // Настраиваем текстовое поле и кнопку удаления
            setupTextWatchers();
            setupRemoveButton();
        }

        /**
         * Привязывает данные шага к элементам UI.
         */
        void bind(Step step, int position) {
            isInitializing = true; // Предотвращаем срабатывание слушателей при начальной установке значений

            // Устанавливаем номер шага (позиция + 1)
            stepNumberTextView.setText(String.format("Шаг %d", position + 1));

            // Устанавливаем описание шага
            stepInstructionEditText.setText(step.getInstruction());
            
            // Скрываем кнопку удаления только для первого шага (индекс 0)
            // Остальные шаги можно удалять, даже если они стали первыми после удаления предыдущих
            if (position == 0) {
                removeButton.setVisibility(View.GONE);
            } else {
                removeButton.setVisibility(View.VISIBLE);
            }

            isInitializing = false;
        }

        /**
         * Настраивает TextWatcher для поля ввода описания.
         */
        private void setupTextWatchers() {
            stepInstructionEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!isInitializing && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        int position = getAdapterPosition();
                        Step step = steps.get(position);
                        step.setInstruction(s.toString());
                        listener.onStepChanged(position, step);
                    }
                }
            });
        }

        /**
         * Настраивает кнопку удаления шага.
         */
        private void setupRemoveButton() {
            removeButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onStepRemove(position);
                }
            });
        }
    }
} 
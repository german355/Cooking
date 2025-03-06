package com.example.cooking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

/**
 * Фрагмент для отображения профиля пользователя и настроек приложения.
 * Позволяет пользователю управлять своим аккаунтом и персональными данными.
 */
public class ProfileFragment extends Fragment {
    private TextView nameProfile;
    private MySharedPreferences user;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        user = new MySharedPreferences(requireContext());
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // Находим кнопку выхода из аккаунта и текстовое поле имени
        Button logoutButton = view.findViewById(R.id.logout_button);
        nameProfile = view.findViewById(R.id.profile_name);
        
        nameProfile.setText("Hi, " + user.getString("userName", "User")); // становка имени не работает так как есть проблема с ее записью в бд
        
        // Устанавливаем обработчик нажатия
        logoutButton.setOnClickListener(v -> logoutUser());

        
        return view;
    }
    
    /**
     * Выполняет выход пользователя из аккаунта:
     * 1. Очищает данные пользователя из SharedPreferences
     * 2. Перенаправляет на экран входа
     */
    private void logoutUser() {
        // Очищаем данные пользователя
        user.putBoolean("auth", false);
        user.putString("userId", null);
        user.putString("userName", null);
        
        // Создаем Intent для перехода на экран входа
        Intent intent = new Intent(requireActivity(), StartActivity.class);
        
        // Добавляем флаги, чтобы очистить бэкстек
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // Запускаем экран входа
        startActivity(intent);
        
        // Закрываем текущую активность
        requireActivity().finish();
    }
}
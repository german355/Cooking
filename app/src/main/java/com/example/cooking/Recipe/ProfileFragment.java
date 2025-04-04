package com.example.cooking.Recipe;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.example.cooking.FireBase.FirebaseAuthManager;
import com.example.cooking.FireBase.FirebaseUserManager;
import com.example.cooking.MySharedPreferences;
import com.example.cooking.R;

/**
 * Фрагмент для отображения профиля пользователя и настроек приложения
 *
 * Позволяет пользователю управлять своим аккаунтом и персональными данными
 */
public class ProfileFragment extends Fragment {
    private TextView nameProfile;
    private FirebaseAuthManager authManager;
    private FirebaseUserManager userManager;

    private MySharedPreferences user;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        user = new MySharedPreferences(requireContext());
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // Инициализация Firebase Auth Manager
        authManager = FirebaseAuthManager.getInstance();
        userManager = FirebaseUserManager.getInstance();
        
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
        // Проверяем, инициализирован ли authManager, чтобы избежать NullPointerException
        if (authManager != null) {
            authManager.signOut();
        }

        // Очищаем данные пользователя
        user.clear();
        
        // Заменяем текущий фрагмент на AuthFragment
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new com.example.cooking.fragments.AuthFragment())
                .commit();
                
        // Показываем сообщение о выходе из аккаунта
        Toast.makeText(requireContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
    }


    private void deleteUser(){
        //userManager.deleteAccount();

        user.clear();



    }
}
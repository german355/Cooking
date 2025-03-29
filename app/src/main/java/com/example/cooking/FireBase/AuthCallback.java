package com.example.cooking.FireBase;

import com.google.firebase.auth.FirebaseUser;

/**
 * Интерфейс обратного вызова для операций аутентификации
 */
public interface AuthCallback {
    /**
     * Вызывается при успешной аутентификации
     * @param user объект авторизованного пользователя
     */
    void onSuccess(FirebaseUser user);

    /**
     * Вызывается при ошибке аутентификации
     * @param exception исключение, содержащее информацию об ошибке
     */
    void onError(Exception exception);
} 
package com.example.cooking.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

/**
 * Менеджер для безопасного хранения JWT-токенов в EncryptedSharedPreferences
 */
public class TokenStorage {
    private static final String PREFS_FILE = "token_prefs";
    private static SharedPreferences sharedPreferences;

    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";

    /**
     * Инициализирует EncryptedSharedPreferences. Вызывать один раз в
     * Application.onCreate().
     */
    public static void init(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sharedPreferences = EncryptedSharedPreferences.create(
                    PREFS_FILE,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось инициализировать TokenStorage", e);
        }
    }

    /** Сохраняет access токен */
    public static void saveAccess(String token) {
        sharedPreferences.edit().putString(KEY_ACCESS, token).apply();
    }

    /** Сохраняет refresh токен */
    public static void saveRefresh(String token) {
        sharedPreferences.edit().putString(KEY_REFRESH, token).apply();
    }

    /** Возвращает сохраненный access токен или null */
    public static String getAccess() {
        return sharedPreferences.getString(KEY_ACCESS, null);
    }

    /** Возвращает сохраненный refresh токен или null */
    public static String getRefresh() {
        return sharedPreferences.getString(KEY_REFRESH, null);
    }

    /** Очищает все токены */
    public static void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
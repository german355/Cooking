package com.example.cooking;

import android.content.Context;
import android.content.SharedPreferences;

public class MySharedPreferences {

    // Имя файла настроек

    private static final String PREF_NAME = "acs";

    // Объект SharedPreferences и его редактор
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    // Конструктор класса принимает контекст приложения или активности
    public MySharedPreferences(Context context) {
        // Получаем SharedPreferences с именем PREF_NAME в приватном режиме
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Получаем редактор для внесения изменений
        editor = sharedPreferences.edit();
    }

    // Сохранение строкового значения по ключу
    public void putString(String key, String value) {
        editor.putString(key, value);
        editor.apply(); // Применяем изменения асинхронно
    }

    // Получение строкового значения по ключу, если значение отсутствует — возвращается defaultValue
    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    // Сохранение целочисленного значения по ключу
    public void putInt(String key, int value) {
        editor.putInt(key, value);
        editor.apply();
    }

    // Получение целочисленного значения по ключу, если значение отсутствует — возвращается defaultValue
    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    // Сохранение вещественного значения (float) по ключу
    public void putFloat(String key, float value) {
        editor.putFloat(key, value);
        editor.apply();
    }

    // Получение вещественного значения (float) по ключу, если значение отсутствует — возвращается defaultValue
    public float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    // Сохранение булевого значения по ключу
    public void putBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    // Получение булевого значения по ключу, если значение отсутствует — возвращается defaultValue
    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    // Удаление значения по указанному ключу
    public void remove(String key) {
        editor.remove(key);
        editor.apply();
    }

    // Очистка всех данных из SharedPreferences
    public void clear() {
        editor.clear();
        editor.apply();
    }
}

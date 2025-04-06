package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.network.services.RecipeManager;
import com.example.cooking.utils.MySharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * ViewModel для экрана добавления рецепта
 * Управляет бизнес-логикой создания нового рецепта
 */
public class AddRecipeViewModel extends AndroidViewModel {
    private static final String TAG = "AddRecipeViewModel";
    
    // LiveData для UI состояний
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);
    
    // LiveData для валидации полей
    private final MutableLiveData<String> titleError = new MutableLiveData<>();
    private final MutableLiveData<String> ingredientsError = new MutableLiveData<>();
    private final MutableLiveData<String> instructionsError = new MutableLiveData<>();
    private final MutableLiveData<String> imageError = new MutableLiveData<>();
    
    // Данные рецепта
    private String title = "";
    private String ingredients = "";
    private String instructions = "";
    private byte[] imageBytes = null;
    
    // Сервисы
    private final RecipeManager recipeManager;
    private final MySharedPreferences preferences;
    
    public AddRecipeViewModel(@NonNull Application application) {
        super(application);
        preferences = new MySharedPreferences(application);
        recipeManager = new RecipeManager(application);
    }
    
    /**
     * Сохраняет новый рецепт
     */
    public void saveRecipe() {
        // Проверяем подключение к интернету
        if (!isNetworkAvailable()) {
            errorMessage.setValue("Отсутствует подключение к интернету");
            return;
        }
        
        // Проверяем валидность всех полей
        if (!validateAll()) {
            return;
        }
        
        // Показываем индикатор загрузки
        isLoading.setValue(true);
        
        // Получаем ID пользователя
        String userId = preferences.getString("userId", "99");
        
        // Сохраняем рецепт
        recipeManager.saveRecipe(
            title,
            ingredients,
            instructions,
            userId,
            null,
            imageBytes,
            new RecipeManager.RecipeSaveCallback() {
                @Override
                public void onSuccess(String message) {
                    isLoading.postValue(false);
                    saveSuccess.postValue(true);
                    Log.d(TAG, "Рецепт успешно сохранен: " + message);
                }

                @Override
                public void onFailure(String error) {
                    isLoading.postValue(false);
                    errorMessage.postValue(error);
                    Log.e(TAG, "Ошибка при сохранении рецепта: " + error);
                }
            }
        );
    }
    
    /**
     * Устанавливает название рецепта
     */
    public void setTitle(String title) {
        this.title = title;
        validateTitle();
    }
    
    /**
     * Устанавливает список ингредиентов
     */
    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
        validateIngredients();
    }
    
    /**
     * Устанавливает инструкции
     */
    public void setInstructions(String instructions) {
        this.instructions = instructions;
        validateInstructions();
    }
    
    /**
     * Обрабатывает выбранное изображение и преобразует его в массив байтов
     */
    public void processSelectedImage(Uri imageUri) {
        try {
            InputStream inputStream = getApplication().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
            
            // Сжимаем изображение до разумного размера
            Bitmap resizedBitmap = resizeBitmap(bitmap, 800);
            
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            imageBytes = byteArrayOutputStream.toByteArray();
            
            if (inputStream != null) {
                inputStream.close();
            }
            
            Log.d(TAG, "Изображение обработано, размер: " + imageBytes.length + " байт");
            
            // Очищаем ошибку изображения, если она была
            imageError.setValue(null);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обработке изображения", e);
            imageError.setValue("Ошибка при обработке изображения: " + e.getMessage());
            imageBytes = null;
        }
    }
    
    /**
     * Изменяет размер Bitmap до указанного максимального значения
     */
    private Bitmap resizeBitmap(Bitmap bitmap, int maxSide) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= maxSide && height <= maxSide) {
            return bitmap; // Нет необходимости менять размер
        }
        
        float ratio = (float) width / height;
        int newWidth, newHeight;
        
        if (width > height) {
            newWidth = maxSide;
            newHeight = (int) (maxSide / ratio);
        } else {
            newHeight = maxSide;
            newWidth = (int) (maxSide * ratio);
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    
    /**
     * Проверяет все поля на валидность
     */
    private boolean validateAll() {
        boolean isValid = true;
        
        // Проверяем название
        if (!validateTitle()) {
            isValid = false;
        }
        
        // Проверяем ингредиенты
        if (!validateIngredients()) {
            isValid = false;
        }
        
        // Проверяем инструкции
        if (!validateInstructions()) {
            isValid = false;
        }
        
        // Проверяем изображение
        if (!validateImage()) {
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Проверяет валидность названия рецепта
     */
    private boolean validateTitle() {
        if (title == null || title.trim().isEmpty() || title.length() < 2) {
            titleError.setValue("Название рецепта не может быть пустым и короче 2 символов");
            return false;
        } else if (title.length() > 255) {
            titleError.setValue("Название рецепта не должно превышать 255 символов");
            return false;
        } else {
            titleError.setValue(null);
            return true;
        }
    }
    
    /**
     * Проверяет валидность списка ингредиентов
     */
    private boolean validateIngredients() {
        if (ingredients == null || ingredients.trim().isEmpty()) {
            ingredientsError.setValue("Список ингредиентов не может быть пустым");
            return false;
        } else if (ingredients.length() > 2000) {
            ingredientsError.setValue("Список ингредиентов не может содержать более 2000 символов");
            return false;
        } else {
            ingredientsError.setValue(null);
            return true;
        }
    }
    
    /**
     * Проверяет валидность инструкций
     */
    private boolean validateInstructions() {
        if (instructions == null || instructions.trim().isEmpty()) {
            instructionsError.setValue("Инструкция не может быть пустой");
            return false;
        } else if (instructions.length() > 2000) {
            instructionsError.setValue("Инструкция не может содержать более 2000 символов");
            return false;
        } else {
            instructionsError.setValue(null);
            return true;
        }
    }
    
    /**
     * Проверяет наличие изображения
     */
    private boolean validateImage() {
        if (imageBytes == null || imageBytes.length == 0) {
            imageError.setValue("Пожалуйста, выберите изображение");
            return false;
        } else {
            imageError.setValue(null);
            return true;
        }
    }
    
    /**
     * Проверяет подключение к интернету
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) 
                getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    // Геттеры для LiveData
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }
    
    public LiveData<String> getTitleError() {
        return titleError;
    }
    
    public LiveData<String> getIngredientsError() {
        return ingredientsError;
    }
    
    public LiveData<String> getInstructionsError() {
        return instructionsError;
    }
    
    public LiveData<String> getImageError() {
        return imageError;
    }
} 
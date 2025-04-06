package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.network.services.RecipeManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel для экрана редактирования рецепта
 */
public class EditRecipeViewModel extends AndroidViewModel {
    private static final String TAG = "EditRecipeViewModel";
    
    private final RecipeManager recipeManager;
    private final MySharedPreferences preferences;
    private final ExecutorService executor;
    
    // LiveData для состояний UI
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    // LiveData для данных рецепта
    private final MutableLiveData<Integer> recipeId = new MutableLiveData<>();
    private final MutableLiveData<String> title = new MutableLiveData<>("");
    private final MutableLiveData<String> ingredients = new MutableLiveData<>("");
    private final MutableLiveData<String> instructions = new MutableLiveData<>("");
    private final MutableLiveData<String> photoUrl = new MutableLiveData<>("");
    private final MutableLiveData<byte[]> imageBytes = new MutableLiveData<>();
    
    // Флаги для валидации
    private boolean titleValid = false;
    private boolean ingredientsValid = false;
    private boolean instructionsValid = false;
    
    public EditRecipeViewModel(@NonNull Application application) {
        super(application);
        recipeManager = new RecipeManager(application);
        preferences = new MySharedPreferences(application);
        executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Устанавливает данные рецепта для редактирования
     */
    public void setRecipeData(int id, String recipeTitle, String recipeIngredients, 
                             String recipeInstructions, String recipePhotoUrl) {
        recipeId.setValue(id);
        title.setValue(recipeTitle);
        ingredients.setValue(recipeIngredients);
        instructions.setValue(recipeInstructions);
        photoUrl.setValue(recipePhotoUrl);
        
        // Сразу проверяем валидность данных
        titleValid = validateTitle(recipeTitle);
        ingredientsValid = validateIngredients(recipeIngredients);
        instructionsValid = validateInstructions(recipeInstructions);
        
        // Если есть URL изображения, загружаем его
        if (recipePhotoUrl != null && !recipePhotoUrl.isEmpty()) {
            loadImageFromUrl(recipePhotoUrl);
        }
    }
    
    /**
     * Загружает изображение по URL и преобразует его в массив байтов
     */
    public void loadImageFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "loadImageFromUrl: URL пустой или null");
            return;
        }
        
        isLoading.setValue(true);
        
        executeIfActive(() -> {
            try {
                // Загружаем изображение из URL
                java.net.URL imageUrl = new java.net.URL(url);
                Bitmap bitmap = BitmapFactory.decodeStream(imageUrl.openConnection().getInputStream());
                
                if (bitmap != null) {
                    // Сжимаем изображение до разумного размера
                    Bitmap resizedBitmap = resizeBitmap(bitmap, 800);
                    
                    // Преобразуем в массив байтов
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    
                    // Устанавливаем в LiveData
                    imageBytes.postValue(baos.toByteArray());
                    isLoading.postValue(false);
                    
                    Log.d(TAG, "loadImageFromUrl: Изображение успешно загружено, размер: " + 
                          baos.toByteArray().length + " байт");
                } else {
                    isLoading.postValue(false);
                    errorMessage.postValue("Не удалось загрузить изображение");
                }
            } catch (Exception e) {
                Log.e(TAG, "loadImageFromUrl: Ошибка при загрузке изображения", e);
                isLoading.postValue(false);
                errorMessage.postValue("Ошибка при загрузке изображения: " + e.getMessage());
            }
        });
    }
    
    /**
     * Обрабатывает выбранное изображение из галереи
     */
    public void processSelectedImage(Uri imageUri) {
        if (imageUri == null) {
            errorMessage.setValue("Ошибка при выборе изображения: Uri = null");
            return;
        }
        
        isLoading.setValue(true);
        
        executeIfActive(() -> {
            try {
                // Получаем InputStream из выбранного Uri
                InputStream inputStream = getApplication().getContentResolver().openInputStream(imageUri);
                
                if (inputStream != null) {
                    // Декодируем изображение
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    
                    if (bitmap != null) {
                        // Сжимаем изображение до разумного размера
                        Bitmap resizedBitmap = resizeBitmap(bitmap, 800);
                        
                        // Преобразуем в массив байтов
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                        
                        // Устанавливаем в LiveData
                        byte[] bytes = baos.toByteArray();
                        imageBytes.postValue(bytes);
                        
                        // Очищаем photoUrl, так как теперь используем локальное изображение
                        photoUrl.postValue("");
                        
                        Log.d(TAG, "processSelectedImage: Изображение обработано, размер: " + bytes.length + " байт");
                    } else {
                        errorMessage.postValue("Не удалось декодировать изображение");
                    }
                } else {
                    errorMessage.postValue("Не удалось открыть поток данных для изображения");
                }
            } catch (Exception e) {
                Log.e(TAG, "processSelectedImage: Ошибка при обработке изображения", e);
                errorMessage.postValue("Ошибка при обработке изображения: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Сохраняет обновленный рецепт
     */
    public void updateRecipe() {
        // Проверяем подключение к интернету
        if (!isNetworkAvailable()) {
            errorMessage.setValue("Отсутствует подключение к интернету");
            return;
        }
        
        // Проверяем валидность полей
        String titleValue = title.getValue();
        String ingredientsValue = ingredients.getValue();
        String instructionsValue = instructions.getValue();
        
        if (!validateTitle(titleValue) || !validateIngredients(ingredientsValue) || 
            !validateInstructions(instructionsValue)) {
            errorMessage.setValue("Пожалуйста, исправьте ошибки в форме");
            return;
        }
        
        // Показываем индикатор загрузки
        isLoading.setValue(true);
        
        int recipeIdValue = recipeId.getValue() != null ? recipeId.getValue() : -1;
        String userId = preferences.getString("userId", "99");
        byte[] imageBytesValue = imageBytes.getValue();
        
        // Обновляем рецепт через RecipeManager
        recipeManager.saveRecipe(titleValue, ingredientsValue, instructionsValue, userId, 
                                recipeIdValue, imageBytesValue, new RecipeManager.RecipeSaveCallback() {
            @Override
            public void onSuccess(String message) {
                isLoading.postValue(false);
                successMessage.postValue(message);
            }
            
            @Override
            public void onFailure(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }
    
    /**
     * Проверяет наличие интернет-соединения
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) 
                getApplication().getSystemService(Application.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    /**
     * Изменяет размер изображения
     */
    private Bitmap resizeBitmap(Bitmap bitmap, int maxSide) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Определяем коэффициент масштабирования
        float scale;
        if (width > height) {
            scale = (float) maxSide / width;
        } else {
            scale = (float) maxSide / height;
        }
        
        // Если изображение уже меньше maxSide, не масштабируем
        if (scale >= 1) {
            return bitmap;
        }
        
        // Вычисляем новые размеры
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        
        // Создаем новое bitmap с нужными размерами
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    
    /**
     * Валидирует название рецепта
     */
    public boolean validateTitle(String titleValue) {
        if (titleValue == null || titleValue.trim().isEmpty()) {
            errorMessage.setValue("Название рецепта не может быть пустым");
            titleValid = false;
            return false;
        }
        
        if (titleValue.length() < 3) {
            errorMessage.setValue("Название рецепта должно быть не менее 3 символов");
            titleValid = false;
            return false;
        }
        
        if (titleValue.length() > 100) {
            errorMessage.setValue("Название рецепта не должно превышать 100 символов");
            titleValid = false;
            return false;
        }
        
        titleValid = true;
        return true;
    }
    
    /**
     * Валидирует список ингредиентов
     */
    public boolean validateIngredients(String ingredientsValue) {
        if (ingredientsValue == null || ingredientsValue.trim().isEmpty()) {
            errorMessage.setValue("Список ингредиентов не может быть пустым");
            ingredientsValid = false;
            return false;
        }
        
        if (ingredientsValue.length() < 10) {
            errorMessage.setValue("Список ингредиентов должен быть не менее 10 символов");
            ingredientsValid = false;
            return false;
        }
        
        if (ingredientsValue.length() > 1000) {
            errorMessage.setValue("Список ингредиентов не должен превышать 1000 символов");
            ingredientsValid = false;
            return false;
        }
        
        ingredientsValid = true;
        return true;
    }
    
    /**
     * Валидирует инструкции приготовления
     */
    public boolean validateInstructions(String instructionsValue) {
        if (instructionsValue == null || instructionsValue.trim().isEmpty()) {
            errorMessage.setValue("Инструкции по приготовлению не могут быть пустыми");
            instructionsValid = false;
            return false;
        }
        
        if (instructionsValue.length() < 30) {
            errorMessage.setValue("Инструкции должны быть не менее 30 символов");
            instructionsValid = false;
            return false;
        }
        
        if (instructionsValue.length() > 2000) {
            errorMessage.setValue("Инструкции не должны превышать 2000 символов");
            instructionsValid = false;
            return false;
        }
        
        instructionsValid = true;
        return true;
    }
    
    /**
     * Сеттеры для данных рецепта
     */
    public void setTitle(String newTitle) {
        title.setValue(newTitle);
        validateTitle(newTitle);
    }
    
    public void setIngredients(String newIngredients) {
        ingredients.setValue(newIngredients);
        validateIngredients(newIngredients);
    }
    
    public void setInstructions(String newInstructions) {
        instructions.setValue(newInstructions);
        validateInstructions(newInstructions);
    }
    
    /**
     * Геттеры для LiveData
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    public LiveData<String> getTitle() {
        return title;
    }
    
    public LiveData<String> getIngredients() {
        return ingredients;
    }
    
    public LiveData<String> getInstructions() {
        return instructions;
    }
    
    public LiveData<String> getPhotoUrl() {
        return photoUrl;
    }
    
    public LiveData<byte[]> getImageBytes() {
        return imageBytes;
    }
    
    /**
     * Метод, выполняющий операцию в executor с проверкой его состояния
     * @param task задача для выполнения
     */
    private void executeIfActive(Runnable task) {
        if (!executor.isShutdown()) {
            executor.execute(task);
        } else {
            Log.w(TAG, "Executor уже завершен, пропускаем задачу");
        }
    }
    
    @Override
    protected void onCleared() {
        executor.shutdown();
        super.onCleared();
    }
} 
package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.auth.FirebaseAuthManager;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.network.services.UserService;
import com.google.firebase.auth.FirebaseUser;

/**
 * ViewModel для управления аутентификацией пользователя
 */
public class AuthViewModel extends AndroidViewModel {
    private static final String TAG = "AuthViewModel";
    
    private final FirebaseAuthManager authManager;
    private final MySharedPreferences preferences;
    private final UserService userService;
    
    // LiveData для состояний UI
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAuthenticated = new MutableLiveData<>(false);
    
    // LiveData для данных пользователя
    private final MutableLiveData<String> displayName = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<Integer> permission = new MutableLiveData<>(1);
    
    // Валидация форм
    private boolean isEmailValid = false;
    private boolean isPasswordValid = false;
    private boolean isNameValid = false;
    private boolean isPasswordConfirmValid = false;
    
    public AuthViewModel(@NonNull Application application) {
        super(application);
        authManager = new FirebaseAuthManager(application);
        preferences = new MySharedPreferences(application);
        userService = new UserService();
        
        // Проверяем текущее состояние аутентификации
        checkAuthenticationState();
    }
    
    /**
     * Проверяет текущее состояние аутентификации пользователя
     */
    private void checkAuthenticationState() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            isAuthenticated.setValue(true);
            
            // Загружаем данные пользователя из SharedPreferences
            displayName.setValue(preferences.getString("username", ""));
            email.setValue(preferences.getString("email", ""));
            permission.setValue(preferences.getInt("permission", 1));
        } else {
            isAuthenticated.setValue(false);
        }
    }
    
    /**
     * Выполняет вход с использованием email и пароля
     */
    public void signInWithEmailPassword(String email, String password) {
        if (!validateEmail(email) || !validatePassword(password)) {
            return;
        }
        
        isLoading.setValue(true);
        
        authManager.signInWithEmailAndPassword(email, password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (user != null) {
                    saveUserData(user);
                    isLoading.postValue(false);
                    isAuthenticated.postValue(true);
                    Log.d(TAG, "signInWithEmailPassword: Успешный вход пользователя: " + user.getEmail());
                }
            }
            
            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
                Log.e(TAG, "signInWithEmailPassword: Ошибка входа: " + message);
            }
        });
    }
    
    /**
     * Регистрирует нового пользователя
     */
    public void registerUser(String email, String password, String username) {
        if (!validateEmail(email) || !validatePassword(password) || !validateName(username)) {
            return;
        }
        
        isLoading.setValue(true);
        
        authManager.registerWithEmailAndPassword(email, password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                // Обновляем имя пользователя
                authManager.updateUserDisplayName(user, username, new FirebaseAuthManager.UpdateProfileCallback() {
                    @Override
                    public void onSuccess() {
                        // Сохраняем данные пользователя на сервере
                        userService.saveUser(user.getUid(), username, email, 1, new UserService.UserServiceCallback() {
                            @Override
                            public void onSuccess(ApiResponse response) {
                                saveUserData(user);
                                isLoading.postValue(false);
                                isAuthenticated.postValue(true);
                                Log.d(TAG, "registerUser: Пользователь успешно зарегистрирован: " + user.getEmail());
                            }
                            
                            @Override
                            public void onFailure(String error) {
                                Log.e(TAG, "registerUser: Ошибка сохранения данных пользователя: " + error);
                                // Даже если не удалось сохранить на сервере, считаем пользователя зарегистрированным
                                saveUserData(user);
                                isLoading.postValue(false);
                                isAuthenticated.postValue(true);
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "registerUser: Ошибка обновления имени пользователя: " + message);
                        // Даже если не удалось обновить имя, считаем пользователя зарегистрированным
                        saveUserData(user);
                        isLoading.postValue(false);
                        isAuthenticated.postValue(true);
                    }
                });
            }
            
            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
                Log.e(TAG, "registerUser: Ошибка регистрации: " + message);
            }
        });
    }
    
    /**
     * Выполняет выход пользователя
     */
    public void signOut() {
        authManager.signOut();
        
        // Очищаем данные пользователя из SharedPreferences
        preferences.putString("userId", "0");
        preferences.putString("username", "");
        preferences.putString("email", "");
        preferences.putInt("permission", 1);
        
        // Обновляем LiveData
        isAuthenticated.setValue(false);
        displayName.setValue("");
        email.setValue("");
        permission.setValue(1);
    }
    
    /**
     * Сохраняет данные пользователя в SharedPreferences
     */
    private void saveUserData(FirebaseUser user) {
        String uid = user.getUid();
        String userName = user.getDisplayName() != null ? user.getDisplayName() : "";
        String userEmail = user.getEmail() != null ? user.getEmail() : "";
        
        // Сохраняем данные в SharedPreferences
        preferences.putString("userId", uid);
        preferences.putString("username", userName);
        preferences.putString("email", userEmail);
        
        // Обновляем LiveData
        displayName.postValue(userName);
        email.postValue(userEmail);
    }
    
    /**
     * Валидация email
     */
    public boolean validateEmail(String emailValue) {
        if (TextUtils.isEmpty(emailValue)) {
            errorMessage.setValue("Email не может быть пустым");
            isEmailValid = false;
            return false;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            errorMessage.setValue("Неверный формат email");
            isEmailValid = false;
            return false;
        }
        
        isEmailValid = true;
        return true;
    }
    
    /**
     * Валидация пароля
     */
    public boolean validatePassword(String passwordValue) {
        if (TextUtils.isEmpty(passwordValue)) {
            errorMessage.setValue("Пароль не может быть пустым");
            isPasswordValid = false;
            return false;
        }
        
        if (passwordValue.length() < 6) {
            errorMessage.setValue("Пароль должен содержать не менее 6 символов");
            isPasswordValid = false;
            return false;
        }
        
        isPasswordValid = true;
        return true;
    }
    
    /**
     * Валидация имени пользователя
     */
    public boolean validateName(String nameValue) {
        if (TextUtils.isEmpty(nameValue)) {
            errorMessage.setValue("Имя не может быть пустым");
            isNameValid = false;
            return false;
        }
        
        if (nameValue.length() < 2) {
            errorMessage.setValue("Имя должно содержать не менее 2 символов");
            isNameValid = false;
            return false;
        }
        
        isNameValid = true;
        return true;
    }
    
    /**
     * Валидация подтверждения пароля
     */
    public boolean validatePasswordConfirmation(String password, String confirmPassword) {
        if (TextUtils.isEmpty(confirmPassword)) {
            errorMessage.setValue("Подтверждение пароля не может быть пустым");
            isPasswordConfirmValid = false;
            return false;
        }
        
        if (!password.equals(confirmPassword)) {
            errorMessage.setValue("Пароли не совпадают");
            isPasswordConfirmValid = false;
            return false;
        }
        
        isPasswordConfirmValid = true;
        return true;
    }
    
    /**
     * Проверяет все входные данные формы регистрации
     */
    public boolean validateAllRegistrationInputs(String name, String email, String password, String confirmPassword) {
        boolean nameValid = validateName(name);
        boolean emailValid = validateEmail(email);
        boolean passwordValid = validatePassword(password);
        boolean confirmValid = validatePasswordConfirmation(password, confirmPassword);
        
        return nameValid && emailValid && passwordValid && confirmValid;
    }
    
    /**
     * Проверяет все поля для входа на валидность
     */
    public boolean validateAllLoginInputs(String email, String password) {
        return validateEmail(email) && validatePassword(password);
    }
    
    /**
     * Проверяет совпадение паролей
     * @param password пароль
     * @param confirmPassword подтверждение пароля
     * @return true, если пароли совпадают
     */
    public boolean doPasswordsMatch(String password, String confirmPassword) {
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            errorMessage.setValue("Подтверждение пароля не может быть пустым");
            isPasswordConfirmValid = false;
            return false;
        }
        
        boolean match = password.equals(confirmPassword);
        
        if (!match) {
            errorMessage.setValue("Пароли не совпадают");
            isPasswordConfirmValid = false;
        } else {
            isPasswordConfirmValid = true;
        }
        
        return match;
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
    
    public LiveData<Boolean> getIsAuthenticated() {
        return isAuthenticated;
    }
    
    public LiveData<String> getDisplayName() {
        return displayName;
    }
    
    public LiveData<String> getEmail() {
        return email;
    }
    
    public LiveData<Integer> getPermission() {
        return permission;
    }
    
    /**
     * Очищает сообщение об ошибке
     */
    public void clearErrorMessage() {
        errorMessage.setValue("");
    }
    
    /**
     * Проверяет, авторизован ли пользователь
     */
    public boolean isUserLoggedIn() {
        return authManager.getCurrentUser() != null;
    }
    
    /**
     * Инициализирует Google Sign-In
     * @param webClientId идентификатор из google-services.json
     */
    public void initGoogleSignIn(String webClientId) {
        authManager.initGoogleSignIn(getApplication().getApplicationContext(), webClientId);
    }
    
    /**
     * Запускает процесс входа через Google
     * @param activity активность для запуска Intent
     */
    public void signInWithGoogle(android.app.Activity activity) {
        isLoading.setValue(true);
        try {
            authManager.signInWithGoogle(activity);
        } catch (Exception e) {
            errorMessage.setValue(e.getMessage());
            isLoading.setValue(false);
        }
    }
    
    /**
     * Обрабатывает результат входа через Google
     * @param requestCode код запроса
     * @param resultCode код результата
     * @param data данные Intent
     */
    public void handleGoogleSignInResult(int requestCode, int resultCode, Intent data) {
        // Проверяем, что это ответ на наш запрос
        if (requestCode == FirebaseAuthManager.RC_SIGN_IN) {
            authManager.handleGoogleSignInResult(data, new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    saveUserData(user);
                    isLoading.postValue(false);
                    isAuthenticated.postValue(true);
                    Log.d(TAG, "Google Sign-In: Успешный вход пользователя: " + user.getEmail());
                }
                
                @Override
                public void onError(String message) {
                    isLoading.postValue(false);
                    errorMessage.postValue(message);
                    Log.e(TAG, "Google Sign-In: Ошибка входа: " + message);
                }
            });
        } else {
            isLoading.setValue(false);
        }
    }
} 
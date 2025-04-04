package com.example.cooking;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cooking.FireBase.AuthCallback;
import com.example.cooking.FireBase.FirebaseAuthManager;
import com.example.cooking.ServerWorker.ApiResponse;
import com.example.cooking.ServerWorker.UserService;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

public class Regist extends AppCompatActivity {
    private TextInputEditText nameEditText;
    private TextInputEditText Email;
    private TextInputEditText Pass;
    private TextInputEditText ConfirmPass;
    private Button firebaseRegist;
    private Button googleSignupButton;
    private TextView enter;
    private FirebaseAuthManager firebaseAuthManager;
    private static final String TAG = "RegistActivity";
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Инициализация Firebase Auth Manager
        firebaseAuthManager = FirebaseAuthManager.getInstance();
        
        // Инициализация Google Sign In
        // Используем requestIdToken с ID клиента для веб-приложения из google-services.json
        String webClientId = getString(R.string.default_web_client_id);
        firebaseAuthManager.initGoogleSignIn(this, webClientId);

        // Инициализация views
        nameEditText = findViewById(R.id.NameEditText);
        Email = findViewById(R.id.emailEditText);
        Pass = findViewById(R.id.passwordEditText);
        ConfirmPass = findViewById(R.id.passwordEditText2);
        enter = findViewById(R.id.loginPromptTextView);
        
        // Инициализация кнопок Firebase
        // Примечание: необходимо добавить эти кнопки в макет activity_register.xml
        firebaseRegist = findViewById(R.id.firebaseRegisterButton);
        googleSignupButton = findViewById(R.id.googleSignupButton);

        // Добавляем валидацию для имени в реальном времени
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                validateName(s.toString());
            }
        });

        // Добавляем валидацию для email в реальном времени
        Email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                validateEmail(s.toString());
            }
        });
        
        // Добавляем валидацию для пароля в реальном времени
        Pass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                validatePassword(password);
                
                // Также проверяем подтверждение пароля, если оно не пустое
                String confirmPassword = ConfirmPass.getText().toString();
                if (!TextUtils.isEmpty(confirmPassword)) {
                    validatePasswordConfirmation(password, confirmPassword);
                }
            }
        });
        
        // Добавляем валидацию для подтверждения пароля в реальном времени
        ConfirmPass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                validatePasswordConfirmation(Pass.getText().toString(), s.toString());
            }
        });
        
        // Обработчик нажатия на кнопку регистрации через Firebase
        firebaseRegist.setOnClickListener(v -> {
            if (!validateAllInputs()) {
                return;
            }

            String name = nameEditText.getText().toString();
            String email = Email.getText().toString();
            String password = Pass.getText().toString();

            // Показываем индикатор загрузки
            firebaseRegist.setEnabled(false);
            firebaseRegist.setText("Подождите...");

            // Регистрация через Firebase
            firebaseAuthManager.registerWithEmailAndPassword(email, password, new AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    // Обновляем профиль пользователя, добавляя имя
                    updateUserProfileAndRedirect(user, name);
                }

                @Override
                public void onError(Exception exception) {
                    runOnUiThread(() -> {
                        firebaseRegist.setEnabled(true);
                        firebaseRegist.setText("Зарегистрироваться");
                        
                        String errorMessage = "Ошибка регистрации: ";
                        if (exception.getMessage().contains("email address is already in use")) {
                            errorMessage += "Этот email уже используется";
                        } else if (exception.getMessage().contains("password is invalid")) {
                            errorMessage += "Пароль должен содержать не менее 6 символов";
                        } else {
                            errorMessage += exception.getMessage();
                        }
                        
                        Toast.makeText(Regist.this, errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Firebase registration error", exception);
                    });
                }
            });
        });
        
        // Обработчик для регистрации через Google
        googleSignupButton.setOnClickListener(v -> {
            firebaseAuthManager.signInWithGoogle(this);
        });

        // Обработчик нажатия на текст входа
        enter.setOnClickListener(v -> {
            Intent intent = new Intent(Regist.this, MainActivity.class);
            // Добавляем флаг, указывающий, что нужно показать фрагмент авторизации
            intent.putExtra("show_auth_fragment", true);
            startActivity(intent);
            finish();
        });
    }
    
    /**
     * Проверяет валидность имени пользователя
     */
    private boolean validateName(String name) {
        if (TextUtils.isEmpty(name) || name.length() < 2) {
            TextInputLayout nameInputLayout = (TextInputLayout) nameEditText.getParent().getParent();
            nameInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            nameInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            nameInputLayout.setError("Длина имени не менее 2 символов");
            //nameEditText.setError("НЕВЕРНЫЙ ФОРМАТ");
            return false;
        } else {
            TextInputLayout nameInputLayout = (TextInputLayout) nameEditText.getParent().getParent();
            nameInputLayout.setError(null);
            nameEditText.setError(null);
            return true;
        }
    }
    
    /**
     * Проверяет валидность адреса электронной почты
     */
    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            TextInputLayout emailInputLayout = (TextInputLayout) Email.getParent().getParent();
            emailInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            emailInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            emailInputLayout.setError("Неверный формат почты");
            //Email.setError("НЕВЕРНЫЙ ФОРМАТ");
            return false;
        } else {
            TextInputLayout emailInputLayout = (TextInputLayout) Email.getParent().getParent();
            emailInputLayout.setError(null);
            Email.setError(null);
            return true;
        }
    }
    
    /**
     * Проверяет валидность пароля
     */
    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            TextInputLayout passwordInputLayout = (TextInputLayout) Pass.getParent().getParent();
            passwordInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            passwordInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            passwordInputLayout.setError("Пароль должен содержать не менее 6 символов");
            //Pass.setError("НЕВЕРНЫЙ ФОРМАТ");
            return false;
        } else {
            TextInputLayout passwordInputLayout = (TextInputLayout) Pass.getParent().getParent();
            passwordInputLayout.setError(null);
            Pass.setError(null);
            return true;
        }
    }
    
    /**
     * Проверяет совпадение паролей
     */
    private boolean validatePasswordConfirmation(String password, String confirmPassword) {
        if (TextUtils.isEmpty(confirmPassword) || !password.equals(confirmPassword)) {
            TextInputLayout confirmInputLayout = (TextInputLayout) ConfirmPass.getParent().getParent();
            confirmInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            confirmInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            confirmInputLayout.setError("НЕВЕРНЫЙ ФОРМАТ");
            ConfirmPass.setError("НЕВЕРНЫЙ ФОРМАТ");
            return false;
        } else {
            TextInputLayout confirmInputLayout = (TextInputLayout) ConfirmPass.getParent().getParent();
            confirmInputLayout.setError(null);
            ConfirmPass.setError(null);
            return true;
        }
    }
    
    /**
     * Проверяет корректность всех полей ввода
     */
    private boolean validateAllInputs() {
        String name = nameEditText.getText().toString();
        String email = Email.getText().toString();
        String password = Pass.getText().toString();
        String confirmPassword = ConfirmPass.getText().toString();

        boolean isNameValid = validateName(name);
        boolean isEmailValid = validateEmail(email);
        boolean isPasswordValid = validatePassword(password);
        boolean isConfirmPasswordValid = validatePasswordConfirmation(password, confirmPassword);
        
        return isNameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Результат из Google Sign In
        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "Получен результат Google Sign In, resultCode: " + resultCode);
            
            // Проверяем, не был ли запрос отменен пользователем
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Вход через Google был отменен", Toast.LENGTH_SHORT).show();
                return;
            }
            
            firebaseAuthManager.handleGoogleSignInResult(data, new AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    Log.d(TAG, "Google Sign In успешен, обрабатываем Firebase авторизацию");
                    // Google авторизовал пользователя, переходим через updateUserProfileAndRedirect
                    // для синхронизации с сервером
                    String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();
                    updateUserProfileAndRedirect(user, displayName);
                }

                @Override
                public void onError(Exception exception) {
                    Log.e(TAG, "Ошибка Google Sign In", exception);
                    
                    // Более понятное сообщение об ошибке для пользователя
                    String errorMessage;
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        int statusCode = apiException.getStatusCode();
                        
                        switch (statusCode) {
                            case 12500: // SIGN_IN_CANCELLED
                                errorMessage = "Вход был отменен пользователем";
                                break;
                            case 12501: // SIGN_IN_FAILED
                                errorMessage = "Не удалось войти через Google";
                                break;
                            case 12502: // SIGN_IN_CURRENTLY_IN_PROGRESS
                                errorMessage = "Процесс входа уже выполняется";
                                break;
                            case 10: // DEVELOPER_ERROR
                                errorMessage = "Ошибка настройки Google Sign-In в Firebase Console. Проверьте SHA-1 в консоли Firebase";
                                break;
                            default:
                                errorMessage = "Код ошибки Google Sign In: " + statusCode;
                        }
                    } else {
                        errorMessage = exception.getMessage();
                    }
                    
                    Toast.makeText(Regist.this, 
                                  "Ошибка входа через Google: " + errorMessage, 
                                  Toast.LENGTH_LONG).show();
                    
                    // В случае серьезной ошибки перенаправляем на MainActivity с флагом для показа фрагмента авторизации
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        int statusCode = apiException.getStatusCode();
                        
                        // Коды ошибок, которые указывают на необходимость перехода к экрану авторизации
                        if (statusCode != 12500 && statusCode != 12502) { // Не отменено пользователем и не в процессе
                            Intent intent = new Intent(Regist.this, MainActivity.class);
                            intent.putExtra("show_auth_fragment", true);
                            startActivity(intent);
                            finish();
                        }
                    }
                }
            });
        }
    }
    
    /**
     * Обновляет профиль пользователя и перенаправляет на главный экран
     */
    private void updateUserProfileAndRedirect(FirebaseUser user, String displayName) {
        // Показываем индикатор, что процесс еще не завершен
        runOnUiThread(() -> {
            Toast.makeText(this, "Регистрация в Firebase успешна. Выполняется регистрация на сервере...", Toast.LENGTH_SHORT).show();
        });
        
        // Получаем email и UID пользователя из Firebase
        String email = user.getEmail();
        String firebaseId = user.getUid();
        Log.d(TAG, "Firebase пользователь: " + email + ", UID: " + firebaseId);
        
        // Создаем экземпляр нового сервиса
        UserService userService = new UserService();
        
        // Регистрируем пользователя на сервере
        userService.registerFirebaseUser(email, displayName, firebaseId, new UserService.UserCallback() {
            @Override
            public void onSuccess(ApiResponse response) {
                // Регистрация на сервере успешна, сохраняем данные и перенаправляем пользователя
                saveUserDataAndRedirect(user, displayName, response);
            }
            
            @Override
            public void onFailure(String errorMessage) {
                // Регистрация на сервере не удалась
                Log.e(TAG, "Ошибка регистрации на сервере: " + errorMessage);
                
                // Выход из Firebase, так как регистрация на сервере не удалась
                firebaseAuthManager.signOut();
                
                runOnUiThread(() -> {
                    // Восстанавливаем кнопки регистрации
                    if (firebaseRegist != null) {
                        firebaseRegist.setEnabled(true);
                        firebaseRegist.setText("Зарегистрироваться");
                    }
                    
                    Toast.makeText(Regist.this, 
                        "Регистрация в Firebase успешна, но не удалось зарегистрироваться на сервере: " + 
                        errorMessage, Toast.LENGTH_LONG).show();
                    
                    // Если ошибка является критической, перенаправляем на главный экран 
                    // с флагом для отображения экрана авторизации
                    if (errorMessage.contains("timeout") || errorMessage.contains("network")) {
                        Intent intent = new Intent(Regist.this, MainActivity.class);
                        intent.putExtra("show_auth_fragment", true);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });
    }
    
    /**
     * Сохраняет данные пользователя и перенаправляет на главный экран
     */
    private void saveUserDataAndRedirect(FirebaseUser user, String displayName, ApiResponse response) {
        // Сохраняем данные пользователя
        MySharedPreferences prefs = new MySharedPreferences(this);
        prefs.putString("userId", response.getUserId()); // ID с сервера
        Log.d("FireBase", response.getUserId());
        prefs.putString("fbUserId", user.getUid()); // Firebase UID
        Log.d("FireBase", user.getUid());
        prefs.putString("userName", displayName);
        Log.d("FireBase", response.getName());
        prefs.putBoolean("auth", true);
        prefs.putInt("permission", response.getPermission()); // Права доступа с сервера
        prefs.putBoolean("isFirebaseUser", true);
        
        // Показываем сообщение об успехе
        runOnUiThread(() -> {
            Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
            
            // Переходим на главный экран
            Intent intent = new Intent(Regist.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}

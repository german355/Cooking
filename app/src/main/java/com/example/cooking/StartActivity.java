package com.example.cooking;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;
import android.os.Bundle;
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

public class StartActivity extends AppCompatActivity {
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton;
    private Button firebaseLoginButton;
    private TextView registerTextView;
    private Button googleLoginButton;
    MySharedPreferences id;
    private FirebaseAuthManager firebaseAuthManager;
    private static final String TAG = "StartActivity";
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.start__activity);
            
            // Инициализация Firebase Auth Manager
            firebaseAuthManager = FirebaseAuthManager.getInstance();
            
            // Проверяем, авторизован ли пользователь через Firebase
            if (firebaseAuthManager.isUserSignedIn()) {
                handleFirebaseAuthSuccess(firebaseAuthManager.getCurrentUser());
                return;
            }
            
            // Инициализация Views
            emailEditText = findViewById(R.id.emailEditText);
            passwordEditText = findViewById(R.id.passwordEditText);
            registerTextView = findViewById(R.id.registerTextView);
            
            // Инициализация кнопок для Firebase Auth
            firebaseLoginButton = findViewById(R.id.firebaseLoginButton);
            googleLoginButton = findViewById(R.id.googleLoginButton);
            
            // Добавляем валидацию для почты в реальном времени
            emailEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Не используется
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Не используется
                }

                @Override
                public void afterTextChanged(Editable s) {
                    validateEmail(s.toString());
                }
            });
            
            // Добавляем валидацию для пароля в реальном времени
            passwordEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Не используется
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Не используется
                }

                @Override
                public void afterTextChanged(Editable s) {
                    validatePassword(s.toString());
                }
            });
            
            // Инициализация Google Sign In
            String webClientId = getString(R.string.default_web_client_id);
            firebaseAuthManager.initGoogleSignIn(this, webClientId);

            // Заполняем поля, если данные переданы
            prefillCredentialsIfProvided();

            // Обработчик нажатия на текст регистрации
            registerTextView.setOnClickListener(v -> {
                Intent intent = new Intent(StartActivity.this, Regist.class);
                startActivity(intent);
            });

            // Обработчик для входа через сервер
            /*loginButton.setOnClickListener(v -> {
                String email = emailEditText.getText().toString();

                // Запускаем асинхронную задачу для входа
                new LoginTask(this).execute(email);
                
                // Показываем индикатор загрузки
                loginButton.setEnabled(false);
                loginButton.setText("Подождите...");
            });*/
            
            // Обработчик для входа через Firebase с email/password
            setupFirebaseLoginButton();
            
            // Обработчик для входа через Google
            googleLoginButton.setOnClickListener(v -> {
                firebaseAuthManager.signInWithGoogle(this);
            });
            
        } catch (Exception e) {
            // Логируем ошибку для отладки
            Log.e("StartActivity", "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Ошибка запуска приложения", Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                    handleFirebaseAuthSuccess(user);
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
                                errorMessage = "Ошибка настройки Google Sign-In в Firebase Console";
                                break;
                            default:
                                errorMessage = "Код ошибки Google Sign In: " + statusCode;
                        }
                    } else {
                        errorMessage = exception.getMessage();
                    }
                    
                    Toast.makeText(StartActivity.this, 
                                  "Ошибка входа через Google: " + errorMessage, 
                                  Toast.LENGTH_LONG).show();
                }
            });
        }
    }
    
    /**
     * Обрабатывает успешную аутентификацию через Firebase
     */
    private void handleFirebaseAuthSuccess(FirebaseUser user) {
        if (user != null) {
            // Показываем сообщение, что входим в аккаунт
            Toast.makeText(this, "Вход в Firebase выполнен. Синхронизация с сервером...", Toast.LENGTH_SHORT).show();
            
            // Получаем данные пользователя из Firebase
            String email = user.getEmail();
            String firebaseId = user.getUid();
            Log.d("UId", user.getUid());
            String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();
            
            // Создаем экземпляр нового сервиса
            UserService userService = new UserService();
            
            // Выполняем вход на сервере
            userService.loginFirebaseUser(email, firebaseId, new UserService.UserCallback() {
                @Override
                public void onSuccess(ApiResponse response) {
                    // Вход на сервере успешен, сохраняем данные
                    saveUserDataAndRedirect(user, response);
                }
                
                @Override
                public void onFailure(String errorMessage) {
                    // Вход на сервере не удался, пробуем зарегистрироваться
                    Log.w(TAG, "Server login failed: " + errorMessage + ". Trying to register...");
                    
                    userService.registerFirebaseUser(email, displayName, firebaseId, new UserService.UserCallback() {
                        @Override
                        public void onSuccess(ApiResponse response) {
                            // Регистрация на сервере успешна, сохраняем данные
                            saveUserDataAndRedirect(user, response);
                        }
                        
                        @Override
                        public void onFailure(String registerErrorMessage) {
                            // Ни вход, ни регистрация не удались
                            Log.e(TAG, "Server registration failed: " + registerErrorMessage);
                            
                            // Даже если синхронизация с сервером не удалась, можем использовать Firebase
                            redirectToMainScreenWithFirebaseOnly(user);
                        }
                    });
                }
            });
        }
    }
    
    /**
     * Сохраняет данные пользователя и перенаправляет на главный экран
     */
    private void saveUserDataAndRedirect(FirebaseUser user, ApiResponse response) {
        // Сохраняем информацию о пользователе
        id = new MySharedPreferences(this);
        id.putString("userId", response.getUserId()); // ID с сервера
        id.putString("fbUserId", user.getUid()); // Firebase UID
        id.putString("userName", response.getName());
        id.putBoolean("auth", true);
        id.putInt("permission", response.getPermission()); // Права доступа с сервера
        id.putBoolean("isFirebaseUser", true);
        
        runOnUiThread(() -> {
            Toast.makeText(this, "Вход выполнен успешно!", Toast.LENGTH_SHORT).show();
            
            // Переходим на главный экран
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
    
    /**
     * Перенаправляет на главный экран с данными только из Firebase
     */
    private void redirectToMainScreenWithFirebaseOnly(FirebaseUser user) {
        // Сохраняем информацию о пользователе
        id = new MySharedPreferences(this);
        id.putString("userId", user.getUid());
        id.putString("userName", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
        id.putBoolean("auth", true);
        id.putInt("permission", 1); // Устанавливаем базовый уровень разрешений
        id.putBoolean("isFirebaseUser", true);
        
        runOnUiThread(() -> {
            Toast.makeText(this, "Вход выполнен успешно (только Firebase)!", Toast.LENGTH_SHORT).show();
            
            // Переходим на главный экран
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void prefillCredentialsIfProvided() {
        // Проверяем, были ли переданы учетные данные с экрана регистрации
        Intent intent = getIntent();
        if (intent != null) {
            String email = intent.getStringExtra("email");
            String password = intent.getStringExtra("password");
            
            if (email != null && !email.isEmpty()) {
                emailEditText.setText(email);
            }
            
            if (password != null && !password.isEmpty()) {
                passwordEditText.setText(password);
            }
        }
    }

    /**
     * Проверяет валидность адреса электронной почты
     * @param email адрес электронной почты для проверки
     * @return true, если email валиден
     */
    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            //emailEditText.setError("НЕВЕРНЫЙ ФОРМАТ");
            // Получаем TextInputLayout, в котором находится TextInputEditText
            TextInputLayout emailInputLayout = findViewById(R.id.emailInputLayout);
            // Устанавливаем цвет ошибки на красный
            emailInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            emailInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            emailInputLayout.setError("неверный формат почты");
            return false;
        } else {
            // Сбрасываем ошибку
            emailEditText.setError(null);
            TextInputLayout emailInputLayout = findViewById(R.id.emailInputLayout);
            emailInputLayout.setError(null);
            return true;
        }
    }
    
    /**
     * Проверяет валидность пароля
     * @param password пароль для проверки
     * @return true, если пароль валиден
     */
    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            //passwordEditText.setError("НЕВЕРНЫЙ ФОРМАТ");
            // Получаем TextInputLayout для пароля
            TextInputLayout passwordInputLayout = findViewById(R.id.passwordInputLayout);
            // Устанавливаем цвет ошибки на красный
            passwordInputLayout.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.RED));
            passwordInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.RED));
            passwordInputLayout.setError("Пароль должен содержать не менее 6 символов");
            return false;
        } else {
            passwordEditText.setError(null);
            TextInputLayout passwordInputLayout = findViewById(R.id.passwordInputLayout);
            passwordInputLayout.setError(null);
            return true;
        }
    }

    // Обновляем обработчик для входа через Firebase с использованием валидации
    private void setupFirebaseLoginButton() {
        firebaseLoginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            // Проверяем валидность полей
            boolean isEmailValid = validateEmail(email);
            boolean isPasswordValid = validatePassword(password);
            
            if (!isEmailValid || !isPasswordValid) {
                return; // Не продолжаем, если поля не валидны
            }

            // Показываем индикатор загрузки
            firebaseLoginButton.setEnabled(false);
            firebaseLoginButton.setText("Подождите...");

            // Вход через Firebase
            firebaseAuthManager.signInWithEmailAndPassword(email, password, new AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    runOnUiThread(() -> {
                        firebaseLoginButton.setEnabled(true);
                        firebaseLoginButton.setText("Войти");
                        handleFirebaseAuthSuccess(user);
                    });
                }

                @Override
                public void onError(Exception exception) {
                    runOnUiThread(() -> {
                        firebaseLoginButton.setEnabled(true);
                        firebaseLoginButton.setText("Войти");
                        Toast.makeText(StartActivity.this, "Ошибка входа: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Firebase auth error", exception);
                    });
                }
            });
        });
    }
}
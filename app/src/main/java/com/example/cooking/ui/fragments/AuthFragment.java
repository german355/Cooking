package com.example.cooking.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.cooking.FireBase.AuthCallback;
import com.example.cooking.FireBase.FirebaseAuthManager;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.R;
import com.example.cooking.ui.activities.Regist;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.network.services.UserService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

/**
 * Фрагмент авторизации, который показывается вместо ProfileFragment, 
 * если пользователь не авторизован.
 */
public class AuthFragment extends Fragment {
    private static final String TAG = "AuthFragment";
    private static final int RC_SIGN_IN = 9001;

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private Button loginButton;
    private Button googleLoginButton;
    private TextView registerTextView;
    private FirebaseAuthManager firebaseAuthManager;
    private MySharedPreferences preferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auth, container, false);

        // Инициализация UI элементов
        emailEditText = view.findViewById(R.id.email_edit_text);
        passwordEditText = view.findViewById(R.id.password_edit_text);
        emailInputLayout = view.findViewById(R.id.email_input_layout);
        passwordInputLayout = view.findViewById(R.id.password_input_layout);
        loginButton = view.findViewById(R.id.login_button);
        googleLoginButton = view.findViewById(R.id.google_login_button);
        registerTextView = view.findViewById(R.id.register_text_view);

        // Инициализация Firebase Auth Manager
        firebaseAuthManager = FirebaseAuthManager.getInstance();
        preferences = new MySharedPreferences(requireContext());

        // Инициализация Google Sign In
        String webClientId = getString(R.string.default_web_client_id);
        firebaseAuthManager.initGoogleSignIn(requireContext(), webClientId);

        // Настройка обработчиков ввода
        setupTextWatchers();

        // Настройка обработчиков нажатий
        loginButton.setOnClickListener(v -> attemptLogin());
        googleLoginButton.setOnClickListener(v -> signInWithGoogle());
        registerTextView.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), Regist.class);
            startActivity(intent);
        });

        return view;
    }

    private void setupTextWatchers() {
        // Email TextWatcher
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!validateEmail(s.toString())) {
                    emailInputLayout.setError("Введите корректный email");
                } else {
                    emailInputLayout.setError(null);
                }
            }
        });

        // Password TextWatcher
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!validatePassword(s.toString())) {
                    passwordInputLayout.setError("Пароль должен содержать не менее 6 символов");
                } else {
                    passwordInputLayout.setError(null);
                }
            }
        });
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (!validateEmail(email) || !validatePassword(password)) {
            return;
        }

        firebaseAuthManager.signInWithEmailAndPassword(email, password, new AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                handleFirebaseAuthSuccess(user);
            }

            @Override
            public void onError(Exception exception) {
                String errorMessage = "Ошибка при входе: " + exception.getMessage();
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                Log.e(TAG, errorMessage, exception);
            }
        });
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false;
        }

        return true;
    }

    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            return false;
        }

        if (password.length() < 6) {
            return false;
        }

        return true;
    }

    private void signInWithGoogle() {
        // Проверяем, прикреплен ли фрагмент к активности
        if (!isAdded()) {
            Log.e(TAG, "Попытка авторизации через Google, когда фрагмент не прикреплен к активности");
            return;
        }
        
        try {
            firebaseAuthManager.signInWithGoogle(requireActivity());
        } catch (IllegalStateException e) {
            // Если Google Sign In не был инициализирован, инициализируем его и пробуем снова
            Log.w(TAG, "Google Sign In не был инициализирован, повторная инициализация");
            
            try {
                String webClientId = getString(R.string.default_web_client_id);
                firebaseAuthManager.initGoogleSignIn(requireContext(), webClientId);
                firebaseAuthManager.signInWithGoogle(requireActivity());
            } catch (Exception ex) {
                Log.e(TAG, "Ошибка при инициализации Google Sign In", ex);
                
                if (isAdded()) {
                    try {
                        Toast.makeText(requireContext(), "Ошибка инициализации входа через Google", Toast.LENGTH_LONG).show();
                    } catch (Exception toastEx) {
                        Log.e(TAG, "Ошибка при показе Toast", toastEx);
                    }
                }
            }
        }
    }

    private void handleFirebaseAuthSuccess(FirebaseUser user) {
        if (user != null) {
            String email = user.getEmail();
            String firebaseId = user.getUid();

            UserService userService = new UserService();
            userService.loginFirebaseUser(email, firebaseId, new UserService.UserCallback() {
                @Override
                public void onSuccess(ApiResponse response) {
                    saveUserDataAndRedirect(user, response);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Ошибка при входе: " + errorMessage);
                    
                    // Проверяем, прикреплен ли фрагмент к активности
                    if (isAdded()) {
                        try {
                            Toast.makeText(requireContext(), "Ошибка при входе: " + errorMessage, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при показе Toast", e);
                        }
                    }
                    
                    // Выход из Firebase в любом случае
                    firebaseAuthManager.signOut();
                }
            });
        }
    }

    private void saveUserDataAndRedirect(FirebaseUser user, ApiResponse response) {
        // Сохраняем данные пользователя
        preferences.putString("userId", response.getUserId());
        preferences.putString("userName", user.getDisplayName() != null ? user.getDisplayName() : "Пользователь");
        preferences.putInt("permission", response.getPermission());
        preferences.putString("userEmail", user.getEmail());
        preferences.putString("firebaseId", user.getUid());

        // Проверяем, прикреплен ли фрагмент к активности перед вызовом UI-методов
        if (isAdded()) {
            try {
                // Обновляем UI - заменяем текущий фрагмент на ProfileFragment
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .commit();

                Toast.makeText(requireContext(), "Вход выполнен успешно", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при переходе на ProfileFragment", e);
            }
        } else {
            Log.d(TAG, "Фрагмент не прикреплен к активности, пропускаем обновление UI");
            // Так как фрагмент не прикреплен, мы не можем обновить UI,
            // но данные пользователя уже сохранены, поэтому при следующем запуске
            // приложение определит, что пользователь авторизован
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + 
                  ", resultCode=" + resultCode + 
                  ", data=" + (data != null ? "не null" : "null") +
                  ", ожидаемый RC_SIGN_IN=" + RC_SIGN_IN);

        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "Получен результат для Google Sign In, передаю в FirebaseAuthManager");
            firebaseAuthManager.handleGoogleSignInResult(data, new AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    Log.d(TAG, "Google Sign In успешно, пользователь: " + 
                              (user != null ? user.getEmail() : "null"));
                    handleFirebaseAuthSuccess(user);
                }

                @Override
                public void onError(Exception exception) {
                    String errorMessage = "Не удалось войти через Google: " + exception.getMessage();
                    Log.e(TAG, errorMessage, exception);
                    
                    // Проверяем, прикреплен ли фрагмент к активности
                    if (isAdded()) {
                        try {
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при показе Toast", e);
                        }
                    }
                }
            });
        } else {
            Log.d(TAG, "Получен неизвестный requestCode: " + requestCode);
        }
    }
} 
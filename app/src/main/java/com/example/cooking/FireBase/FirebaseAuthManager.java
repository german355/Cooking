package com.example.cooking.FireBase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.cooking.FireBase.AuthCallback;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Основной класс для работы с Firebase Authentication
 */
public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";
    private static FirebaseAuthManager instance;
    private final FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    
    // Константа для запроса авторизации через Google
    public static final int RC_SIGN_IN = 9001;

    private FirebaseAuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseAuthManager getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthManager();
        }
        return instance;
    }

    /**
     * Инициализирует клиент для входа через Google
     * @param context контекст приложения
     * @param defaultWebClientId строковый идентификатор из google-services.json
     */
    public void initGoogleSignIn(Context context, String defaultWebClientId) {
        Log.d(TAG, "Initializing Google Sign In with client ID: " + defaultWebClientId);
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(defaultWebClientId)
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(context, gso);
            
            // Проверка, что клиент успешно инициализирован
            if (googleSignInClient != null) {
                Log.d(TAG, "Google Sign In client initialized successfully");
            } else {
                Log.e(TAG, "Google Sign In client is null after initialization");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Google Sign In client", e);
            Toast.makeText(context, "Ошибка настройки Google Sign In", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Проверяет, авторизован ли пользователь в данный момент
     * @return true, если пользователь авторизован, иначе false
     */
    public boolean isUserSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Возвращает текущего пользователя Firebase
     * @return объект FirebaseUser или null, если пользователь не авторизован
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Регистрация нового пользователя по email и паролю
     * @param email email для регистрации
     * @param password пароль для регистрации
     * @param authCallback интерфейс для обработки результата
     */
    public void registerWithEmailAndPassword(String email, String password, AuthCallback authCallback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        authCallback.onSuccess(user);
                    } else {
                        authCallback.onError(task.getException());
                    }
                });
    }

    /**
     * Вход с использованием email и пароля
     * @param email email для входа
     * @param password пароль для входа
     * @param authCallback интерфейс для обработки результата
     */
    public void signInWithEmailAndPassword(String email, String password, AuthCallback authCallback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        authCallback.onSuccess(user);
                    } else {
                        authCallback.onError(task.getException());
                    }
                });
    }

    /**
     * Начинает процесс входа через Google аккаунт
     * @param activity активность для запуска Intent
     */
    public void signInWithGoogle(Activity activity) {
        if (googleSignInClient == null) {
            Log.e(TAG, "Google Sign In client not initialized");
            Toast.makeText(activity, "Ошибка инициализации Google Sign In", Toast.LENGTH_SHORT).show();
            throw new IllegalStateException("Google Sign In was not initialized. Call initGoogleSignIn() first.");
        }
        
        try {
            Log.d(TAG, "Starting Google Sign In flow");
            Intent signInIntent = googleSignInClient.getSignInIntent();
            if (signInIntent != null) {
                Log.d(TAG, "Got sign in intent, starting activity for result with RC_SIGN_IN=" + RC_SIGN_IN);
                activity.startActivityForResult(signInIntent, RC_SIGN_IN);
            } else {
                Log.e(TAG, "Sign in intent is null");
                Toast.makeText(activity, "Ошибка запуска Google Sign In", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting Google Sign In flow", e);
            Toast.makeText(activity, "Ошибка запуска Google Sign In: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Обработка результата входа через Google аккаунт
     * @param data данные из onActivityResult
     * @param authCallback интерфейс для обработки результата
     */
    public void handleGoogleSignInResult(Intent data, AuthCallback authCallback) {
        try {
            Log.d(TAG, "Handling Google Sign In result, intent: " + 
                  (data != null ? "not null" : "null"));
            
            if (data == null) {
                Log.e(TAG, "Google Sign In data is null");
                authCallback.onError(new Exception("Данные для Google Sign In отсутствуют"));
                return;
            }
            
            // Логируем extras из intent для отладки
            if (data.getExtras() != null) {
                for (String key : data.getExtras().keySet()) {
                    Log.d(TAG, "Intent extra - key: " + key + 
                          ", value: " + String.valueOf(data.getExtras().get(key)));
                }
            } else {
                Log.d(TAG, "Intent extras are null");
            }
            
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            Log.d(TAG, "Created task from intent: " + (task != null ? "not null" : "null") + 
                  ", isComplete: " + (task != null ? task.isComplete() : "N/A") + 
                  ", isSuccessful: " + (task != null && task.isComplete() ? task.isSuccessful() : "N/A"));
            
            // Регистрируем слушатели задачи для более детального логирования
            task.addOnSuccessListener(account -> {
                Log.d(TAG, "Google Sign In successful, account email: " + account.getEmail() + 
                      ", ID: " + account.getId() + ", has idToken: " + (account.getIdToken() != null));
                
                if (account.getIdToken() == null) {
                    Log.e(TAG, "ID token is null, cannot authenticate with Firebase");
                    authCallback.onError(new Exception("ID токен отсутствует"));
                    return;
                }
                
                firebaseAuthWithGoogle(account.getIdToken(), authCallback);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Google Sign In task failed with exception", e);
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    int statusCode = apiException.getStatusCode();
                    Log.e(TAG, "Google Sign In API Exception with status code: " + statusCode);
                    
                    // Добавим более детальное логирование для различных кодов
                    if (statusCode == 10) {
                        Log.e(TAG, "DEVELOPER_ERROR (10): This error occurs when the client ID comes " +
                               "from a different project than the project that the app is running in, " +
                               "or if SHA-1 fingerprint is not configured properly in Firebase Console.");
                    } else if (statusCode == 12501) {
                        Log.e(TAG, "SIGN_IN_CANCELLED (12501): User cancelled the sign-in flow.");
                    } else if (statusCode == 7) {
                        Log.e(TAG, "NETWORK_ERROR (7): A network error occurred. Retry might solve this.");
                    }
                }
                authCallback.onError(e);
            });
            
            try {
                // Попытка получить результат напрямую (может вызвать исключение)
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Got Google Sign In account result directly, proceeding with Firebase auth");
                firebaseAuthWithGoogle(account.getIdToken(), authCallback);
            } catch (ApiException e) {
                // Ничего не делаем здесь, так как обработка ошибки будет через OnFailureListener
                Log.d(TAG, "Caught ApiException when trying to get result directly. " +
                      "This is expected behavior, failure will be handled by OnFailureListener");
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during Google Sign In", e);
            authCallback.onError(e);
        }
    }

    /**
     * Аутентификация в Firebase с использованием токена Google
     * @param idToken токен аутентификации Google
     * @param authCallback интерфейс для обработки результата
     */
    private void firebaseAuthWithGoogle(String idToken, AuthCallback authCallback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        authCallback.onSuccess(user);
                    } else {
                        authCallback.onError(task.getException());
                    }
                });
    }

    /**
     * Выход из аккаунта
     */
    public void signOut() {
        firebaseAuth.signOut();
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
    }
} 
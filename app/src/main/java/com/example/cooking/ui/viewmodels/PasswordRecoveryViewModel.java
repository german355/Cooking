package com.example.cooking.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Patterns;

import com.example.cooking.data.models.PasswordResetRequest;
import com.example.cooking.data.models.PasswordResetResponse;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.services.RetrofitClient; // Предполагаем, что RetrofitClient здесь

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// import com.example.cooking.data.repository.UserRepository; // Пример
// import com.example.cooking.domain.usecase.PasswordRecoveryUseCase; // Пример

public class PasswordRecoveryViewModel extends ViewModel {

    private final MutableLiveData<String> _email = new MutableLiveData<>();
    public LiveData<String> email = _email;

    private final MutableLiveData<RecoveryStatus> _recoveryStatus = new MutableLiveData<>();
    public LiveData<RecoveryStatus> recoveryStatus = _recoveryStatus;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    private ApiService apiService;

    // Конструктор
    public PasswordRecoveryViewModel() {
        // Получаем экземпляр сервиса.
        apiService = RetrofitClient.getClient().create(ApiService.class);
    }

    // public PasswordRecoveryViewModel(AuthApiService apiService) { // Вариант с DI
    //     this.authApiService = apiService;
    // }

    public void onEmailChanged(String newEmail) {
        _email.setValue(newEmail);
    }

    public void requestPasswordRecovery() {
        _isLoading.setValue(true);
        String currentEmail = _email.getValue();

        if (currentEmail == null || currentEmail.trim().isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            _recoveryStatus.setValue(new RecoveryStatus.Error("Введите корректный email"));
            _isLoading.setValue(false);
            return;
        }

        // Сетевой запрос
        PasswordResetRequest request = new PasswordResetRequest(currentEmail);
        apiService.requestPasswordReset(request).enqueue(new Callback<PasswordResetResponse>() {
            @Override
            public void onResponse(Call<PasswordResetResponse> call, Response<PasswordResetResponse> response) {
                _isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    _recoveryStatus.postValue(new RecoveryStatus.Success(response.body().getMessage()));
                } else {
                    // Обработка ошибок сервера (например, код 4xx, 5xx)
                    // Можно получить более детальное сообщение из response.errorBody()
                    String errorMessage = "Ошибка сервера. Попробуйте позже.";
                    if (response.errorBody() != null) {
                        try {
                            // Попытка извлечь сообщение об ошибке, если сервер его отправляет в известном формате
                            // errorMessage = response.errorBody().string(); // Это нужно делать осторожно
                        } catch (Exception e) {
                            // Log.e("ViewModel", "Error parsing error body", e);
                        }
                    }
                    _recoveryStatus.postValue(new RecoveryStatus.Error(errorMessage));
                }
            }

            @Override
            public void onFailure(Call<PasswordResetResponse> call, Throwable t) {
                _isLoading.postValue(false);
                _recoveryStatus.postValue(new RecoveryStatus.Error("Ошибка сети: " + t.getMessage()));
            }
        });
    }

    // Запечатанный класс для состояния восстановления (эквивалент sealed class в Kotlin)
    public static abstract class RecoveryStatus {
        private RecoveryStatus() {}

        public static final class Success extends RecoveryStatus {
            public final String message;
            public Success(String message) {
                this.message = message;
            }
        }

        public static final class Error extends RecoveryStatus {
            public final String errorMessage;
            public Error(String errorMessage) {
                this.errorMessage = errorMessage;
            }
        }
    }
} 
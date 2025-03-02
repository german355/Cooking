package com.example.cooking;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONObject;
import okhttp3.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class LoginTask extends AsyncTask<String, Void, LoginTask.Result> {
    private final LoginCallback callback;
    private final OkHttpClient client;

    // Создаем класс для хранения результата
    public static class Result {
        final boolean success;
        final String errorMessage;
        final String userId;

        private Result(boolean success, String userId, String errorMessage) {
            this.success = success;
            this.userId = userId;
            this.errorMessage = errorMessage;
        }

        static Result success(String userId) {
            return new Result(true, userId, null);
        }

        static Result failure(String errorMessage) {
            return new Result(false, null, errorMessage);
        }
    }

    public interface LoginCallback {
        void onLoginSuccess(String userId);
        void onLoginFailure(String error);
    }

    public LoginTask(LoginCallback callback) {
        this.callback = callback;

        // Настраиваем клиент с повторами и логированием
        this.client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
    }

    @Override
    protected Result doInBackground(String... params) {
        String email = params[0];
        String password = params[1];

        // 1. Формируем JSON-тело с проверкой
        JSONObject jsonInput = new JSONObject();
        try {
            jsonInput.put("email", email);
            jsonInput.put("password", password);
        } catch (Exception e) {
            Log.e("LoginTask", "JSON creation error", e);
            return Result.failure("Invalid input data");
        }

        // 2. Создаём запрос
        RequestBody body = RequestBody.create(
                jsonInput.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("http://g3.veroid.network:19029/login")
                .post(body)
                .addHeader("Accept", "application/json")
                .build();

        // 3. Выполняем запрос
        try (Response response = client.newCall(request).execute()) {
            // 4. Обрабатываем ответ
            if (!response.isSuccessful()) {
                return Result.failure("HTTP Error: " + response.code());
            }

            final ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return Result.failure("Empty server response");
            }

            // Читаем ответ как строку с проверкой
            String responseData;
            try {
                responseData = responseBody.string();
                if (responseData.isEmpty()) {
                    return Result.failure("Empty response body");
                }
            } catch (IOException e) {
                Log.e("LoginTask", "Response read error", e);
                return Result.failure("Connection interrupted");
            }

            Log.d("LoginTask", "Raw response: " + responseData);

            // 5. Парсим JSON
            try {
                JSONObject jsonResponse = new JSONObject(responseData);
                boolean success = jsonResponse.optBoolean("success", false);
                if (success) {
                    // Извлекаем userId из ответа
                    String userId = jsonResponse.optString("userId", "");
                    return Result.success(userId);
                } else {
                    String message = jsonResponse.optString("message", "Invalid credentials");
                    return Result.failure(message);
                }
            } catch (Exception e) {
                Log.e("LoginTask", "JSON parse error", e);
                return Result.failure("Invalid server response");
            }

        } catch (SocketTimeoutException e) {
            return Result.failure("Connection timeout");
        } catch (IOException e) {
            Log.e("LoginTask", "Network error: " + e.getClass().getSimpleName(), e);
            return Result.failure("Network error: " + e.getMessage());
        } catch (Exception e) {
            Log.e("LoginTask", "Unexpected error", e);
            return Result.failure("System error: " + e.getClass().getSimpleName());
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        if (callback == null) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            if (result.success) {
                callback.onLoginSuccess(result.userId);
            } else {
                callback.onLoginFailure(
                        result.errorMessage != null ? result.errorMessage : "Unknown error occurred"
                );
            }
        });
    }
}

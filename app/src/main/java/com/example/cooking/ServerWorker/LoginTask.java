package com.example.cooking.ServerWorker;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONObject;
import okhttp3.*;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class LoginTask extends AsyncTask<String, Void, LoginTask.Result> {
    private final LoginCallback callback;
    private final OkHttpClient client;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    // Создаем класс для хранения результата
    public static class Result {
        final boolean success;
        final String userId;
        final String userName;
        final String errorMessage;

        private Result(boolean success, String userId, String userName, String errorMessage) {
            this.success = success;
            this.userId = userId;
            this.userName = userName;
            this.errorMessage = errorMessage;
        }

        static Result success(String userId, String userName) {
            return new Result(true, userId, userName, null);
        }

        static Result failure(String errorMessage) {
            return new Result(false, null, null, errorMessage);
        }
    }

    public interface LoginCallback {
        void onLoginSuccess(String userId, String userName);
        void onLoginFailure(String error);
    }

    public LoginTask(LoginCallback callback) {
        this.callback = callback;

        // Настраиваем клиент с повторами
        this.client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
    }

    @Override
    protected Result doInBackground(String... params) {
        if (params.length < 2) {
            return Result.failure("Missing credentials");
        }

        String email = params[0];
        String password = params[1];
        
        // Реализуем механизм повторных попыток
        IOException lastException = null;
        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                // 1. Формируем JSON-тело
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
                    String responseData = responseBody.string();
                    if (responseData.isEmpty()) {
                        return Result.failure("Empty response body");
                    }

                    Log.d("LoginTask", "Raw response: " + responseData);

                    // 5. Парсим JSON
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        boolean success = jsonResponse.optBoolean("success", false);
                        if (success) {
                            String userId = jsonResponse.optString("userId", "");
                            String userName = jsonResponse.optString("name", "");
                            return Result.success(userId, userName);
                        } else {
                            String message = jsonResponse.optString("message", "Login failed");
                            return Result.failure(message);
                        }
                    } catch (Exception e) {
                        Log.e("LoginTask", "JSON parse error", e);
                        return Result.failure("Invalid server response");
                    }
                }
                
            } catch (SocketTimeoutException e) {
                lastException = e;
                Log.e("LoginTask", "Attempt " + (attempt + 1) + " failed: Connection timeout", e);
                // Дождемся немного перед следующей попыткой
                try {
                    Thread.sleep(1000 * (attempt + 1));
                } catch (InterruptedException ignored) {}
                
            } catch (IOException e) {
                lastException = e;
                Log.e("LoginTask", "Attempt " + (attempt + 1) + " failed: " + e.getClass().getSimpleName(), e);
                // Дождемся немного перед следующей попыткой
                try {
                    Thread.sleep(1000 * (attempt + 1));
                } catch (InterruptedException ignored) {}
            }
        }
        
        // Если дошли сюда, значит все попытки завершились неудачно
        if (lastException instanceof ProtocolException) {
            return Result.failure("Ошибка соединения: соединение прервано. Пожалуйста, проверьте подключение к интернету.");
        }
        return Result.failure("Не удалось подключиться к серверу после " + MAX_RETRY_ATTEMPTS + " попыток. " + 
                             (lastException != null ? lastException.getMessage() : ""));
    }

    @Override
    protected void onPostExecute(Result result) {
        if (callback == null) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            if (result.success) {
                callback.onLoginSuccess(result.userId, result.userName);
            } else {
                callback.onLoginFailure(
                        result.errorMessage != null ? result.errorMessage : "Unknown error occurred"
                );
            }
        });
    }
}
package com.example.cooking.ServerWorker;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class RegistrationTask extends AsyncTask<String, Void, RegistrationTask.Result> {
    private final RegistrationCallback callback;
    private final OkHttpClient client;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    // Класс результата
    public static class Result {
        final boolean success;
        final String errorMessage;

        private Result(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        static Result success() {
            return new Result(true, null);
        }

        static Result failure(String errorMessage) {
            return new Result(false, errorMessage);
        }
    }

    public interface RegistrationCallback {
        void onRegistrationSuccess();
        void onRegistrationFailure(String error);
    }

    public RegistrationTask(RegistrationCallback callback) {
        this.callback = callback;

        // Настраиваем клиент с повторами и таймаутами
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
        if (params.length < 3) {
            return Result.failure("Необходимо передать email, пароль и имя пользователя");
        }

        String email = params[0];
        String password = params[1];
        String name = params[2]; // Получаем имя пользователя из параметров
        
        // Реализуем механизм повторных попыток
        IOException lastException = null;
        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                // 1. Формируем JSON-тело
                JSONObject jsonInput = new JSONObject();
                try {
                    jsonInput.put("email", email);
                    jsonInput.put("password", password);
                    jsonInput.put("name", name); // Добавляем имя к JSON объекту
                } catch (Exception e) {
                    Log.e("RegistrationTask", "JSON creation error", e);
                    return Result.failure("Invalid input data");
                }

                // 2. Создаём запрос
                RequestBody body = RequestBody.create(
                        jsonInput.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url("http://g3.veroid.network:19029/register")
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

                    // 5. Парсим JSON
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        boolean success = jsonResponse.optBoolean("success", false);
                        if (success) {
                            return Result.success();
                        } else {
                            String message = jsonResponse.optString("message", "Registration failed");
                            return Result.failure(message);
                        }
                    } catch (Exception e) {
                        Log.e("RegistrationTask", "JSON parse error", e);
                        return Result.failure("Invalid server response");
                    }
                }
                
            } catch (SocketTimeoutException e) {
                lastException = e;
                Log.e("RegistrationTask", "Attempt " + (attempt + 1) + " failed: Connection timeout", e);
                // Дождемся немного перед следующей попыткой
                try {
                    Thread.sleep(1000 * (attempt + 1));
                } catch (InterruptedException ignored) {}
                
            } catch (IOException e) {
                lastException = e;
                Log.e("RegistrationTask", "Attempt " + (attempt + 1) + " failed: " + e.getClass().getSimpleName(), e);
                
                // Проверяем содержание сообщения об ошибке
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("unexpected end of stream")) {
                    // Это может быть случай, когда соединение прервалось после успешной регистрации
                    // Проверим, был ли пользователь успешно зарегистрирован
                    try {
                        // Проверяем, существует ли уже пользователь с этими данными
                        JSONObject checkJson = new JSONObject();
                        checkJson.put("email", email);
                        checkJson.put("password", password);
                        
                        RequestBody checkBody = RequestBody.create(
                                checkJson.toString(),
                                MediaType.parse("application/json; charset=utf-8")
                        );
                        
                        Request checkRequest = new Request.Builder()
                                .url("http://g3.veroid.network:19029/login")
                                .post(checkBody)
                                .addHeader("Accept", "application/json")
                                .build();
                        
                        try (Response checkResponse = client.newCall(checkRequest).execute()) {
                            if (checkResponse.isSuccessful()) {
                                ResponseBody checkResponseBody = checkResponse.body();
                                if (checkResponseBody != null) {
                                    String checkData = checkResponseBody.string();
                                    JSONObject checkResult = new JSONObject(checkData);
                                    if (checkResult.optBoolean("success", false)) {
                                        // Пользователь существует и был успешно аутентифицирован,
                                        // значит регистрация прошла успешно
                                        return Result.success();
                                    }
                                }
                            }
                        } catch (Exception checkEx) {
                            // Игнорируем ошибку проверки, продолжаем с основной ошибкой
                            Log.e("RegistrationTask", "Check login failed", checkEx);
                        }
                    } catch (Exception jsonEx) {
                        // Игнорируем ошибку JSON, продолжаем с основной ошибкой
                    }
                }
                
                // Дождемся немного перед следующей попыткой
                try {
                    Thread.sleep(1000 * (attempt + 1));
                } catch (InterruptedException ignored) {}
            }
        }
        
        // Если дошли сюда, значит все попытки завершились неудачно
        if (lastException instanceof ProtocolException) {
            return Result.failure("Ошибка соединения: соединение прервано. Проверьте подключение к интернету.");
        }
        return Result.failure("Не удалось подключиться к серверу после " + MAX_RETRY_ATTEMPTS + " попыток. " + 
                             (lastException != null ? lastException.getMessage() : ""));
    }

    @Override
    protected void onPostExecute(Result result) {
        if (callback == null) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            if (result.success) {
                callback.onRegistrationSuccess();
            } else {
                callback.onRegistrationFailure(
                        result.errorMessage != null ? result.errorMessage : "Unknown error occurred"
                );
            }
        });
    }
}
package com.example.cooking;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import okhttp3.*;

import java.io.IOException;

public class RegistrationTask extends AsyncTask<String, Void, Boolean> {
    private final RegistrationCallback callback;
    private String errorMessage;
    private final OkHttpClient client;

    public interface RegistrationCallback {
        void onRegistrationSuccess();
        void onRegistrationFailure(String error);
    }

    public RegistrationTask(RegistrationCallback callback) {
        this.callback = callback;
        this.client = new OkHttpClient.Builder().build();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        String email = params[0];
        String password = params[1];

        // 1. Формируем JSON-тело
        JSONObject jsonInput = new JSONObject();
        try {
            jsonInput.put("email", email);
            jsonInput.put("password", password);
        } catch (Exception e) {
            Log.e("RegistrationTask", "JSON error", e);
            errorMessage = "Invalid input data";
            return false;
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
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                errorMessage = "Empty server response";
                return false;
            }

            String responseData = responseBody.string();
            Log.d("RegistrationTask", "Response: " + responseData);

            JSONObject jsonResponse = new JSONObject(responseData);
            if (jsonResponse.optBoolean("success", false)) {
                return true;
            } else {
                errorMessage = jsonResponse.optString("message", "Registration failed");
                return false;
            }

        } catch (IOException e) {
            Log.e("RegistrationTask", "Network error", e);
            errorMessage = "Connection error: " + e.getMessage();
            return false;
        } catch (Exception e) {
            Log.e("RegistrationTask", "Unexpected error", e);
            errorMessage = "Error: " + e.getClass().getSimpleName();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (callback == null) return;

        // Гарантируем выполнение в UI-потоке
        new Handler(Looper.getMainLooper()).post(() -> {
            if (success) {
                callback.onRegistrationSuccess();
            } else {
                callback.onRegistrationFailure(
                        errorMessage != null ? errorMessage : "Unknown error"
                );
            }
        });
    }
}

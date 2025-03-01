package com.example.cooking;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RegistrationTask extends AsyncTask<String, Void, Boolean> {
    private RegistrationCallback callback;
    private String errorMessage;

    public interface RegistrationCallback {
        void onRegistrationSuccess();
        void onRegistrationFailure(String error);
    }

    // Конструктор для передачи callback
    public RegistrationTask(RegistrationCallback callback) {
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        // Ожидаем, что params[0] - email, params[1] - password.
        // При необходимости можно передать дополнительные параметры, например, имя пользователя.
        String email = params[0];
        String password = params[1];
        HttpURLConnection conn = null;

        try {
            // Замените URL на адрес вашего сервера для регистрации
            URL url = new URL("http://g3.veroid.network:19029/register");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // Подготовка JSON данных для регистрации
            JSONObject jsonInput = new JSONObject();
            jsonInput.put("email", email);
            jsonInput.put("password", password);
            // Если необходимо, можно добавить дополнительные поля
            // Например, если передан третий параметр, добавить имя пользователя:
            if (params.length > 2) {
                String username = params[2];
                jsonInput.put("username", username);
            }
            String jsonInputString = jsonInput.toString();
            Log.d("RegistrationTask", "Отправка запроса: " + jsonInputString);

            // Отправка данных
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            // Получение кода ответа
            int responseCode = conn.getResponseCode();
            Log.d("RegistrationTask", "Код ответа: " + responseCode);

            // Чтение ответа
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            String responseBody = response.toString();
            Log.d("RegistrationTask", "Ответ сервера: " + responseBody);

            // Анализ ответа сервера
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.has("success")) {
                        boolean success = jsonResponse.getBoolean("success");
                        if (success) {
                            return true;
                        } else {
                            errorMessage = jsonResponse.optString("message", "Ошибка регистрации");
                            return false;
                        }
                    } else {
                        errorMessage = "Некорректный формат ответа сервера";
                        return false;
                    }
                } catch (Exception e) {
                    Log.e("RegistrationTask", "Ошибка обработки JSON", e);
                    errorMessage = "Ошибка обработки ответа сервера";
                    return false;
                }
            } else {
                errorMessage = "Ошибка сервера: " + responseCode;
                return false;
            }
        } catch (Exception e) {
            Log.e("RegistrationTask", "Ошибка при выполнении запроса регистрации", e);
            errorMessage = "Ошибка соединения: " + e.getMessage();
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            callback.onRegistrationSuccess();
        } else {
            callback.onRegistrationFailure(errorMessage);
        }
    }
}

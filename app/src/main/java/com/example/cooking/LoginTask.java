package com.example.cooking;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

// AsyncTask для выполнения POST-запроса на сервер
public class LoginTask extends AsyncTask<Void, Void, String> {
    private String email;
    private String password;

    // Конструктор для передачи email и password
    public LoginTask(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            // Если сервер запущен на вашем компьютере, используйте 10.0.2.2 вместо localhost
            URL url = new URL("http://10.0.2.2:3000/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            // Указываем, что отправляем JSON
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Формируем JSON-объект с email и password
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("email", email);
            jsonParam.put("password", password);

            // Отправляем JSON в теле запроса
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonParam.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Читаем ответ сервера
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Обработка результата запроса в основном потоке
    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            Log.d("LoginResult", result);
            try {
                JSONObject responseJson = new JSONObject(result);
                boolean success = responseJson.getBoolean("success");
                if (success) {
                    // Логин успешен, выполняйте нужные действия (например, переход на другой экран)
                    Log.d("Login", "Пользователь найден");
                } else {
                    // Неверный логин или пароль
                    Log.d("Login", "Неверный email или пароль");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e("LoginTask", "Ошибка при выполнении запроса");
        }
    }
}


package com.example.cooking.auth;

import android.util.Log;
import com.example.cooking.config.ServerConfig;
import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import org.json.JSONObject;

/**
 * Authenticator для автоматического обновления access-токена при получении 401.
 */
public class TokenAuthenticator implements Authenticator {
    private static final String TAG = "TokenAuthenticator";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        // Предотвращаем бесконечный цикл: если уже пытались обновить, выходим
        if (responseCount(response) >= 2) {
            return null;
        }
        String refreshToken = TokenStorage.getRefresh();
        if (refreshToken == null || refreshToken.isEmpty()) {
            Log.w(TAG, "Нет refresh токена, очистка сессии");
            TokenStorage.clear();
            return null;
        }
        try {
            OkHttpClient client = new OkHttpClient();
            Request refreshRequest = new Request.Builder()
                    .url(ServerConfig.BASE_API_URL + "auth/refresh")
                    .post(RequestBody.create("{}", JSON))
                    .header("Authorization", "Bearer " + refreshToken)
                    .build();

            Response refreshResponse = client.newCall(refreshRequest).execute();
            if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) {
                Log.e(TAG, "Ошибка обновления токена: " + refreshResponse.code());
                TokenStorage.clear();
                return null;
            }
            String body = refreshResponse.body().string();
            refreshResponse.body().close();
            JSONObject json = new JSONObject(body);
            boolean success = json.optBoolean("success", false);
            if (!success) {
                Log.e(TAG, "Обновление токена неуспешно: " + body);
                TokenStorage.clear();
                return null;
            }
            String newAccess = json.optString("access_token", null);
            if (newAccess == null) {
                Log.e(TAG, "В ответе нет access_token");
                TokenStorage.clear();
                return null;
            }
            TokenStorage.saveAccess(newAccess);
            Log.d(TAG, "Access токен обновлен");
            // Строим новый запрос с новым токеном
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + newAccess)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка в TokenAuthenticator", e);
            TokenStorage.clear();
            return null;
        }
    }

    private int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}
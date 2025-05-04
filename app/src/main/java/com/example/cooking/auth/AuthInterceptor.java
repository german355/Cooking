package com.example.cooking.auth;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Интерцептор для добавления Authorization header с access токеном из
 * TokenStorage
 */
public class AuthInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String token = TokenStorage.getAccess();

        if (token != null && !token.isEmpty()) {
            Request authorizedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
            return chain.proceed(authorizedRequest);
        }

        return chain.proceed(originalRequest);
    }
}
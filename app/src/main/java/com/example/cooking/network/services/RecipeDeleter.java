package com.example.cooking.network.services;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.cooking.config.ServerConfig;
import com.example.cooking.data.repositories.RecipeRepository;
import org.json.JSONObject;
import java.io.IOException;
import java.lang.ref.WeakReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecipeDeleter {

    private static final String TAG = "RecipeDeleter";
    private static final String DELETE_ENDPOINT = "/deliterecipe";
    private final OkHttpClient client;
    private final Context context;

    public interface DeleteRecipeCallback {
        void onDeleteSuccess();
        void onDeleteFailure(String error);
    }



    public RecipeDeleter(Context context) {
        client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        this.context = context;
    }

    public void deleteRecipe(final int recipeId, final String userId, final int permission, final DeleteRecipeCallback callback) {
        new DeleteRecipeTask(this, recipeId, userId, permission,  callback).execute();
    }

    // Статический inner-класс AsyncTask с использованием WeakReference на RecipeDeleter
    private static class DeleteRecipeTask extends AsyncTask<Void, Void, Boolean> {
        private final WeakReference<RecipeDeleter> deleterRef;
        private final int recipeId;
        private final String userId;
        private final int permission;
        private final DeleteRecipeCallback callback;
        private String errorMessage = "";

        DeleteRecipeTask(RecipeDeleter deleter, int recipeId, String userId, int permission, DeleteRecipeCallback callback) {
            this.deleterRef = new WeakReference<>(deleter);
            this.recipeId = recipeId;
            this.userId = userId;
            this.permission = permission;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            RecipeDeleter deleter = deleterRef.get();
            if (deleter == null) {
                errorMessage = "RecipeDeleter is no longer available";
                return false;
            }
            try {
                JSONObject json = new JSONObject();
                json.put("id", recipeId);
                json.put("userId", userId);
                json.put("permission", permission);

                MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(json.toString(), JSON_TYPE);

                Request request = new Request.Builder()
                        .url(ServerConfig.getFullUrl(DELETE_ENDPOINT))
                        .post(body)
                        .build();

                Response response = deleter.client.newCall(request).execute();
                if (response.isSuccessful()) {
                    return true;
                } else {
                    // Специальная обработка для кода 403 (Forbidden)
                    if (response.code() == 403) {
                        errorMessage = "У вас нет прав на удаление этого рецепта. Только автор рецепта или администратор могут удалять рецепты.";
                    } else {
                        errorMessage = "Server error: " + response.code();
                    }
                    return false;
                }
            } catch (IOException e) {
                Log.e(TAG, "Ошибка при удалении рецепта", e);
                errorMessage = e.getMessage();
                return false;
            } catch (Exception e) {
                Log.e(TAG, "Непредвиденная ошибка", e);
                errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                callback.onDeleteSuccess();
                RecipeDeleter deleter = deleterRef.get();
                if (deleter != null) {
                    // Очищаем основной кэш рецептов
                    RecipeRepository repository = new RecipeRepository(deleter.context);
                    repository.clearCache();
                    
                    // Также очищаем кэш лайкнутых рецептов
                    // Получаем текущего пользователя из SharedPreferences
                    com.example.cooking.utils.MySharedPreferences prefs = new com.example.cooking.utils.MySharedPreferences(deleter.context);
                    String currentUserId = prefs.getString("userId", "0");
                    
                    // Очищаем кэш лайкнутых рецептов
                    com.example.cooking.data.repositories.LikedRecipesRepository likedRepo = 
                        new com.example.cooking.data.repositories.LikedRecipesRepository(deleter.context);
                    likedRepo.clearCache(currentUserId);
                    
                    Log.d(TAG, "Все кэши очищены после удаления рецепта");
                }
                Log.d(TAG, "Рецепт был удален");
            } else {
                callback.onDeleteFailure(errorMessage);
            }
        }
    }
}

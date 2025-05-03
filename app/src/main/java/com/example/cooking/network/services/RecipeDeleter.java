package com.example.cooking.network.services;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.cooking.config.ServerConfig;
import com.example.cooking.data.repositories.RecipeRepository;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.data.repositories.RecipeLocalRepository;
import com.example.cooking.utils.MySharedPreferences;
import org.json.JSONObject;
import java.io.IOException;
import java.lang.ref.WeakReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.example.cooking.network.services.HttpClientManager;

public class RecipeDeleter {

    private static final String TAG = "RecipeDeleter";
    private static final String DELETE_ENDPOINT = "/recipes/delete";
    private final OkHttpClient client;
    private final Context context;

    public interface DeleteRecipeCallback {
        void onDeleteSuccess();
        void onDeleteFailure(String error);
    }



    public RecipeDeleter(Context context) {
        client = HttpClientManager.getClient();
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
                // Формируем DELETE запрос к /recipes/{id} с заголовками авторизации
                Request request = new Request.Builder()
                        .url(ServerConfig.getFullUrl("/recipes/" + recipeId))
                        .delete()
                        .header("X-User-ID", userId)
                        .header("X-User-Permission", String.valueOf(permission))
                        .build();

                Response response = deleter.client.newCall(request).execute();
                if (response.isSuccessful()) {
                    return true;
                } else {
                    // Специальная обработка для кода 403 (Forbidden)
                    if (response.code() == 403) {
                        errorMessage = "У вас нет прав на удаление этого рецепта. Только автор рецепта или администратор могут удалять рецепты.";
                    } else {
                         String errorBody = response.body() != null ? response.body().string() : "null";
                         errorMessage = "Server error: " + response.code() + " Body: " + errorBody;
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
                    // 1. Очищаем основной кэш рецептов (SharedPreferences + OkHttp)
                    RecipeRepository repository = new RecipeRepository(deleter.context);
                    repository.clearCache();
                    Log.d(TAG, "Кэш RecipeRepository очищен.");

                    // 2. Удаляем запись об этом рецепте из локальной базы лайкнутых, если она там была
                    MySharedPreferences prefs = new MySharedPreferences(deleter.context);
                    String currentUserId = prefs.getString("userId", "0");

                    if (!currentUserId.equals("0")) {
                        LikedRecipesRepository likedRepo = new LikedRecipesRepository(deleter.context);
                        likedRepo.deleteLikedRecipeLocal(recipeId, currentUserId);
                        Log.d(TAG, "Запись о лайке для удаленного рецепта (ID: " + recipeId + ") удалена из локальной базы лайков.");
                    } else {
                        Log.w(TAG, "Не удалось получить currentUserId, удаление лайка пропущено.");
                    }

                    // 3. Удаляем сам рецепт из основной локальной базы данных (Room)
                    RecipeLocalRepository localRepository = new RecipeLocalRepository(deleter.context);
                    localRepository.deleteRecipe(recipeId);
                    // Лог об удалении будет внутри deleteRecipe

                    Log.d(TAG, "Кэш очищен, локальный лайк удален (если был), рецепт удален из локальной БД после успешного удаления с сервера.");
                }
                Log.d(TAG, "Рецепт был удален с сервера.");
            } else {
                callback.onDeleteFailure(errorMessage);
            }
        }
    }
}

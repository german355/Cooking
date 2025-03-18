package com.example.cooking.ServerWorker;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;
import java.io.IOException;
import java.lang.ref.WeakReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewLike {

    private static final String TAG = "NewLike";
    private static final String API_URL = "http://g3.veroid.network:19029";
    private final OkHttpClient client;
    private final Context context;

    public interface DeleteRecipeCallback {
        void onDeleteSuccess();
        void onDeleteFailure(String error);
    }



    public NewLike(Context context) {
        client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        this.context = context;
    }

    public void likeRecipe(final int recipeId, final String userId, final DeleteRecipeCallback callback) {
        new LikeRecipeTask(this, recipeId, userId, callback).execute();
    }

    // Статический inner-класс AsyncTask с использованием WeakReference на RecipeDeleter
    private static class LikeRecipeTask extends AsyncTask<Void, Void, Boolean> {
        private final WeakReference<NewLike> deleterRef;
        private final int recipeId;
        private final String userId;
        private final DeleteRecipeCallback callback;
        private String errorMessage = "";

        LikeRecipeTask(NewLike deleter, int recipeId, String userId, DeleteRecipeCallback callback) {
            this.deleterRef = new WeakReference<>(deleter);
            this.recipeId = recipeId;
            this.userId = userId;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            NewLike deleter = deleterRef.get();
            if (deleter == null) {
                errorMessage = "RecipeLike is no longer available";
                return false;
            }
            try {
                JSONObject json = new JSONObject();
                Log.d("LikeId", String.valueOf(recipeId));
                json.put("recipeId", recipeId);
                Log.d("LikeUserId", userId);
                json.put("userId", userId);

                MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(json.toString(), JSON_TYPE);

                Request request = new Request.Builder()
                        .url(API_URL + "/like")
                        .post(body)
                        .build();

                Response response = deleter.client.newCall(request).execute();
                if (response.isSuccessful()) {
                    return true;
                } else {
                    errorMessage = "Server error: " + response.code();
                    return false;
                }
            } catch (IOException e) {
                Log.e(TAG, "Ошибка при лайке", e);
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
                NewLike deleter = deleterRef.get();
                RecipeRepository repository = new RecipeRepository(deleter.context);
                repository.clearCache();
                Log.d("Recipe", "Рецепт был лайкнут");
            } else {
                callback.onDeleteFailure(errorMessage);
            }
        }
    }
}

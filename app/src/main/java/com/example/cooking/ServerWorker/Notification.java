package com.example.cooking.ServerWorker;

import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
// Класс в ходе разработки.
public class Notification {
    private static Socket msocket;
     static int recipeId;
    public  static boolean reLoad(){
        try{
            msocket = IO.socket("g3.veroid.network:19029/addrecipes");
            msocket.connect();
            msocket.on("new_recipe", new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    recipeId = (int) objects[0];
                    Log.d("RecipeLoad", "Был добавлен новый рецепт");
                }
            });
        } catch (Exception e) {
            Log.d("Socket", "Нет подключения");
        }
        return recipeId == 1;


    }

}

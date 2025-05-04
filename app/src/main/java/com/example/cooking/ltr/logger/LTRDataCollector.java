package com.example.cooking.ltr.logger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.cooking.ltr.config.LTRServerConfig;
import com.example.cooking.ltr.models.Recipe;
import com.example.cooking.ltr.models.SearchResult;
import com.example.cooking.ltr.network.api.LTRApiService;
import com.example.cooking.ltr.network.models.ClickEventRequest;
import com.example.cooking.ltr.network.models.FavoriteActionRequest;
import com.example.cooking.network.services.RetrofitClient;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Класс для сбора данных о взаимодействии пользователя с приложением
 * и отправки их на сервер для анализа и улучшения результатов
 */
public class LTRDataCollector {
    // Базовый URL из конфигурации
    private static final String BASE_URL = LTRServerConfig.BASE_URL;
    private static final int MAX_QUEUE_SIZE = 100;
    private static final long SYNC_INTERVAL_SECONDS = 300; // 5 минут

    private final Context context;
    private String sessionId;
    private Queue<UserInteractionEvent> eventQueue;
    private LTRApiService apiService;
    private ScheduledExecutorService scheduler;
    private final Handler mainHandler;

    /**
     * Конструктор
     */
    public LTRDataCollector(Context context) {
        this.context = context;
        this.eventQueue = new ArrayDeque<>();
        this.sessionId = UUID.randomUUID().toString();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Инициализация коллектора данных
     */
    public void initialize() {
        // Используем общий Retrofit клиент с URL для LTR
        Retrofit retrofit = RetrofitClient.getLtrClient(BASE_URL);

        // Создаем API сервис
        apiService = retrofit.create(LTRApiService.class);

        // Инициализируем планировщик фоновых задач
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // Планируем периодическую отправку данных в фоне
        scheduler.scheduleAtFixedRate(
                this::sendQueuedEvents,
                SYNC_INTERVAL_SECONDS,
                SYNC_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }

    /**
     * Сбор данных о клике на результат поиска
     */
    public void collectClickEvent(SearchResult result, int position) {
        if (result == null)
            return;

        // Создаем событие
        UserInteractionEvent event = new UserInteractionEvent();
        event.setType(UserInteractionEvent.TYPE_CLICK);
        event.setRecipeId(result.getRecipeId());
        event.setQuery(result.getQuery());
        event.setPosition(position);
        event.setTimestamp(System.currentTimeMillis());

        // Добавляем событие в очередь
        addEventToQueue(event);

        // Если есть соединение, отправляем данные сразу
        if (isNetworkAvailable()) {
            sendClickEventToServer(result.getQuery(), result.getRecipeId(), position);
        }
    }

    /**
     * Сбор данных о времени просмотра рецепта
     */
    public void collectViewDuration(Recipe recipe, long durationMs) {
        if (recipe == null || durationMs < 1000)
            return; // Игнорируем очень короткие просмотры

        // Создаем событие
        UserInteractionEvent event = new UserInteractionEvent();
        event.setType(UserInteractionEvent.TYPE_VIEW);
        event.setRecipeId(recipe.getId());
        event.setDurationMs(durationMs);
        event.setTimestamp(System.currentTimeMillis());

        // Добавляем событие в очередь
        addEventToQueue(event);

        // Планируем отправку данных в фоне
        trySendQueuedEvents();
    }

    /**
     * Сбор данных о сохранении рецепта
     */
    public void collectSaveAction(Recipe recipe) {
        if (recipe == null)
            return;

        // Создаем событие
        UserInteractionEvent event = new UserInteractionEvent();
        event.setType(UserInteractionEvent.TYPE_SAVE);
        event.setRecipeId(recipe.getId());
        event.setTimestamp(System.currentTimeMillis());

        // Добавляем событие в очередь
        addEventToQueue(event);

        // Планируем отправку данных в фоне
        trySendQueuedEvents();
    }

    /**
     * Сбор данных о добавлении/удалении из избранного
     */
    public void collectFavoriteAction(Recipe recipe, boolean isFavorite) {
        if (recipe == null)
            return;

        // Создаем событие
        UserInteractionEvent event = new UserInteractionEvent();
        event.setType(isFavorite ? UserInteractionEvent.TYPE_FAVORITE : UserInteractionEvent.TYPE_UNFAVORITE);
        event.setRecipeId(recipe.getId());
        event.setTimestamp(System.currentTimeMillis());

        // Добавляем событие в очередь
        addEventToQueue(event);

        // Если есть соединение, отправляем данные сразу
        if (isNetworkAvailable()) {
            sendFavoriteActionToServer(recipe.getId(), isFavorite);
        }
    }

    /**
     * Пытается отправить накопленные события, если есть соединение
     */
    private void trySendQueuedEvents() {
        // Выполняем в фоновом потоке
        scheduler.execute(() -> {
            if (isNetworkAvailable()) {
                sendQueuedEvents();
            }
        });
    }

    /**
     * Отправка всех накопленных событий на сервер
     */
    public void sendQueuedEvents() {
        if (eventQueue.isEmpty())
            return;

        // Если нет соединения, просто выходим
        if (!isNetworkAvailable()) {
            return;
        }

        // Создаем копию очереди для безопасной работы с ней
        final Queue<UserInteractionEvent> eventsToSend = new ArrayDeque<>(eventQueue);

        // Отправляем каждое событие на сервер
        while (!eventsToSend.isEmpty()) {
            final UserInteractionEvent event = eventsToSend.poll();

            // В зависимости от типа события выбираем способ отправки
            switch (event.getType()) {
                case UserInteractionEvent.TYPE_CLICK:
                    sendClickEventToServer(event.getQuery(), event.getRecipeId(), event.getPosition());
                    break;
                case UserInteractionEvent.TYPE_FAVORITE:
                    sendFavoriteActionToServer(event.getRecipeId(), true);
                    break;
                case UserInteractionEvent.TYPE_UNFAVORITE:
                    sendFavoriteActionToServer(event.getRecipeId(), false);
                    break;
                // Другие типы событий...
            }

            // Удаляем событие из основной очереди
            // Безопасно делаем это в основном потоке
            final UserInteractionEvent finalEvent = event;
            mainHandler.post(() -> {
                eventQueue.remove(finalEvent);
            });
        }
    }

    /**
     * Добавление события в очередь с проверкой на превышение максимального размера
     */
    private void addEventToQueue(UserInteractionEvent event) {
        // Если очередь переполнена, удаляем самое старое событие
        if (eventQueue.size() >= MAX_QUEUE_SIZE) {
            eventQueue.poll();
        }

        // Добавляем новое событие
        eventQueue.offer(event);
    }

    /**
     * Отправка данных о клике на сервер
     */
    private void sendClickEventToServer(String query, long recipeId, int position) {
        // Создаем запрос
        ClickEventRequest request = new ClickEventRequest();
        request.setRecipeId(recipeId);
        request.setQuery(query);
        request.setPosition(position);
        request.setTimestamp(System.currentTimeMillis());
        request.setSessionId(sessionId);

        // Добавляем информацию об устройстве
        request.setDeviceInfo(getDeviceInfo());

        // Отправляем запрос
        apiService.sendClickEvent(request, getAuthHeader())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        // Можно добавить логирование успешной отправки
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        // В случае ошибки можно добавить событие обратно в очередь
                    }
                });
    }

    /**
     * Отправка данных о добавлении/удалении из избранного на сервер
     */
    private void sendFavoriteActionToServer(long recipeId, boolean isFavorite) {
        // Создаем запрос
        FavoriteActionRequest request = new FavoriteActionRequest();
        request.setRecipeId(recipeId);
        request.setIsFavorite(isFavorite);
        request.setTimestamp(System.currentTimeMillis());

        // Отправляем запрос
        apiService.favoriteAction(request, getAuthHeader())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        // Можно добавить логирование успешной отправки
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        // В случае ошибки можно добавить событие обратно в очередь
                    }
                });
    }

    /**
     * Получение информации об устройстве
     */
    private String getDeviceInfo() {
        return "Android " + Build.VERSION.RELEASE + " (" + Build.MODEL + ")";
    }

    /**
     * Получение заголовка авторизации
     */
    private String getAuthHeader() {
        return "Bearer " + LTRServerConfig.API_KEY;
    }

    /**
     * Проверка доступности сетевого соединения
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null)
            return false;

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Очистка ресурсов при завершении работы
     * Вызывается в onDestroy() активити или в onCleared() ViewModel
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * Внутренний класс для представления события взаимодействия пользователя
     */
    private static class UserInteractionEvent {
        public static final int TYPE_CLICK = 1;
        public static final int TYPE_VIEW = 2;
        public static final int TYPE_SAVE = 3;
        public static final int TYPE_FAVORITE = 4;
        public static final int TYPE_UNFAVORITE = 5;

        private int type;
        private long recipeId;
        private String query;
        private int position;
        private long timestamp;
        private long durationMs;

        // Геттеры и сеттеры

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public long getRecipeId() {
            return recipeId;
        }

        public void setRecipeId(long recipeId) {
            this.recipeId = recipeId;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }
    }
}
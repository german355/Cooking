package com.example.cooking.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.core.util.Pair;

/**
 * Shared ViewModel для синхронизации статуса лайков между разными фрагментами (например, Home и Favorites).
 * Используется для оповещения одного фрагмента об изменении лайка, сделанном в другом.
 */
public class LikeSyncViewModel extends ViewModel {

    // Pair<RecipeId, IsLiked>
    private final MutableLiveData<Pair<Integer, Boolean>> likeChangeEvent = new MutableLiveData<>();

    /**
     * Возвращает LiveData, за которым могут наблюдать другие компоненты,
     * чтобы узнать об изменении статуса лайка.
     */
    public LiveData<Pair<Integer, Boolean>> getLikeChangeEvent() {
        return likeChangeEvent;
    }

    /**
     * Метод для оповещения об изменении статуса лайка.
     * Вызывается из ViewModel того фрагмента, где произошло действие.
     * @param recipeId ID рецепта
     * @param isLiked Новое состояние лайка
     */
    public void notifyLikeChanged(int recipeId, boolean isLiked) {
        // Используем postValue, так как оповещение может прийти из фонового потока ViewModel
        likeChangeEvent.postValue(new Pair<>(recipeId, isLiked));
    }
} 
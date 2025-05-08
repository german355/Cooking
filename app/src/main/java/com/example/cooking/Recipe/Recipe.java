package com.example.cooking.Recipe;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Класс, представляющий рецепт.
 * Содержит всю информацию о рецепте, необходимую для отображения
 *
 */
public class Recipe implements Parcelable {
    private int id;
    private String title;
    private String created_at;
    private String userId;
    private boolean isLiked;

    @SerializedName("ingredients")
    @JsonAdapter(IngredientsAdapter.class)
    private ArrayList<Ingredient> ingredients = new ArrayList<>();

    @SerializedName("instructions")
    @JsonAdapter(StepsAdapter.class)
    private ArrayList<Step> steps = new ArrayList<>();
    @SerializedName("food_type")
    private String mealType;
    @SerializedName("meal_type")
    private String foodType;

    /**
     * Адаптер для десериализации поля ingredients, которое может прийти как строка JSON или как массив
     */
    public static class IngredientsAdapter extends TypeAdapter<ArrayList<Ingredient>> {
        private final Gson gson = new Gson();
        private final Type ingredientListType = new TypeToken<ArrayList<Ingredient>>() {}.getType();

        @Override
        public void write(JsonWriter out, ArrayList<Ingredient> value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            gson.toJson(value, ingredientListType, out);
        }

        @Override
        public ArrayList<Ingredient> read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return new ArrayList<>();
            }

            if (in.peek() == JsonToken.STRING) {
                String jsonString = in.nextString();
                if (jsonString.isEmpty()) {
                    return new ArrayList<>();
                }
                try {
                    ArrayList<Ingredient> parsedIngredients = gson.fromJson(jsonString, ingredientListType);
                    return parsedIngredients != null ? parsedIngredients : new ArrayList<>();
                } catch (Exception e) {
                    android.util.Log.e("Recipe", "Ошибка преобразования JSON-строки ингредиентов: " + jsonString, e);
                    return new ArrayList<>();
                }
            } else if (in.peek() == JsonToken.BEGIN_ARRAY) {
                try {
                    ArrayList<Ingredient> parsedIngredients = gson.fromJson(in, ingredientListType);
                    return parsedIngredients != null ? parsedIngredients : new ArrayList<>();
                } catch (Exception e) {
                    android.util.Log.e("Recipe", "Ошибка парсинга массива ингредиентов", e);
                    return new ArrayList<>();
                }
            } else {
                android.util.Log.w("Recipe", "Неожиданный JSON токен для ingredients: " + in.peek());
                in.skipValue();
                return new ArrayList<>();
            }
        }
    }

    /**
     * Адаптер для десериализации поля steps, которое может прийти как строка JSON или как массив
     */
    public static class StepsAdapter extends TypeAdapter<ArrayList<Step>> {
        private final Gson gson = new Gson();
        private final Type stepListType = new TypeToken<ArrayList<Step>>() {}.getType();

        @Override
        public void write(JsonWriter out, ArrayList<Step> value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            gson.toJson(value, stepListType, out);
        }

        @Override
        public ArrayList<Step> read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                android.util.Log.d("StepsAdapter", "Read null, returning empty list.");
                return new ArrayList<>();
            }

            if (in.peek() == JsonToken.STRING) {
                String jsonString = in.nextString();
                android.util.Log.d("StepsAdapter", "Read string: " + jsonString);
                if (jsonString.isEmpty()) {
                    android.util.Log.d("StepsAdapter", "String is empty, returning empty list.");
                    return new ArrayList<>();
                }
                try {
                    ArrayList<Step> parsedSteps = gson.fromJson(jsonString, stepListType);
                    int size = (parsedSteps != null) ? parsedSteps.size() : 0;
                    android.util.Log.d("StepsAdapter", "Parsed string to list, size: " + size);
                    return parsedSteps != null ? parsedSteps : new ArrayList<>();
                } catch (Exception e) {
                    android.util.Log.e("StepsAdapter", "Error parsing string: " + jsonString, e);
                    return new ArrayList<>();
                }
            } else if (in.peek() == JsonToken.BEGIN_ARRAY) {
                android.util.Log.d("StepsAdapter", "Reading as array.");
                try {
                    ArrayList<Step> parsedSteps = gson.fromJson(in, stepListType);
                    int size = (parsedSteps != null) ? parsedSteps.size() : 0;
                    android.util.Log.d("StepsAdapter", "Parsed array to list, size: " + size);
                    return parsedSteps != null ? parsedSteps : new ArrayList<>();
                } catch (Exception e) {
                    android.util.Log.e("StepsAdapter", "Error parsing array", e);
                    return new ArrayList<>();
                }
            } else {
                JsonToken token = in.peek();
                android.util.Log.w("StepsAdapter", "Unexpected JSON token: " + token);
                in.skipValue();
                return new ArrayList<>();
            }
        }
    }

    public void setIngredients(ArrayList<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public ArrayList<Step> getSteps() {
        return steps;
    }

    public void setSteps(ArrayList<Step> steps) {
        this.steps = steps;
    }

    @SerializedName("photo")
    private String photo_url;

    public String getPhoto_url() {
        return photo_url;
    }

    public void setPhoto_url(String photo_url) {
        this.photo_url = photo_url;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    // Конструктор по умолчанию
    public Recipe() {
        // Пустой конструктор для создания объекта через сеттеры
    }

    // Конструктор для тестовых данных

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getFoodType() {
        return foodType;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreated_at() {return created_at;}

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Recipe{id=").append(id);
        sb.append(", title='").append(title).append(' ');
        sb.append(", userId='").append(userId).append(' ');
        sb.append(", isLiked=").append(isLiked);
        sb.append(", mealType=").append(mealType);
        sb.append(", foodType=").append(foodType);
        sb.append(", createdAt='").append(created_at).append(' ');
        sb.append(", photo_url='").append(photo_url).append(' ');

        sb.append(", ingredients=");
        sb.append(new Gson().toJson(ingredients));

        sb.append(", steps=");
        sb.append(new Gson().toJson(steps));

        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Recipe recipe = (Recipe) o;
        return id == recipe.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public ArrayList<Ingredient> getIngredients() {
        return ingredients;
    }

    /**
     * Возвращает список ингредиентов для совместимости с новым адаптером
     * @return список ингредиентов
     */
    public List<Ingredient> getIngredientsAsList() {
        return ingredients == null ? new ArrayList<>() : new ArrayList<>(ingredients);
    }

    /**
     * Возвращает список шагов для совместимости с новым адаптером
     * @return список шагов
     */
    public List<Step> getStepsAsList() {
        return steps == null ? new ArrayList<>() : new ArrayList<>(steps);
    }

    /**
     * Возвращает JSON-строку списка ингредиентов для отправки на сервер
     * @return JSON-строка списка ингредиентов
     */
    public String getIngredientsJson() {
        Gson gson = new Gson();
        return gson.toJson(ingredients);
    }

    /**
     * Возвращает JSON-строку списка шагов для отправки на сервер
     * @return JSON-строка списка шагов
     */
    public String getStepsJson() {
        Gson gson = new Gson();
        return gson.toJson(steps);
    }

    // --- Parcelable Implementation --- 

    protected Recipe(Parcel in) {
        id = in.readInt();
        title = in.readString();
        created_at = in.readString();
        userId = in.readString();
        mealType = in.readString();
        foodType = in.readString();
        isLiked = in.readByte() != 0; // isLiked == true if byte != 0
        photo_url = in.readString();
        // Читаем списки
        ingredients = in.createTypedArrayList(Ingredient.CREATOR);
        steps = in.createTypedArrayList(Step.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(created_at);
        dest.writeString(userId);
        dest.writeString(mealType);
        dest.writeString(foodType);
        dest.writeByte((byte) (isLiked ? 1 : 0)); // if isLiked == true, byte == 1
        dest.writeString(photo_url);
        // Записываем списки
        dest.writeTypedList(ingredients);
        dest.writeTypedList(steps);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Recipe> CREATOR = new Creator<Recipe>() {
        @Override
        public Recipe createFromParcel(Parcel in) {
            return new Recipe(in);
        }

        @Override
        public Recipe[] newArray(int size) {
            return new Recipe[size];
        }
    };

    // --- End Parcelable Implementation ---
}
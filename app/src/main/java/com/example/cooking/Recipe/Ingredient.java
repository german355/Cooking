package com.example.cooking.Recipe;

import android.os.Parcel;
import android.os.Parcelable;

public class Ingredient implements Parcelable {
    private String name;
    private int count;
    private String type;

    // Пустой конструктор (может понадобиться для некоторых библиотек)
    public Ingredient() {}

    protected Ingredient(Parcel in) {
        name = in.readString();
        count = in.readInt();
        type = in.readString();
    }

    public static final Creator<Ingredient> CREATOR = new Creator<Ingredient>() {
        @Override
        public Ingredient createFromParcel(Parcel in) {
            return new Ingredient(in);
        }

        @Override
        public Ingredient[] newArray(int size) {
            return new Ingredient[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
    
    /**
     * Получает количество ингредиента (для совместимости с новым адаптером)
     * @return количество ингредиента
     */
    public float getAmount() {
        return count;
    }
    
    /**
     * Получает единицу измерения (для совместимости с новым адаптером)
     * @return единица измерения
     */
    public String getUnit() {
        return type != null ? type : "";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(count);
        dest.writeString(type);
    }

    // toString() для отладки
    @Override
    public String toString() {
        return "Ingredient{" +
                "name='" + name + '\'' +
                ", count=" + count +
                ", type='" + type + '\'' +
                '}';
    }
}

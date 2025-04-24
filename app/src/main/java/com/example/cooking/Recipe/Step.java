package com.example.cooking.Recipe;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public class Step implements Parcelable {
    private int number;
    private String instruction;
    private String url;

    public Step() {}

    protected Step(Parcel in) {
        number = in.readInt();
        instruction = in.readString();
        url = in.readString();
    }

    public static final Creator<Step> CREATOR = new Creator<Step>() {
        @Override
        public Step createFromParcel(Parcel in) {
            return new Step(in);
        }

        @Override
        public Step[] newArray(int size) {
            return new Step[size];
        }
    };

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(number);
        dest.writeString(instruction);
        dest.writeString(url);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Step step = (Step) o;
        return number == step.number &&
               Objects.equals(instruction, step.instruction) &&
               Objects.equals(url, step.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, instruction, url);
    }

    @Override
    public String toString() {
        return "Step{" +
                "number=" + number +
                ", instruction='" + instruction + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}

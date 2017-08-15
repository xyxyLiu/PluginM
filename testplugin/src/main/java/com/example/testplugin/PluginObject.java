package com.example.testplugin;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by lxy on 17-8-15.
 */

public class PluginObject implements Parcelable {

    public String name;

    public PluginObject(String str) {
        name = str;
    }

    @Override
    public String toString() {
        return String.format("PluginObject[ name = %s ]", name);
    }

    protected PluginObject(Parcel in) {
        name = in.readString();
    }

    public static final Creator<PluginObject> CREATOR = new Creator<PluginObject>() {
        @Override
        public PluginObject createFromParcel(Parcel in) {
            return new PluginObject(in);
        }

        @Override
        public PluginObject[] newArray(int size) {
            return new PluginObject[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
    }


}

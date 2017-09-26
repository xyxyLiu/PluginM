package com.reginald.pluginm.demo.pluginsharelib;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by lxy on 17-9-26.
 */

public class PluginItem implements Parcelable{
    public String pluginName;
    public int id;

    public PluginItem(String name, int id) {
        this.pluginName = name;
        this.id = id;
    }

    protected PluginItem(Parcel in) {
        pluginName = in.readString();
        id = in.readInt();
    }

    public static final Creator<PluginItem> CREATOR = new Creator<PluginItem>() {
        @Override
        public PluginItem createFromParcel(Parcel in) {
            return new PluginItem(in);
        }

        @Override
        public PluginItem[] newArray(int size) {
            return new PluginItem[size];
        }
    };

    public String toString() {
        return String.format("PluginItem[ pluginName = %s, id = %d ]", pluginName, id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(pluginName);
        dest.writeInt(id);
    }
}

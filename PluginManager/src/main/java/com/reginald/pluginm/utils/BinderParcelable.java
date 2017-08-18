package com.reginald.pluginm.utils;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Created by lxy on 16-10-26.
 */
public class BinderParcelable implements Parcelable {

    public IBinder mIBinder;

    public static final Creator<BinderParcelable> CREATOR = new Creator<BinderParcelable>() {
        @Override
        public BinderParcelable createFromParcel(Parcel parcel) {
            return new BinderParcelable(parcel);
        }

        @Override
        public BinderParcelable[] newArray(int i) {
            return new BinderParcelable[0];
        }
    };

    public BinderParcelable(@NonNull IBinder iBinder) {
        mIBinder = iBinder;
    }

    public BinderParcelable(Parcel parcel) {
        mIBinder = parcel.readStrongBinder();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongBinder(mIBinder);
    }
}

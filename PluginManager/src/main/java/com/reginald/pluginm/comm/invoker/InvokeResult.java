package com.reginald.pluginm.comm.invoker;

import android.os.Parcel;
import android.os.Parcelable;

import com.reginald.pluginm.pluginapi.IInvokeResult;

/**
 * Created by lxy on 17-9-19.
 */

public class InvokeResult implements Parcelable {
    private int mResultCode;
    private String mResult;

    public InvokeResult(int resultCode, String result) {
        mResultCode = resultCode;
        mResult = result;
    }

    private InvokeResult(IInvokeResult iInvokeResult) {
        mResultCode = iInvokeResult.getResultCode();
        mResult = iInvokeResult.getResult();
    }

    protected InvokeResult(Parcel in) {
        mResultCode = in.readInt();
        mResult = in.readString();
    }

    public int getResultCode() {
        return mResultCode;
    }

    public String getResult() {
        return mResult;
    }

    public static final Creator<InvokeResult> CREATOR = new Creator<InvokeResult>() {
        @Override
        public InvokeResult createFromParcel(Parcel in) {
            return new InvokeResult(in);
        }

        @Override
        public InvokeResult[] newArray(int size) {
            return new InvokeResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mResultCode);
        dest.writeString(mResult);
    }

    public static InvokeResult build(IInvokeResult iInvokeResult) {
        if (iInvokeResult != null) {
            return new InvokeResult(iInvokeResult);
        }
        return null;
    }

    public static IInvokeResult newIInvokerResult(final InvokeResult invokeResult) {
        return invokeResult != null ? new IInvokeResult() {
            @Override
            public int getResultCode() {
                return invokeResult.getResultCode();
            }

            @Override
            public String getResult() {
                return invokeResult.getResult();
            }
        } : IInvokeResult.INVOKERESULT_VOID_OK;
    }

    // TODO make error result static final
    public static InvokeResult buildErrorResult(int errorCode) {
        return new InvokeResult(errorCode, null);
    }
}

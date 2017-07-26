package com.android.settings.search;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Payload for settings which are selected from multiple values. For example, Location can be
 * set to multiple degrees of accuracy.
 */
public class InlineListPayload extends InlinePayload {

    /**
     * Number of selections in the list.
     */
    private int mNumOptions;

    public InlineListPayload(String key, @PayloadType int payloadType, Intent intent,
            boolean isDeviceSupported, int numOptions, int defaultValue) {
        super(key, payloadType, intent, isDeviceSupported, defaultValue);
        mNumOptions = numOptions;
    }

    private InlineListPayload(Parcel in) {
        super(in);
        mNumOptions = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mNumOptions);
    }

    @Override
    protected int standardizeInput(int input) throws IllegalArgumentException {
        if (input < 0 || input >= mNumOptions) {
            throw new IllegalArgumentException(
                    "Invalid argument for ListSelect. Expected between 0 and "
                            + mNumOptions + " but found: " + input);
        }
        return input;
    }

    @Override
    @PayloadType public int getType() {
        return PayloadType.INLINE_LIST;
    }

    public static final Parcelable.Creator<InlineListPayload> CREATOR =
            new Parcelable.Creator<InlineListPayload>() {
                @Override
                public InlineListPayload createFromParcel(Parcel in) {
                    return new InlineListPayload(in);
                }

                @Override
                public InlineListPayload[] newArray(int size) {
                    return new InlineListPayload[size];
                }
            };
}

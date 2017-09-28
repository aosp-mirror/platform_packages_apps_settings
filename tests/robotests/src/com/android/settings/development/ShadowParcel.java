package com.android.settings.development;

import android.os.Parcel;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * This class provides helpers to test logic that reads from parcels.
 */
@Implements(Parcel.class)
public class ShadowParcel {

    static int sReadIntResult;

    @Implementation
    public int readInt() {
        return sReadIntResult;
    }
}
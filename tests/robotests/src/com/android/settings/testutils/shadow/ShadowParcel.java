package com.android.settings.testutils.shadow;

import android.os.Parcel;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * This class provides helpers to test logic that reads from parcels.
 */
@Implements(Parcel.class)
public class ShadowParcel {

    public static int sReadIntResult;
    public static int sWriteIntResult;
    public static boolean sReadBoolResult;

    @Implementation
    public int readInt() {
        return sReadIntResult;
    }

    @Implementation
    public void writeInt(int val) {
        sWriteIntResult = val;
    }

    @Implementation
    public boolean readBoolean() {
        return sReadBoolResult;
    }
}

package com.android.settings.testutils.shadow;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(android.os.Process.class)
public class ShadowProcess {
    private static int sUid;

    public static void setMyUid(int uid) {
        sUid = uid;
    }

    @Implementation
    public static int myUid() {
        return sUid;
    }
}
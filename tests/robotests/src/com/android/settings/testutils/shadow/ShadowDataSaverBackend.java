package com.android.settings.testutils.shadow;

import com.android.settings.datausage.DataSaverBackend;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(DataSaverBackend.class)
public class ShadowDataSaverBackend {

    private static boolean isEnabled = true;

    @Implementation
    protected boolean isDataSaverEnabled() {
        return isEnabled;
    }

    @Implementation
    protected void setDataSaverEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}

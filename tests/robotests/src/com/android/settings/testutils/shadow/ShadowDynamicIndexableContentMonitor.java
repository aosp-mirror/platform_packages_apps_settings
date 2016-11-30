package com.android.settings.testutils.shadow;

import android.app.Activity;
import android.os.UserManager;
import com.android.settings.search.DynamicIndexableContentMonitor;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

/**
 * A shadow class of {@link DynamicIndexableContentMonitor}. The real implementation of
 * {@link DynamicIndexableContentMonitor#register} calls {@link UserManager#isUserUnlocked()}, which
 * Robolectric has not yet been updated to support, so throws a NoSuchMethodError exception.
 */
// TODO: Delete this once Robolectric is updated to the latest SDK.
@Implements(DynamicIndexableContentMonitor.class)
public class ShadowDynamicIndexableContentMonitor {

    @Implementation
    public void register(Activity activity, int loaderId) {
    }
}

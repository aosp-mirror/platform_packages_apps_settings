package com.android.settings.notification;

import android.content.Context;
import android.os.UserHandle;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeBypassingAppsPreferenceController extends AbstractZenModePreferenceController {

    protected static final String KEY = "zen_mode_bypassing_apps";
    private NotificationBackend mNotificationBackend = new NotificationBackend();

    public ZenModeBypassingAppsPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        return mNotificationBackend.getNumAppsBypassingDnd(UserHandle.getCallingUserId()) != 0;
    }

    @Override
    public String getSummary() {
        final int channelsBypassing =
                mNotificationBackend.getNumAppsBypassingDnd(UserHandle.getCallingUserId());
        return mContext.getResources().getQuantityString(R.plurals.zen_mode_bypassing_apps_subtext,
                channelsBypassing, channelsBypassing);
    }
}

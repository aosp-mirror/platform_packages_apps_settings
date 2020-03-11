package com.android.settings.notification.zen;

import android.content.Context;
import android.os.UserHandle;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeBypassingAppsPreferenceController extends AbstractZenModePreferenceController {

    protected static final String KEY = "zen_mode_behavior_apps";
    private NotificationBackend mNotificationBackend = new NotificationBackend();

    public ZenModeBypassingAppsPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getSummary() {
        final int channelsBypassing =
                mNotificationBackend.getNumAppsBypassingDnd(UserHandle.getCallingUserId());
        if (channelsBypassing == 0) {
            return mContext.getResources().getString(R.string.zen_mode_bypassing_apps_subtext_none);
        }
        return mContext.getResources().getQuantityString(R.plurals.zen_mode_bypassing_apps_subtext,
                channelsBypassing, channelsBypassing);
    }
}

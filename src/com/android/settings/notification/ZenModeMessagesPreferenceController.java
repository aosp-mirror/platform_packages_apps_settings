package com.android.settings.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeMessagesPreferenceController extends AbstractZenModePreferenceController {

    protected static final String KEY = "zen_mode_messages";

    public ZenModeMessagesPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY, lifecycle);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_ALARMS:
                preference.setEnabled(false);
                preference.setSummary(mBackend.getContactsSummary(mBackend.SOURCE_NONE));
                break;
            default:
                preference.setEnabled(true);
                preference.setSummary(mBackend.getContactsSummary(
                        NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES));
        }
    }
}

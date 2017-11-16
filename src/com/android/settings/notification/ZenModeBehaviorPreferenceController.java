package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeBehaviorPreferenceController extends
        AbstractZenModePreferenceController implements PreferenceControllerMixin {

    protected static final String KEY_BEHAVIOR_SETTINGS = "zen_mode_behavior_settings";
    private final ZenModeSettings.SummaryBuilder mSummaryBuilder;

    public ZenModeBehaviorPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY_BEHAVIOR_SETTINGS, lifecycle);
        mSummaryBuilder = new ZenModeSettings.SummaryBuilder(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BEHAVIOR_SETTINGS;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        preference.setSummary(mSummaryBuilder.getBehaviorSettingSummary(getPolicy(),
                getZenMode()));
    }
}

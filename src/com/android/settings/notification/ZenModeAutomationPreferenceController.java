package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class ZenModeAutomationPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin {

    protected static final String KEY_ZEN_MODE_AUTOMATION = "zen_mode_automation_settings";
    private final ZenModeSettings.SummaryBuilder mSummaryBuilder;

    public ZenModeAutomationPreferenceController(Context context) {
        super(context);
        mSummaryBuilder = new ZenModeSettings.SummaryBuilder(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ZEN_MODE_AUTOMATION;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(mSummaryBuilder.getAutomaticRulesSummary());
    }
}

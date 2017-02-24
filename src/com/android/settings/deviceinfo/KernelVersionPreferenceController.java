package com.android.settings.deviceinfo;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceController;
import com.android.settingslib.DeviceInfoUtils;

public class KernelVersionPreferenceController extends PreferenceController {

    private static final String KEY_KERNEL_VERSION = "kernel_version";

    public KernelVersionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(DeviceInfoUtils.getFormattedKernelVersion());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_KERNEL_VERSION;
    }
}

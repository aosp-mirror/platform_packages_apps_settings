/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.fuelgauge;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.ApplicationsState.AppEntry;

public class HighPowerDetail extends AppInfoWithHeader implements OnPreferenceChangeListener {

    private static final String KEY_HIGH_POWER_SWITCH = "high_power_switch";

    private final PowerWhitelistBackend mBackend = PowerWhitelistBackend.getInstance();

    private SwitchPreference mUsageSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.high_power_details);
        mUsageSwitch = (SwitchPreference) findPreference(KEY_HIGH_POWER_SWITCH);
        mUsageSwitch.setOnPreferenceChangeListener(this);
    }

    @Override
    protected boolean refreshUi() {
        mUsageSwitch.setEnabled(!mBackend.isSysWhitelisted(mPackageName));
        mUsageSwitch.setChecked(mBackend.isWhitelisted(mPackageName));
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == Boolean.TRUE) {
            mBackend.addApp(mPackageName);
        } else {
            mBackend.removeApp(mPackageName);
        }
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.FUELGAUGE_HIGH_POWER_DETAILS;
    }

    public static CharSequence getSummary(Context context, AppEntry entry) {
        return getSummary(context, entry.info.packageName);
    }

    public static CharSequence getSummary(Context context, String pkg) {
        return context.getString(PowerWhitelistBackend.getInstance().isWhitelisted(pkg)
                ? R.string.high_power_on : R.string.high_power_off);
    }

}

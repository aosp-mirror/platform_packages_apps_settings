/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ConnectivityMonitorPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String KEY_CONNECTIVITY_MONITOR_SWITCH = "connectivity_monitor_switch";
    @VisibleForTesting
    static final String BUILD_TYPE = "ro.build.type";
    @VisibleForTesting
    static final String PROPERTY_CONNECTIVITY_MONITOR = "persist.radio.enable_tel_mon";

    @VisibleForTesting
    static final String ENABLED_STATUS = "enabled";
    @VisibleForTesting
    static final String DISABLED_STATUS = "disabled";
    @VisibleForTesting
    static final String USER_ENABLED_STATUS = "user_enabled";
    @VisibleForTesting
    static final String USER_DISABLED_STATUS = "user_disabled";

    @VisibleForTesting
    static final String USERDEBUG_BUILD = "userdebug";
    @VisibleForTesting
    static final String ENG_BUILD = "eng";

    public ConnectivityMonitorPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CONNECTIVITY_MONITOR_SWITCH;
    }

    @Override
    public boolean isAvailable() {
        final String buildType = SystemProperties.get(BUILD_TYPE);
        return mContext.getResources().getBoolean(R.bool.config_show_connectivity_monitor)
                && (TextUtils.equals(buildType, USERDEBUG_BUILD)
                || TextUtils.equals(buildType, ENG_BUILD));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        SystemProperties.set(PROPERTY_CONNECTIVITY_MONITOR,
                isEnabled ? USER_ENABLED_STATUS : USER_DISABLED_STATUS);
        Toast.makeText(mContext, R.string.connectivity_monitor_toast,
                Toast.LENGTH_LONG).show();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean enabled = isConnectivityMonitorEnabled();
        ((SwitchPreference) mPreference).setChecked(enabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(PROPERTY_CONNECTIVITY_MONITOR, USER_DISABLED_STATUS);
        ((SwitchPreference) mPreference).setChecked(false);
    }

    private boolean isConnectivityMonitorEnabled() {
        final String cmStatus = SystemProperties.get(PROPERTY_CONNECTIVITY_MONITOR,
                DISABLED_STATUS);
        return TextUtils.equals(ENABLED_STATUS, cmStatus) || TextUtils.equals(USER_ENABLED_STATUS,
                cmStatus);
    }
}

/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.network.telephony.MobileNetworkUtils;

public class TestingSettings extends SettingsPreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.testing_settings);

        if (!isRadioInfoVisible(getContext())) {
            PreferenceScreen preferenceScreen = (PreferenceScreen)
                    findPreference("radio_info_settings");
            getPreferenceScreen().removePreference(preferenceScreen);
        }
    }

    @VisibleForTesting
    protected boolean isRadioInfoVisible(Context context) {
        UserManager um = context.getSystemService(UserManager.class);
        if (um != null) {
            if (!um.isAdminUser()) {
                return false;
            }
        }
        return !MobileNetworkUtils.isMobileNetworkUserRestricted(context);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TESTING;
    }
}

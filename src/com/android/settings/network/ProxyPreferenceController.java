/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.network;

import android.app.admin.DevicePolicyManager;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class ProxyPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_PROXY_SETTINGS = "proxy_settings";

    public ProxyPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        // proxy UI disabled until we have better app support
        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        // Enable Proxy selector settings if allowed.
        final Preference pref = screen.findPreference(KEY_PROXY_SETTINGS);
        if (pref != null) {
            final DevicePolicyManager dpm = (DevicePolicyManager)
                    mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
            pref.setEnabled(dpm.getGlobalProxyAdmin() == null);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PROXY_SETTINGS;
    }
}

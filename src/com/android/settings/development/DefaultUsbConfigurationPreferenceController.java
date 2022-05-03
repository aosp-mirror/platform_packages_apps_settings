/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settingslib.RestrictedLockUtilsInternal.checkIfUsbDataSignalingIsDisabled;

import android.content.Context;
import android.os.UserHandle;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class DefaultUsbConfigurationPreferenceController extends
        DeveloperOptionsPreferenceController {

    private static final String PREFERENCE_KEY = "default_usb_configuration";

    private RestrictedPreference mPreference;

    public DefaultUsbConfigurationPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        mPreference.setDisabledByAdmin(
                checkIfUsbDataSignalingIsDisabled(mContext, UserHandle.myUserId()));
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        super.onDeveloperOptionsSwitchEnabled();
        mPreference.setDisabledByAdmin(
                checkIfUsbDataSignalingIsDisabled(mContext, UserHandle.myUserId()));
    }
}

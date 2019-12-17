/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BubbleGlobalPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    @VisibleForTesting
    static final int ON = 1;
    @VisibleForTesting
    static final int OFF = 0;

    public BubbleGlobalPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return Settings.Global.NOTIFICATION_BUBBLES;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        writeSetting((boolean) newValue);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) mPreference).setChecked(isEnabled());
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeSetting(false /* isEnabled */);
        updateState(mPreference);
    }

    private boolean isEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.NOTIFICATION_BUBBLES, OFF) == ON;
    }

    private void writeSetting(boolean isEnabled) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.NOTIFICATION_BUBBLES, isEnabled ? ON : OFF);
    }
}

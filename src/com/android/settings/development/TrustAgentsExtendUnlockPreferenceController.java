/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class TrustAgentsExtendUnlockPreferenceController extends
        DeveloperOptionsPreferenceController implements
                Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String KEY_TRUST_AGENTS_EXTEND_UNLOCK =
        "security_setting_trust_agents_extend_unlock";

    public TrustAgentsExtendUnlockPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TRUST_AGENTS_EXTEND_UNLOCK;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.TRUST_AGENTS_EXTEND_UNLOCK, isEnabled ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        int trustAgentsExtendUnlock = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.TRUST_AGENTS_EXTEND_UNLOCK, 0);
        ((SwitchPreference) mPreference).setChecked(trustAgentsExtendUnlock != 0);
    }
}

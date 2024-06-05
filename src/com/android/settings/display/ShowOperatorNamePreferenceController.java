/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class ShowOperatorNamePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_SHOW_OPERATOR_NAME = "show_operator_name";

    public ShowOperatorNamePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        final CarrierConfigManager configMgr = mContext
                .getSystemService(CarrierConfigManager.class);
        if (configMgr == null) {
            return false;
        }
        final PersistableBundle b = configMgr.getConfigForSubId(SubscriptionManager
                .getDefaultDataSubscriptionId());
        return b != null && b.getBoolean(CarrierConfigManager
                .KEY_SHOW_OPERATOR_NAME_IN_STATUSBAR_BOOL, false);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SHOW_OPERATOR_NAME;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                KEY_SHOW_OPERATOR_NAME, value ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        int value = Settings.Secure.getInt(mContext.getContentResolver(),
                KEY_SHOW_OPERATOR_NAME, 1);
        ((TwoStatePreference) preference).setChecked(value != 0);
    }
}

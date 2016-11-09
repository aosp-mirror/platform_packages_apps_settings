/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;

import com.android.settings.core.PreferenceController;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

import static android.provider.Settings.Secure.DOZE_ENABLED;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_AMBIENT_DISPLAY;

public class DozePreferenceController extends PreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_DOZE = "doze";

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public DozePreferenceController(Context context) {
        super(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DOZE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_DOZE.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext, ACTION_AMBIENT_DISPLAY);
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        int value = Settings.Secure.getInt(mContext.getContentResolver(), DOZE_ENABLED, 1);
        ((SwitchPreference) preference).setChecked(value != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(), DOZE_ENABLED, value ? 1 : 0);
        return true;
    }

    @Override
    public boolean isAvailable() {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = mContext.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }
}

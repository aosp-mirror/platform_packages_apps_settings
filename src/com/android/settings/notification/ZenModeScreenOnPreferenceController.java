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

package com.android.settings.notification;

import android.app.NotificationManager.Policy;
import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeScreenOnPreferenceController extends
        AbstractZenModePreferenceController implements Preference.OnPreferenceChangeListener {

    protected static final String KEY = "zen_mode_screen_on";

    public ZenModeScreenOnPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY, lifecycle);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        SwitchPreference pref = (SwitchPreference) preference;
        pref.setChecked(mBackend.isEffectAllowed(Policy.SUPPRESSED_EFFECT_SCREEN_ON));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean bypass = (Boolean) newValue;
        if (ZenModeSettingsBase.DEBUG) Log.d(TAG, "onPrefChange allowWhenScreenOn="
                + bypass);
        mMetricsFeatureProvider.action(mContext,
                MetricsProto.MetricsEvent.ACTION_ZEN_ALLOW_WHEN_SCREEN_ON, bypass);
        mBackend.saveVisualEffectsPolicy(Policy.SUPPRESSED_EFFECT_SCREEN_ON, bypass);
        return true;
    }
}

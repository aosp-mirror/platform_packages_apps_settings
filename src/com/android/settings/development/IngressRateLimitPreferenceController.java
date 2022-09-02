/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.net.ConnectivitySettingsManager;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Controller for ingress rate limit developer setting.
 */
public class IngressRateLimitPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private static final String TAG = "IngressRateLimitPreferenceController";
    private static final String INGRESS_RATE_LIMIT_KEY = "ingress_rate_limit";
    private static final int RATE_LIMIT_DISABLED = -1;

    public IngressRateLimitPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return INGRESS_RATE_LIMIT_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final long value = Long.parseLong(newValue.toString());
        try {
            ConnectivitySettingsManager.setIngressRateLimitInBytesPerSecond(mContext, value);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "invalid rate limit", e);
            return false;
        }
    }

    @Override
    public void updateState(Preference preference) {
        final String ingressRateLimit = String.valueOf(
                ConnectivitySettingsManager.getIngressRateLimitInBytesPerSecond(mContext));

        // verify ingressRateLimit is valid / present in ListPreference; else do nothing.
        final CharSequence[] entryValues = ((ListPreference) preference).getEntryValues();
        for (int i = 0; i < entryValues.length; i++) {
            if (ingressRateLimit.contentEquals(entryValues[i])) {
                ((ListPreference) preference).setValue(ingressRateLimit);
                return;
            }
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        // disable rate limiting when developer options are disabled
        ConnectivitySettingsManager.setIngressRateLimitInBytesPerSecond(mContext,
                RATE_LIMIT_DISABLED);
        ((ListPreference) mPreference).setValue(String.valueOf(RATE_LIMIT_DISABLED));
    }
}

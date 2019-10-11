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

package com.android.settings.notification;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

public class SwipeDirectionPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public SwipeDirectionPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference pref) {
        ((ListPreference) pref).setValue(String.valueOf(Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.NOTIFICATION_DISMISS_RTL,
                1)));
        super.updateState(pref);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.NOTIFICATION_DISMISS_RTL,
                Integer.valueOf((String) newValue));
        refreshSummary(preference);
        return true;
    }

    @Override
    public CharSequence getSummary() {
        int value = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.NOTIFICATION_DISMISS_RTL, 1);
        String[] values = mContext.getResources().getStringArray(R.array.swipe_direction_values);
        String[] titles = mContext.getResources().getStringArray(R.array.swipe_direction_titles);
        if (values == null) {
            return null;
        }
        for (int i = 0; i < values.length; i++) {
            int valueAt = Integer.parseInt(values[i]);
            if (value == valueAt) {
                return titles[i];
            }
        }
        return null;
    }
}

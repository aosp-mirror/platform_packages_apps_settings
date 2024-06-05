/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.emergency;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.emergencynumber.EmergencyNumberUtils;
import com.android.settingslib.widget.MainSwitchPreference;

/**
 * Preference controller for emergency gesture setting
 */
public class EmergencyGesturePreferenceController extends BasePreferenceController implements
        OnCheckedChangeListener {

    @VisibleForTesting
    EmergencyNumberUtils mEmergencyNumberUtils;

    private MainSwitchPreference mSwitchBar;

    public EmergencyGesturePreferenceController(Context context, String key) {
        super(context, key);
        mEmergencyNumberUtils = new EmergencyNumberUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean isConfigEnabled = mContext.getResources()
                .getBoolean(R.bool.config_show_emergency_gesture_settings);

        if (!isConfigEnabled) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(mPreferenceKey);
        mSwitchBar = (MainSwitchPreference) pref;
        mSwitchBar.setTitle(mContext.getString(R.string.emergency_gesture_switchbar_title));
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.updateStatus(isChecked());
    }

    @VisibleForTesting
    public boolean isChecked() {
        return mEmergencyNumberUtils.getEmergencyGestureEnabled();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mEmergencyNumberUtils.setEmergencyGestureEnabled(isChecked);
    }
}

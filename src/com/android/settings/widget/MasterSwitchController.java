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

package com.android.settings.widget;

import androidx.preference.Preference;

import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/*
 * The switch controller that is used to update the switch widget in the MasterSwitchPreference
 * layout.
 */
public class MasterSwitchController extends SwitchWidgetController implements
    Preference.OnPreferenceChangeListener {

    private final MasterSwitchPreference mPreference;

    public MasterSwitchController(MasterSwitchPreference preference) {
        mPreference = preference;
    }

    @Override
    public void updateTitle(boolean isChecked) {
    }

    @Override
    public void startListening() {
        mPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void stopListening() {
        mPreference.setOnPreferenceChangeListener(null);
    }

    @Override
    public void setChecked(boolean checked) {
        mPreference.setChecked(checked);
    }

    @Override
    public boolean isChecked() {
        return mPreference.isChecked();
    }

    @Override
    public void setEnabled(boolean enabled) {
        mPreference.setSwitchEnabled(enabled);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mListener != null) {
            return mListener.onSwitchToggled((Boolean) newValue);
        }
        return false;
    }

    @Override
    public void setDisabledByAdmin(EnforcedAdmin admin) {
        mPreference.setDisabledByAdmin(admin);
    }
}

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

package com.android.settings.security;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class MemtagPreferenceController extends TogglePreferenceController {
    private Preference mPreference;
    private Fragment mFragment;

    public MemtagPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void setFragment(Fragment fragment) {
        mFragment = fragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return MemtagHelper.getAvailabilityStatus();
    }

    @Override
    public boolean isChecked() {
        return MemtagHelper.isChecked();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        MemtagHelper.setChecked(isChecked);
        if (mPreference != null) {
            refreshSummary(mPreference);
        }
        if (isChecked != MemtagHelper.isOn()) {
            MemtagRebootDialog.show(mContext, mFragment, isChecked);
        }
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_security;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        refreshSummary(mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshSummary(preference);
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getResources().getString(MemtagHelper.getSummary());
    }
}

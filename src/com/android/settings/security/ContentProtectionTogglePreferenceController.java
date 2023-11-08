/*
 * Copyright (C) 2023 The Android Open Source Project
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


import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.widget.Switch;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

/** Preference controller for content protection toggle switch bar. */
public class ContentProtectionTogglePreferenceController extends TogglePreferenceController
        implements OnMainSwitchChangeListener {

    @VisibleForTesting
    static final String KEY_CONTENT_PROTECTION_PREFERENCE = "content_protection_user_consent";

    private SettingsMainSwitchPreference mSwitchBar;
    private final ContentResolver mContentResolver;
    private final boolean isFullyManagedDevice = Utils.getDeviceOwnerComponent(mContext) != null;

    public ContentProtectionTogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        if (isFullyManagedDevice) {
            // If fully managed device, it should always unchecked
            return false;
        }
        return Settings.Global.getInt(mContentResolver, KEY_CONTENT_PROTECTION_PREFERENCE, 0) >= 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mSwitchBar.setChecked(isChecked);
        Settings.Global.putInt(
                mContentResolver, KEY_CONTENT_PROTECTION_PREFERENCE, isChecked ? 1 : -1);
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mSwitchBar = screen.findPreference(getPreferenceKey());
        mSwitchBar.addOnSwitchChangeListener(this);
        if (isFullyManagedDevice) {
            // If fully managed device, the switch bar is greyed out
            mSwitchBar.setEnabled(false);
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked != isChecked()) {
            setChecked(isChecked);
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_security;
    }
}

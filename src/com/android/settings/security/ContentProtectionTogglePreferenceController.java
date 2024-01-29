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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

/** Preference controller for content protection toggle switch bar. */
public class ContentProtectionTogglePreferenceController extends TogglePreferenceController
        implements OnCheckedChangeListener {

    @VisibleForTesting
    static final String KEY_CONTENT_PROTECTION_PREFERENCE = "content_protection_user_consent";

    @Nullable private SettingsMainSwitchPreference mSwitchBar;
    @Nullable private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    private final ContentResolver mContentResolver;

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
        if (mEnforcedAdmin != null) {
            // If fully managed device, it should always unchecked
            return false;
        }
        return Settings.Global.getInt(mContentResolver, KEY_CONTENT_PROTECTION_PREFERENCE, 0) >= 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Global.putInt(
                mContentResolver, KEY_CONTENT_PROTECTION_PREFERENCE, isChecked ? 1 : -1);
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());

        if (preference instanceof SettingsMainSwitchPreference) {
            mSwitchBar = (SettingsMainSwitchPreference) preference;
            mSwitchBar.addOnSwitchChangeListener(this);
        }
    }

    /**
     * Temporary workaround for SettingsMainSwitchPreference.setDisabledByAdmin without user
     * restriction.
     */
    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        // Assign the value to mEnforcedAdmin since it's needed in isChecked()
        mEnforcedAdmin = getEnforcedAdmin();
        if (mSwitchBar != null && mEnforcedAdmin != null) {
            mSwitchBar.setDisabledByAdmin(mEnforcedAdmin);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked != isChecked()) {
            setChecked(isChecked);
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_security;
    }

    @VisibleForTesting
    protected RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin() {
        return RestrictedLockUtilsInternal.getDeviceOwner(mContext);
    }
}

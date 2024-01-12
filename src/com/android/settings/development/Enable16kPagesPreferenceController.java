/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.os.SystemProperties;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/** Controller for 16K pages developer option */
public class Enable16kPagesPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener,
                PreferenceControllerMixin,
                Enable16kbPagesDialogHost {

    private static final String ENABLE_16K_PAGES = "enable_16k_pages";
    private static final String DEV_OPTION_PROPERTY = "ro.product.build.16k_page.enabled";
    private static final int ENABLE_4K_PAGE_SIZE = 0;
    private static final int ENABLE_16K_PAGE_SIZE = 1;

    private @Nullable DevelopmentSettingsDashboardFragment mFragment = null;

    public Enable16kPagesPreferenceController(
            @NonNull Context context, @Nullable DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return SystemProperties.getBoolean(DEV_OPTION_PROPERTY, false);
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_16K_PAGES;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean optionEnabled = (Boolean) newValue;
        if (optionEnabled) {
            Enable16kPagesWarningDialog.show(mFragment, this);
        } else {
            // TODO(b/295573133):Directly reboot into 4k
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int optionValue =
                Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Settings.Global.ENABLE_16K_PAGES,
                        ENABLE_4K_PAGE_SIZE /* default */);

        ((SwitchPreference) mPreference).setChecked(optionValue == ENABLE_16K_PAGE_SIZE);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        // TODO(b/295573133):Directly reboot into 4k
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.ENABLE_16K_PAGES,
                ENABLE_4K_PAGE_SIZE);
        ((SwitchPreference) mPreference).setChecked(false);
    }

    /** Called when user confirms reboot dialog */
    @Override
    public void on16kPagesDialogConfirmed() {
        // TODO(b/295573133) : integrate update engine
    }

    /** Called when user dismisses to reboot dialog */
    @Override
    public void on16kPagesDialogDismissed() {}
}

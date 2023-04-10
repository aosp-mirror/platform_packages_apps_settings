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
import android.os.SystemProperties;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.security.MemtagHelper;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

public class RebootWithMtePreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin {
    private static final String KEY_REBOOT_WITH_MTE = "reboot_with_mte";

    private Fragment mFragment;

    public RebootWithMtePreferenceController(Context context) {
        super(context, KEY_REBOOT_WITH_MTE);
    }

    @Override
    public int getAvailabilityStatus() {
        return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)
                        && SystemProperties.getBoolean("ro.arm64.memtag.bootctl_supported", false)
                ? BasePreferenceController.AVAILABLE
                : BasePreferenceController.UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        if (MemtagHelper.isChecked()) {
            return mContext.getResources().getString(R.string.reboot_with_mte_already_enabled);
        }
        return mContext.getResources().getString(R.string.reboot_with_mte_summary);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!MemtagHelper.isChecked());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_REBOOT_WITH_MTE;
    }

    public void setFragment(Fragment fragment) {
        mFragment = fragment;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (Utils.isMonkeyRunning()) {
            return false;
        }

        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            RebootWithMteDialog.show(mContext, mFragment);
            return true;
        }
        return false;
    }
}

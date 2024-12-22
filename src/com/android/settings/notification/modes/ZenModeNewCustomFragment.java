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

package com.android.settings.notification.modes;

import android.app.settings.SettingsEnums;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settingslib.notification.modes.ZenMode;

import com.google.common.base.Strings;

public class ZenModeNewCustomFragment extends ZenModeEditNameIconFragmentBase {

    @Nullable
    @Override
    protected ZenMode onCreateInstantiateZenMode() {
        return ZenMode.newCustomManual(
                requireContext().getString(R.string.zen_mode_new_custom_default_name),
                /* iconResId= */ 0);
    }

    @Override
    public void onStart() {
        super.onStart();
        requireActivity().setTitle(R.string.zen_mode_new_custom_title);
    }

    @Override
    void saveMode(ZenMode mode) {
        String modeName = Strings.isNullOrEmpty(mode.getName())
                ? requireContext().getString(R.string.zen_mode_new_custom_default_name)
                : mode.getName();

        ZenMode created = requireBackend().addCustomManualMode(modeName,
                mode.getRule().getIconResId());
        if (created != null) {
            // Open the mode view fragment and close the "add mode" fragment, so exiting the mode
            // view goes back to previous screen (which should be the modes list).
            ZenSubSettingLauncher.forModeFragment(requireContext(), ZenModeFragment.class,
                    created.getId(), getMetricsCategory()).launch();
            finish();
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZEN_MODE_ADD_NEW;
    }

    @Override
    protected String getLogTag() {
        return "ZenModeNewCustomFragment";
    }
}

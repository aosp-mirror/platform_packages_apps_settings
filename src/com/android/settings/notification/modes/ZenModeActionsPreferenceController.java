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

import static com.android.settings.notification.modes.ZenModeFragmentBase.MODE_ID;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.widget.ActionButtonsPreference;

class ZenModeActionsPreferenceController extends AbstractZenModePreferenceController {

    private ActionButtonsPreference mPreference;

    ZenModeActionsPreferenceController(@NonNull Context context, @NonNull String key,
            @Nullable ZenModesBackend backend) {
        super(context, key, backend);
    }

    @Override
    void updateState(Preference preference, @NonNull ZenMode zenMode) {
        ActionButtonsPreference buttonsPreference = (ActionButtonsPreference) preference;

        // TODO: b/346278854 - Add rename action (with setButton1Enabled(zenMode.canEditName())
        buttonsPreference.setButton1Text(R.string.zen_mode_action_change_name);
        buttonsPreference.setButton1Icon(R.drawable.ic_mode_edit);
        buttonsPreference.setButton1Enabled(false);

        buttonsPreference.setButton2Text(R.string.zen_mode_action_change_icon);
        buttonsPreference.setButton2Icon(R.drawable.ic_zen_mode_action_change_icon);
        buttonsPreference.setButton2Enabled(zenMode.canEditIcon());
        buttonsPreference.setButton2OnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString(MODE_ID, zenMode.getId());
            new SubSettingLauncher(mContext)
                    .setDestination(ZenModeIconPickerFragment.class.getName())
                    // TODO: b/332937635 - Update metrics category
                    .setSourceMetricsCategory(0)
                    .setArguments(bundle)
                    .launch();
        });
    }
}

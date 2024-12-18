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

import android.content.Context;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.widget.LayoutPreference;

class ZenModeEditDonePreferenceController extends AbstractZenModePreferenceController {

    private final Runnable mConfirmSave;
    @Nullable private Button mButton;

    ZenModeEditDonePreferenceController(@NonNull Context context, @NonNull String key,
            Runnable confirmSave) {
        super(context, key);
        mConfirmSave = confirmSave;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        LayoutPreference pref = screen.findPreference(getPreferenceKey());
        if (pref != null) {
            mButton = pref.findViewById(R.id.done);
            if (mButton != null) {
                mButton.setOnClickListener(v -> mConfirmSave.run());
            }
        }
    }

    @Override
    void updateState(Preference preference, @NonNull ZenMode zenMode) {
        if (mButton != null) {
            mButton.setEnabled(!zenMode.getName().isBlank());
        }
    }
}

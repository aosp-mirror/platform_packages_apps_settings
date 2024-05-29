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

import android.annotation.NonNull;
import android.content.Context;
import android.widget.Button;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.widget.LayoutPreference;

public class ZenModeButtonPreferenceController extends AbstractZenModePreferenceController {

    private Button mZenButton;

    public ZenModeButtonPreferenceController(Context context, String key, ZenModesBackend backend) {
        super(context, key, backend);
    }

    @Override
    public boolean isAvailable(ZenMode zenMode) {
        return zenMode.getRule().isManualInvocationAllowed() && zenMode.getRule().isEnabled();
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        if (mZenButton == null) {
            mZenButton = ((LayoutPreference) preference).findViewById(R.id.activate_mode);
        }
        mZenButton.setOnClickListener(v -> {
            if (zenMode.isActive()) {
                mBackend.deactivateMode(zenMode);
            } else {
                mBackend.activateMode(zenMode, null);
            }
        });
        if (zenMode.isActive()) {
            mZenButton.setText(R.string.zen_mode_button_turn_off);
        } else {
            mZenButton.setText(R.string.zen_mode_button_turn_on);
        }
    }
}

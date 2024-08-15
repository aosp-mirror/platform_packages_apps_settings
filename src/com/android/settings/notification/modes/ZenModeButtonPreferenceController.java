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

import static com.google.common.base.Preconditions.checkNotNull;

import android.annotation.NonNull;
import android.content.Context;
import android.provider.Settings;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.notification.SettingsEnableZenModeDialog;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;
import com.android.settingslib.widget.LayoutPreference;

import java.time.Duration;

class ZenModeButtonPreferenceController extends AbstractZenModePreferenceController {
    private static final String TAG = "ZenModeButtonPrefController";

    private Button mZenButton;
    private final Fragment mParent;
    private final ManualDurationHelper mDurationHelper;

    ZenModeButtonPreferenceController(Context context, String key, Fragment parent,
            ZenModesBackend backend) {
        super(context, key, backend);
        mParent = parent;
        mDurationHelper = new ManualDurationHelper(context);
    }

    @Override
    public boolean isAvailable(ZenMode zenMode) {
        return zenMode.isEnabled()
                && (zenMode.isActive() || zenMode.getRule().isManualInvocationAllowed());
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        if (mZenButton == null) {
            mZenButton = ((LayoutPreference) preference).findViewById(R.id.activate_mode);
        }
        mZenButton.setOnClickListener(v -> {
            checkNotNull(mBackend, "Backend not available!");
            if (zenMode.isActive()) {
                mBackend.deactivateMode(zenMode);
            } else {
                if (zenMode.isManualDnd()) {
                    // if manual DND, potentially ask for or use desired duration
                    int zenDuration = mDurationHelper.getZenDuration();
                    switch (zenDuration) {
                        case Settings.Secure.ZEN_DURATION_PROMPT:
                            new SettingsEnableZenModeDialog().show(
                                    mParent.getParentFragmentManager(), TAG);
                            break;
                        case Settings.Secure.ZEN_DURATION_FOREVER:
                            mBackend.activateMode(zenMode, null);
                            break;
                        default:
                            mBackend.activateMode(zenMode, Duration.ofMinutes(zenDuration));
                    }
                } else {
                    mBackend.activateMode(zenMode, null);
                }
            }
        });
        if (zenMode.isActive()) {
            mZenButton.setText(R.string.zen_mode_action_deactivate);
        } else {
            mZenButton.setText(R.string.zen_mode_action_activate);
        }
    }
}

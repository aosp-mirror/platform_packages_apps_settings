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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.notification.zen.SettingsZenDurationDialog;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

public class ManualDurationPreferenceController extends AbstractZenModePreferenceController {
    private static final String TAG = "QsDurationPrefController";

    private final Fragment mParent;
    private final ManualDurationHelper mDurationHelper;
    private final ManualDurationHelper.SettingsObserver mSettingsObserver;

    ManualDurationPreferenceController(Context context, String key, Fragment parent,
            ZenModesBackend backend) {
        super(context, key, backend);
        mParent = parent;
        mDurationHelper = new ManualDurationHelper(context);
        mSettingsObserver = mDurationHelper.makeSettingsObserver(this);
    }

    @Override
    public boolean isAvailable(ZenMode zenMode) {
        if (!super.isAvailable(zenMode)) {
            return false;
        }
        return zenMode.isManualDnd();
    }

    // Called by parent fragment onStart().
    void registerSettingsObserver() {
        mSettingsObserver.register();
    }

    // Called by parent fragment onStop().
    void unregisterSettingsObserver() {
        mSettingsObserver.unregister();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference pref = screen.findPreference(getPreferenceKey());
        if (pref != null) {
            mSettingsObserver.setPreference(pref);
        }
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode unusedZenMode) {
        // This controller is a link between a Settings value (ZEN_DURATION) and the manual DND
        // mode. The status of the zen mode object itself doesn't affect the preference
        // value, as that comes from settings; that value from settings will determine the
        // condition that is attached to the mode on manual activation. Thus we ignore the actual
        // zen mode value provided here.
        preference.setSummary(mDurationHelper.getSummary());
        preference.setOnPreferenceClickListener(pref -> {
            // The new setting value is set by the dialog, so we don't need to do it here.
            final SettingsZenDurationDialog durationDialog = new SettingsZenDurationDialog();
            durationDialog.show(mParent.getParentFragmentManager(), TAG);
            return true;
        });
    }
}

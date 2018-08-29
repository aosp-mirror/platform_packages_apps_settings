/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.view.IWindowManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class StrictModePreferenceController extends DeveloperOptionsPreferenceController implements
        Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String STRICT_MODE_KEY = "strict_mode";
    private static final String WINDOW_MANAGER_KEY = "window";

    @VisibleForTesting
    static final String STRICT_MODE_ENABLED = "1";
    @VisibleForTesting
    static final String STRICT_MODE_DISABLED = "";

    private final IWindowManager mWindowManager;

    public StrictModePreferenceController(Context context) {
        super(context);

        mWindowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(WINDOW_MANAGER_KEY));
    }

    @Override
    public String getPreferenceKey() {
        return STRICT_MODE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        writeStrictModeVisualOptions(isEnabled);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) mPreference).setChecked(isStrictModeEnabled());
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeStrictModeVisualOptions(false);
        ((SwitchPreference) mPreference).setChecked(false);
    }

    private boolean isStrictModeEnabled() {
        return SystemProperties.getBoolean(StrictMode.VISUAL_PROPERTY, false /* default */);
    }

    private void writeStrictModeVisualOptions(boolean isEnabled) {
        try {
            mWindowManager.setStrictModeVisualIndicatorPreference(
                    isEnabled ? STRICT_MODE_ENABLED : STRICT_MODE_DISABLED);
        } catch (RemoteException e) {
            // intentional no-op
        }
    }
}

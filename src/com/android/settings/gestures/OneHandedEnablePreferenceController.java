/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.net.Uri;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for One-handed mode shortcut settings
 */
public class OneHandedEnablePreferenceController extends BasePreferenceController
        implements OneHandedSettingsUtils.TogglesCallback, LifecycleObserver, OnStart, OnStop {

    private final OneHandedSettingsUtils mUtils;
    private Preference mPreference;

    public OneHandedEnablePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUtils = new OneHandedSettingsUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return OneHandedSettingsUtils.isSupportOneHandedMode() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getText(
                OneHandedSettingsUtils.isOneHandedModeEnabled(mContext)
                        ? R.string.gesture_setting_on : R.string.gesture_setting_off);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mUtils.registerToggleAwareObserver(this);
    }

    @Override
    public void onStop() {
        mUtils.unregisterToggleAwareObserver();
    }

    @Override
    public void onChange(Uri uri) {
        if (mPreference == null) {
            return;
        }
        if (uri.equals(OneHandedSettingsUtils.ONE_HANDED_MODE_ENABLED_URI)) {
            refreshSummary(mPreference);
        }
    }
}

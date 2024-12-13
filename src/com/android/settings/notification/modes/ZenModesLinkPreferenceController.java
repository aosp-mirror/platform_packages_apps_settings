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

import android.app.Flags;
import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.notification.modes.ZenModesBackend;

public class ZenModesLinkPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "ModesLinkPrefController";

    private final ZenModesBackend mBackend;
    private final ZenSettingsObserver mSettingObserver;
    private final ZenModeSummaryHelper mSummaryBuilder;

    private Preference mPreference;

    public ZenModesLinkPreferenceController(Context context, String key) {
        super(context, key);
        mBackend = ZenModesBackend.getInstance(context);
        mSummaryBuilder = new ZenModeSummaryHelper(context, ZenHelperBackend.getInstance(context));
        mSettingObserver = new ZenSettingsObserver(context, this::onZenSettingsChanged);
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return Flags.modesUi() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        if (mSettingObserver != null) {
            mSettingObserver.register();
        }
    }

    private void onZenSettingsChanged() {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public void updateState(Preference preference) {
        try {
            preference.setSummary(mSummaryBuilder.getModesSummary(mBackend.getModes()));
        } catch (SecurityException e) {
            // Standard usage should have the correct permissions to read zen state. But if we don't
            // for whatever reason, don't crash.
            Log.w(TAG, "No permission to read mode state");
        }
    }

    @Override
    public void onStop() {
        if (mSettingObserver != null) {
            mSettingObserver.unregister();
        }
    }
}

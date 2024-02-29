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

package com.android.settings.bluetooth;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.flags.Flags;
import com.android.settingslib.utils.ThreadUtils;

public class BluetoothAutoOnPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "BluetoothAutoOnPreferenceController";
    @VisibleForTesting static final String PREF_KEY = "bluetooth_auto_on_settings_toggle";
    static final String SETTING_NAME = "bluetooth_automatic_turn_on";
    static final int UNSET = -1;
    @VisibleForTesting static final int ENABLED = 1;
    @VisibleForTesting static final int DISABLED = 0;
    private final ContentObserver mContentObserver =
            new ContentObserver(new Handler(/* async= */ true)) {
                @Override
                public void onChange(boolean selfChange) {
                    var unused =
                            ThreadUtils.postOnBackgroundThread(
                                    () -> {
                                        updateValue();
                                        mContext.getMainExecutor()
                                                .execute(
                                                        () -> {
                                                            if (mPreference != null) {
                                                                updateState(mPreference);
                                                            }
                                                        });
                                    });
                }
            };
    private int mAutoOnValue = UNSET;
    @Nullable private TwoStatePreference mPreference;

    public BluetoothAutoOnPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void onStart() {
        mContext.getContentResolver()
                .registerContentObserver(
                        Settings.Secure.getUriFor(SETTING_NAME),
                        /* notifyForDescendants= */ false,
                        mContentObserver);
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Flags.bluetoothQsTileDialogAutoOnToggle()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        updateValue();
        return mAutoOnValue != UNSET ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean isChecked() {
        return mAutoOnValue == ENABLED;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (getAvailabilityStatus() != AVAILABLE) {
            Log.w(TAG, "Trying to set toggle value while feature not available.");
            return false;
        }
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            boolean updated =
                                    Settings.Secure.putIntForUser(
                                            mContext.getContentResolver(),
                                            SETTING_NAME,
                                            isChecked ? ENABLED : DISABLED,
                                            UserHandle.myUserId());
                            if (updated) {
                                updateValue();
                            }
                        });
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    private void updateValue() {
        mAutoOnValue =
                Settings.Secure.getIntForUser(
                        mContext.getContentResolver(), SETTING_NAME, UNSET, UserHandle.myUserId());
    }
}

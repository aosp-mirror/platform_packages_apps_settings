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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.ThreadUtils;

public class BluetoothAutoOnPreferenceController extends TogglePreferenceController
        implements BluetoothCallback, LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "BluetoothAutoOnPrefCtlr";
    @VisibleForTesting static final String PREF_KEY = "bluetooth_auto_on_settings_toggle";
    @VisibleForTesting BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final LocalBluetoothManager mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
    private boolean mAutoOnValue = false;
    @Nullable private TwoStatePreference mPreference;

    public BluetoothAutoOnPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void onAutoOnStateChanged(int state) {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            Log.i(TAG, "onAutoOnStateChanged() state: " + state);
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

    @Override
    public void onStart() {
        if (mLocalBluetoothManager == null) {
            return;
        }
        mLocalBluetoothManager.getEventManager().registerCallback(this);
    }

    @Override
    public void onStop() {
        if (mLocalBluetoothManager == null) {
            return;
        }
        mLocalBluetoothManager.getEventManager().unregisterCallback(this);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mBluetoothAdapter == null) {
            return UNSUPPORTED_ON_DEVICE;
        }
        try {
            boolean isSupported = mBluetoothAdapter.isAutoOnSupported();
            Log.i(TAG, "getAvailabilityStatus() isSupported: " + isSupported);
            if (isSupported) {
                var unused = ThreadUtils.postOnBackgroundThread(this::updateValue);
            }
            return isSupported ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        } catch (Exception | NoSuchMethodError e) {
            // Server could throw TimeoutException, InterruptedException or ExecutionException
            return UNSUPPORTED_ON_DEVICE;
        }
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
        return mAutoOnValue;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            try {
                                mBluetoothAdapter.setAutoOnEnabled(isChecked);
                            } catch (Exception e) {
                                // Server could throw IllegalStateException, TimeoutException,
                                // InterruptedException or ExecutionException
                                Log.e(TAG, "Error calling setAutoOnEnabled()", e);
                            }
                        });
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    private void updateValue() {
        if (mBluetoothAdapter == null) {
            return;
        }
        try {
            mAutoOnValue = mBluetoothAdapter.isAutoOnEnabled();
        } catch (Exception e) {
            // Server could throw TimeoutException, InterruptedException or ExecutionException
            Log.e(TAG, "Error calling isAutoOnEnabled()", e);
        }
    }
}

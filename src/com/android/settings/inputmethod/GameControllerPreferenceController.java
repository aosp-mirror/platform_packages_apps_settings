/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.content.Context;
import android.hardware.input.InputManager;
import android.provider.Settings;
import android.view.InputDevice;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class GameControllerPreferenceController extends TogglePreferenceController
        implements PreferenceControllerMixin, InputManager.InputDeviceListener, LifecycleObserver,
        OnResume, OnPause {

    private final InputManager mIm;

    private Preference mPreference;

    public GameControllerPreferenceController(Context context, String key) {
        super(context, key);
        mIm = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
    }

    @Override
    public void onResume() {
        mIm.registerInputDeviceListener(this, null);
    }

    @Override
    public void onPause() {
        mIm.unregisterInputDeviceListener(this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        // If device explicitly wants to hide this, return early.
        if (!mContext.getResources().getBoolean(R.bool.config_show_vibrate_input_devices)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        final int[] devices = mIm.getInputDeviceIds();
        for (int deviceId : devices) {
            InputDevice device = mIm.getInputDevice(deviceId);
            if (device != null && !device.isVirtual() && device.getVibrator().hasVibrator()) {
                return AVAILABLE;
            }
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }
        mPreference.setVisible(isAvailable());
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.VIBRATE_INPUT_DEVICES, 1) > 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.VIBRATE_INPUT_DEVICES, isChecked ? 1 : 0);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        updateState(mPreference);
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        updateState(mPreference);
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updateState(mPreference);
    }
}

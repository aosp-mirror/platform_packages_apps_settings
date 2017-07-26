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
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.InputDevice;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;

public class GameControllerPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, InputManager.InputDeviceListener, LifecycleObserver,
        OnResume, OnPause {

    @VisibleForTesting
    static final String PREF_KEY = "vibrate_input_devices";
    private static final String CATEGORY_KEY = "game_controller_settings_category";

    private final InputManager mIm;

    private PreferenceScreen mScreen;
    private Preference mCategory;
    private Preference mPreference;

    public GameControllerPreferenceController(Context context) {
        super(context);
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
        mScreen = screen;
        mCategory = screen.findPreference(CATEGORY_KEY);
        mPreference = screen.findPreference(PREF_KEY);
    }

    @Override
    public boolean isAvailable() {
        final int[] devices = mIm.getInputDeviceIds();
        for (int deviceId : devices) {
            InputDevice device = mIm.getInputDevice(deviceId);
            if (device != null && !device.isVirtual() && device.getVibrator().hasVibrator()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(PREF_KEY, preference.getKey())) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.VIBRATE_INPUT_DEVICES,
                    ((SwitchPreference) preference).isChecked() ? 1 : 0);
            return true;
        }
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return CATEGORY_KEY;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            return;
        }
        ((SwitchPreference) preference).setChecked(Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.VIBRATE_INPUT_DEVICES, 1) > 0);
    }

    @Override
    public void updateNonIndexableKeys(List<String> keys) {
        if (!isAvailable()) {
            keys.add(CATEGORY_KEY);
            keys.add(PREF_KEY);
        }
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        updateGameControllers();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        updateGameControllers();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updateGameControllers();
    }

    private void updateGameControllers() {
        if (isAvailable()) {
            mScreen.addPreference(mCategory);
            updateState(mPreference);
        } else {
            if (mCategory != null) {
                mScreen.removePreference(mCategory);
            }
        }
    }
}

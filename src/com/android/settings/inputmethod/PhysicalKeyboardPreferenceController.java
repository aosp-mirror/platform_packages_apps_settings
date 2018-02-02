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

package com.android.settings.inputmethod;

import android.content.Context;
import android.hardware.input.InputManager;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.inputmethod.PhysicalKeyboardFragment.HardKeyboardDeviceInfo;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;

public class PhysicalKeyboardPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause,
        InputManager.InputDeviceListener {

    private final InputManager mIm;

    private Preference mPreference;

    public PhysicalKeyboardPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mIm = (InputManager) context.getSystemService(Context.INPUT_SERVICE);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        mPreference = preference;
        updateSummary();
    }

    @Override
    public String getPreferenceKey() {
        return "physical_keyboard_pref";
    }

    @Override
    public void onPause() {
        mIm.unregisterInputDeviceListener(this);
    }

    @Override
    public void onResume() {
        mIm.registerInputDeviceListener(this, null);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        updateSummary();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        updateSummary();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updateSummary();
    }

    private void updateSummary() {
        if (mPreference == null) {
            return;
        }
        final List<HardKeyboardDeviceInfo> keyboards =
                PhysicalKeyboardFragment.getHardKeyboards();
        if (keyboards.isEmpty()) {
            mPreference.setSummary(R.string.disconnected);
            return;
        }
        String summary = null;
        for (HardKeyboardDeviceInfo info : keyboards) {
            if (summary == null) {
                summary = info.mDeviceName;
            } else {
                summary = mContext.getString(R.string.join_many_items_middle, summary,
                        info.mDeviceName);
            }
        }
        mPreference.setSummary(summary);
    }
}

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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.icu.text.ListFormatter;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.inputmethod.PhysicalKeyboardFragment.HardKeyboardDeviceInfo;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.ArrayList;
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
        return !getKeyboards().isEmpty()
                && mContext.getResources().getBoolean(R.bool.config_show_physical_keyboard_pref);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }
        Intent intent = new Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(Settings.EXTRA_ENTRYPOINT, SettingsEnums.KEYBOARD_SETTINGS);
        mContext.startActivity(intent);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        mPreference = preference;
        updateEntry();
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
        updateEntry();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        updateEntry();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updateEntry();
    }

    private void updateEntry() {
        if (mPreference == null) {
            return;
        }
        List<HardKeyboardDeviceInfo> keyboards = getKeyboards();
        if (keyboards.isEmpty()) {
            mPreference.setVisible(false);
            return;
        }
        updateSummary(keyboards);
    }

    private void updateSummary(List<HardKeyboardDeviceInfo> keyboards) {
        mPreference.setVisible(true);
        List<String> summaries = new ArrayList<>();
        for (HardKeyboardDeviceInfo info : keyboards) {
            summaries.add(info.mDeviceName);
        }
        mPreference.setSummary(ListFormatter.getInstance().format(summaries));
    }

    @VisibleForTesting
    List<HardKeyboardDeviceInfo> getKeyboards() {
        return PhysicalKeyboardFragment.getHardKeyboards(mContext);
    }
}

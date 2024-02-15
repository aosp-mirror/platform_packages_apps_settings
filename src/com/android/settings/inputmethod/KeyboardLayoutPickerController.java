/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class KeyboardLayoutPickerController extends BasePreferenceController implements
        InputManager.InputDeviceListener, LifecycleObserver, OnStart, OnStop {

    private final InputManager mIm;
    private final Map<TwoStatePreference, KeyboardLayout> mPreferenceMap;

    private Fragment mParent;
    private int mInputDeviceId;
    private InputDeviceIdentifier mInputDeviceIdentifier;
    private KeyboardLayout[] mKeyboardLayouts;
    private PreferenceScreen mScreen;


    public KeyboardLayoutPickerController(Context context, String key) {
        super(context, key);
        mIm = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        mInputDeviceId = -1;
        mPreferenceMap = new HashMap<>();
    }

    public void initialize(Fragment parent, InputDeviceIdentifier inputDeviceIdentifier) {
        mParent = parent;
        mInputDeviceIdentifier = inputDeviceIdentifier;
        mKeyboardLayouts = mIm.getKeyboardLayoutsForInputDevice(mInputDeviceIdentifier);
        Arrays.sort(mKeyboardLayouts);
    }

    @Override
    public void onStart() {
        mIm.registerInputDeviceListener(this, null);
        if (mInputDeviceIdentifier == null
                || NewKeyboardSettingsUtils.getInputDevice(mIm, mInputDeviceIdentifier) == null) {
            return;
        }
        mInputDeviceId =
                NewKeyboardSettingsUtils.getInputDevice(mIm, mInputDeviceIdentifier).getId();
        updateCheckedState();
    }

    @Override
    public void onStop() {
        mIm.unregisterInputDeviceListener(this);
        mInputDeviceId = -1;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        createPreferenceHierarchy();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!(preference instanceof TwoStatePreference switchPref)) {
            return false;
        }

        final KeyboardLayout layout = mPreferenceMap.get(switchPref);
        if (layout != null) {
            final boolean checked = switchPref.isChecked();
            if (checked) {
                mIm.addKeyboardLayoutForInputDevice(mInputDeviceIdentifier,
                        layout.getDescriptor());
            } else {
                mIm.removeKeyboardLayoutForInputDevice(mInputDeviceIdentifier,
                        layout.getDescriptor());
            }
        }
        return true;
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {

    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        if (mInputDeviceId >= 0 && deviceId == mInputDeviceId) {
            mParent.getActivity().finish();
        }
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        if (mInputDeviceId >= 0 && deviceId == mInputDeviceId) {
            updateCheckedState();
        }
    }

    private void updateCheckedState() {
        final String[] enabledKeyboardLayouts = mIm.getEnabledKeyboardLayoutsForInputDevice(
                mInputDeviceIdentifier);
        Arrays.sort(enabledKeyboardLayouts);

        for (Map.Entry<TwoStatePreference, KeyboardLayout> entry : mPreferenceMap.entrySet()) {
            entry.getKey().setChecked(Arrays.binarySearch(enabledKeyboardLayouts,
                    entry.getValue().getDescriptor()) >= 0);
        }
    }

    private void createPreferenceHierarchy() {
        if (mKeyboardLayouts == null) {
            return;
        }
        for (KeyboardLayout layout : mKeyboardLayouts) {
            final TwoStatePreference pref = new SwitchPreferenceCompat(mScreen.getContext());
            pref.setTitle(layout.getLabel());
            pref.setSummary(layout.getCollection());
            // TODO: Waiting for new API to use a prefix with special number to setKey
            pref.setKey(layout.getDescriptor());
            mScreen.addPreference(pref);
            mPreferenceMap.put(pref, layout);
        }
    }
}


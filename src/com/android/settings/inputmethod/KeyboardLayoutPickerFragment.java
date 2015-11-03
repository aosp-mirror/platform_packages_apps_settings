/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.internal.logging.MetricsLogger;
import com.android.settings.SettingsPreferenceFragment;

import android.content.Context;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.InputDevice;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class KeyboardLayoutPickerFragment extends SettingsPreferenceFragment
        implements InputDeviceListener {
    private InputDeviceIdentifier mInputDeviceIdentifier;
    private int mInputDeviceId = -1;
    private InputManager mIm;
    private KeyboardLayout[] mKeyboardLayouts;
    private HashMap<CheckBoxPreference, KeyboardLayout> mPreferenceMap =
            new HashMap<CheckBoxPreference, KeyboardLayout>();

    /**
     * Intent extra: The input device descriptor of the keyboard whose keyboard
     * layout is to be changed.
     */
    public static final String EXTRA_INPUT_DEVICE_IDENTIFIER = "input_device_identifier";

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.INPUTMETHOD_KEYBOARD;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mInputDeviceIdentifier = getActivity().getIntent().getParcelableExtra(
                EXTRA_INPUT_DEVICE_IDENTIFIER);
        if (mInputDeviceIdentifier == null) {
            getActivity().finish();
        }

        mIm = (InputManager)getSystemService(Context.INPUT_SERVICE);
        mKeyboardLayouts = mIm.getKeyboardLayoutsForInputDevice(mInputDeviceIdentifier);
        Arrays.sort(mKeyboardLayouts);
        setPreferenceScreen(createPreferenceHierarchy());
    }

    @Override
    public void onResume() {
        super.onResume();

        mIm.registerInputDeviceListener(this, null);

        InputDevice inputDevice =
                mIm.getInputDeviceByDescriptor(mInputDeviceIdentifier.getDescriptor());
        if (inputDevice == null) {
            getActivity().finish();
            return;
        }
        mInputDeviceId = inputDevice.getId();

        updateCheckedState();
    }

    @Override
    public void onPause() {
        mIm.unregisterInputDeviceListener(this);
        mInputDeviceId = -1;

        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference instanceof CheckBoxPreference) {
            CheckBoxPreference checkboxPref = (CheckBoxPreference)preference;
            KeyboardLayout layout = mPreferenceMap.get(checkboxPref);
            if (layout != null) {
                boolean checked = checkboxPref.isChecked();
                if (checked) {
                    mIm.addKeyboardLayoutForInputDevice(mInputDeviceIdentifier,
                            layout.getDescriptor());
                } else {
                    mIm.removeKeyboardLayoutForInputDevice(mInputDeviceIdentifier,
                            layout.getDescriptor());
                }
                return true;
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        if (mInputDeviceId >= 0 && deviceId == mInputDeviceId) {
            updateCheckedState();
        }
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        if (mInputDeviceId >= 0 && deviceId == mInputDeviceId) {
            getActivity().finish();
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
        Context context = getActivity();

        for (KeyboardLayout layout : mKeyboardLayouts) {
            CheckBoxPreference pref = new CheckBoxPreference(context);
            pref.setTitle(layout.getLabel());
            pref.setSummary(layout.getCollection());
            root.addPreference(pref);
            mPreferenceMap.put(pref, layout);
        }
        return root;
    }

    private void updateCheckedState() {
        String[] enabledKeyboardLayouts = mIm.getEnabledKeyboardLayoutsForInputDevice(
                mInputDeviceIdentifier);
        Arrays.sort(enabledKeyboardLayouts);

        for (Map.Entry<CheckBoxPreference, KeyboardLayout> entry : mPreferenceMap.entrySet()) {
            entry.getKey().setChecked(Arrays.binarySearch(enabledKeyboardLayouts,
                    entry.getValue().getDescriptor()) >= 0);
        }
    }
}

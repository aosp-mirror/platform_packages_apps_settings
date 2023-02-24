/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.HashMap;
import java.util.Map;

public class NewKeyboardLayoutPickerController extends BasePreferenceController implements
        InputManager.InputDeviceListener, LifecycleObserver, OnStart, OnStop {
    private final InputManager mIm;
    private final Map<KeyboardLayoutPreference, KeyboardLayout> mPreferenceMap;

    private Fragment mParent;
    private int mInputDeviceId;
    private int mUserId;
    private InputDeviceIdentifier mInputDeviceIdentifier;
    private InputMethodInfo mInputMethodInfo;
    private InputMethodSubtype mInputMethodSubtype;

    private KeyboardLayout[] mKeyboardLayouts;
    private PreferenceScreen mScreen;
    private String mPreviousSelection;
    private String mLayout;

    public NewKeyboardLayoutPickerController(Context context, String key) {
        super(context, key);
        mIm = context.getSystemService(InputManager.class);
        mInputDeviceId = -1;
        mPreferenceMap = new HashMap<>();
    }

    public void initialize(Fragment parent, int userId, InputDeviceIdentifier inputDeviceIdentifier,
            InputMethodInfo imeInfo, InputMethodSubtype imeSubtype, String layout) {
        mParent = parent;
        mUserId = userId;
        mInputDeviceIdentifier = inputDeviceIdentifier;
        mInputMethodInfo = imeInfo;
        mInputMethodSubtype = imeSubtype;
        mLayout = layout;
        mKeyboardLayouts = mIm.getKeyboardLayoutListForInputDevice(
                inputDeviceIdentifier, userId, imeInfo, imeSubtype);
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

        if (!(preference instanceof KeyboardLayoutPreference)) {
            return false;
        }

        final KeyboardLayoutPreference pref = (KeyboardLayoutPreference) preference;
        pref.setCheckMark(true);
        if (mPreviousSelection != null && !mPreviousSelection.equals(preference.getKey())) {
            KeyboardLayoutPreference preSelectedPref = mScreen.findPreference(mPreviousSelection);
            preSelectedPref.setCheckMark(false);
        }
        setLayout(pref);
        mPreviousSelection = preference.getKey();
        return true;
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        // Do nothing.
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        if (mInputDeviceId >= 0 && deviceId == mInputDeviceId) {
            mParent.getActivity().finish();
        }
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        // Do nothing.
    }

    private void createPreferenceHierarchy() {
        if (mKeyboardLayouts == null) {
            return;
        }
        for (KeyboardLayout layout : mKeyboardLayouts) {
            final KeyboardLayoutPreference pref;
            if (mLayout.equals(layout.getLabel())) {
                pref = new KeyboardLayoutPreference(mScreen.getContext(), layout.getLabel(), true);
                mPreviousSelection = layout.getDescriptor();
            } else {
                pref = new KeyboardLayoutPreference(mScreen.getContext(), layout.getLabel(), false);
            }
            pref.setKey(layout.getDescriptor());
            mScreen.addPreference(pref);
            mPreferenceMap.put(pref, layout);
        }
    }

    private void setLayout(KeyboardLayoutPreference preference) {
        mIm.setKeyboardLayoutForInputDevice(
                mInputDeviceIdentifier,
                mUserId,
                mInputMethodInfo,
                mInputMethodSubtype,
                mPreferenceMap.get(preference).getDescriptor());
    }
}

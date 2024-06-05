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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.hardware.input.KeyboardLayoutSelectionResult;
import android.os.Bundle;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.TickButtonPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.HashMap;
import java.util.Map;

public class NewKeyboardLayoutPickerController extends BasePreferenceController implements
        InputManager.InputDeviceListener, LifecycleObserver, OnStart, OnStop {

    private final InputManager mIm;
    private final Map<TickButtonPreference, KeyboardLayout> mPreferenceMap;
    private Fragment mParent;
    private CharSequence mTitle;
    private int mInputDeviceId;
    private int mUserId;
    private InputDeviceIdentifier mInputDeviceIdentifier;
    private InputMethodInfo mInputMethodInfo;
    private InputMethodSubtype mInputMethodSubtype;
    private KeyboardLayout[] mKeyboardLayouts;
    private PreferenceScreen mScreen;
    private String mPreviousSelection;
    private String mFinalSelectedLayout;
    private String mLayout;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private KeyboardLayoutSelectedCallback mKeyboardLayoutSelectedCallback;

    public NewKeyboardLayoutPickerController(Context context, String key) {
        super(context, key);
        mIm = context.getSystemService(InputManager.class);
        mInputDeviceId = -1;
        mPreferenceMap = new HashMap<>();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    public void initialize(Fragment parent) {
        mParent = parent;
        Bundle arguments = parent.getArguments();
        mTitle = arguments.getCharSequence(NewKeyboardSettingsUtils.EXTRA_TITLE);
        mUserId = arguments.getInt(NewKeyboardSettingsUtils.EXTRA_USER_ID);
        mInputDeviceIdentifier =
                arguments.getParcelable(NewKeyboardSettingsUtils.EXTRA_INPUT_DEVICE_IDENTIFIER);
        mInputMethodInfo =
                arguments.getParcelable(NewKeyboardSettingsUtils.EXTRA_INPUT_METHOD_INFO);
        mInputMethodSubtype =
                arguments.getParcelable(NewKeyboardSettingsUtils.EXTRA_INPUT_METHOD_SUBTYPE);
        mLayout = getSelectedLayoutLabel();
        mFinalSelectedLayout = mLayout;
        mKeyboardLayouts = mIm.getKeyboardLayoutListForInputDevice(
                mInputDeviceIdentifier, mUserId, mInputMethodInfo, mInputMethodSubtype);
        NewKeyboardSettingsUtils.sortKeyboardLayoutsByLabel(mKeyboardLayouts);
        parent.getActivity().setTitle(mTitle);
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
        if (mLayout != null && !mLayout.equals(mFinalSelectedLayout)) {
            String change = "From:" + mLayout + ", to:" + mFinalSelectedLayout;
            mMetricsFeatureProvider.action(
                    mContext, SettingsEnums.ACTION_PK_LAYOUT_CHANGED, change);
        }
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

    /**
     * Registers {@link KeyboardLayoutSelectedCallback} and get updated.
     */
    public void registerKeyboardSelectedCallback(KeyboardLayoutSelectedCallback
            keyboardLayoutSelectedCallback) {
        this.mKeyboardLayoutSelectedCallback = keyboardLayoutSelectedCallback;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!(preference instanceof TickButtonPreference)) {
            return false;
        }

        final TickButtonPreference pref = (TickButtonPreference) preference;
        if (mKeyboardLayoutSelectedCallback != null && mPreferenceMap.containsKey(preference)) {
            mKeyboardLayoutSelectedCallback.onSelected(mPreferenceMap.get(preference));
        }
        pref.setSelected(true);
        if (mPreviousSelection != null && !mPreviousSelection.equals(preference.getKey())) {
            TickButtonPreference preSelectedPref = mScreen.findPreference(mPreviousSelection);
            preSelectedPref.setSelected(false);
        }
        setLayout(pref);
        mPreviousSelection = preference.getKey();
        mFinalSelectedLayout = pref.getTitle().toString();
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
            final TickButtonPreference pref;
            pref = new TickButtonPreference(mScreen.getContext());
            pref.setTitle(layout.getLabel());

            if (mLayout.equals(layout.getLabel())) {
                if (mKeyboardLayoutSelectedCallback != null) {
                    mKeyboardLayoutSelectedCallback.onSelected(layout);
                }
                pref.setSelected(true);
                mPreviousSelection = layout.getDescriptor();
            }
            pref.setKey(layout.getDescriptor());
            mScreen.addPreference(pref);
            mPreferenceMap.put(pref, layout);
        }
    }

    private void setLayout(TickButtonPreference preference) {
        mIm.setKeyboardLayoutForInputDevice(
                mInputDeviceIdentifier,
                mUserId,
                mInputMethodInfo,
                mInputMethodSubtype,
                mPreferenceMap.get(preference).getDescriptor());
    }

    private String getSelectedLayoutLabel() {
        String label = mContext.getString(R.string.keyboard_default_layout);
        KeyboardLayoutSelectionResult result = NewKeyboardSettingsUtils.getKeyboardLayout(
                mIm, mUserId, mInputDeviceIdentifier, mInputMethodInfo, mInputMethodSubtype);
        KeyboardLayout[] keyboardLayouts = NewKeyboardSettingsUtils.getKeyboardLayouts(
                mIm, mUserId, mInputDeviceIdentifier, mInputMethodInfo, mInputMethodSubtype);
        if (result.getLayoutDescriptor() != null) {
            for (KeyboardLayout keyboardLayout : keyboardLayouts) {
                if (keyboardLayout.getDescriptor().equals(result.getLayoutDescriptor())) {
                    label = keyboardLayout.getLabel();
                    break;
                }
            }
        }
        return label;
    }

    public interface KeyboardLayoutSelectedCallback {
        /**
         * Called when KeyboardLayout been selected.
         */
        void onSelected(KeyboardLayout keyboardLayout);
    }
}

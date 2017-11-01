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

import android.annotation.Nullable;
import android.app.Activity;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.InputDevice;

import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.inputmethod.PhysicalKeyboardFragment.KeyboardInfoPreference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class KeyboardLayoutPickerFragment extends SettingsPreferenceFragment
        implements InputDeviceListener {

    private InputDeviceIdentifier mInputDeviceIdentifier;
    private int mInputDeviceId = -1;
    private InputManager mIm;
    private InputMethodInfo mImi;
    @Nullable
    private InputMethodSubtype mSubtype;
    private KeyboardLayout[] mKeyboardLayouts;
    private Map<Preference, KeyboardLayout> mPreferenceMap = new HashMap<>();

    // TODO: Make these constants public API for b/25752827

    /**
     * Intent extra: The input device descriptor of the keyboard whose keyboard
     * layout is to be changed.
     */
    public static final String EXTRA_INPUT_DEVICE_IDENTIFIER = "input_device_identifier";

    /**
     * Intent extra: The associated {@link InputMethodInfo}.
     */
    public static final String EXTRA_INPUT_METHOD_INFO = "input_method_info";

    /**
     * Intent extra: The associated {@link InputMethodSubtype}.
     */
    public static final String EXTRA_INPUT_METHOD_SUBTYPE = "input_method_subtype";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.INPUTMETHOD_KEYBOARD;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Activity activity = Preconditions.checkNotNull(getActivity());

        mInputDeviceIdentifier = activity.getIntent().getParcelableExtra(
                EXTRA_INPUT_DEVICE_IDENTIFIER);
        mImi = activity.getIntent().getParcelableExtra(EXTRA_INPUT_METHOD_INFO);
        mSubtype = activity.getIntent().getParcelableExtra(EXTRA_INPUT_METHOD_SUBTYPE);

        if (mInputDeviceIdentifier == null || mImi == null) {
            activity.finish();
        }

        mIm = activity.getSystemService(InputManager.class);
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
    }

    @Override
    public void onPause() {
        mIm.unregisterInputDeviceListener(this);
        mInputDeviceId = -1;

        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        KeyboardLayout layout = mPreferenceMap.get(preference);
        if (layout != null) {
            mIm.setKeyboardLayoutForInputDevice(mInputDeviceIdentifier, mImi, mSubtype,
                    layout.getDescriptor());
            getActivity().finish();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {}

    @Override
    public void onInputDeviceChanged(int deviceId) {}

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        if (mInputDeviceId >= 0 && deviceId == mInputDeviceId) {
            getActivity().finish();
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());

        for (KeyboardLayout layout : mKeyboardLayouts) {
            Preference pref = new Preference(getPrefContext());
            pref.setTitle(layout.getLabel());
            pref.setSummary(layout.getCollection());
            root.addPreference(pref);
            mPreferenceMap.put(pref, layout);
        }

        root.setTitle(KeyboardInfoPreference.getDisplayName(getContext(), mImi, mSubtype));
        return root;
    }
}

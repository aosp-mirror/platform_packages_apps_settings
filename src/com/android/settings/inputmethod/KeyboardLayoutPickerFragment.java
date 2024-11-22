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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

public class KeyboardLayoutPickerFragment extends DashboardFragment {

    private static final String TAG = "KeyboardLayoutPicker";

    /**
     * Intent extra: The input device descriptor of the keyboard whose keyboard
     * layout is to be changed.
     */
    public static final String EXTRA_INPUT_DEVICE_IDENTIFIER = "input_device_identifier";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.INPUTMETHOD_KEYBOARD;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        final InputDeviceIdentifier inputDeviceIdentifier = getActivity().getIntent().
                getParcelableExtra(EXTRA_INPUT_DEVICE_IDENTIFIER);
        final InputManager im = context.getSystemService(InputManager.class);
        if (InputPeripheralsSettingsUtils.getInputDevice(im, inputDeviceIdentifier) == null) {
            return;
        }
        use(KeyboardLayoutPickerController.class).initialize(this /*parent*/,
                inputDeviceIdentifier);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    protected int getPreferenceScreenResId() {
        return R.xml.keyboard_layout_picker_fragment;
    }
}

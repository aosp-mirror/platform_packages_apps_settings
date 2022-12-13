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
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

public class NewKeyboardLayoutPickerContent extends DashboardFragment {

    private static final String TAG = "KeyboardLayoutPicker";

    static final String EXTRA_TITLE = "keyboard_layout_picker_title";
    static final String EXTRA_KEYBOARD_LAYOUT = "keyboard_layout";

    /**
     * Intent extra: The input device descriptor of the keyboard whose keyboard
     * layout is to be changed.
     */
    public static final String EXTRA_INPUT_DEVICE_IDENTIFIER = "input_device_identifier";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Bundle arguments = getArguments();
        final String title = arguments.getString(EXTRA_TITLE);
        final String layout = arguments.getString(EXTRA_KEYBOARD_LAYOUT);
        final InputDeviceIdentifier inputDeviceIdentifier =
                arguments.getParcelable(EXTRA_INPUT_DEVICE_IDENTIFIER);

        if (inputDeviceIdentifier == null) {
            getActivity().finish();
        }
        getActivity().setTitle(title);
        use(NewKeyboardLayoutPickerController.class).initialize(this /*parent*/,
                inputDeviceIdentifier, layout);
    }

    @Override
    public int getMetricsCategory() {
        // TODO: add new SettingsEnums SETTINGS_KEYBOARDS_LAYOUT_PICKER_CONTENT
        return SettingsEnums.SETTINGS_KEYBOARDS_LAYOUT_PICKER;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    protected int getPreferenceScreenResId() {
        return R.xml.new_keyboard_layout_picker_fragment;
    }
}

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
import android.os.Bundle;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

public class NewKeyboardLayoutPickerContent extends DashboardFragment {

    private static final String TAG = "KeyboardLayoutPicker";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        InputManager inputManager = getContext().getSystemService(InputManager.class);
        Bundle arguments = getArguments();
        final String title = arguments.getString(NewKeyboardSettingsUtils.EXTRA_TITLE);
        final String layout = arguments.getString(NewKeyboardSettingsUtils.EXTRA_KEYBOARD_LAYOUT);
        final int userId = arguments.getInt(NewKeyboardSettingsUtils.EXTRA_USER_ID);
        final InputDeviceIdentifier identifier =
                arguments.getParcelable(NewKeyboardSettingsUtils.EXTRA_INPUT_DEVICE_IDENTIFIER);
        final InputMethodInfo inputMethodInfo =
                arguments.getParcelable(NewKeyboardSettingsUtils.EXTRA_INPUT_METHOD_INFO);
        final InputMethodSubtype inputMethodSubtype =
                arguments.getParcelable(NewKeyboardSettingsUtils.EXTRA_INPUT_METHOD_SUBTYPE);
        if (identifier == null
                || NewKeyboardSettingsUtils.getInputDevice(inputManager, identifier) == null) {
            return;
        }
        getActivity().setTitle(title);
        use(NewKeyboardLayoutPickerController.class).initialize(this /*parent*/, userId,
                identifier, inputMethodInfo, inputMethodSubtype, layout);
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

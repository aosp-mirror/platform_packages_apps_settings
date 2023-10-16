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
import android.os.Bundle;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

public class NewKeyboardLayoutPickerContent extends DashboardFragment {

    private static final String TAG = "KeyboardLayoutPicker";

    private InputManager mIm;
    private int mUserId;
    private InputDeviceIdentifier mIdentifier;
    private InputMethodInfo mInputMethodInfo;
    private InputMethodSubtype mInputMethodSubtype;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mIm = getContext().getSystemService(InputManager.class);
        Bundle arguments = getArguments();
        final CharSequence title = arguments.getCharSequence(NewKeyboardSettingsUtils.EXTRA_TITLE);
        mUserId = arguments.getInt(NewKeyboardSettingsUtils.EXTRA_USER_ID);
        mIdentifier =
                arguments.getParcelable(NewKeyboardSettingsUtils.EXTRA_INPUT_DEVICE_IDENTIFIER);
        mInputMethodInfo =
                arguments.getParcelable(NewKeyboardSettingsUtils.EXTRA_INPUT_METHOD_INFO);
        mInputMethodSubtype =
                arguments.getParcelable(NewKeyboardSettingsUtils.EXTRA_INPUT_METHOD_SUBTYPE);
        if (mIdentifier == null
                || NewKeyboardSettingsUtils.getInputDevice(mIm, mIdentifier) == null) {
            getActivity().finish();
            return;
        }
        getActivity().setTitle(title);
        use(NewKeyboardLayoutPickerController.class).initialize(this /*parent*/, mUserId,
                mIdentifier, mInputMethodInfo, mInputMethodSubtype, getSelectedLayoutLabel());
    }

    private String getSelectedLayoutLabel() {
        String label = getContext().getString(R.string.keyboard_default_layout);
        String layout = NewKeyboardSettingsUtils.getKeyboardLayout(
                mIm, mUserId, mIdentifier, mInputMethodInfo, mInputMethodSubtype);
        KeyboardLayout[] keyboardLayouts = NewKeyboardSettingsUtils.getKeyboardLayouts(
                mIm, mUserId, mIdentifier, mInputMethodInfo, mInputMethodSubtype);
        if (layout != null) {
            for (int i = 0; i < keyboardLayouts.length; i++) {
                if (keyboardLayouts[i].getDescriptor().equals(layout)) {
                    label = keyboardLayouts[i].getLabel();
                    break;
                }
            }
        }
        return label;
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

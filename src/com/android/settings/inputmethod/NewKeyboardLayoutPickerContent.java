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

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

public class NewKeyboardLayoutPickerContent extends DashboardFragment {

    private static final String TAG = "KeyboardLayoutPicker";
    private NewKeyboardLayoutPickerController mNewKeyboardLayoutPickerController;
    private ControllerUpdateCallback mControllerUpdateCallback;

    public interface ControllerUpdateCallback {
        /**
         * Called when mNewKeyBoardLayoutPickerController been initialized.
         */
        void onControllerUpdated(NewKeyboardLayoutPickerController
                newKeyboardLayoutPickerController);
    }

    public void setControllerUpdateCallback(ControllerUpdateCallback controllerUpdateCallback) {
        this.mControllerUpdateCallback = controllerUpdateCallback;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        InputManager im = getContext().getSystemService(InputManager.class);
        InputDeviceIdentifier identifier =
                getArguments().getParcelable(
                        NewKeyboardSettingsUtils.EXTRA_INPUT_DEVICE_IDENTIFIER);
        if (identifier == null
                || NewKeyboardSettingsUtils.getInputDevice(im, identifier) == null) {
            getActivity().finish();
            return;
        }
        mNewKeyboardLayoutPickerController = use(NewKeyboardLayoutPickerController.class);
        mNewKeyboardLayoutPickerController.initialize(this);
        if (mControllerUpdateCallback != null) {
            mControllerUpdateCallback.onControllerUpdated(mNewKeyboardLayoutPickerController);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_KEYBOARDS_LAYOUT_PICKER;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    protected int getPreferenceScreenResId() {
        return R.xml.new_keyboard_layout_picker_fragment;
    }

    public NewKeyboardLayoutPickerController getController() {
        return mNewKeyboardLayoutPickerController;
    }
}

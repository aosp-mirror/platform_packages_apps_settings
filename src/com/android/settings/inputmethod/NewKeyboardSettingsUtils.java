/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities of keyboard settings
 */
public class NewKeyboardSettingsUtils {

    static final String EXTRA_KEYBOARD_DEVICE_NAME = "extra_keyboard_device_name";
    static final String EXTRA_TITLE = "keyboard_layout_picker_title";
    static final String EXTRA_KEYBOARD_LAYOUT = "keyboard_layout";
    static final String EXTRA_USER_ID = "user_id";
    static final String EXTRA_INPUT_DEVICE_IDENTIFIER = "input_device_identifier";
    static final String EXTRA_INPUT_METHOD_INFO = "input_method_info";
    static final String EXTRA_INPUT_METHOD_SUBTYPE = "input_method_subtype";

    static boolean isTouchpad() {
        for (int deviceId : InputDevice.getDeviceIds()) {
            final InputDevice device = InputDevice.getDevice(deviceId);
            if (device == null) {
                continue;
            }
            if ((device.getSources() & InputDevice.SOURCE_TOUCHPAD)
                    == InputDevice.SOURCE_TOUCHPAD) {
                return true;
            }
        }
        return false;
    }

    static List<String> getSuitableImeLabels(Context context, InputMethodManager imm, int userId) {
        List<String> suitableInputMethodInfoLabels = new ArrayList<>();
        List<InputMethodInfo> infoList = imm.getEnabledInputMethodListAsUser(userId);
        for (InputMethodInfo info : infoList) {
            List<InputMethodSubtype> subtypes =
                    imm.getEnabledInputMethodSubtypeList(info, true);
            for (InputMethodSubtype subtype : subtypes) {
                if (subtype.isSuitableForPhysicalKeyboardLayoutMapping()) {
                    suitableInputMethodInfoLabels.add(
                            info.loadLabel(context.getPackageManager()).toString());
                    break;
                }
            }
        }
        return suitableInputMethodInfoLabels;
    }

    static class KeyboardInfo {
        String mLanguage;
        String mLayout;
        InputMethodInfo mInputMethodInfo;
        InputMethodSubtype mInputMethodSubtype;

        KeyboardInfo(
                String language,
                String layout,
                InputMethodInfo inputMethodInfo,
                InputMethodSubtype inputMethodSubtype) {
            mLanguage = language;
            mLayout = layout;
            mInputMethodInfo = inputMethodInfo;
            mInputMethodSubtype = inputMethodSubtype;
        }

        String getLanguage() {
            return mLanguage;
        }

        String getLayout() {
            return mLayout;
        }

        InputMethodInfo getInputMethodInfo() {
            return mInputMethodInfo;
        }

        InputMethodSubtype getInputMethodSubtype() {
            return mInputMethodSubtype;
        }
    }
}

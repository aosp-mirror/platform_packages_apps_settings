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
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.UserHandle;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Utilities of keyboard settings
 */
public class NewKeyboardSettingsUtils {

    static final String EXTRA_TITLE = "keyboard_layout_picker_title";
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
        List<InputMethodInfo> infoList = imm.getEnabledInputMethodListAsUser(UserHandle.of(userId));
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
        CharSequence mSubtypeLabel;
        String mLayout;
        InputMethodInfo mInputMethodInfo;
        InputMethodSubtype mInputMethodSubtype;

        KeyboardInfo(
                CharSequence subtypeLabel,
                String layout,
                InputMethodInfo inputMethodInfo,
                InputMethodSubtype inputMethodSubtype) {
            mSubtypeLabel = subtypeLabel;
            mLayout = layout;
            mInputMethodInfo = inputMethodInfo;
            mInputMethodSubtype = inputMethodSubtype;
        }

        String getPrefId() {
            return mInputMethodInfo.getId() + "_" + mInputMethodSubtype.hashCode();
        }

        CharSequence getSubtypeLabel() {
            return mSubtypeLabel;
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

    static InputDevice getInputDevice(InputManager im, InputDeviceIdentifier identifier) {
        return identifier == null ? null : im.getInputDeviceByDescriptor(
                identifier.getDescriptor());
    }

    static KeyboardLayout[] getKeyboardLayouts(InputManager inputManager, int userId,
            InputDeviceIdentifier identifier, InputMethodInfo info, InputMethodSubtype subtype) {
        return inputManager.getKeyboardLayoutListForInputDevice(identifier, userId, info, subtype);
    }

    static String getKeyboardLayout(InputManager inputManager, int userId,
            InputDeviceIdentifier identifier, InputMethodInfo info, InputMethodSubtype subtype) {
        return inputManager.getKeyboardLayoutForInputDevice(identifier, userId, info, subtype);
    }

    static void sortKeyboardLayoutsByLabel(KeyboardLayout[] keyboardLayouts) {
        Arrays.sort(
                keyboardLayouts,
                Comparator.comparing(KeyboardLayout::getLabel)
        );
    }
}

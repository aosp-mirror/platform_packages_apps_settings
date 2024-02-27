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

import static android.hardware.input.KeyboardLayoutSelectionResult.LAYOUT_SELECTION_CRITERIA_USER;
import static android.hardware.input.KeyboardLayoutSelectionResult.LAYOUT_SELECTION_CRITERIA_DEVICE;
import static android.hardware.input.KeyboardLayoutSelectionResult.LAYOUT_SELECTION_CRITERIA_VIRTUAL_KEYBOARD;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.annotation.UserIdInt;
import android.content.Context;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.hardware.input.KeyboardLayoutSelectionResult;
import android.hardware.input.KeyboardLayoutSelectionResult.LayoutSelectionCriteria;
import android.os.UserHandle;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.android.settings.R;

import java.util.Arrays;
import java.util.Comparator;

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

    @SuppressLint("MissingPermission")
    @Nullable
    static String getSelectedKeyboardLayoutLabelForUser(Context context, @UserIdInt int userId,
            InputDeviceIdentifier inputDeviceIdentifier) {
        InputMethodManager imm = context.getSystemService(InputMethodManager.class);
        InputManager im = context.getSystemService(InputManager.class);
        if (imm == null || im == null) {
            return null;
        }
        InputMethodInfo imeInfo = imm.getCurrentInputMethodInfoAsUser(UserHandle.of(userId));
        InputMethodSubtype subtype = imm.getCurrentInputMethodSubtype();
        KeyboardLayout[] keyboardLayouts = getKeyboardLayouts(im, userId, inputDeviceIdentifier,
                imeInfo, subtype);
        KeyboardLayoutSelectionResult result = getKeyboardLayout(im, userId, inputDeviceIdentifier,
                imeInfo, subtype);
        if (result != null) {
            for (KeyboardLayout keyboardLayout : keyboardLayouts) {
                if (keyboardLayout.getDescriptor().equals(result.getLayoutDescriptor())) {
                    return keyboardLayout.getLabel();
                }
            }
        }
        return null;
    }

    static class KeyboardInfo {
        CharSequence mSubtypeLabel;
        String mLayout;
        @LayoutSelectionCriteria int mSelectionCriteria;
        InputMethodInfo mInputMethodInfo;
        InputMethodSubtype mInputMethodSubtype;

        KeyboardInfo(
                CharSequence subtypeLabel,
                String layout,
                @LayoutSelectionCriteria int selectionCriteria,
                InputMethodInfo inputMethodInfo,
                InputMethodSubtype inputMethodSubtype) {
            mSubtypeLabel = subtypeLabel;
            mLayout = layout;
            mSelectionCriteria = selectionCriteria;
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

        String getLayoutSummaryText(Context context) {
            if (isAutomaticSelection(mSelectionCriteria)) {
                return context.getResources().getString(R.string.automatic_keyboard_layout_label,
                        mLayout);
            } else if (isUserSelection(mSelectionCriteria)) {
                return context.getResources().getString(
                        R.string.user_selected_keyboard_layout_label, mLayout);
            }
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

    @NonNull
    static KeyboardLayoutSelectionResult getKeyboardLayout(InputManager inputManager, int userId,
            InputDeviceIdentifier identifier, InputMethodInfo info, InputMethodSubtype subtype) {
        return inputManager.getKeyboardLayoutForInputDevice(identifier, userId, info, subtype);
    }

    static boolean isAutomaticSelection(@LayoutSelectionCriteria int criteria) {
        return criteria == LAYOUT_SELECTION_CRITERIA_DEVICE
                || criteria == LAYOUT_SELECTION_CRITERIA_VIRTUAL_KEYBOARD;
    }

    static boolean isUserSelection(@LayoutSelectionCriteria int criteria) {
        return criteria == LAYOUT_SELECTION_CRITERIA_USER;
    }

    static void sortKeyboardLayoutsByLabel(KeyboardLayout[] keyboardLayouts) {
        Arrays.sort(
                keyboardLayouts,
                Comparator.comparing(KeyboardLayout::getLabel)
        );
    }
}

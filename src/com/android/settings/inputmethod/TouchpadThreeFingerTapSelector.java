/*
 * Copyright 2024 The Android Open Source Project
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

import static android.hardware.input.InputGestureData.TOUCHPAD_GESTURE_TYPE_THREE_FINGER_TAP;
import static android.hardware.input.InputGestureData.createTouchpadTrigger;

import android.content.Context;
import android.hardware.input.InputGestureData;
import android.hardware.input.InputManager;
import android.hardware.input.KeyGestureEvent;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.PointerIcon;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class TouchpadThreeFingerTapSelector extends Preference {
    private static final InputGestureData.Trigger THREE_FINGER_TAP_TOUCHPAD_TRIGGER =
            createTouchpadTrigger(TOUCHPAD_GESTURE_TYPE_THREE_FINGER_TAP);
    private final InputManager mInputManager;

    public TouchpadThreeFingerTapSelector(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.touchpad_three_finger_tap_layout);
        mInputManager = context.getSystemService(InputManager.class);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        LinearLayout buttonHolder = (LinearLayout) holder.findViewById(R.id.button_holder);
        // Intercept hover events so setting row does not highlight when hovering buttons.
        buttonHolder.setOnHoverListener((v, e) -> true);

        int currentCustomization = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.TOUCHPAD_THREE_FINGER_TAP_CUSTOMIZATION,
                KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED, UserHandle.USER_CURRENT);
        initRadioButton(holder, R.id.launch_gemini,
                KeyGestureEvent.KEY_GESTURE_TYPE_LAUNCH_ASSISTANT, currentCustomization);
        initRadioButton(holder, R.id.go_home, KeyGestureEvent.KEY_GESTURE_TYPE_HOME,
                currentCustomization);
        initRadioButton(holder, R.id.go_back, KeyGestureEvent.KEY_GESTURE_TYPE_BACK,
                currentCustomization);
        initRadioButton(holder, R.id.recent_apps, KeyGestureEvent.KEY_GESTURE_TYPE_RECENT_APPS,
                currentCustomization);
        initRadioButton(holder, R.id.middle_click,
                KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED, currentCustomization);
    }

    private void initRadioButton(@NonNull PreferenceViewHolder holder, int id,
            int customGestureType, int currentCustomization) {
        RadioButton radioButton = (RadioButton) holder.findViewById(id);
        if (radioButton == null) {
            return;
        }
        boolean isUnspecified = customGestureType == KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED;
        InputGestureData gesture = isUnspecified ? null : new InputGestureData.Builder()
                .setTrigger(THREE_FINGER_TAP_TOUCHPAD_TRIGGER)
                .setKeyGestureType(customGestureType)
                .build();
        radioButton.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                mInputManager.removeAllCustomInputGestures(InputGestureData.Filter.TOUCHPAD);
                if (!isUnspecified) {
                    mInputManager.addCustomInputGesture(gesture);
                }
                Settings.System.putIntForUser(getContext().getContentResolver(),
                        Settings.System.TOUCHPAD_THREE_FINGER_TAP_CUSTOMIZATION, customGestureType,
                        UserHandle.USER_CURRENT);
            }
        });
        radioButton.setChecked(currentCustomization == customGestureType);
        radioButton.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_ARROW));
    }
}

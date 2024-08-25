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

import static com.android.systemui.shared.Flags.newTouchpadGesturesTutorial;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.FeatureFlagUtils;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.ButtonPreference;

public class TouchGesturesButtonPreferenceController extends BasePreferenceController {

    private static final int ORDER_TOP = 0;
    private static final int ORDER_BOTTOM = 100;
    private static final String PREFERENCE_KEY = "trackpad_touch_gesture";
    private static final String GESTURE_DIALOG_TAG = "GESTURE_DIALOG_TAG";
    private static final String TUTORIAL_ACTION = "com.android.systemui.action.TOUCHPAD_TUTORIAL";

    private Fragment mParent;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    public TouchGesturesButtonPreferenceController(Context context, String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    public void setFragment(Fragment parent) {
        mParent = parent;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        ButtonPreference buttonPreference =
                (ButtonPreference) screen.findPreference(getPreferenceKey());
        boolean touchGestureDeveloperMode = FeatureFlagUtils
                .isEnabled(mContext, FeatureFlagUtils.SETTINGS_NEW_KEYBOARD_TRACKPAD_GESTURE);
        if (getPreferenceKey().equals(PREFERENCE_KEY)) {
            if (touchGestureDeveloperMode) {
                buttonPreference.setOrder(ORDER_TOP);
            } else {
                buttonPreference.setOrder(ORDER_BOTTOM);
            }
        }
        buttonPreference.setOnClickListener(v -> {
            showTouchpadGestureEducation();
        });
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isTouchpad = NewKeyboardSettingsUtils.isTouchpad();
        return isTouchpad ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    private void showTouchpadGestureEducation() {
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_LEARN_TOUCHPAD_GESTURE_CLICK);
        if (newTouchpadGesturesTutorial()) {
            Intent intent = new Intent(TUTORIAL_ACTION)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setPackage(Utils.SYSTEMUI_PACKAGE_NAME);
            // touchpad tutorial must be started as system user as it needs to have access to state
            // of user 0 sysui instance
            mContext.startActivityAsUser(intent, UserHandle.SYSTEM);
        } else {
            TrackpadGestureDialogFragment fragment = new TrackpadGestureDialogFragment();
            fragment.setTargetFragment(mParent, 0);
            fragment.show(mParent.getActivity().getSupportFragmentManager(), GESTURE_DIALOG_TAG);
        }
    }
}

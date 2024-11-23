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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.input.InputSettings;
import android.hardware.input.KeyGestureEvent;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.Map;

public class TouchpadThreeFingerTapPreferenceController extends BasePreferenceController
        implements LifecycleEventObserver {

    private final Map<Integer, String> mKeyGestureTypeNameMap;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private @Nullable Preference mPreference;

    public TouchpadThreeFingerTapPreferenceController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();

        mKeyGestureTypeNameMap = Map.ofEntries(
                Map.entry(KeyGestureEvent.KEY_GESTURE_TYPE_LAUNCH_ASSISTANT,
                        context.getString(R.string.three_finger_tap_launch_gemini)),
                Map.entry(KeyGestureEvent.KEY_GESTURE_TYPE_HOME,
                        context.getString(R.string.three_finger_tap_go_home)),
                Map.entry(KeyGestureEvent.KEY_GESTURE_TYPE_BACK,
                        context.getString(R.string.three_finger_tap_go_back)),
                Map.entry(KeyGestureEvent.KEY_GESTURE_TYPE_RECENT_APPS,
                        context.getString(R.string.three_finger_tap_recent_apps)),
                Map.entry(KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED,
                        context.getString(R.string.three_finger_tap_middle_click)));
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isTouchpad = InputPeripheralsSettingsUtils.isTouchpad();
        return (InputSettings.isTouchpadThreeFingerTapShortcutFeatureFlagEnabled() && isTouchpad)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    @Override
    public @Nullable CharSequence getSummary() {
        int currentType = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.TOUCHPAD_THREE_FINGER_TAP_CUSTOMIZATION,
                KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED, UserHandle.USER_CURRENT);
        return mKeyGestureTypeNameMap.get(currentType);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        refreshSummary(mPreference);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        refreshSummary(mPreference);
        if (event == Lifecycle.Event.ON_PAUSE) {
            int currentValue =
                    Settings.System.getIntForUser(mContext.getContentResolver(),
                            Settings.System.TOUCHPAD_THREE_FINGER_TAP_CUSTOMIZATION,
                            KeyGestureEvent.KEY_GESTURE_TYPE_UNSPECIFIED, UserHandle.USER_CURRENT);
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_TOUCHPAD_THREE_FINGER_TAP_CUSTOMIZATION_CHANGED,
                    currentValue);
        }
    }
}

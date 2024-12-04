/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.gestures;

import static com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_BUTTON_GESTURE_TARGET_ACTION_URI;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.IllustrationPreference;

/** Configures the behaviour of the double tap power illustration. */
public class DoubleTapPowerIllustrationPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    @Nullable
    private IllustrationPreference mIllustrationPreference;
    private final ContentObserver mSettingsObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    if (mIllustrationPreference != null && uri != null) {
                        updateState(mIllustrationPreference);
                    }
                }
            };

    public DoubleTapPowerIllustrationPreferenceController(
            @NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mIllustrationPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);

        ((IllustrationPreference) preference)
                .setLottieAnimationResId(
                        DoubleTapPowerSettingsUtils
                                .isDoubleTapPowerButtonGestureForCameraLaunchEnabled(
                                        mContext)
                                ? R.drawable.quickly_open_camera
                                : R.drawable.double_tap_power_for_wallet);
    }

    @Override
    public void onStart() {
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(
                DOUBLE_TAP_POWER_BUTTON_GESTURE_TARGET_ACTION_URI, true, mSettingsObserver);
    }

    @Override
    public void onStop() {
        DoubleTapPowerSettingsUtils.unregisterObserver(mContext, mSettingsObserver);
    }
}

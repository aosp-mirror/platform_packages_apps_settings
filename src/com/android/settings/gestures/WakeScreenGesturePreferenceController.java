/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.provider.Settings.Secure.DOZE_WAKE_SCREEN_GESTURE;

import android.annotation.UserIdInt;
import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.aware.AwareFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

public class WakeScreenGesturePreferenceController extends GesturePreferenceController {

    private static final int ON = 1;
    private static final int OFF = 0;

    private static final String PREF_KEY_VIDEO = "gesture_wake_screen_video";

    private final AwareFeatureProvider mFeatureProvider;
    private AmbientDisplayConfiguration mAmbientConfig;
    @UserIdInt
    private final int mUserId;

    public WakeScreenGesturePreferenceController(Context context, String key) {
        super(context, key);
        mUserId = UserHandle.myUserId();
        mFeatureProvider = FeatureFactory.getFactory(context).getAwareFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        if (!getAmbientConfig().wakeScreenGestureAvailable()
                || !mFeatureProvider.isSupported(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        if (!mFeatureProvider.isEnabled(mContext)) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        return getAmbientConfig().alwaysOnEnabled(mUserId)
                ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    protected boolean canHandleClicks() {
        return getAmbientConfig().alwaysOnEnabled(mUserId);
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_wake_screen");
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isChecked() {
        return getAmbientConfig().wakeScreenGestureEnabled(mUserId);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), DOZE_WAKE_SCREEN_GESTURE,
                isChecked ? ON : OFF);
    }

    private AmbientDisplayConfiguration getAmbientConfig() {
        if (mAmbientConfig == null) {
            mAmbientConfig = new AmbientDisplayConfiguration(mContext);
        }

        return mAmbientConfig;
    }

    @VisibleForTesting
    public void setConfig(AmbientDisplayConfiguration config) {
        mAmbientConfig = config;
    }
}
